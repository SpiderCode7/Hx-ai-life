package com.hxy.hxailife.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.hxy.hxailife.travel.TravelPlanService;
import com.hxy.hxailife.travel.TravelPlanGenerationService;
import com.hxy.hxailife.travel.vo.TravelPlanGenerateVO;
import com.hxy.hxailife.travel.vo.TravelPlanGenerationVO;
import com.hxy.hxailife.travel.vo.TravelPlanDetailVO;
import com.hxy.hxailife.travel.dto.TravelPlanSaveDTO;
import com.hxy.hxailife.travel.vo.TravelPlanSummaryVO;
import com.hxy.hxailife.travel.vo.TravelPlanHistoryVO;
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
@RequestMapping("/travel-plans")
@SaCheckLogin
@Tag(name = "攻略表单")
public class TravelPlanController {

    private final TravelPlanService travelPlanService;
    private final TravelPlanGenerationService generationService;

    public TravelPlanController(TravelPlanService travelPlanService, TravelPlanGenerationService generationService) {
        this.travelPlanService = travelPlanService;
        this.generationService = generationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增历史表单，仅保存条件，不调用 AI")
    public TravelPlanDetailVO create(@Valid @RequestBody TravelPlanSaveDTO request) {
        return travelPlanService.create(request);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "提交表单并生成攻略，成功后保存表单、攻略和 Redis 缓存")
    public TravelPlanGenerateVO generate(@Valid @RequestBody TravelPlanSaveDTO request) {
        return generationService.submit(request);
    }

    @PostMapping("/{id}/generations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "按已保存表单继续生成，每份表单最多保存 5 套，暂不保证推荐去重")
    public TravelPlanGenerateVO generateForPlan(@PathVariable Long id) {
        return generationService.generate(id);
    }

    @GetMapping("/{id}/generations")
    @Operation(summary = "查看表单的全部成功攻略，按创建时间正序")
    public List<TravelPlanGenerationVO> generations(@PathVariable Long id) {
        return generationService.list(id);
    }

    @GetMapping
    @Operation(summary = "查询我的历史表单列表，按创建时间倒序")
    public List<TravelPlanSummaryVO> list() {
        return travelPlanService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询攻略表单详情")
    public TravelPlanDetailVO detail(@PathVariable Long id) {
        return travelPlanService.detail(id);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "打开历史表单，返回表单详情及该表单全部成功攻略，不重新调用 AI")
    public TravelPlanHistoryVO history(@PathVariable Long id) {
        return generationService.history(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改历史表单，完整替换输入条件，不覆盖已生成攻略")
    public TravelPlanDetailVO update(@PathVariable Long id,
                                           @Valid @RequestBody TravelPlanSaveDTO request) {
        return travelPlanService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除历史表单及未收藏攻略，已收藏攻略独立保留")
    public void delete(@PathVariable Long id) {
        travelPlanService.delete(id);
    }
}
