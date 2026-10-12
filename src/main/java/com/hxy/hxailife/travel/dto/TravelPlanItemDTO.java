package com.hxy.hxailife.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hxy.hxailife.travel.BudgetLevel;
import com.hxy.hxailife.travel.PlanItemType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanItemDTO {
    @NotNull(message = "卡片类型不能为空")
    private PlanItemType type;

    @Size(max = 255, message = "卡片名称不能超过 255 个字符")
    private String name;

    @Size(max = 255, message = "卡片地点不能超过 255 个字符")
    private String place;

    private LocalTime startTime;

    private LocalTime endTime;

    @Positive(message = "卡片人数至少为 1 人")
    private Integer peopleNum;

    @Size(max = 500, message = "活动或餐饮偏好不能超过 500 个字符")
    private String preference;

    @NotNull(message = "预算档位不能为空")
    private BudgetLevel budgetLevel;

    @DecimalMin(value = "0.00", message = "自定义预算不能为负数")
    @Digits(integer = 8, fraction = 2, message = "预算最多为 8 位整数和 2 位小数")
    private BigDecimal customBudgetAmount;

    @Size(max = 1000, message = "卡片备注不能超过 1000 个字符")
    private String remark;

    @JsonIgnore
    @AssertTrue(message = "自定义档位必须填写金额，其他档位不能填写金额")
    public boolean isBudgetValid() {
        return budgetLevel == null || (budgetLevel == BudgetLevel.CUSTOM
                ? customBudgetAmount != null : customBudgetAmount == null);
    }

    @JsonIgnore
    @AssertTrue(message = "卡片结束时间必须晚于开始时间，仅支持当天行程")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
