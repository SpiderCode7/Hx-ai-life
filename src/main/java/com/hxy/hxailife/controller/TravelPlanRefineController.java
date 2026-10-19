package com.hxy.hxailife.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.hxy.hxailife.common.ChatMessageDTO;
import com.hxy.hxailife.travel.dto.TravelPlanGenerateOptionsDTO;
import com.hxy.hxailife.travel.TravelPlanRefineService;
import com.hxy.hxailife.travel.vo.TravelPlanRefineChatVO;
import com.hxy.hxailife.travel.vo.TravelPlanRefineGenerateVO;
import com.hxy.hxailife.travel.vo.TravelPlanRefineSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SaCheckLogin
@Tag(name = "攻略微调")
@RequestMapping("/travel-plans/{planId}/generations/{generationId}/refine")
public class TravelPlanRefineController {

    private final TravelPlanRefineService refineService;

    public TravelPlanRefineController(TravelPlanRefineService refineService) {
        this.refineService = refineService;
    }

    @PostMapping("/chat")
    @Operation(summary = "微调聊天，仅保存 Mongo 聊天消息；首次可不传 conversationId")
    public TravelPlanRefineChatVO chat(@PathVariable Long planId, @PathVariable Long generationId,
                                     @Valid @RequestBody ChatMessageDTO request) {
        return refineService.chat(planId, generationId, request);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "根据聊天记忆微调生成，保存 MySQL、Redis；message 可为空")
    public TravelPlanRefineGenerateVO generate(@PathVariable Long planId, @PathVariable Long generationId,
                                             @Valid @RequestBody TravelPlanGenerateOptionsDTO request) {
        return refineService.generate(planId, generationId, request);
    }

    @GetMapping("/session")
    @Operation(summary = "打开微调聊天框，读取会话信息和最近聊天记录")
    public TravelPlanRefineSessionVO session(@PathVariable Long planId, @PathVariable Long generationId,
                                             @RequestParam(required = false) String conversationId) {
        return refineService.session(planId, generationId, conversationId);
    }
}
