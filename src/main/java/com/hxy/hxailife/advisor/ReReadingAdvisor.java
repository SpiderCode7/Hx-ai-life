package com.hxy.hxailife.advisor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.lang.Nullable;
import reactor.core.scheduler.Scheduler;

/**
 * A simple logger advisor that logs the request and response messages.
 *
 * @author Christian Tzolov
 */
@Slf4j
public class ReReadingAdvisor implements BaseAdvisor {

    private final int order;

    public ReReadingAdvisor() {
        this(0);
    }

    public ReReadingAdvisor(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        String userText = request.prompt().getUserMessage().getText();
        Map<String, Object> context = new HashMap<>(request.context());
        //给其他 advisor/逻辑读的，可选
        context.put("re2_input_query", userText);
        //渲染
        String newUserText = new PromptTemplate("""
                {re2_input_query}
                Read the question again:{re2_input_query}
                """)
                .render(Map.of("re2_input_query", userText));

        List<Message> messages = new ArrayList<>(request.prompt().getInstructions());
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage) {
                messages.set(i, new UserMessage(newUserText));
                break;
            }
        }

        Prompt newPrompt = new Prompt(messages, request.prompt().getOptions());
        return ChatClientRequest.builder()
                .prompt(newPrompt)
                .context(context)
                .build();
    }

    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain){
        return response;
    }

    protected void logRequest(ChatClientRequest request) {
        log.debug("request: {}", request);
    }

    protected void logResponse(ChatClientResponse chatClientResponse) {
        log.debug("response: {}", chatClientResponse.chatResponse());
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        return BaseAdvisor.super.adviseCall(chatClientRequest, callAdvisorChain);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        return BaseAdvisor.super.adviseStream(chatClientRequest, streamAdvisorChain);
    }

    @Override
    public String getName() {
        return BaseAdvisor.super.getName();
    }

    @Override
    public Scheduler getScheduler() {
        return BaseAdvisor.super.getScheduler();
    }

}

