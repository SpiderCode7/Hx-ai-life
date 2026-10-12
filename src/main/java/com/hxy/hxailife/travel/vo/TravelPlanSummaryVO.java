package com.hxy.hxailife.travel.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanSummaryVO {
    private Long id;

    private String title;

    private LocalDate planDate;

    private String startPlace;

    private String endPlace;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
