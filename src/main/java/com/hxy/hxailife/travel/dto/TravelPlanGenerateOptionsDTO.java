package com.hxy.hxailife.travel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 本次换方案或微调生成的搜索选项；未传时沿用已保存表单。 */
@Data
public class TravelPlanGenerateOptionsDTO {
    private String conversationId;

    @Size(max = 500, message = "聊天内容不能超过 500 个字符")
    private String message;

    @Min(0)
    @Max(1)
    private Integer xhsEnabled;

    @Min(0)
    @Max(1)
    private Integer dianpingEnabled;
}
