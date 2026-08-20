package com.hxy.hxailife.controller;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.hxy.hxailife.app.LifeApp;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.BiFunction;

@RestController
@RequestMapping("/Hx")
public class HxController {

    @Autowired
    private LifeApp lifeApp;

    private final ChatModel chatModel;

    public HxController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/text")
    private String text() {
        return "seccess!";
    }

    @GetMapping("/AiText")
    private String text2() {
        return chatModel.call("你好，介绍一下你自己");
    }

    @GetMapping("/AgentText")
    private String AgentText(String message){
        String result = lifeApp.result(message,"1");
        return result;
    }
}
