package com.hxy.hxailife.travel.vo;

import com.hxy.hxailife.travel.GenerationItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/** 攻略中的起点、去哪、吃啥和终点节点。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanGenerationItemVO {

    private Long id;
    @Schema(nullable = true, description = "普通聊天为空，攻略微调时为攻略结果 ID")
    private Long generationId;
    @NotNull
    private GenerationItemType type;
    private Integer sortOrder;
    @NotNull
    private LocalTime startTime;
    private LocalTime endTime;
    @Positive
    private Integer peopleNum;
    @NotBlank
    @Size(max = 255)
    private String name;
    @Size(max = 500)
    private String address;
    private String placeInfo;
    @Size(max = 2048)
    private String imageUrl;
    @Size(max = 50)
    private String poiSource;
    @Size(max = 128)
    private String poiId;
    private Boolean fixedByUser;
    @DecimalMin("0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal costPerPerson;
    private String transitPlan;
    @PositiveOrZero
    private Integer transitMinutes;
    @DecimalMin("0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal transitCostPerPerson;
    @Size(max = 500)
    private String xhsSearchQuery;
    @Size(max = 2048)
    private String xhsSearchUrl;
    @Size(max = 2048)
    private String dianpingUrl;
    private String remark;
}
