package com.lijay.lijayaiagent.app;

import com.lijay.lijayaiagent.advisor.MyLoggerAdvisor;
import com.lijay.lijayaiagent.multimodal.MultimodalChatRequest;
import com.lijay.lijayaiagent.multimodal.MultimodalChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;
    private final MultimodalChatService multimodalChatService;

    private static final String SYSTEM_PROMPT = "你的名字是小雨，扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";
    /*
        使用@Resource注解是为了优先依据名称进行注入,
        @Autowired则是优先根据类型注入（可以自定义名称，同时Spring 会先按类型匹配，如果找到多个，会按变量名匹配 Bean 名称。)
    */
    @Autowired
    private VectorStore appVectorStore;

    @Autowired
    private Advisor appRagCloudAdvisor;


    public LoveApp(ChatModel dashscopeChatModel, MultimodalChatService multimodalChatService, ChatMemory chatMemory) {
        // 初始化基于MyBatis-Plus的JDBC对话记忆（推荐用于生产环境）

        // 初始化基于文件的对话记忆（适合开发测试）
//        String fileDir = System.getProperty("user.dir") + "/tmp/file-based-chat-memory";
//        chatMemory = new FileBasedChatMemory(fileDir);

        // 初始化基于内存的对话记忆（简单快速，但重启后数据丢失）
//        ChatMemory chatMemory = MessageWindowChatMemory.builder()
//                .chatMemoryRepository(new InMemoryChatMemoryRepository())
//                .maxMessages(10)
//                .build();

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志 Advisor，输出简答对话
                        new MyLoggerAdvisor()
                        // 自定义推理增强 Advisor，可按需开启
//                        new ReReadingAdvisor()
                )
                .build();

        this.multimodalChatService = multimodalChatService;
    }

    /**
     * Ai 基础对话(支持多轮对话记忆)
     */
    public String doChat(String message, String chatId) {
        // 同步阻塞调用
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();

        // 更详细的调试信息
//        log.info("[DashScope] 响应内容：{}", content);
        return chatResponse.getResult().getOutput().getText();
    }

    /**
     * Ai 流式对话(支持多轮对话记忆)
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    record LoveReport(String title, List<String> suggestions) {

    }

    /**
     * Ai 报告功能(支持结构化输出)
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        // 同步阻塞调用
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成报告结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);

        // 更详细的调试信息
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    // ==================== 多模态对话功能 ====================

    /**
     * 多模态对话 - 同步调用（支持文本+图片）
     *
     * @param text   用户输入的文本
     * @param images 图片列表（URL或Base64编码）
     * @param chatId 对话ID，用于记忆管理
     * @return AI回复文本
     */
    public String doMultimodalChat(String text, List<String> images, String chatId) {
        MultimodalChatRequest request = MultimodalChatRequest.builder()
                .chatId(chatId)
                .text(text)
                .images(images)
                .model("qwen-vl-plus")
                .build();

        log.info("[多模态对话] chatId: {}, 文本长度: {}, 图片数: {}",
                chatId, text != null ? text.length() : 0, images != null ? images.size() : 0);

        return multimodalChatService.chat(request);
    }

    /**
     * 多模态对话 - 同步调用（仅文本）
     *
     * @param text   用户输入的文本
     * @param chatId 对话ID，用于记忆管理
     * @return AI回复文本
     */
    public String doMultimodalChat(String text, String chatId) {
        return doMultimodalChat(text, null, chatId);
    }

    /**
     * 多模态对话 - 流式调用（支持文本+图片）
     *
     * @param text   用户输入的文本
     * @param images 图片列表（URL或Base64编码）
     * @param chatId 对话ID，用于记忆管理
     * @return 流式响应
     */
    public Flux<String> doMultimodalChatStream(String text, List<String> images, String chatId) {
        MultimodalChatRequest request = MultimodalChatRequest.builder()
                .chatId(chatId)
                .text(text)
                .images(images)
                .model("qwen-vl-plus")
                .stream(true)
                .build();

        log.info("[多模态流式对话] chatId: {}, 文本长度: {}, 图片数: {}",
                chatId, text != null ? text.length() : 0, images != null ? images.size() : 0);

        return multimodalChatService.chatStream(request);
    }

    /**
     * 多模态对话 - 流式调用（仅文本）
     *
     * @param text   用户输入的文本
     * @param chatId 对话ID，用于记忆管理
     * @return 流式响应
     */
    public Flux<String> doMultimodalChatStream(String text, String chatId) {
        return doMultimodalChatStream(text, null, chatId);
    }

    /**
     * 根据本地RAG知识库回答
     */

    public String doChatWithRag(String message, String chatId) {
        // 构建 RAG Advisor(检索增强顾问)
        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.30)
                        .vectorStore(appVectorStore)
                        .build())
                .build();

        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(retrievalAugmentationAdvisor)
                .call()
                .content();
    }

    /**
     * 根据云知识库RAG回答

     * @return
     */

    public String doChatWithCloudRag(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(appRagCloudAdvisor)
                .call()
                .content();
    }

}