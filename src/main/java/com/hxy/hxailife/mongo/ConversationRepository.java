package com.hxy.hxailife.mongo;

import com.hxy.hxailife.mongo.entity.ConversationDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConversationRepository extends MongoRepository<ConversationDoc, String> {


}
