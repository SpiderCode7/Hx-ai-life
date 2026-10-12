package com.hxy.hxailife.travel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * “换一个”请求对象。
 *
 * 原始表单直接复用 TravelPlanSaveDTO，避免维护两套相同的表单字段。
 * excludedItems 由后端根据历史生成结果提取，用于告诉大模型本次需要避开的候选。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanChangeDTO {

    @Valid
    @NotNull(message = "原始表单不能为空")
    private TravelPlanSaveDTO form;

    @Valid
    private List<TravelPlanChangeItemDTO> excludedItems = new ArrayList<>();
}
