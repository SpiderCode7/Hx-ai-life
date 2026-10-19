package com.hxy.hxailife.chat.vo;

import com.hxy.hxailife.common.ChatHistoryMessageVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 普通聊天页面初始化数据。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionVO {
    private String conversationId;
    private String title;
    private List<ChatHistoryMessageVO> messages;
}
