package com.hxy.hxailife.mongo;

import com.hxy.hxailife.mongo.entity.MessageDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MessageRepository extends MongoRepository<MessageDoc, String> {

    /** 按时间正序取出一个会话的全部消息 */
    List<MessageDoc> findByConversationIdOrderByTimestampAsc(String conversationId);

    /** 清空某个会话的历史 */
    void deleteByConversationId(String conversationId);
}
