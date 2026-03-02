package com.lijay.lijayaiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private MyKeywordEnricher myKeywordEnricher;
    /**
     * 创建向量存储 Bean
     * 如果嵌入服务不可用，返回空的向量存储，应用仍可正常启动
     */
    @Bean
    VectorStore appVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();

        List<Document> documents = appDocumentLoader.loadMarkdown();
        List<Document> enrichDocuments = myKeywordEnricher.enrich(documents);
        simpleVectorStore.doAdd(enrichDocuments);
        return simpleVectorStore;
    }

}
