package com.hxy.hxailife.chat.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 普通聊天发送接口返回值。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatReplyVO {
    private String conversationId;
    private String reply;
}
