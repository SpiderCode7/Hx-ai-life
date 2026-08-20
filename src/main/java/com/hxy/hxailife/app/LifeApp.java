package com.hxy.hxailife.app;

import com.hxy.hxailife.advisor.MyLoggerAdvisor;
import com.hxy.hxailife.advisor.ReReadingAdvisor;
import com.hxy.hxailife.chatMemoryRepository.MongoChatMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LifeApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "你是\"周末生活规划师\"，"
            + "帮用户和朋友策划周末聚会：把去哪、吃什么、玩什么、花多少、怎么去、要不要排队，"
            + "整合成一份可直接执行的方案。像靠谱朋友，先给结论，不啰嗦。";

//    public LifeApp(ChatModel dashscopeChatModel, ChatMemory chatMemory) {
//        this.chatClient = ChatClient.builder(dashscopeChatModel)
//                .defaultSystem(SYSTEM_PROMPT)
//                .defaultAdvisors(
//                        MessageChatMemoryAdvisor.builder(chatMemory)
//                                .build(),
//                        new MyLoggerAdvisor(),
//                        new ReReadingAdvisor())
//                .build();
//    }

    public LifeApp(ChatModel dashscopeChatModel,MongoChatMemoryRepository mongoMemoryRepository){
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(mongoMemoryRepository)
                .maxMessages(20)
                .build();
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLoggerAdvisor(),
                        new ReReadingAdvisor()
                )
                .build();
    }


    public String result(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }
}
