package com.hxy.hxailife.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class WebSearchToolsTest {

    @Autowired
    private WebSearchTools webSearchTools;

    @Test
    void searchWeb() {
        String query = "海底捞餐厅的营业时间";
//        String result = webSearchTools.searchWeb(query);
//        System.out.println(result);
    }
}