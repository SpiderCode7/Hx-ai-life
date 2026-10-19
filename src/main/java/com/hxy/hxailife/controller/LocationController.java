package com.hxy.hxailife.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.hxy.hxailife.location.AmapLocationService;
import com.hxy.hxailife.location.LocationSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@SaCheckLogin
@RequestMapping("/locations")
@Tag(name = "地点搜索")
public class LocationController {

    private final AmapLocationService locationService;

    public LocationController(AmapLocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/search")
    @Operation(summary = "搜索高德地点并返回可选 POI")
    public List<LocationSearchVO> search(@RequestParam String keywords,
                                         @RequestParam(required = false) String city) {
        return locationService.search(keywords, city);
    }

    @GetMapping("/current")
    @Operation(summary = "根据 GCJ-02 坐标查询当前位置地址及附近 POI")
    public List<LocationSearchVO> current(@RequestParam double longitude, @RequestParam double latitude) {
        return locationService.current(longitude, latitude);
    }
}
