package com.hxy.hxailife.travel;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum GenerationAction {
    GENERATE,
    REGENERATE,
    REFINE;

    @EnumValue
    private final String code = name();
}
