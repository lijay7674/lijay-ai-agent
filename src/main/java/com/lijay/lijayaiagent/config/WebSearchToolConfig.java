package com.lijay.lijayaiagent.config;

import com.lijay.lijayaiagent.tools.WebSearchTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WebSearchTool 配置类
 * 用于配置和初始化百度搜索工具
 *
 * @author lijay
 */
@Configuration
@Slf4j
public class WebSearchToolConfig {

    @Value("${searchapi.api-key}")
    private String apiKey;

    /**
     * 创建 WebSearchTool Bean
     * 只有当 searchapi.api-key 配置存在时才会创建
     *
     * @return WebSearchTool 实例
     */
    @Bean
    @ConditionalOnProperty(prefix = "searchapi", name = "api-key")
    public WebSearchTool webSearchTool() {
        log.info("初始化 WebSearchTool，API Key 已配置");
        return new WebSearchTool(apiKey);
    }
}
