package com.hxy.hxailife.toolsRegistration;

import cn.hutool.core.builder.Builder;
import com.hxy.hxailife.tools.DianpingTool;
import com.hxy.hxailife.tools.WebScrapingTool;
import com.hxy.hxailife.tools.WebSearchTools;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ToolsRegistration {

    @Autowired
    private WebSearchTools webSearchTools;

    @Autowired
    private WebScrapingTool webScrapingTool;

    @Autowired
    private DianpingTool dianpingTool;

//    @Value("${tools.search-api-key}")
//    private String searchApi;
//
//    private RestClient.Builder restClientBuilder;

    @Bean
    public ToolCallback[] allTools(){
//        WebScrapingTool webScrapingTool = new WebScrapingTool();
//        WebSearchTools webSearchTools = new WebSearchTools(restClientBuilder,searchApi);
        return ToolCallbacks.from(
                webScrapingTool,
                webSearchTools,
                dianpingTool
        );
    }
}
