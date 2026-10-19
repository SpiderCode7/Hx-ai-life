package com.hxy.hxailife.chatMemoryRepository;

import com.hxy.hxailife.mongo.entity.MessageDoc;
import com.hxy.hxailife.mongo.entity.ConversationDoc;
import com.hxy.hxailife.mongo.ConversationRepository;
import com.hxy.hxailife.mongo.MessageRepository;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MongoChatMemoryRepository implements ChatMemoryRepository {

    private final MessageRepository repository;
    private final MongoTemplate mongoTemplate;
    private final ConversationRepository conversationRepository;

    public MongoChatMemoryRepository(MessageRepository repository, MongoTemplate mongoTemplate,
                                     ConversationRepository conversationRepository) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.conversationRepository = conversationRepository;
    }

    @Override
    public List<String> findConversationIds() {
        return mongoTemplate.query(MessageDoc.class)
                .distinct("conversationId")
                .as(String.class)
                .all();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return repository.findByConversationIdOrderByTimestampAsc(conversationId)
                .stream()
                .map(MongoChatMemoryRepository::toSpringAiMessage)
                .toList();
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // 保存时从会话取得所属用户，一次查询即可；不依赖当前请求线程的登录状态。
        Long userId = conversationRepository.findById(conversationId)
                .map(ConversationDoc::getUserId)
                .orElseThrow(() -> new IllegalStateException("会话不存在或未关联用户，无法保存聊天记忆"));
        repository.deleteByConversationId(conversationId);
        repository.saveAll(messages.stream()
                .map(message -> toMessageDoc(conversationId, userId, message))
                .toList());
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        repository.deleteByConversationId(conversationId);
    }

    private static MessageDoc toMessageDoc(String conversationId, Long userId, Message message) {
        MessageDoc doc = new MessageDoc();
        doc.setConversationId(conversationId);
        doc.setUserId(userId);
        doc.setRole(message.getMessageType().name().toLowerCase());
        doc.setContent(message.getText());
        return doc;
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
