package com.lijay.lijayaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
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
        try {
            if (request != null) {
                var instructions = request.prompt().getInstructions();
                if (!instructions.isEmpty()) {
                    // 获取最后一条用户输入（通常是最新的一条）
                    String userMessage = instructions.getLast().getText();
                    log.info("用户输入: {}", userMessage);
                } else {
                    log.warn("请求指令为空");
                }
            } else {
                log.warn("请求对象为空");
            }
        } catch (Exception e) {
            log.error("记录请求日志失败", e);
        }
    }
    
    protected void logResponse(ChatClientResponse chatClientResponse) {
        try {
            if (chatClientResponse != null && chatClientResponse.chatResponse() != null) {
                var results = chatClientResponse.chatResponse().getResults();
                if (!results.isEmpty()) {
                    var firstResult = results.getFirst();
                    String responseText = firstResult.getOutput().getText();
                    log.info("模型回复: {}", responseText);
                } else {
                    log.warn("响应结果为空");
                }
            } else {
                log.warn("响应对象为空");
            }
        } catch (Exception e) {
            log.error("记录响应日志失败", e);
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
