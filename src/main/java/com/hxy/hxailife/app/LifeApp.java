package com.hxy.hxailife.app;

import com.hxy.hxailife.advisor.MyLoggerAdvisor;
import com.hxy.hxailife.advisor.ReReadingAdvisor;
import com.hxy.hxailife.chatMemoryRepository.MongoChatMemoryRepository;
import com.hxy.hxailife.tools.WebSearchTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TransferQueue;

@Component
@Slf4j
public class LifeApp {

//    @Resource
//    private VectorStore lifeAppVectorStore;

    @Autowired
    private ToolCallback[] allTools;

    private final ChatClient chatClient;

    private final ChatClient chatClientRag;

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

    public LifeApp(ChatModel dashscopeChatModel,MongoChatMemoryRepository mongoMemoryRepository,VectorStore lifeAppVectorStore){
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(mongoMemoryRepository)
                .maxMessages(20)
                .build();

        //构建检索器（把用户问题转化为向量）
        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(lifeAppVectorStore) //指定数据源（ES查找数据（添加esVectorStore依赖，并配置文件配置es））
                .similarityThreshold(0.3) //设置相似度及格线（相似度得分达到 0.3 及以上的文档才会被选中）
                .topK(6) //限制最大返回数量（按相似度从高到低排序，最多只截取前 6 条最相关的结果拿给大模型参考）
                .build();
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLoggerAdvisor(),
                        new ReReadingAdvisor()
                )
                .build();
        this.chatClientRag = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                // 日志
                .defaultAdvisors(new MyLoggerAdvisor())
                // 重读
//                .defaultAdvisors(new ReReadingAdvisor())
                // Rag知识库
                .defaultAdvisors(RetrievalAugmentationAdvisor.builder()
                        .documentRetriever(documentRetriever)
                        .build())
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

    public String resultWithRag(String message,String chatId){
        ChatResponse chatResponse = chatClientRag.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String resultUseMCP(String message,String chatId){
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }
}
