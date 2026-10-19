package com.hxy.hxailife.travel.vo;

import com.hxy.hxailife.travel.GenerationAction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 可直接返回前端的单套攻略结果。数据库元数据由后端生成，不要求模型填写。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanGenerationVO {

    private Long id;
    private Long planId;
    private GenerationAction action;
    private Long parentGenerationId;
    @NotBlank
    @Size(max = 255)
    private String title;
    /** 整份攻略的推荐理由。 */
    private String recommendationReason;
    @DecimalMin("0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal totalCostPerPerson;
    private String tips;
    private Boolean favorite;
    private LocalDateTime createdAt;
    @NotEmpty
    private List<@NotNull @Valid TravelPlanGenerationItemVO> items;
}
