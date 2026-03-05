package com.lijay.lijayaiagent.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;

/**
 * 查询重写器，用于将用户查询重写为更具体的查询
 */
@Component
public class QueryRewriter {
    private final QueryTransformer queryTransformer;

    public QueryRewriter(ChatModel dashscopeChatModel){
        ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel);

        this.queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(builder)
                .build();
    }

    public String doQueryRewrite(String prompt){
        Query query = new Query(prompt);
        //执行查询重写
        Query transform = queryTransformer.transform(query);
        //输出重写后的查询
        return transform.text();
    }
}
