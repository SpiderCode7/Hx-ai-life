package com.hxy.hxailife.travel;

import java.util.List;
import java.util.stream.Collectors;

/** 将表单中的多选出行方式转换为模型可直接执行的最终约束。 */
public final class TravelModePolicy {

    private TravelModePolicy() {
    }

    public static String describe(List<TravelMode> modes) {
        List<TravelMode> concrete = modes == null ? List.of() : modes.stream()
                .filter(mode -> mode != TravelMode.AUTO)
                .distinct()
                .toList();
        if (concrete.isEmpty()) {
            return "仅选择 AI 安排：可在公共交通、打车、自驾等合理方式中自由安排";
        }
        String allowed = concrete.stream()
                .map(TravelModePolicy::label)
                .collect(Collectors.joining("、"));
        return "最终只允许使用：" + allowed + "；AI 安排仅负责在这些已选方式中分配，禁止使用其他方式";
    }

    private static String label(TravelMode mode) {
        return switch (mode) {
            case PUBLIC_TRANSIT -> "公共交通";
            case TAXI -> "打车";
            case SELF_DRIVING -> "自驾";
            case AUTO -> "AI 安排";
        };
    }
}
