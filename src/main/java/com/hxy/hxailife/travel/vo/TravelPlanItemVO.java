package com.hxy.hxailife.travel.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.hxy.hxailife.travel.BudgetLevel;
import com.hxy.hxailife.travel.PlanItemType;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanItemVO {
    private Long id;

    private PlanItemType type;

    private Integer sortOrder;

    private String name;

    private String place;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer peopleNum;

    private String preference;

    private BudgetLevel budgetLevel;

    private BigDecimal customBudgetAmount;

    private String remark;

}
