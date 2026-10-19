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

@TableName(value = "travel_plans", autoResultMap = true)
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
    private String startPlaceAddress;
    private String startPlacePoiId;
    private Double startPlaceLongitude;
    private Double startPlaceLatitude;
    private LocalTime startTime;
    private String endPlace;
    private String endPlaceAddress;
    private String endPlacePoiId;
    private Double endPlaceLongitude;
    private Double endPlaceLatitude;
    private LocalTime latestReturnTime;
    private Integer peopleNum;
    @TableField(value = "travel_mode", typeHandler = TravelModeListTypeHandler.class)
    private java.util.List<TravelMode> travelModes;
    /** 兼容旧代码和旧接口的首选出行方式，不直接映射数据库列。 */
    @TableField(exist = false)
    private TravelMode travelMode;
    private Integer dianpingEnabled;
    private Integer xhsEnabled;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String specialRequirements;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
