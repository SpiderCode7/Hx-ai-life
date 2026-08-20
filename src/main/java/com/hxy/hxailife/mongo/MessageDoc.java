package com.hxy.hxailife.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document("messages")
public class MessageDoc {

    @Id
    private String id;

    /** 会话 ID，对应 ChatMemory.CONVERSATION_ID */
    private String conversationId;

    /** user / assistant / system */
    private String role;

    /** 消息内容 */
    private String content;

    private LocalDateTime timestamp = LocalDateTime.now();
}
