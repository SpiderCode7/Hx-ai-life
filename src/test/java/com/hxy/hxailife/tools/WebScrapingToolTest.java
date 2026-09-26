package com.hxy.hxailife.tools;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class WebScrapingToolTest {

    @Resource
    private WebScrapingTool webScrapingTool;

    @Test
    void scrapeWebPage() {
        String url = "https://www.haidilao.com/";
        String result = webScrapingTool.scrapeWebPage(url);
        System.out.println(result);
    }
}