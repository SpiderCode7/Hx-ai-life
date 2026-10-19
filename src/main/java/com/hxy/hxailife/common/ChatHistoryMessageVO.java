package com.hxy.hxailife.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 普通聊天和攻略微调共用的历史消息展示对象。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryMessageVO {
    private String role;
    private Long generationId;
    private String content;
    private LocalDateTime timestamp;
}
