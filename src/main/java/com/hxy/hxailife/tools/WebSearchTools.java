package com.hxy.hxailife.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 联网搜索工具
 */
@Component
public class WebSearchTools {

    private final RestClient restClient;
    private final String searchApiKey;

    public WebSearchTools(RestClient.Builder restClientBuilder,
                          @Value("${tools.search-api-key}") String searchApiKey) {
        this.restClient = restClientBuilder
                .baseUrl("https://www.searchapi.io/api/v1")
                .build();
        this.searchApiKey = searchApiKey;
    }

    @Tool(description = "Search for information from Bing Search Engine")
    public String searchWeb(String query) {
        JsonNode result = searchResult(query);
        if(filterOrganicResults(result).toString() != null){
            return filterOrganicResults(result).toString();
        }else return new String("联网搜索失败或搜索为空");
    }

    @Tool(description = "Search for information from Bing Ai-Search Engine")
    public String searchWebAi(String query) {
        JsonNode result = searchResult(query);
        if(filterAiOverview(result).toString() != null){
            return filterAiOverview(result).toString();
        }else return new String("联网搜索失败或搜索为空");
    }

    /**
     * searchApi联网查询结果
     * @param query
     * @return
     */
    JsonNode searchResult(String query){
        return restClient.get() //发送get请求
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        //.queryParam封装，自动拼装 ？和 &
                        .queryParam("engine", "bing")
                        .queryParam("q", query)
                        .queryParam("location","China")
                        .queryParam("api_key", searchApiKey)
                        .build())
                .retrieve() //执行这个网络请求，并获取服务器响应的数据。
                .body(JsonNode.class); //将服务器返回的 JSON 解析为树形结构
    }

    /**
     * 过滤获取联网搜索的前三条数据
     * @param result
     * @return
     */
    ArrayNode filterOrganicResults(JsonNode result) {
        ArrayNode filteredResults = JsonNodeFactory.instance.arrayNode();
        if (result == null) {
            return filteredResults;
        }

        JsonNode organicResults = result.path("organic_results");
        int resultCount = Math.min(3, organicResults.size());
        for (int i = 0; i < resultCount; i++) {
            JsonNode source = organicResults.get(i);
            ObjectNode filteredResult = filteredResults.addObject();
            filteredResult.put("title", source.path("title").asText());
            filteredResult.put("link", source.path("link").asText());
            filteredResult.put("snippet", source.path("snippet").asText());
        }
        return filteredResults;
    }

    /**
     * 过滤获取搜索引擎的Ai回答
     * @param result
     * @return
     */
    ArrayNode filterAiOverview(JsonNode result){
        ArrayNode filteredResults = JsonNodeFactory.instance.arrayNode();
        if (result == null) {
            return filteredResults;
        }
        JsonNode organicResults = result.path("ai_overview").path("text_blocks");
        int count = Math.min(2,organicResults.size());
        for(int i=0;i < count;i++){
            String answer = organicResults
                    .get(i)
                    .path("answer")
                    .asText();

            filteredResults.add(answer);
        }

        return filteredResults;
    }

}
