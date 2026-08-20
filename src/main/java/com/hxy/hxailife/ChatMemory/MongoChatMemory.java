package com.hxy.hxailife.ChatMemory;

import com.hxy.hxailife.mongo.MessageDoc;
import com.hxy.hxailife.mongo.MessageRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于 MongoDB 的 ChatMemory 实现：消息按字段映射成文档存储，
 * 可查询、可调试、Spring AI 升级不影响历史数据。
 */
@Component
public class MongoChatMemory implements ChatMemory {

    private final MessageRepository repository;

    public MongoChatMemory(MessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        for (Message message : messages) {
            MessageDoc doc = new MessageDoc();
            doc.setConversationId(conversationId);
            doc.setRole(message.getMessageType().name().toLowerCase());
            doc.setContent(message.getText());
            repository.save(doc);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        return repository.findByConversationIdOrderByTimestampAsc(conversationId)
                .stream()
                .map(MongoChatMemory::toSpringAiMessage)
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        repository.deleteByConversationId(conversationId);
    }

    private static Message toSpringAiMessage(MessageDoc doc) {
        return switch (MessageType.valueOf(doc.getRole().toUpperCase())) {
            case SYSTEM -> new SystemMessage(doc.getContent());
            case USER -> new UserMessage(doc.getContent());
            case ASSISTANT -> new AssistantMessage(doc.getContent());
            // 工具消息暂按普通文本还原，当前对话链路不涉及工具
            default -> new UserMessage(doc.getContent());
        };
    }
}
