package com.hxy.hxailife.travel.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** generation 已在 MySQL 提交；缓存或记忆写入失败不应再次点击生成。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanRefineGenerateVO {
    private String conversationId;
    private TravelPlanGenerationVO generation;
    private boolean cached;
    private boolean memorySaved;
}
