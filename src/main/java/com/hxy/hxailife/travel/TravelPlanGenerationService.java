package com.hxy.hxailife.travel;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxy.hxailife.app.PlanApp;
import com.hxy.hxailife.mapper.TravelPlanGenerationItemMapper;
import com.hxy.hxailife.mapper.TravelPlanGenerationMapper;
import com.hxy.hxailife.mapper.TravelPlanMapper;
import com.hxy.hxailife.travel.dto.TravelPlanItemDTO;
import com.hxy.hxailife.travel.dto.TravelPlanSaveDTO;
import com.hxy.hxailife.travel.vo.TravelPlanDetailVO;
import com.hxy.hxailife.travel.vo.TravelPlanGenerateVO;
import com.hxy.hxailife.travel.vo.TravelPlanGenerationItemVO;
import com.hxy.hxailife.travel.vo.TravelPlanGenerationVO;
import com.hxy.hxailife.travel.vo.TravelPlanHistoryVO;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Slf4j
public class TravelPlanGenerationService {

    public static final int MAX_RESULTS_PER_PLAN = 5;

    private final TravelPlanService planService;
    private final PlanApp planApp;
    private final TravelPlanMapper planMapper;
    private final TravelPlanGenerationMapper generationMapper;
    private final TravelPlanGenerationItemMapper itemMapper;
    private final TravelPlanResultCache cache;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final TransactionTemplate transaction;

    public TravelPlanGenerationService(TravelPlanService planService, PlanApp planApp,
                                      TravelPlanMapper planMapper, TravelPlanGenerationMapper generationMapper,
                                      TravelPlanGenerationItemMapper itemMapper, TravelPlanResultCache cache,
                                      ObjectMapper objectMapper, Validator validator,
                                      PlatformTransactionManager transactionManager) {
        this.planService = planService;
        this.planApp = planApp;
        this.planMapper = planMapper;
        this.generationMapper = generationMapper;
        this.itemMapper = itemMapper;
        this.cache = cache;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.transaction = new TransactionTemplate(transactionManager);
        // 等待行锁后读取最新已提交数量，不使用旧的一致性读快照。
        this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    /** 模型成功后用短事务一起保存表单和结果，失败不产生空历史。 */
    public TravelPlanGenerateVO submit(TravelPlanSaveDTO request) {
        Long userId = StpUtil.getLoginIdAsLong();
        TravelPlanSaveDTO form = normalize(request);
        String fingerprint = fingerprint(form);
        TravelPlanGenerationVO generated = callModel(form);
        TravelPlanGenerationVO saved = transaction.execute(status -> {
            Long planId = planService.create(form).getId();
            return save(userId, planId, fingerprint, generated, GenerationAction.GENERATE);
        });
        return new TravelPlanGenerateVO(saved, cache.save(userId, fingerprint, saved));
    }

    /** 可用于原表单重试/继续生成；推荐去重属于后续“换一套”流程。 */
    public TravelPlanGenerateVO generate(Long planId) {
        Long userId = StpUtil.getLoginIdAsLong();
        TravelPlanSaveDTO form = transaction.execute(status -> {
            lockOwnedPlan(planId, userId);
            requireCapacity(planId, userId);
            return savedForm(planId);
        });
        String fingerprint = fingerprint(form);
        TravelPlanGenerationVO generated = callModel(form);
        TravelPlanGenerationVO saved = transaction.execute(status -> {
            lockOwnedPlan(planId, userId);
            long count = requireCapacity(planId, userId);
            if (!fingerprint.equals(fingerprint(savedForm(planId)))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "生成期间表单已修改，请按最新表单重新生成");
            }
            return save(userId, planId, fingerprint, generated,
                    count == 0 ? GenerationAction.GENERATE : GenerationAction.REGENERATE);
        });
        // 必须在 MySQL 事务提交之后写缓存。
        return new TravelPlanGenerateVO(saved, cache.save(userId, fingerprint, saved));
    }

    /** 一次返回历史表单和所属攻略；同一读取快照避免查询期间修改或删除造成混合结果。 */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public TravelPlanHistoryVO history(Long planId) {
        TravelPlanDetailVO form = planService.detail(planId);
        return new TravelPlanHistoryVO(form, list(planId));
    }

    @Transactional(readOnly = true)
    public List<TravelPlanGenerationVO> list(Long planId) {
        planService.detail(planId);
        return generationMapper.selectList(new LambdaQueryWrapper<TravelPlanGeneration>()
                        .eq(TravelPlanGeneration::getPlanId, planId)
                        .eq(TravelPlanGeneration::getUserId, StpUtil.getLoginIdAsLong())
                        .orderByAsc(TravelPlanGeneration::getCreatedAt, TravelPlanGeneration::getId))
                .stream().map(generation -> {
                    TravelPlanGenerationVO vo = new TravelPlanGenerationVO();
                    BeanUtils.copyProperties(generation, vo);
                    vo.setItems(itemMapper.selectList(new LambdaQueryWrapper<TravelPlanGenerationItem>()
                                    .eq(TravelPlanGenerationItem::getGenerationId, generation.getId())
                                    .orderByAsc(TravelPlanGenerationItem::getSortOrder))
                            .stream().map(item -> {
                                TravelPlanGenerationItemVO itemVO = new TravelPlanGenerationItemVO();
                                BeanUtils.copyProperties(item, itemVO);
                                return itemVO;
                            }).toList());
                    return vo;
                }).toList();
    }

    private TravelPlanGenerationVO callModel(TravelPlanSaveDTO form) {
        TravelPlanGenerationVO result;
        try {
            result = planApp.strategy(form);
        } catch (RuntimeException exception) {
            log.warn("攻略模型调用或结构化解析失败", exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "攻略生成失败，请稍后重试");
        }
        validateResult(form, result);
        return result;
    }

    private void validateResult(TravelPlanSaveDTO form, TravelPlanGenerationVO result) {
        if (result == null || !validator.validate(result).isEmpty()
                || result.getItems().size() != form.getItems().size() + 2) {
            invalidResult();
        }
        List<TravelPlanGenerationItemVO> items = result.getItems();
        if (items.getFirst().getType() != GenerationItemType.START
                || items.getLast().getType() != GenerationItemType.END
                || !form.getStartTime().equals(items.getFirst().getStartTime())) {
            invalidResult();
        }
        LocalTime previousEnd = form.getStartTime();
        for (int i = 0; i < items.size(); i++) {
            TravelPlanGenerationItemVO item = items.get(i);
            if (item.getStartTime().isBefore(previousEnd)
                    || item.getStartTime().isAfter(form.getLatestReturnTime())
                    || (item.getEndTime() != null && (item.getEndTime().isBefore(item.getStartTime())
                    || item.getEndTime().isAfter(form.getLatestReturnTime())))) {
                invalidResult();
            }
            previousEnd = item.getEndTime() == null ? item.getStartTime() : item.getEndTime();
            Integer people = form.getPeopleNum();
            boolean fixed = i == 0 || i == items.size() - 1;
            if (!fixed) {
                TravelPlanItemDTO card = form.getItems().get(i - 1);
                if (item.getType() != GenerationItemType.valueOf(card.getType().name())
                        || (card.getStartTime() != null && !card.getStartTime().equals(item.getStartTime()))
                        || (card.getEndTime() != null && !card.getEndTime().equals(item.getEndTime()))
                        || (card.getCustomBudgetAmount() != null && item.getCostPerPerson() != null
                        && item.getCostPerPerson().compareTo(card.getCustomBudgetAmount()) > 0)) {
                    invalidResult();
                }
                if (card.getPeopleNum() != null) {
                    people = card.getPeopleNum();
                }
                // 卡片 name 也可能是“午餐”等标签，只对与实际推荐名称一致的目标标记固定。
                fixed = StringUtils.hasText(card.getName()) && card.getName().equals(item.getName().trim());
            }
            if (item.getPeopleNum() != null && !people.equals(item.getPeopleNum())) {
                invalidResult();
            }
            item.setPeopleNum(people);
            item.setFixedByUser(fixed);
            item.setSortOrder(i);
            if (!Integer.valueOf(1).equals(form.getDianpingEnabled())) {
                item.setDianpingUrl(null);
            }
        }
        items.getFirst().setName(form.getStartPlace());
        items.getLast().setName(form.getEndPlace());
    }

    private void invalidResult() {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "模型返回的攻略不完整或不符合表单约束，请重新生成");
    }

    private TravelPlanGenerationVO save(Long userId, Long planId, String formFingerprint,
                                        TravelPlanGenerationVO result, GenerationAction action) {
        TravelPlanGeneration generation = new TravelPlanGeneration();
        BeanUtils.copyProperties(result, generation);
        generation.setId(null);
        generation.setUserId(userId);
        generation.setPlanId(planId);
        generation.setAction(action);
        generation.setParentGenerationId(null);
        generation.setFormFingerprint(formFingerprint);
        generation.setRecommendationFingerprint(fingerprint(result.getItems().stream()
                .filter(item -> item.getType() == GenerationItemType.DESTINATION || item.getType() == GenerationItemType.MEAL)
                .map(item -> List.of(item.getType().name(), canonical(item.getPoiSource()),
                        StringUtils.hasText(item.getPoiId()) && StringUtils.hasText(item.getPoiSource())
                                ? canonical(item.getPoiId()) : canonical(item.getName()) + "|" + canonical(item.getAddress())))
                .toList()));
        generation.setFavorite(false);
        generation.setCreatedAt(LocalDateTime.now());
        generationMapper.insert(generation);
        BeanUtils.copyProperties(generation, result);
        for (TravelPlanGenerationItemVO itemVO : result.getItems()) {
            TravelPlanGenerationItem item = new TravelPlanGenerationItem();
            BeanUtils.copyProperties(itemVO, item);
            item.setId(null);
            item.setGenerationId(generation.getId());
            itemMapper.insert(item);
            BeanUtils.copyProperties(item, itemVO);
        }
        return result;
    }

    private void lockOwnedPlan(Long planId, Long userId) {
        TravelPlan plan = planMapper.selectOne(new LambdaQueryWrapper<TravelPlan>()
                .eq(TravelPlan::getId, planId).eq(TravelPlan::getUserId, userId).last("FOR UPDATE"));
        if (plan == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "攻略表单不存在");
        }
    }

    private long requireCapacity(Long planId, Long userId) {
        long count = generationMapper.selectCount(new LambdaQueryWrapper<TravelPlanGeneration>()
                .eq(TravelPlanGeneration::getPlanId, planId).eq(TravelPlanGeneration::getUserId, userId));
        if (count >= MAX_RESULTS_PER_PLAN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "每份表单最多保存 5 套攻略，已达到上限");
        }
        return count;
    }

    private TravelPlanSaveDTO savedForm(Long planId) {
        TravelPlanDetailVO detail = planService.detail(planId);
        TravelPlanSaveDTO form = new TravelPlanSaveDTO();
        BeanUtils.copyProperties(detail, form);
        form.setItems(detail.getItems().stream().map(item -> {
            TravelPlanItemDTO dto = new TravelPlanItemDTO();
            BeanUtils.copyProperties(item, dto);
            return dto;
        }).toList());
        return normalize(form);
    }

    private TravelPlanSaveDTO normalize(TravelPlanSaveDTO request) {
        TravelPlanSaveDTO form = objectMapper.convertValue(request, TravelPlanSaveDTO.class);
        form.setStartPlace(form.getStartPlace().trim());
        form.setEndPlace(form.getEndPlace().trim());
        form.setTravelMode(form.getTravelMode() == null ? TravelMode.AUTO : form.getTravelMode());
        form.setDianpingEnabled(form.getDianpingEnabled() == null ? 0 : form.getDianpingEnabled());
        form.setTitle(trim(form.getTitle()));
        form.setSpecialRequirements(trim(form.getSpecialRequirements()));
        for (TravelPlanItemDTO item : form.getItems()) {
            item.setName(trim(item.getName()));
            item.setPlace(trim(item.getPlace()));
            item.setPreference(trim(item.getPreference()));
            item.setRemark(trim(item.getRemark()));
            if (item.getCustomBudgetAmount() != null) {
                item.setCustomBudgetAmount(item.getCustomBudgetAmount().setScale(2));
            }
        }
        return form;
    }

    private String fingerprint(Object value) {
        try {
            byte[] bytes = objectMapper.writeValueAsString(value).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("无法计算攻略指纹", exception);
        }
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String canonical(String value) {
        return Objects.toString(trim(value), "").toLowerCase(Locale.ROOT);
    }
}
