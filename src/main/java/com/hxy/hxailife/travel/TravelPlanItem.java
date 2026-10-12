package com.hxy.hxailife.travel;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@TableName("travel_plan_items")
@Getter
@Setter
public class TravelPlanItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    @TableField("item_type")
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
