package com.hxy.hxailife.travel;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum TravelMode {
    AUTO,
    PUBLIC_TRANSIT,
    TAXI,
    SELF_DRIVING;

    @EnumValue
    private final String code = name();
}
