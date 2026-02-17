package com.lijay.lijayaiagent.multimodal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 多模态内容封装类
 * 用于封装文本或图片类型的消息内容
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultimodalContent {

    /**
     * 内容类型：text 或 image
     */
    private String type;

    /**
     * 文本内容（当type为text时使用）
     */
    private String text;

    /**
     * 图片内容（当type为image时使用）
     * 支持：
     * 1. URL地址（http/https）
     * 2. Base64编码的图片数据（data:image/jpeg;base64,...）
     * 3. 本地文件路径（将自动转换为base64）
     */
    private String image;

    /**
     * 创建文本内容
     */
    public static MultimodalContent text(String text) {
        return MultimodalContent.builder()
                .type("text")
                .text(text)
                .build();
    }

    /**
     * 创建图片内容（URL或Base64）
     */
    public static MultimodalContent image(String imageUrlOrBase64) {
        return MultimodalContent.builder()
                .type("image")
                .image(imageUrlOrBase64)
                .build();
    }
}
