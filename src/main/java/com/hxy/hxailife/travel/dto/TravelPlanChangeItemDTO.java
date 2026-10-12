package com.hxy.hxailife.travel.dto;

import com.hxy.hxailife.travel.GenerationItemType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 历史攻略中已经使用过的地点或餐厅的精简信息。
 * 仅用于换方案时生成排除列表，不落库。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanChangeItemDTO {

    private GenerationItemType type;

    @Size(max = 255, message = "历史候选名称不能超过 255 个字符")
    private String name;

    @Size(max = 255, message = "历史候选地址不能超过 255 个字符")
    private String address;

    @Size(max = 128, message = "历史候选 POI ID 不能超过 128 个字符")
    private String poiId;
}
