package com.hxy.hxailife.mongo.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document("conversations")
@CompoundIndex(name = "idx_conversations_user_updated", def = "{'userId': 1, 'updatedAt': -1}")
public class ConversationDoc {

    @Id
    private String id;
    private Long userId;
    private ConversationType type;
    private String title;
    /** 以下三个字段仅攻略微调会话填写；普通聊天为空。 */
    private Long planId;
    /** 微调会话开始时关联的初始攻略版本。 */
    private Long initialGenerationId;
    /** 微调成功后更新为新结果方案ID；换一套创建独立会话。 */
    private Long currentGenerationId;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = createdAt;
}
