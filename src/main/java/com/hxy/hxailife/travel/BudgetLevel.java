package com.hxy.hxailife.travel;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum BudgetLevel {
    COST_EFFECTIVE,
    MEDIUM,
    HIGH,
    UNLIMITED,
    CUSTOM;

    @EnumValue
    private final String code = name();
}
