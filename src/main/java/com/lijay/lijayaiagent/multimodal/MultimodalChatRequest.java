package com.lijay.lijayaiagent.multimodal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 多模态对话请求封装类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultimodalChatRequest {

    /**
     * 对话ID，用于记忆管理
     */
    private String chatId;

    /**
     * 用户输入的文本内容
     */
    private String text;

    /**
     * 图片列表（URL或Base64编码）
     */
    private List<String> images;

    /**
     * 模型名称，默认为 qwen-vl-plus
     */
    @Builder.Default
    private String model = "qwen-vl-plus";

    /**
     * 是否启用流式输出
     */
    @Builder.Default
    private boolean stream = false;

    /**
     * 创建简单请求（仅文本）
     */
    public static MultimodalChatRequest of(String chatId, String text) {
        return MultimodalChatRequest.builder()
                .chatId(chatId)
                .text(text)
                .build();
    }

    /**
     * 创建图文请求
     */
    public static MultimodalChatRequest of(String chatId, String text, List<String> images) {
        return MultimodalChatRequest.builder()
                .chatId(chatId)
                .text(text)
                .images(images)
                .build();
    }
}
