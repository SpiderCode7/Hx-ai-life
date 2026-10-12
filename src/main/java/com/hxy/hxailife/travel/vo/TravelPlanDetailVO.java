package com.hxy.hxailife.travel.vo;

import com.hxy.hxailife.travel.TravelMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanDetailVO{
    private Long id;
    private String title;
    private LocalDate planDate;
    private String startPlace;
    private LocalTime startTime;
    private String endPlace;
    private LocalTime latestReturnTime;
    private Integer peopleNum;
    private TravelMode travelMode;
    private Integer dianpingEnabled;
    private String specialRequirements;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TravelPlanItemVO> items;
}
