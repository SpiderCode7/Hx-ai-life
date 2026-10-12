package com.hxy.hxailife.travel.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** MySQL 已提交的攻略及本次 Redis 写入状态；cached=false 时不要重新生成。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanGenerateVO {
    private TravelPlanGenerationVO generation;
    private boolean cached;
}
