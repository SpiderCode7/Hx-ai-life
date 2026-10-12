package com.hxy.hxailife.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

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

//    @Test
//    void resultUseMCP() {
//        String message = """
//        你可以调用高德的mcp帮我查看广州西塱地铁站附近的海底捞在哪吗？然后给出通勤方案，看一下过去要多久。然后调用小红书的mcp找出这家海底捞的帖子，把帖子的链接发给我。再调用大众点评的工具，帮我找出这家海底捞的链接。最后告诉我所有的mcp和工具是否都成功用上了。最后告诉我李思曼是谁？
//        """;
//        String uuid = UUID.randomUUID().toString();
//        String result = lifeApp.resultUseMCPwithRag(message,uuid);
//    }
}