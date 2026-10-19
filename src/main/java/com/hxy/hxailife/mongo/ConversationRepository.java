package com.hxy.hxailife.mongo;

import com.hxy.hxailife.mongo.entity.ConversationDoc;
import com.hxy.hxailife.mongo.entity.ConversationType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ConversationRepository extends MongoRepository<ConversationDoc, String> {
    List<ConversationDoc> findByUserIdAndTypeOrderByUpdatedAtDesc(Long userId, ConversationType type);
}
