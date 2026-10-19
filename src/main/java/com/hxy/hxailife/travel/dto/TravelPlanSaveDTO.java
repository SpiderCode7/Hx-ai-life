package com.hxy.hxailife.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hxy.hxailife.travel.TravelMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanSaveDTO {
    @Size(max = 255, message = "攻略标题不能超过 255 个字符")
    private String title;

    @NotNull(message = "行程日期不能为空")
    private LocalDate planDate;

    @NotBlank(message = "起始地点不能为空")
    @Size(max = 255, message = "起始地点不能超过 255 个字符")
    private String startPlace;

    @Size(max = 500, message = "起始地点地址不能超过 500 个字符")
    private String startPlaceAddress;

    @Size(max = 128, message = "起始地点 POI ID 不能超过 128 个字符")
    private String startPlacePoiId;
    private Double startPlaceLongitude;
    private Double startPlaceLatitude;

    @NotNull(message = "出发时间不能为空")
    private LocalTime startTime;

    @NotBlank(message = "返回地点不能为空")
    @Size(max = 255, message = "返回地点不能超过 255 个字符")
    private String endPlace;

    @Size(max = 500, message = "返回地点地址不能超过 500 个字符")
    private String endPlaceAddress;

    @Size(max = 128, message = "返回地点 POI ID 不能超过 128 个字符")
    private String endPlacePoiId;
    private Double endPlaceLongitude;
    private Double endPlaceLatitude;

    @NotNull(message = "最晚返回时间不能为空")
    private LocalTime latestReturnTime;

    @NotNull(message = "大概人数不能为空")
    @Positive(message = "大概人数至少为 1 人")
    private Integer peopleNum;

    /** 旧客户端仍可传单值 travelMode；新客户端使用 travelModes。 */
    private TravelMode travelMode;

    private List<TravelMode> travelModes = new ArrayList<>();

    @Min(value = 0, message = "大众点评开关只能为 0 或 1")
    @Max(value = 1, message = "大众点评开关只能为 0 或 1")
    private Integer dianpingEnabled = 0;

    @Min(value = 0, message = "小红书搜索开关只能为 0 或 1")
    @Max(value = 1, message = "小红书搜索开关只能为 0 或 1")
    private Integer xhsEnabled = 0;

    @Size(max = 1000, message = "全局备注不能超过 1000 个字符")
    private String specialRequirements;

    @NotEmpty(message = "至少添加一张去哪或吃啥卡片")
    private List<@NotNull(message = "卡片不能为空") @Valid TravelPlanItemDTO> items;

    @JsonIgnore
    @AssertTrue(message = "最晚返回时间必须晚于出发时间，仅支持当天行程")
    public boolean isTimeRangeValid() {
        return startTime == null || latestReturnTime == null || latestReturnTime.isAfter(startTime);
    }
}
