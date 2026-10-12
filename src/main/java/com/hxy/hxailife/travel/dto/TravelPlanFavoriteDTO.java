package com.hxy.hxailife.travel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanFavoriteDTO {

    @NotNull(message = "收藏状态不能为空")
    private Boolean favorite;
}
