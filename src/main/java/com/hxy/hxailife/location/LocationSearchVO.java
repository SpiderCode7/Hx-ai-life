package com.hxy.hxailife.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 前端地点选择所需的最小高德 POI 信息。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationSearchVO {
    private String poiId;
    private String name;
    private String address;
    private String city;
    private Double longitude;
    private Double latitude;
}
