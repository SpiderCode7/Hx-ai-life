package com.hxy.hxailife.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.hxy.hxailife.travel.TravelPlanFavoriteService;
import com.hxy.hxailife.travel.dto.TravelPlanFavoriteDTO;
import com.hxy.hxailife.travel.vo.TravelPlanGenerationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/travel-plan-favorites")
@SaCheckLogin
@Tag(name = "攻略收藏")
public class TravelPlanFavoriteController {

    private final TravelPlanFavoriteService favoriteService;

    public TravelPlanFavoriteController(TravelPlanFavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/{generationId}")
    @Operation(summary = "收藏指定攻略，重复收藏不新增记录")
    public TravelPlanGenerationVO add(@PathVariable Long generationId) {
        return favoriteService.setFavorite(generationId, true);
    }

    @DeleteMapping("/{generationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "取消收藏，不删除攻略或释放生成名额")
    public void remove(@PathVariable Long generationId) {
        favoriteService.setFavorite(generationId, false);
    }

    @PutMapping("/{generationId}")
    @Operation(summary = "修改攻略的收藏状态，不修改攻略内容")
    public TravelPlanGenerationVO update(@PathVariable Long generationId,
                                         @Valid @RequestBody TravelPlanFavoriteDTO request) {
        return favoriteService.setFavorite(generationId, request.getFavorite());
    }

    @GetMapping
    @Operation(summary = "查询我的收藏攻略，按攻略创建时间倒序")
    public List<TravelPlanGenerationVO> list() {
        return favoriteService.list();
    }

    @GetMapping("/{generationId}")
    @Operation(summary = "查看收藏攻略详情，来源表单删除后仍可查看")
    public TravelPlanGenerationVO detail(@PathVariable Long generationId) {
        return favoriteService.detail(generationId);
    }
}
