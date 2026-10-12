package com.hxy.hxailife.travel;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@TableName("travel_plans")
@Getter
@Setter
public class TravelPlan {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String title;
    private LocalDate planDate;
    private String startPlace;
    private LocalTime startTime;
    private String endPlace;
    private LocalTime latestReturnTime;
    private Integer peopleNum;
    private TravelMode travelMode;
    private Integer dianpingEnabled;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String specialRequirements;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
