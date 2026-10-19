package com.hxy.hxailife.travel.dto;

import com.hxy.hxailife.travel.vo.TravelPlanGenerationItemVO;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/** 换一个的模型输出：仅生成中间节点和本次返程，不生成数据库元数据及固定边界卡片。 */
public record TravelPlanChangeResultDTO(
        String title,
        String recommendationReason,
        BigDecimal totalCostPerPerson,
        String tips,
        List<TravelPlanGenerationItemVO> items,
        ReturnJourney returnJourney) {

    public record ReturnJourney(LocalTime arrivalTime, String transitPlan,
                                Integer transitMinutes, BigDecimal transitCostPerPerson) {
    }
}
