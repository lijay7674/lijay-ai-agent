package com.lijay.lijayaiagent.chatmemory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public class JdbcChatMemory implements ChatMemory {

    @Override
    public void add(String conversationId, List<Message> messages) {

    }

    @Override
    public List<Message> get(String conversationId) {
        return List.of();
    }

    @Override
    public void clear(String conversationId) {

    }
}
