package com.hxy.hxailife.travel.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 前端保留 conversationId，继续同一个微调会话。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanRefineChatVO {
    private String conversationId;
    private Long generationId;
    private String reply;
}
