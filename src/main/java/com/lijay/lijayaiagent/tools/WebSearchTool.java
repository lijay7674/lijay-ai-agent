package com.lijay.lijayaiagent.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网络搜索工具类
 * 基于 SearchAPI.io 的百度搜索引擎 API 实现网络搜索功能
 * 
 * @author lijay
 * @see <a href="https://www.searchapi.io/docs/baidu">SearchAPI Baidu Documentation</a>
 */
@Slf4j
public class WebSearchTool {

    /**
     * SearchAPI 基础 URL
     */
    private static final String BASE_URL = "https://www.searchapi.io/api/v1/search";

    /**
     * 默认引擎类型
     */
    private static final String DEFAULT_ENGINE = "baidu";

    /**
     * 默认每页结果数量
     */
    private static final int DEFAULT_NUM = 10;

    /**
     * 默认页码
     */
    private static final int DEFAULT_PAGE = 1;

    /**
     * API 密钥（从环境变量或配置中获取）
     */
    private final String apiKey;

    /**
     * 构造函数
     *
     * @param apiKey SearchAPI 的 API 密钥
     */
    public WebSearchTool(String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            throw new IllegalArgumentException("API Key 不能为空，请配置 searchapi.api-key");
        }
        this.apiKey = apiKey;
    }

    /**
     * 执行百度搜索
     * 
     * @param query 搜索查询词
     * @return 搜索结果的 JSON 字符串
     */
    @Tool(description = "Search the web using Baidu search engine. Returns relevant search results including titles, snippets, and links.")
    public String search(@ToolParam(description = "The search query text to search for on Baidu") String query) {
        return search(query, null, null, null, null);
    }

    /**
     * 执行百度搜索（带完整参数）
     *
     * @param query 搜索查询词（必需）
     * @param ct    语言控制参数：0-简繁中文(默认)，1-简体中文，2-繁体中文
     * @param gpc   时间范围过滤，格式：stf=START_TIME,END_TIME|stftype=1（Unix时间戳）
     * @param num   每页结果数量（最大50）
     * @param page  页码（默认1）
     * @return 搜索结果的 JSON 字符串
     */
    public String search(String query, Integer ct, String gpc, Integer num, Integer page) {
        // 参数验证
        if (StrUtil.isBlank(query)) {
            log.error("搜索查询词不能为空");
            return buildErrorResponse("搜索查询词不能为空");
        }

        log.info("开始执行百度搜索，查询词: {}", query);

        try {
            // 构建请求参数
            Map<String, Object> params = buildRequestParams(query, ct, gpc, num, page);
            
            log.debug("请求参数: {}", params);

            // 发送 HTTP 请求
            HttpResponse response = executeRequest(params);

            // 解析响应
            if (!response.isOk()) {
                log.error("搜索请求失败，HTTP状态码: {}", response.getStatus());
                return buildErrorResponse("搜索请求失败，HTTP状态码: " + response.getStatus());
            }

            String responseBody = response.body();
            log.debug("搜索响应: {}", responseBody);

            // 解析并格式化结果
            String formattedResult = parseAndFormatResponse(responseBody);
            
            log.info("百度搜索完成，查询词: {}", query);
            return formattedResult;

        } catch (Exception e) {
            log.error("搜索过程中发生异常", e);
            return buildErrorResponse("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 构建请求参数
     */
    private Map<String, Object> buildRequestParams(String query, Integer ct, String gpc, Integer num, Integer page) {
        Map<String, Object> params = new HashMap<>();
        params.put("engine", DEFAULT_ENGINE);
        params.put("q", query);
        params.put("api_key", apiKey);

        // 可选参数
        if (ct != null && ct >= 0 && ct <= 2) {
            params.put("ct", ct);
        }
        if (StrUtil.isNotBlank(gpc)) {
            params.put("gpc", gpc);
        }
        if (num != null && num > 0 && num <= 50) {
            params.put("num", num);
        } else {
            params.put("num", DEFAULT_NUM);
        }
        if (page != null && page > 0) {
            params.put("page", page);
        } else {
            params.put("page", DEFAULT_PAGE);
        }

        return params;
    }

    /**
     * 执行 HTTP 请求
     */
    private HttpResponse executeRequest(Map<String, Object> params) {
        return HttpRequest.get(BASE_URL)
                .form(params)
                .timeout(30000) // 30秒超时
                .execute();
    }

    /**
     * 解析并格式化响应结果
     */
    private String parseAndFormatResponse(String responseBody) {
        try {
            JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
            
            // 构建格式化结果
            StringBuilder result = new StringBuilder();
            
            // 1. 解析 Answer Box（答案框）
            if (jsonResponse.containsKey("answer_box")) {
                JSONObject answerBox = jsonResponse.getJSONObject("answer_box");
                result.append(formatAnswerBox(answerBox));
            }

            // 2. 解析 Organic Results（有机搜索结果）
            if (jsonResponse.containsKey("organic_results")) {
                JSONArray organicResults = jsonResponse.getJSONArray("organic_results");
                result.append(formatOrganicResults(organicResults));
            }

            // 3. 解析 Ads（广告结果）
            if (jsonResponse.containsKey("ads")) {
                JSONArray ads = jsonResponse.getJSONArray("ads");
                result.append(formatAds(ads));
            }

            if (result.length() == 0) {
                return "未找到相关搜索结果";
            }

            return result.toString();

        } catch (Exception e) {
            log.error("解析响应失败", e);
            return "解析搜索结果失败: " + e.getMessage();
        }
    }

    /**
     * 格式化答案框内容
     */
    private String formatAnswerBox(JSONObject answerBox) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 答案框 ===\n");

        String type = answerBox.getStr("type");
        sb.append("类型: ").append(type).append("\n");

        // AI 内容
        if ("ai_content".equals(type) || "ai_search".equals(type)) {
            String title = answerBox.getStr("title");
            String answer = answerBox.getStr("answer");
            if (StrUtil.isNotBlank(title)) {
                sb.append("标题: ").append(title).append("\n");
            }
            if (StrUtil.isNotBlank(answer)) {
                sb.append("答案: ").append(answer).append("\n");
            }
        }
        // 计算器
        else if ("calculator".equals(type)) {
            String calcQuery = answerBox.getStr("query");
            String answer = answerBox.getStr("answer");
            if (StrUtil.isNotBlank(calcQuery)) {
                sb.append("计算: ").append(calcQuery).append("\n");
            }
            if (StrUtil.isNotBlank(answer)) {
                sb.append("结果: ").append(answer).append("\n");
            }
        }
        // 字典/翻译
        else if ("dictionary".equals(type)) {
            String title = answerBox.getStr("title");
            String translation = answerBox.getStr("translation_meaning");
            if (StrUtil.isNotBlank(title)) {
                sb.append("词条: ").append(title).append("\n");
            }
            if (StrUtil.isNotBlank(translation)) {
                sb.append("翻译: ").append(translation).append("\n");
            }
        }
        // 其他类型
        else {
            String title = answerBox.getStr("title");
            String answer = answerBox.getStr("answer");
            String link = answerBox.getStr("link");
            if (StrUtil.isNotBlank(title)) {
                sb.append("标题: ").append(title).append("\n");
            }
            if (StrUtil.isNotBlank(answer)) {
                sb.append("内容: ").append(answer).append("\n");
            }
            if (StrUtil.isNotBlank(link)) {
                sb.append("链接: ").append(link).append("\n");
            }
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * 格式化有机搜索结果
     */
    private String formatOrganicResults(JSONArray organicResults) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 搜索结果 ===\n");

        for (int i = 0; i < organicResults.size(); i++) {
            JSONObject result = organicResults.getJSONObject(i);
            Integer position = result.getInt("position");
            String title = result.getStr("title");
            String link = result.getStr("link");
            String displayedLink = result.getStr("displayed_link");
            String snippet = result.getStr("snippet");

            sb.append("\n【").append(position != null ? position : i + 1).append("】");
            
            if (StrUtil.isNotBlank(title)) {
                sb.append(" ").append(title);
            }
            sb.append("\n");
            
            if (StrUtil.isNotBlank(displayedLink)) {
                sb.append("来源: ").append(displayedLink).append("\n");
            }
            if (StrUtil.isNotBlank(link)) {
                sb.append("链接: ").append(link).append("\n");
            }
            if (StrUtil.isNotBlank(snippet)) {
                sb.append("摘要: ").append(snippet).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 格式化广告结果
     */
    private String formatAds(JSONArray ads) {
        if (ads == null || ads.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== 广告结果 ===\n");

        for (int i = 0; i < ads.size(); i++) {
            JSONObject ad = ads.getJSONObject(i);
            Integer position = ad.getInt("position");
            String title = ad.getStr("title");
            String link = ad.getStr("link");
            String displayedLink = ad.getStr("displayed_link");
            String snippet = ad.getStr("snippet");

            sb.append("\n【广告 ").append(position != null ? position : i + 1).append("】");
            
            if (StrUtil.isNotBlank(title)) {
                sb.append(" ").append(title);
            }
            sb.append("\n");
            
            if (StrUtil.isNotBlank(displayedLink)) {
                sb.append("来源: ").append(displayedLink).append("\n");
            }
            if (StrUtil.isNotBlank(link)) {
                sb.append("链接: ").append(link).append("\n");
            }
            if (StrUtil.isNotBlank(snippet)) {
                sb.append("描述: ").append(snippet).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 构建错误响应
     */
    private String buildErrorResponse(String message) {
        JSONObject error = new JSONObject();
        error.set("success", false);
        error.set("error", message);
        return error.toString();
    }

    /**
     * 执行搜索并返回结构化结果对象
     * 用于程序化处理搜索结果
     *
     * @param query 搜索查询词
     * @return 搜索结果对象
     */
    public SearchResult searchWithStructuredResult(String query) {
        return searchWithStructuredResult(query, null, null, null, null);
    }

    /**
     * 执行搜索并返回结构化结果对象（带完整参数）
     */
    public SearchResult searchWithStructuredResult(String query, Integer ct, String gpc, Integer num, Integer page) {
        if (StrUtil.isBlank(query)) {
            log.error("搜索查询词不能为空");
            return SearchResult.error("搜索查询词不能为空");
        }

        log.info("开始执行百度搜索（结构化），查询词: {}", query);

        try {
            Map<String, Object> params = buildRequestParams(query, ct, gpc, num, page);
            HttpResponse response = executeRequest(params);

            if (!response.isOk()) {
                log.error("搜索请求失败，HTTP状态码: {}", response.getStatus());
                return SearchResult.error("搜索请求失败，HTTP状态码: " + response.getStatus());
            }

            String responseBody = response.body();
            JSONObject jsonResponse = JSONUtil.parseObj(responseBody);

            // 解析结果
            SearchResult result = new SearchResult();
            result.setSuccess(true);
            result.setQuery(query);

            // 解析答案框
            if (jsonResponse.containsKey("answer_box")) {
                result.setAnswerBox(parseAnswerBox(jsonResponse.getJSONObject("answer_box")));
            }

            // 解析有机结果
            if (jsonResponse.containsKey("organic_results")) {
                result.setOrganicResults(parseOrganicResults(jsonResponse.getJSONArray("organic_results")));
            }

            // 解析广告
            if (jsonResponse.containsKey("ads")) {
                result.setAds(parseAds(jsonResponse.getJSONArray("ads")));
            }

            log.info("百度搜索完成（结构化），查询词: {}, 结果数: {}", 
                    query, 
                    result.getOrganicResults() != null ? result.getOrganicResults().size() : 0);
            
            return result;

        } catch (Exception e) {
            log.error("搜索过程中发生异常", e);
            return SearchResult.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 解析答案框为对象
     */
    private AnswerBox parseAnswerBox(JSONObject answerBox) {
        if (answerBox == null) {
            return null;
        }
        AnswerBox box = new AnswerBox();
        box.setType(answerBox.getStr("type"));
        box.setTitle(answerBox.getStr("title"));
        box.setAnswer(answerBox.getStr("answer"));
        box.setLink(answerBox.getStr("link"));
        box.setQuery(answerBox.getStr("query"));
        box.setTranslationMeaning(answerBox.getStr("translation_meaning"));
        return box;
    }

    /**
     * 解析有机搜索结果为列表
     */
    private List<OrganicResult> parseOrganicResults(JSONArray organicResults) {
        if (organicResults == null) {
            return new ArrayList<>();
        }
        List<OrganicResult> results = new ArrayList<>();
        for (int i = 0; i < organicResults.size(); i++) {
            JSONObject item = organicResults.getJSONObject(i);
            OrganicResult result = new OrganicResult();
            result.setPosition(item.getInt("position"));
            result.setTitle(item.getStr("title"));
            result.setLink(item.getStr("link"));
            result.setDisplayedLink(item.getStr("displayed_link"));
            result.setSnippet(item.getStr("snippet"));
            result.setThumbnail(item.getStr("thumbnail"));
            results.add(result);
        }
        return results;
    }

    /**
     * 解析广告结果为列表
     */
    private List<AdResult> parseAds(JSONArray ads) {
        if (ads == null) {
            return new ArrayList<>();
        }
        List<AdResult> results = new ArrayList<>();
        for (int i = 0; i < ads.size(); i++) {
            JSONObject item = ads.getJSONObject(i);
            AdResult result = new AdResult();
            result.setPosition(item.getInt("position"));
            result.setTitle(item.getStr("title"));
            result.setLink(item.getStr("link"));
            result.setDisplayedLink(item.getStr("displayed_link"));
            result.setSnippet(item.getStr("snippet"));
            results.add(result);
        }
        return results;
    }

    // ==================== 内部数据类 ====================

    /**
     * 搜索结果
     */
    @lombok.Data
    public static class SearchResult {
        private boolean success;
        private String error;
        private String query;
        private AnswerBox answerBox;
        private List<OrganicResult> organicResults;
        private List<AdResult> ads;

        public static SearchResult error(String message) {
            SearchResult result = new SearchResult();
            result.setSuccess(false);
            result.setError(message);
            return result;
        }
    }

    /**
     * 答案框
     */
    @lombok.Data
    public static class AnswerBox {
        private String type;       // ai_content, ai_search, calculator, dictionary
        private String title;
        private String answer;
        private String link;
        private String query;      // 计算器类型专用
        private String translationMeaning; // 字典类型专用
    }

    /**
     * 有机搜索结果
     */
    @lombok.Data
    public static class OrganicResult {
        private Integer position;
        private String title;
        private String link;
        private String displayedLink;
        private String snippet;
        private String thumbnail;
    }

    /**
     * 广告结果
     */
    @lombok.Data
    public static class AdResult {
        private Integer position;
        private String title;
        private String link;
        private String displayedLink;
        private String snippet;
    }
}
