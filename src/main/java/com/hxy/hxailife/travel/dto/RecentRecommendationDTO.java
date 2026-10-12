package com.hxy.hxailife.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Redis List 中一条近期成功攻略摘要，不是 ChatMemory 的消息。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentRecommendationDTO {

    private Long generationId;
    private String recommendationFingerprint;
    private String summary;
    /** 按实际行程顺序保存去哪/吃啥节点，起点和终点不参与去重。 */
    private List<RecentRecommendationItemDTO> items;
}
