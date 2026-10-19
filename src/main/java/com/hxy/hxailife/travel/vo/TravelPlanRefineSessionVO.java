package com.hxy.hxailife.travel.vo;

import com.hxy.hxailife.common.ChatHistoryMessageVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 打开攻略微调聊天框时返回的会话和历史消息。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanRefineSessionVO {

    /** 尚未发送过消息时为空；发送第一条消息后由 chat 接口返回。 */
    private String conversationId;
    private Long planId;
    private Long generationId;
    private String title;
    private List<ChatHistoryMessageVO> messages;
}
