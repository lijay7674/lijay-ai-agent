CREATE TABLE chat_memory_message (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     conversation_id VARCHAR(255) NOT NULL COMMENT '对话ID',
                                     role VARCHAR(50) COMMENT '消息角色',
                                     message_bytes BLOB NOT NULL COMMENT '消息序列化字节数组',
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     INDEX idx_conversation_id (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话记忆消息表';