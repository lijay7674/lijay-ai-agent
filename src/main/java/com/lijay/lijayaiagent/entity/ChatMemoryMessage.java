package com.lijay.lijayaiagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话记忆消息实体类
 * 对应数据库表 chat_memory_message
 */
@TableName("chat_memory_message")
@Data
public class ChatMemoryMessage {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 对话ID，用于区分不同对话
     */
    @TableField("conversation_id")
    private String conversationId;

    /**
     * 消息角色（user/assistant/system等）
     */
    @TableField("role")
    private String role;

    /**
     * 消息内容序列化后的字节数组
     */
    @TableField("message_bytes")
    private byte[] messageBytes;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

}
