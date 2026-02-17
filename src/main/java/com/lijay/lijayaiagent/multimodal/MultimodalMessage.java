package com.lijay.lijayaiagent.multimodal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 多模态消息类
 * 封装角色和内容列表，用于多模态对话
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultimodalMessage {

    /**
     * 角色：system、user、assistant
     */
    private String role;

    /**
     * 内容列表，可包含文本和图片
     */
    private List<MultimodalContent> contents;

    /**
     * 创建用户消息
     */
    public static MultimodalMessage user(List<MultimodalContent> contents) {
        return MultimodalMessage.builder()
                .role("user")
                .contents(contents)
                .build();
    }

    /**
     * 创建系统消息
     */
    public static MultimodalMessage system(String text) {
        return MultimodalMessage.builder()
                .role("system")
                .contents(List.of(MultimodalContent.text(text)))
                .build();
    }

    /**
     * 创建助手消息
     */
    public static MultimodalMessage assistant(String text) {
        return MultimodalMessage.builder()
                .role("assistant")
                .contents(List.of(MultimodalContent.text(text)))
                .build();
    }
}
