package com.hxy.hxailife.travel.dto;

import com.hxy.hxailife.travel.GenerationItemType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentRecommendationItemDTO {

    private GenerationItemType type;
    private String poiSource;
    private String poiId;
    private String name;
    private String address;
    private Boolean fixedByUser;
}
