package com.lijay.lijayaiagent.tools;

import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSearchTool 测试类
 * 
 * 注意：运行测试前请设置环境变量 SEARCHAPI_API_KEY 或直接设置 apiKey
 *
 * @author lijay
 */
@SpringBootTest
class WebSearchToolTest {
    @Autowired
    private WebSearchTool webSearchTool;


    @Test
    @DisplayName("测试构造函数 - API Key 为空时抛出异常")
    void testConstructorWithBlankApiKey() {
        assertThrows(IllegalArgumentException.class, () -> new WebSearchTool(""));
        assertThrows(IllegalArgumentException.class, () -> new WebSearchTool(null));
        assertThrows(IllegalArgumentException.class, () -> new WebSearchTool("   "));
    }

    @Test
    @DisplayName("测试搜索 - 查询词为空时返回错误")
    void testSearchWithBlankQuery() {
        // 使用一个假的 API Key 来测试参数验证
        WebSearchTool tool = new WebSearchTool("fake-api-key");
        
        String result = tool.search("");
        assertTrue(result.contains("失败") || result.contains("错误") || result.contains("不能为空"));
        
        result = tool.search(null);
        assertTrue(result.contains("失败") || result.contains("错误") || result.contains("不能为空"));
        
        result = tool.search("   ");
        assertTrue(result.contains("失败") || result.contains("错误") || result.contains("不能为空"));
    }

    @Test
    @Disabled("需要真实的 API Key，手动启用测试")
    @DisplayName("测试真实搜索 - 基础搜索功能")
    void testRealSearch() {
        if (webSearchTool == null) {
            System.out.println("跳过测试：未配置 API Key");
            return;
        }
        
        String result = webSearchTool.search("Java Spring Boot 教程");
        System.out.println("搜索结果:\n" + result);
        
        assertNotNull(result);
        assertFalse(result.contains("失败"));
        assertTrue(result.contains("搜索结果") || result.contains("答案框"));
    }

    @Test
    @Disabled("需要真实的 API Key，手动启用测试")
    @DisplayName("测试真实搜索 - 带完整参数")
    void testRealSearchWithFullParams() {
        if (webSearchTool == null) {
            System.out.println("跳过测试：未配置 API Key");
            return;
        }
        
        String result = webSearchTool.search("Spring AI", 1, null, 5, 1);
        System.out.println("搜索结果:\n" + result);
        
        assertNotNull(result);
        assertFalse(result.contains("失败"));
    }

    @Test
    @Disabled("需要真实的 API Key，手动启用测试")
    @DisplayName("测试结构化搜索结果")
    void testSearchWithStructuredResult() {
        if (webSearchTool == null) {
            System.out.println("跳过测试：未配置 API Key");
            return;
        }
        
        WebSearchTool.SearchResult result = webSearchTool.searchWithStructuredResult("Python 机器学习");
        
        System.out.println("结构化结果:\n" + JSONUtil.toJsonPrettyStr(result));
        
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getQuery());
        
        if (result.getOrganicResults() != null && !result.getOrganicResults().isEmpty()) {
            WebSearchTool.OrganicResult firstResult = result.getOrganicResults().get(0);
            assertNotNull(firstResult.getTitle());
            assertNotNull(firstResult.getLink());
            System.out.println("第一条结果 - 标题: " + firstResult.getTitle());
            System.out.println("第一条结果 - 链接: " + firstResult.getLink());
        }
    }

    @Test
    @Disabled("需要真实的 API Key，手动启用测试")
    @DisplayName("测试答案框解析 - AI 内容")
    void testAnswerBoxAiContent() {
        if (webSearchTool == null) {
            System.out.println("跳过测试：未配置 API Key");
            return;
        }
        
        // 搜索可能触发 AI 答案框的查询
        String result = webSearchTool.search("拍照怎样找角度");
        System.out.println("答案框测试结果:\n" + result);
        
        assertNotNull(result);
    }

    @Test
    @Disabled("需要真实的 API Key，手动启用测试")
    @DisplayName("测试字典翻译功能")
    void testDictionarySearch() {
        if (webSearchTool == null) {
            System.out.println("跳过测试：未配置 API Key");
            return;
        }
        
        String result = webSearchTool.search("New York");
        System.out.println("字典测试结果:\n" + result);
        
        assertNotNull(result);
    }

    @Test
    @Disabled("需要真实的 API Key，手动启用测试")
    @DisplayName("测试计算器功能")
    void testCalculatorSearch() {
        if (webSearchTool == null) {
            System.out.println("跳过测试：未配置 API Key");
            return;
        }
        
        String result = webSearchTool.search("5!*4!");
        System.out.println("计算器测试结果:\n" + result);
        
        assertNotNull(result);
    }
}
