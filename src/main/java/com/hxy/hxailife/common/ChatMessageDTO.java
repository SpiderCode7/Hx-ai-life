package com.hxy.hxailife.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    private String conversationId;

    /** 微调生成可以不传本次消息，由已有会话记忆驱动重新生成。 */
    @Size(max = 500, message = "聊天内容不能超过 500 个字符")
    private String message;

    /** 普通聊天的联网按钮状态；不传或 null 均视为关闭，不影响攻略微调。 */
    private Boolean webSearchEnabled = false;

    /** 普通聊天的深度模式：启用轻量 ReAct；不传或 null 均关闭，与联网开关独立。 */
    private Boolean deepThinkingEnabled = false;
}
