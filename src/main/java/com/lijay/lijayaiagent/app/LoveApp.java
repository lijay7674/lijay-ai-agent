package com.lijay.lijayaiagent.app;

import com.lijay.lijayaiagent.advisor.MyLoggerAdvisor;
import com.lijay.lijayaiagent.advisor.ReReadingAdvisor;
import com.lijay.lijayaiagent.chatmemory.FileBasedChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "你的名字是小雨，扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";
    /**
     * Ai 初始化，Spring会自动执行构造方法，并将bean注入到该类中
     */
    public LoveApp(ChatModel dashscopeChatModel) {
        //初始化一个基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/tmp/file-based-chat-memory";
        FileBasedChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        // 初始化基于内存的对话记忆
//        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
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
        String content = chatResponse.getResult().getOutput().getText();

        // 更详细的调试信息
//        log.info("[DashScope] 响应内容：{}", content);
        return content;
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


}
