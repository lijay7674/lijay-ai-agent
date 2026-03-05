package com.lijay.lijayaiagent.rag;

import com.alibaba.cloud.ai.advisor.RetrievalRerankAdvisor;
import com.lijay.lijayaiagent.advisor.ReReadingAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;

import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * 创建自定义的 RAG 检索增强顾问的工厂
 */
public class AppRagCustomAdvisorFactory {
    /**
     * 创建自定义的 RAG 检索增强顾问
     *
     * @param vectorStore 向量存储
     * @param status      状态
     * @return 自定义的 RAG 检索增强顾问
     */
    public static Advisor doCreate(VectorStore vectorStore, String status) {
        //过滤特定状态工厂
        Filter.Expression expression = new FilterExpressionBuilder().eq("status", status).build();
        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(expression) // 添加过滤表达式
                .similarityThreshold(0.3) //相似度阈值
                .topK(3)
                .build();
        // 过滤特定状态的文档
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(AppContextualQueryAugmenterFactory.doCreate())
                .build();
    }
}
