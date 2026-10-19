package com.hxy.hxailife.chat.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 左侧普通聊天历史列表使用的会话摘要。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationSummaryVO {
    private String conversationId;
    private String title;
    private LocalDateTime updatedAt;
}
