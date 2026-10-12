package com.hxy.hxailife.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class DianpingToolTest {

    @Autowired
    private DianpingTool dianpingTool;

    @Test
    void searchDianping() {
        String query = "点都德永庆坊";
        String code = dianpingTool.searchDianping(query);
        System.out.println("复制码：" + code);
    }

    @Test
    void testSearchDianping() {
        String code = dianpingTool.searchDianping("广州西关永庆坊旅游区","广州西关永庆坊旅游区","DESTINATION",null);
        System.out.println("复制码：" + code);
    }
}