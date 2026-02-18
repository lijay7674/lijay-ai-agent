package com.lijay.lijayaiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

/**
 * 向量数据库，基于内存的向量数据库Bean
 */
@Configuration
@Slf4j
public class AppVectorStoreConfig {
    @Resource
    private AppDocumentLoader appDocumentLoader;

    /**
     * 创建向量存储 Bean
     * 使用 @Lazy 延迟初始化，避免启动时立即调用嵌入服务
     * 如果嵌入服务不可用，返回空的向量存储，应用仍可正常启动
     */
    @Bean
    @Lazy
    VectorStore appVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
        
        try {
            List<Document> documents = appDocumentLoader.loadMarkdown();
            if (!documents.isEmpty()) {
                log.info("开始加载文档到向量存储，文档数量: {}", documents.size());
                simpleVectorStore.doAdd(documents);
                log.info("文档加载完成");
            } else {
                log.warn("未找到任何文档，向量存储为空");
            }
        } catch (Exception e) {
            // 嵌入服务不可用时，返回空的向量存储，RAG 功能暂时不可用
            log.warn("向量存储初始化失败，RAG 功能暂时不可用: {}", e.getMessage());
            log.debug("详细错误信息:", e);
        }
        
        return simpleVectorStore;
    }

}
