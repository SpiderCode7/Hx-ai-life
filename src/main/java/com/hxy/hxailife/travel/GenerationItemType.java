package com.hxy.hxailife.travel;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum GenerationItemType {
    START,
    DESTINATION,
    MEAL,
    END;

    @EnumValue
    private final String code = name();
}
