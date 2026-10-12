package com.hxy.hxailife.travel;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hxy.hxailife.travel.vo.TravelPlanDetailVO;
import com.hxy.hxailife.travel.vo.TravelPlanItemVO;
import org.springframework.beans.BeanUtils;
import com.hxy.hxailife.travel.dto.TravelPlanItemDTO;
import com.hxy.hxailife.travel.dto.TravelPlanSaveDTO;
import com.hxy.hxailife.travel.vo.TravelPlanSummaryVO;
import com.hxy.hxailife.mapper.TravelPlanItemMapper;
import com.hxy.hxailife.mapper.TravelPlanMapper;
import com.hxy.hxailife.mapper.TravelPlanGenerationMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TravelPlanService {

    private final TravelPlanMapper planMapper;
    private final TravelPlanItemMapper itemMapper;
    private final TravelPlanGenerationMapper generationMapper;

    public TravelPlanService(TravelPlanMapper planMapper, TravelPlanItemMapper itemMapper,
                             TravelPlanGenerationMapper generationMapper) {
        this.planMapper = planMapper;
        this.itemMapper = itemMapper;
        this.generationMapper = generationMapper;
    }

    @Transactional
    public TravelPlanDetailVO create(TravelPlanSaveDTO request) {
        TravelPlan plan = new TravelPlan();
        plan.setUserId(StpUtil.getLoginIdAsLong());
        applyRequest(plan, request);
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(plan.getCreatedAt());
        planMapper.insert(plan);
        return toDetailVO(plan, insertItems(plan.getId(), request.getItems()));
    }

    @Transactional(readOnly = true)
    public List<TravelPlanSummaryVO> list() {
        return planMapper.selectList(new LambdaQueryWrapper<TravelPlan>()
                        .eq(TravelPlan::getUserId, StpUtil.getLoginIdAsLong())
                        .orderByDesc(TravelPlan::getCreatedAt, TravelPlan::getId))
                .stream().map(this::toSummaryVO).toList();
    }

    @Transactional(readOnly = true)
    public TravelPlanDetailVO detail(Long id) {
        TravelPlan plan = requireOwnedPlan(id, StpUtil.getLoginIdAsLong());
        List<TravelPlanItem> items = itemMapper.selectList(new LambdaQueryWrapper<TravelPlanItem>()
                .eq(TravelPlanItem::getPlanId, plan.getId())
                .orderByAsc(TravelPlanItem::getSortOrder, TravelPlanItem::getId));
        return toDetailVO(plan, items);
    }

    @Transactional
    public TravelPlanDetailVO update(Long id, TravelPlanSaveDTO request) {
        Long userId = StpUtil.getLoginIdAsLong();
        TravelPlan plan = requireOwnedPlan(id, userId, true);
        applyRequest(plan, request);
        plan.setUpdatedAt(LocalDateTime.now());
        if (planMapper.update(plan, new LambdaUpdateWrapper<TravelPlan>()
                .eq(TravelPlan::getId, id).eq(TravelPlan::getUserId, userId)) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "攻略表单不存在");
        }
        // 整份表单提交：事务内替换卡片，排序以请求数组为准。
        itemMapper.delete(new LambdaQueryWrapper<TravelPlanItem>().eq(TravelPlanItem::getPlanId, id));
        return toDetailVO(plan, insertItems(id, request.getItems()));
    }

    @Transactional
    public void delete(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        requireOwnedPlan(id, userId, true);

        // 未收藏攻略及其节点直接删除；收藏攻略脱离表单后继续保留。
        generationMapper.delete(new LambdaQueryWrapper<TravelPlanGeneration>()
                .eq(TravelPlanGeneration::getPlanId, id)
                .eq(TravelPlanGeneration::getUserId, userId)
                .eq(TravelPlanGeneration::getFavorite, false));
        generationMapper.update(null, new LambdaUpdateWrapper<TravelPlanGeneration>()
                .eq(TravelPlanGeneration::getPlanId, id)
                .eq(TravelPlanGeneration::getUserId, userId)
                .eq(TravelPlanGeneration::getFavorite, true)
                .set(TravelPlanGeneration::getPlanId, null));

        itemMapper.delete(new LambdaQueryWrapper<TravelPlanItem>().eq(TravelPlanItem::getPlanId, id));
        if (planMapper.delete(new LambdaQueryWrapper<TravelPlan>()
                .eq(TravelPlan::getId, id).eq(TravelPlan::getUserId, userId)) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "攻略表单不存在");
        }
    }

    private TravelPlan requireOwnedPlan(Long id, Long userId) {
        return requireOwnedPlan(id, userId, false);
    }

    private TravelPlan requireOwnedPlan(Long id, Long userId, boolean lock) {
        TravelPlan plan = planMapper.selectOne(new LambdaQueryWrapper<TravelPlan>()
                .eq(TravelPlan::getId, id).eq(TravelPlan::getUserId, userId)
                .last(lock ? "FOR UPDATE" : ""));
        if (plan == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "攻略表单不存在");
        }
        return plan;
    }

    private TravelPlanDetailVO toDetailVO(TravelPlan plan, List<TravelPlanItem> items) {
        TravelPlanDetailVO vo = new TravelPlanDetailVO();
        BeanUtils.copyProperties(plan, vo);
        vo.setItems(items.stream().map(item -> {
            TravelPlanItemVO itemVO = new TravelPlanItemVO();
            BeanUtils.copyProperties(item, itemVO);
            return itemVO;
        }).toList());
        return vo;
    }

    private TravelPlanSummaryVO toSummaryVO(TravelPlan plan) {
        TravelPlanSummaryVO vo = new TravelPlanSummaryVO();
        BeanUtils.copyProperties(plan, vo);
        return vo;
    }

    private void applyRequest(TravelPlan plan, TravelPlanSaveDTO request) {
        plan.setTitle(request.getTitle());
        plan.setPlanDate(request.getPlanDate());
        plan.setStartPlace(request.getStartPlace().trim());
        plan.setStartTime(request.getStartTime());
        plan.setEndPlace(request.getEndPlace().trim());
        plan.setLatestReturnTime(request.getLatestReturnTime());
        plan.setPeopleNum(request.getPeopleNum());
        plan.setTravelMode(request.getTravelMode() == null ? TravelMode.AUTO : request.getTravelMode());
        plan.setDianpingEnabled(request.getDianpingEnabled() == null ? 0 : request.getDianpingEnabled());
        plan.setSpecialRequirements(request.getSpecialRequirements());
    }

    private List<TravelPlanItem> insertItems(Long planId, List<TravelPlanItemDTO> requests) {
        List<TravelPlanItem> items = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            TravelPlanItemDTO request = requests.get(i);
            TravelPlanItem item = new TravelPlanItem();
            item.setPlanId(planId);
            item.setType(request.getType());
            item.setSortOrder(i);
            item.setName(request.getName());
            item.setPlace(request.getPlace());
            item.setStartTime(request.getStartTime());
            item.setEndTime(request.getEndTime());
            item.setPeopleNum(request.getPeopleNum());
            item.setPreference(request.getPreference());
            item.setBudgetLevel(request.getBudgetLevel());
            item.setCustomBudgetAmount(request.getCustomBudgetAmount());
            item.setRemark(request.getRemark());
            itemMapper.insert(item);
            items.add(item);
        }
        return items;
    }
}
