package com.hxy.hxailife.travel.vo;

import com.hxy.hxailife.travel.TravelMode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
public class TravelPlanDetailVO{
    private Long id;
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
    private TravelMode travelMode;
    private List<TravelMode> travelModes;
    private Integer dianpingEnabled;
    private Integer xhsEnabled;
    private String specialRequirements;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TravelPlanItemVO> items;

    public TravelPlanDetailVO(Long id, String title, LocalDate planDate, String startPlace,
                              LocalTime startTime, String endPlace, LocalTime latestReturnTime,
                              Integer peopleNum, TravelMode travelMode, Integer dianpingEnabled,
                              String specialRequirements, LocalDateTime createdAt,
                              LocalDateTime updatedAt, List<TravelPlanItemVO> items) {
        this.id = id;
        this.title = title;
        this.planDate = planDate;
        this.startPlace = startPlace;
        this.startTime = startTime;
        this.endPlace = endPlace;
        this.latestReturnTime = latestReturnTime;
        this.peopleNum = peopleNum;
        this.travelMode = travelMode;
        this.travelModes = travelMode == null ? List.of() : List.of(travelMode);
        this.dianpingEnabled = dianpingEnabled;
        this.specialRequirements = specialRequirements;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = items;
    }

    public TravelPlanDetailVO(Long id, String title, LocalDate planDate, String startPlace,
                              LocalTime startTime, String endPlace, LocalTime latestReturnTime,
                              Integer peopleNum, TravelMode travelMode, List<TravelMode> travelModes,
                              Integer dianpingEnabled, String specialRequirements,
                              LocalDateTime createdAt, LocalDateTime updatedAt,
                              List<TravelPlanItemVO> items) {
        this(id, title, planDate, startPlace, startTime, endPlace, latestReturnTime, peopleNum,
                travelMode, dianpingEnabled, specialRequirements, createdAt, updatedAt, items);
        this.travelModes = travelModes;
    }
}
