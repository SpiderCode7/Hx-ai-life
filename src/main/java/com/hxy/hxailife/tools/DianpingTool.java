package com.hxy.hxailife.tools;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DianpingTool {

    private final RestClient restClient;

    public DianpingTool(RestClient.Builder restClientBuilder,
                        @Value("${tools.dianping.api-url}") String apiUrl) {
        this.restClient = restClientBuilder
                .baseUrl(apiUrl)
                .build();
    }

    @Tool(description = "调用大众点评自动化服务，单独搜索一个目的地或一个商户并获取其分享链接；一次调用只处理一个查询目标，不要把多个目的地或商户合并")
    public String searchDianping(
            @ToolParam(description = "一个目的地或一个商户关键词，例如：永庆坊，或：点都德 永庆坊") String query) {
        // 本次优化：保留模型原有的单字符串工具入口，结构化上下文由后端攻略流程额外传入。
        return searchDianping(query, null, null, null, false);
    }

    /**
     * 本次优化：后端攻略流程使用结构化上下文调用，Python 只校验详情页名称是否匹配目标。
     */
    public String searchDianping(String query, String targetName,
                                 String placeType, String nearbyPlace) {
        return searchDianping(query, targetName, placeType, nearbyPlace, true);
    }

    private String searchDianping(String query, String targetName,
                                  String placeType, String nearbyPlace, boolean requireVerified) {
        if (query == null || query.isBlank()) {
            return "大众点评查询失败：query 不能为空";
        }

        try {
            DianpingVO response = restClient.post()
                    .uri("/dianping")
                    .contentType(MediaType.APPLICATION_JSON)//发送为JSON格式
                    .body(new DianpingDTO(query.trim(), clean(targetName),
                            clean(placeType), clean(nearbyPlace)))
                    .retrieve()
                    .body(DianpingVO.class);

            if (response == null) {
                return "大众点评查询失败：服务没有返回结果";
            }
            // 本次优化：success 只代表 HTTP/脚本调用成功，只有 VERIFIED 才代表链接经过详情页核验。
            if (!response.isSuccess() || (requireVerified
                    && !"VERIFIED".equalsIgnoreCase(response.getStatus()))) {
                String reason = response.getMessage() == null || response.getMessage().isBlank()
                        ? response.getStatus() : response.getMessage();
                return "大众点评查询失败：" + reason;
            }
            if (response.getValue() == null || response.getValue().isBlank()) {
                return "大众点评查询失败：复制结果为空";
            }
            return response.getValue();
        } catch (RestClientException exception) {
            return "大众点评查询失败：无法连接自动化服务（" + exception.getMessage() + "）";
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    //快速封装一个类
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class DianpingDTO {
        private String query;
        private String targetName;
        private String placeType;
        private String nearbyPlace;
    }

    //快速封装一个接收的类
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class DianpingVO {
        private boolean success;

        private String status;

        private String query;

        private String value;

        private String message;

    }
}
