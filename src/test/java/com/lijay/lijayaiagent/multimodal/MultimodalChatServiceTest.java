package com.lijay.lijayaiagent.multimodal;

import com.lijay.lijayaiagent.app.LoveApp;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 多模态对话服务测试类
 * 测试多模态对话功能和记忆机制
 */
@Slf4j
@SpringBootTest
class MultimodalChatServiceTest {

    @Autowired
    private MultimodalChatService multimodalChatService;

    @Autowired
    private LoveApp loveApp;

    /**
     * 测试纯文本多模态对话
     */
    @Test
    void testTextOnlyMultimodalChat() {
        String chatId = UUID.randomUUID().toString();
        
        MultimodalChatRequest request = MultimodalChatRequest.builder()
                .chatId(chatId)
                .text("你好，请介绍一下你自己")
                .model("qwen-vl-plus")
                .build();

        String response = multimodalChatService.chat(request);
        
        log.info("纯文本多模态对话响应: {}", response);
        Assertions.assertNotNull(response);
        Assertions.assertFalse(response.isEmpty());
    }

    /**
     * 测试多模态对话记忆功能 - 多轮对话
     */
    @Test
    void testMultimodalChatMemory() {
        String chatId = UUID.randomUUID().toString();

        // 第一轮：自我介绍
        String response1 = loveApp.doMultimodalChat("你好，我是程序员lijay，我想咨询恋爱问题", chatId);
        log.info("第一轮响应: {}", response1);
        Assertions.assertNotNull(response1);

        // 第二轮：询问记忆
        String response2 = loveApp.doMultimodalChat("你还记得我是谁吗？我是什么职业？", chatId);
        log.info("第二轮响应: {}", response2);
        Assertions.assertNotNull(response2);
        Assertions.assertTrue(
                response2.toLowerCase().contains("程序") || 
                response2.toLowerCase().contains("李"),
                "AI应该记得用户的职业和姓名"
        );

        // 第三轮：继续对话
        String response3 = loveApp.doMultimodalChat("我想知道如何让另一半更爱我", chatId);
        log.info("第三轮响应: {}", response3);
        Assertions.assertNotNull(response3);
    }

    /**
     * 测试带图片的多模态对话（使用网络图片URL）
     * 注意：此测试需要有效的网络连接和可用的图片URL
     */
    @Test
    void testMultimodalChatWithImageUrl() {
        // 使用阿里云文档中的示例图片
        String imageUrl = "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20241022/emyrja/dog_and_girl.jpeg";
        String chatId = UUID.randomUUID().toString();

        String response = loveApp.doMultimodalChat(
                "请描述这张图片，并分析一下图中人物的情感状态",
                List.of(imageUrl),
                chatId
        );

        log.info("图片分析响应: {}", response);
        Assertions.assertNotNull(response);
        Assertions.assertFalse(response.isEmpty());
    }

    /**
     * 测试多图输入的多模态对话
     */
    @Test
    void testMultimodalChatWithMultipleImages() {
        // 使用相同的示例图片两次来测试多图输入
        String imageUrl = "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20241022/emyrja/dog_and_girl.jpeg";
        String chatId = UUID.randomUUID().toString();

        String response = loveApp.doMultimodalChat(
                "这两张图片有什么相似之处？",
                List.of(imageUrl, imageUrl),
                chatId
        );

        log.info("多图分析响应: {}", response);
        Assertions.assertNotNull(response);
        Assertions.assertFalse(response.isEmpty());
    }

    /**
     * 测试流式多模态对话
     */
    @Test
    void testMultimodalChatStream() {
        String chatId = UUID.randomUUID().toString();

        Flux<String> responseFlux = loveApp.doMultimodalChatStream(
                "请给我一些关于恋爱沟通的建议",
                chatId
        );

        StepVerifier.create(responseFlux)
                .expectNextMatches(text -> text != null && !text.isEmpty())
                .expectNextCount(2) // 期望至少收到3个片段
                .thenConsumeWhile(text -> text != null)
                .expectComplete()
                .verify(Duration.ofSeconds(30));
    }



    /**
     * 测试多模态对话与记忆的结合
     * 先进行图文对话，再验证AI能记住上下文
     */
    @Test
    void testMultimodalMemoryIntegration() {
        String imageUrl = "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20241022/emyrja/dog_and_girl.jpeg";
        String chatId = UUID.randomUUID().toString();

        // 第一轮：发送图片并提问
        String response1 = loveApp.doMultimodalChat(
                "这张图片中的场景有什么特别之处？",
                List.of(imageUrl),
                chatId
        );
        log.info("第一轮（图文）响应: {}", response1);
        Assertions.assertNotNull(response1);

        // 第二轮：不发送图片，询问之前图片的内容
        String response2 = loveApp.doMultimodalChat(
                "根据刚才的图片，你觉得他们的关系怎么样？",
                chatId
        );
        log.info("第二轮（纯文本）响应: {}", response2);
        Assertions.assertNotNull(response2);
        // AI应该能引用之前图片中的内容
    }
}
