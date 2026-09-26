package com.hxy.hxailife.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class LifeAppTest {

    @Resource
    private LifeApp lifeApp;

    @Test
    void resultWithRag() {
        String message = "帮我调用网页抓取工具，把这个网页的html爬取出来：https://www.haidilao.com/";
        String result = lifeApp.resultWithRag(message,"2");
        System.out.println(result);
    }

    @Test
    void result() {
    }

    @Test
    void resultUseMCP() {
        String message = """
        必须调用高德地图 MCP 工具进行查询，不要根据模型知识直接回答。
        查询广州江南西地铁站附近的火锅店，推荐三家给我
        """;
        String result = lifeApp.resultUseMCP(message,"3");
        System.out.println(result);
    }
}