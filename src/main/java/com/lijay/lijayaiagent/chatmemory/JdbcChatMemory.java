package com.lijay.lijayaiagent.chatmemory;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.lijay.lijayaiagent.entity.ChatMemoryMessage;
import com.lijay.lijayaiagent.mapper.ChatMemoryMessageMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 基于 MyBatis-Plus 的对话记忆实现类。
 *
 * 设计目标：与 {@link FileBasedChatMemory} 的语义保持一致
 * - add：向某个 conversationId 追加消息
 * - get：读取某个 conversationId 的完整历史
 * - clear：清空某个 conversationId 的历史
 *
 * 使用 MyBatis-Plus 替代原始 JDBC 操作的优势：
 * - 类型安全：编译时检查字段名和类型
 * - 代码简洁：减少样板代码
 * - 易于维护：ORM 映射清晰直观
 * - 自动分页：内置分页支持
 *
 * Message 序列化：
 * - 使用 Kryo 将每个 Message 序列化为 byte[]
 * - 通过 ThreadLocal 确保线程安全
 */
@Component
public class JdbcChatMemory implements ChatMemory {

    private static final ThreadLocal<Kryo> KRYO = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        return kryo;
    });

    @Autowired
    private ChatMemoryMessageMapper chatMemoryMessageMapper;

    @Override
    public void add(String conversationId, List<Message> messages) {
        Objects.requireNonNull(conversationId, "conversationId 不能为空");
        if (messages == null || messages.isEmpty()) {
            return;
        }

        // 逐条插入消息
        for (Message message : messages) {
            ChatMemoryMessage entity = new ChatMemoryMessage();
            entity.setConversationId(conversationId);
            entity.setMessageBytes(serializeMessage(message));
            entity.setRole(message.getMessageType().toString());
            chatMemoryMessageMapper.insert(entity);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        Objects.requireNonNull(conversationId, "conversationId 不能为空");

        // 查询消息并转换为Message对象
        QueryWrapper<ChatMemoryMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId);
        queryWrapper.orderByAsc("created_at");
        List<ChatMemoryMessage> entities = chatMemoryMessageMapper.selectList(queryWrapper);
        
        return entities.stream()
                .map(entity -> deserializeMessage(entity.getMessageBytes()))
                .collect(Collectors.toList());
    }

    @Override
    public void clear(String conversationId) {
        Objects.requireNonNull(conversationId, "conversationId 不能为空");
        // 使用MyBatis-Plus的条件删除
        chatMemoryMessageMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMemoryMessage>()
                .eq("conversation_id", conversationId)
        );
    }

    private static byte[] serializeMessage(Message message) {
        Kryo kryo = KRYO.get();
        try (Output output = new Output(512, -1)) {
            kryo.writeClassAndObject(output, message);
            return output.toBytes();
        }
    }

    private static Message deserializeMessage(byte[] bytes) {
        Kryo kryo = KRYO.get();
        try (Input input = new Input(bytes)) {
            Object obj = kryo.readClassAndObject(input);
            return (Message) obj;
        }
    }
}
