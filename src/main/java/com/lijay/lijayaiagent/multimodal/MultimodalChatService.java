package com.lijay.lijayaiagent.multimodal;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.Constants;
import com.lijay.lijayaiagent.chatmemory.JdbcChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 多模态对话服务
 * 基于阿里云DashScope SDK实现图文对话功能
 * 支持与现有ChatMemory记忆机制集成
 */
@Slf4j
@Service
public class MultimodalChatService {

    @Value("${spring.ai.dashscope.base-url:https://dashscope.aliyuncs.com/api/v1}")
    private String baseUrl;

    @Value("${spring.ai.dashscope.api-key:${DASHSCOPE_API_KEY:}}")
    private String apiKey;

    @Autowired
    private ChatMemory chatMemory; // 当前项目默认注入JdbcChatMemory

    private MultiModalConversation multiModalConversation;

    @PostConstruct
    public void init() {
        // 配置DashScope API地址
        Constants.baseHttpApiUrl = baseUrl;
        multiModalConversation = new MultiModalConversation();
        log.info("多模态对话服务初始化完成，baseUrl: {}", baseUrl);
    }

    /**
     * 执行多模态对话（同步）
     *
     * @param request 多模态对话请求
     * @return AI回复文本
     */
    public String chat(MultimodalChatRequest request) {
        String chatId = request.getChatId();
        
        try {
            // 1. 获取历史对话记忆
            List<Message> historyMessages = chatMemory.get(chatId);
            
            // 2. 构建多模态消息列表
            List<MultiModalMessage> multimodalMessages = convertToMultimodalMessages(historyMessages);
            
            // 3. 添加当前用户消息（包含图片）
            MultiModalMessage currentMessage = buildCurrentUserMessage(request);
            multimodalMessages.add(currentMessage);
            
            // 4. 调用DashScope多模态API
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(getApiKey())
                    .model(request.getModel())
                    .messages(multimodalMessages)
                    .build();
            
            log.info("[多模态对话] chatId: {}, model: {}, 历史消息数: {}", 
                    chatId, request.getModel(), historyMessages.size());
            
            MultiModalConversationResult result = multiModalConversation.call(param);
            
            // 5. 解析响应
            String responseText = extractResponseText(result);
            
            // 6. 更新对话记忆
            updateChatMemory(chatId, request, responseText);
            
            log.info("[多模态对话] 响应成功, chatId: {}", chatId);
            return responseText;
            
        } catch (ApiException | NoApiKeyException | UploadFileException e) {
            log.error("[多模态对话] 调用失败, chatId: {}, error: {}", chatId, e.getMessage(), e);
            throw new RuntimeException("多模态对话调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行多模态对话（流式）
     *
     * @param request 多模态对话请求
     * @return 流式响应
     */
    public Flux<String> chatStream(MultimodalChatRequest request) {
        String chatId = request.getChatId();
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        
        // 使用异步方式处理流式响应
        new Thread(() -> {
            try {
                // 1. 获取历史对话记忆
                List<Message> historyMessages = chatMemory.get(chatId);
                
                // 2. 构建多模态消息列表
                List<MultiModalMessage> multimodalMessages = convertToMultimodalMessages(historyMessages);
                
                // 3. 添加当前用户消息
                MultiModalMessage currentMessage = buildCurrentUserMessage(request);
                multimodalMessages.add(currentMessage);
                
                // 4. 调用流式API
                MultiModalConversationParam param = MultiModalConversationParam.builder()
                        .apiKey(getApiKey())
                        .model(request.getModel())
                        .messages(multimodalMessages)
                        .build();
                
                log.info("[多模态流式对话] chatId: {}, model: {}", chatId, request.getModel());
                
                StringBuilder fullResponse = new StringBuilder();
                
                multiModalConversation.streamCall(param)
                        .subscribe(
                                result -> {
                                    String text = extractResponseText(result);
                                    if (text != null && !text.isEmpty()) {
                                        sink.tryEmitNext(text);
                                        fullResponse.append(text);
                                    }
                                },
                                error -> {
                                    log.error("[多模态流式对话] 错误, chatId: {}", chatId, error);
                                    sink.tryEmitError(error);
                                },
                                () -> {
                                    // 流结束，更新记忆
                                    sink.tryEmitComplete();
                                    updateChatMemory(chatId, request, fullResponse.toString());
                                    log.info("[多模态流式对话] 完成, chatId: {}", chatId);
                                }
                        );
                        
            } catch (Exception e) {
                log.error("[多模态流式对话] 调用失败, chatId: {}", chatId, e);
                sink.tryEmitError(e);
            }
        }).start();
        
        return sink.asFlux();
    }

    /**
     * 将Spring AI Message转换为DashScope MultiModalMessage
     */
    private List<MultiModalMessage> convertToMultimodalMessages(List<Message> messages) {
        List<MultiModalMessage> result = new ArrayList<>();
        
        for (Message message : messages) {
            String role = convertRole(message.getMessageType().name());
            String content = message.getText();
            
            MultiModalMessage mmMessage = MultiModalMessage.builder()
                    .role(role)
                    .content(Collections.singletonList(Collections.singletonMap("text", content)))
                    .build();
            
            result.add(mmMessage);
        }
        
        return result;
    }

    /**
     * 构建当前用户的多模态消息
     */
    private MultiModalMessage buildCurrentUserMessage(MultimodalChatRequest request) {
        List<Map<String, Object>> contents = new ArrayList<>();
        
        // 添加图片内容
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (String image : request.getImages()) {
                // 处理本地文件路径
                String processedImage = processImage(image);
                contents.add(Collections.singletonMap("image", processedImage));
            }
        }
        
        // 添加文本内容
        if (request.getText() != null && !request.getText().isEmpty()) {
            contents.add(Collections.singletonMap("text", request.getText()));
        }
        
        return MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(contents)
                .build();
    }

    /**
     * 处理图片路径，支持本地文件自动转base64
     */
    private String processImage(String imageInput) {
        // 如果是URL或已经是base64格式，直接返回
        if (imageInput.startsWith("http://") || 
            imageInput.startsWith("https://") ||
            imageInput.startsWith("data:image")) {
            return imageInput;
        }
        
        // 如果是本地文件路径，转换为base64
        try {
            Path path = Paths.get(imageInput);
            if (Files.exists(path)) {
                byte[] fileContent = Files.readAllBytes(path);
                String base64 = Base64.getEncoder().encodeToString(fileContent);
                // 检测文件类型
                String mimeType = detectMimeType(path);
                return "data:" + mimeType + ";base64," + base64;
            }
        } catch (IOException e) {
            log.warn("处理本地图片失败: {}, 将原样使用", imageInput, e);
        }
        
        return imageInput;
    }

    /**
     * 检测文件MIME类型
     */
    private String detectMimeType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg"; // 默认
    }

    /**
     * 提取响应文本
     */
    private String extractResponseText(MultiModalConversationResult result) {
        if (result == null || result.getOutput() == null || 
            result.getOutput().getChoices() == null || 
            result.getOutput().getChoices().isEmpty()) {
            return "";
        }
        
        Object content = result.getOutput().getChoices().get(0).getMessage().getContent();
        
        // DashScope返回的内容可能是List<Map>或String
        if (content instanceof List) {
            List<?> contentList = (List<?>) content;
            if (!contentList.isEmpty() && contentList.get(0) instanceof Map) {
                Map<?, ?> contentMap = (Map<?, ?>) contentList.get(0);
                Object text = contentMap.get("text");
                return text != null ? text.toString() : "";
            }
        }
        
        return content != null ? content.toString() : "";
    }

    /**
     * 更新对话记忆
     */
    private void updateChatMemory(String chatId, MultimodalChatRequest request, String responseText) {
        try {
            // 构建用户消息文本（包含图片描述）
            StringBuilder userText = new StringBuilder();
            if (request.getText() != null) {
                userText.append(request.getText());
            }
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                userText.append(" [包含 ").append(request.getImages().size()).append(" 张图片]");
            }
            
            // 添加到记忆
            chatMemory.add(chatId, List.of(
                    new UserMessage(userText.toString()),
                    new AssistantMessage(responseText)
            ));
        } catch (Exception e) {
            log.warn("更新对话记忆失败, chatId: {}", chatId, e);
        }
    }

    /**
     * 转换角色名称
     */
    private String convertRole(String messageType) {
        return switch (messageType.toUpperCase()) {
            case "USER" -> Role.USER.getValue();
            case "ASSISTANT" -> Role.ASSISTANT.getValue();
            case "SYSTEM" -> Role.SYSTEM.getValue();
            default -> Role.USER.getValue();
        };
    }

    /**
     * 获取API Key
     */
    private String getApiKey() {
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }
        String envKey = System.getenv("DASHSCOPE_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            return envKey;
        }
        throw new RuntimeException("DashScope API Key未配置，请设置环境变量DASHSCOPE_API_KEY或在配置文件中指定");
    }
}
