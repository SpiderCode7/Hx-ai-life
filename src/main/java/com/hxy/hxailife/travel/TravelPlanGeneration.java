package com.hxy.hxailife.travel;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 每次成功生成、换一套或微调均保存一版；每表单保留上限由业务层控制。 */
@Data
@TableName("travel_plan_generations")
public class TravelPlanGeneration {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 结果所属用户；表单删除后收藏结果仍靠此字段查询。 */
    private Long userId;
    private Long planId;
    private GenerationAction action;
    /** 换一套或微调时的来源结果 ID，首次生成为空；删除来源不删除后续版本。 */
    private Long parentGenerationId;
    /** 生成时的表单内容 SHA-256，用于隔离不同输入条件。 */
    private String formFingerprint;
    /** 推荐节点组合 SHA-256，不以文案措辞判断是否重复。 */
    private String recommendationFingerprint;
    private String title;
    /** 整份攻略的推荐理由。 */
    private String recommendationReason;
    private BigDecimal totalCostPerPerson;
    private String tips;
    /** 收藏具体这一版结果；收藏页可直接查询，不随换一套或微调自动转移。 */
    private Boolean favorite;
    /** 按 createdAt、id 排序浏览历史；收藏只修改标记，不改变结果顺序。 */
    private LocalDateTime createdAt;
}
