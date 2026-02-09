package com.lijay.lijayaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;

/**
 * 自定义日志Advisor
 * 打印 info 级别日志， 只输出单次用户提示词和 Ai 回复的文本
 */
@Slf4j
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {
    
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        this.logRequest(chatClientRequest);
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        this.logResponse(chatClientResponse);
        return chatClientResponse;
    }
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        this.logRequest(chatClientRequest);
        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);
        return (new ChatClientMessageAggregator()).aggregateChatClientResponse(chatClientResponses, this::logResponse);
    }

    protected void logRequest(ChatClientRequest request) {
        log.info("request: {}", ((UserMessage) request.prompt().getInstructions().get(1)).getText());
    }
    
    protected void logResponse(ChatClientResponse chatClientResponse) {
        if (chatClientResponse.chatResponse() != null) {
            log.info("response: {}", chatClientResponse.chatResponse().getResults().getFirst().getOutput().getText());
        }
    }
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
    @Override
    public int getOrder() {
        return 100;
    }

    public String toString() {
        return MyLoggerAdvisor.class.getSimpleName();
    }

}
