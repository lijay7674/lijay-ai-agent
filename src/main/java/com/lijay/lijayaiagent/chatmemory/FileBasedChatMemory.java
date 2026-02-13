package com.lijay.lijayaiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.objenesis.strategy.StdInstantiatorStrategy;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于文件的对话记忆实现类
 * 这是一个持久化的对话记忆存储实现，使用Kryo序列化框架将对话历史保存到文件系统中。
 */
public class FileBasedChatMemory implements ChatMemory {
    /**
     * 基础存储目录路径
     * 所有对话文件都将存储在此目录下
     */
    private final String BASE_DIR;
    
    /**
     * Kryo序列化器实例
     * 用于对象的序列化和反序列化操作
     */
    private static final ThreadLocal<Kryo> KRYO = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        return kryo;
    });

    /**
     * 静态初始化块
     * 配置Kryo序列化器的基本设置
     * 配置说明：
     * - setRegistrationRequired(false): 允许序列化未注册的类
     * - setInstantiatorStrategy(): 设置对象实例化策略为标准策略
     */

    /**
     * 构造函数
     * 初始化文件存储目录，如果目录不存在则自动创建。
     */
    // 构造对象时，指定文件保存目录
    public FileBasedChatMemory(String dir) {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    /**
     * 向指定会话添加消息
     * 将新的消息添加到指定会话的对话历史中，并持久化保存。
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> conversationMessages = getOrCreateConversation(conversationId);
        conversationMessages.addAll(messages);
        saveConversation(conversationId, conversationMessages);
    }

    /**
     * 获取指定会话的所有消息
     * 从持久化存储中读取指定会话的完整对话历史。
     */
    @Override
    public List<Message> get(String conversationId) {
        return getOrCreateConversation(conversationId);
    }

    /**
     * 清除指定会话的所有消息
     * 删除指定会话的完整对话历史。
     * @param conversationId 要清除的会话唯一标识符
     */
    @Override
    public void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 获取或创建会话消息列表
     * @param conversationId 会话唯一标识符
     * @return 会话消息列表（可能是空列表）
     */
    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                messages = KRYO.get().readObject(input, ArrayList.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return messages;
    }

    /**
     * 保存会话消息到文件
     * @param conversationId 会话唯一标识符
     * @param messages 要保存的消息列表
     */
    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            KRYO.get().writeObject(output, messages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 构造会话文件路径
     * 私有辅助方法，根据会话ID生成对应的文件路径。
     * @param conversationId 会话唯一标识符
     * @return 对应的文件对象
     */
    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }
}