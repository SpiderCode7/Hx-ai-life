package com.hxy.hxailife.travel;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum PlanItemType {
    DESTINATION,
    MEAL;

    @EnumValue
    private final String code = name();
}
