package com.hxy.hxailife.location;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
public class AmapLocationService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public AmapLocationService(RestClient.Builder builder, ObjectMapper objectMapper,
                               @Value("${amap.web-key:}") String apiKey) {
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public List<LocationSearchVO> search(String keywords, String city) {
        if (keywords == null || keywords.isBlank()) {
            return List.of();
        }
        JsonNode root = request(UriComponentsBuilder.fromUriString("https://restapi.amap.com/v3/place/text")
                .queryParam("keywords", keywords.trim())
                .queryParam("city", city == null ? "" : city.trim())
                .queryParam("citylimit", city != null && !city.isBlank())
                .queryParam("offset", 10)
                .queryParam("page", 1)
                .queryParam("extensions", "base"));
        List<LocationSearchVO> result = new ArrayList<>();
        for (JsonNode poi : root.path("pois")) {
            String id = text(poi, "id");
            String name = text(poi, "name");
            if (id == null || name == null) {
                continue;
            }
            Double[] location = location(poi.path("location").asText(null));
            result.add(new LocationSearchVO(id, name, text(poi, "address"), text(poi, "cityname"),
                    location[0], location[1]));
        }
        return result;
    }

    public List<LocationSearchVO> current(double longitude, double latitude) {
        if (!Double.isFinite(longitude) || !Double.isFinite(latitude)
                || longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "经纬度不正确");
        }
        JsonNode address = request(UriComponentsBuilder.fromUriString("https://restapi.amap.com/v3/geocode/regeo")
                .queryParam("location", longitude + "," + latitude)
                .queryParam("coordsys", "gcj02")
                .queryParam("radius", 1000)
                .queryParam("extensions", "all")).path("regeocode");
        String formattedAddress = text(address, "formatted_address");
        if (formattedAddress == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "未能识别当前位置，请手动选择地点");
        }
        JsonNode component = address.path("addressComponent");
        String city = text(component, "city");
        if (city == null) city = text(component, "province");
        List<LocationSearchVO> result = new ArrayList<>();
        String nearestName = text(address.path("pois").path(0), "name");
        // 定位点不是附近 POI：保留设备坐标，不能借用附近店铺的 POI ID。
        result.add(new LocationSearchVO(null, nearestName == null ? formattedAddress : nearestName + "附近",
                formattedAddress, city, longitude, latitude));
        for (JsonNode poi : address.path("pois")) {
            if (result.size() >= 11) break;
            String id = text(poi, "id");
            String name = text(poi, "name");
            Double[] point = location(poi.path("location").asText(null));
            if (id != null && name != null && point[0] != null && point[1] != null) {
                result.add(new LocationSearchVO(id, name, text(poi, "address"), city, point[0], point[1]));
            }
        }
        return result;
    }

    private JsonNode request(UriComponentsBuilder uri) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "高德地图搜索暂未配置");
        }
        try {
            String body = restClient.get().uri(uri.queryParam("key", apiKey).build().toUri())
                    .retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(body);
            if (!"1".equals(root.path("status").asText())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "高德地图搜索失败：" + root.path("info").asText("未知错误"));
            }
            return root;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "高德地图搜索失败，请稍后重试", exception);
        }
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Double[] location(String value) {
        if (value == null || value.isBlank() || !value.contains(",")) {
            return new Double[] {null, null};
        }
        try {
            String[] parts = value.split(",", 2);
            return new Double[] {Double.valueOf(parts[0]), Double.valueOf(parts[1])};
        } catch (NumberFormatException exception) {
            return new Double[] {null, null};
        }
    }
}
