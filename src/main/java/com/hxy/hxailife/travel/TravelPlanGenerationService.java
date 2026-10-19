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
import com.hxy.hxailife.travel.dto.TravelPlanChangeDTO;
import com.hxy.hxailife.travel.dto.TravelPlanChangeItemDTO;
import com.hxy.hxailife.travel.dto.TravelPlanSaveDTO;
import com.hxy.hxailife.travel.dto.TravelPlanGenerateOptionsDTO;
import com.hxy.hxailife.travel.dto.TravelPlanRefineContextDTO;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
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
        String title = request.getStartPlace() + "-" + request.getEndPlace();
        // 标题不再由用户填写：首次生成成功后使用模型标题作为历史表单标题。
        form.setTitle(trim(generated.getTitle()));
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
                    count == 0 ? GenerationAction.GENERATE : GenerationAction.REGENERATE, null);
        });
        // 必须在 MySQL 事务提交之后写缓存。
        return new TravelPlanGenerateVO(saved, cache.save(userId, fingerprint, saved));
    }

    /**
     * 按当前攻略换一套：后端查询原始表单和历史候选，不信任前端传入的完整攻略 JSON。
     */
    public TravelPlanGenerateVO change(Long planId, Long generationId, TravelPlanGenerateOptionsDTO options) {
        Long userId = StpUtil.getLoginIdAsLong();
        ChangeContext context = transaction.execute(status -> {
            lockOwnedPlan(planId, userId);
            requireCapacity(planId, userId);
            requireOwnedGeneration(planId, generationId, userId);
            TravelPlanSaveDTO form = savedForm(planId);
            return new ChangeContext(form, excludedItems(planId, form), fingerprint(form));
        });

        if (context == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "换方案上下文创建失败");
        }

        applySearchOptions(context.form(), options);
        TravelPlanChangeDTO request = new TravelPlanChangeDTO(context.form(), context.excludedItems());
        TravelPlanGenerationVO generated = callChangeModel(request);
        String recommendationFingerprint = recommendationFingerprint(generated);

        TravelPlanGenerationVO saved = transaction.execute(status -> {
            lockOwnedPlan(planId, userId);
            long count = requireCapacity(planId, userId);
            if (!context.formFingerprint().equals(fingerprint(savedForm(planId)))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "换方案期间表单已修改，请按最新表单重新操作");
            }
            if (isDuplicateRecommendation(planId, recommendationFingerprint)
                    || containsExcludedCandidate(generated, context.excludedItems(), context.form())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "暂时没有新的推荐，请稍后重试");
            }
            return save(userId, planId, context.formFingerprint(), generated,
                    GenerationAction.REGENERATE, generationId);
        });
        return new TravelPlanGenerateVO(saved, cache.save(userId, context.formFingerprint(), saved));
    }

    /** 兼容没有本次搜索选项的旧调用，沿用已保存表单设置。 */
    public TravelPlanGenerateVO change(Long planId, Long generationId) {
        return change(planId, generationId, null);
    }

    /** 删除单套结果；数据库外的缓存只在事务成功提交后清理。 */
    public void delete(Long planId, Long generationId) {
        Long userId = StpUtil.getLoginIdAsLong();
        TravelPlanGeneration generation = transaction.execute(status -> {
            lockOwnedPlan(planId, userId);
            TravelPlanGeneration owned = requireOwnedGeneration(planId, generationId, userId);
            if (generationMapper.delete(new LambdaQueryWrapper<TravelPlanGeneration>()
                    .eq(TravelPlanGeneration::getId, generationId)
                    .eq(TravelPlanGeneration::getPlanId, planId)
                    .eq(TravelPlanGeneration::getUserId, userId)) == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "攻略不存在或不属于该表单");
            }
            return owned;
        });
        cache.evict(generation);
    }

    /** 微调上下文只由后端读取，先核验当前攻略和表单的用户归属。 */
    TravelPlanRefineContextDTO refineContext(Long planId, Long generationId, String message, boolean generating) {
        Long userId = StpUtil.getLoginIdAsLong();
        return transaction.execute(status -> {
            lockOwnedPlan(planId, userId);
            if (generating) {
                requireCapacity(planId, userId);
            }
            TravelPlanGeneration generation = requireOwnedGeneration(planId, generationId, userId);
            TravelPlanGenerationVO current = new TravelPlanGenerationVO();
            BeanUtils.copyProperties(generation, current);
            current.setItems(itemMapper.selectList(new LambdaQueryWrapper<TravelPlanGenerationItem>()
                            .eq(TravelPlanGenerationItem::getGenerationId, generationId)
                            .orderByAsc(TravelPlanGenerationItem::getSortOrder))
                    .stream().map(item -> {
                        TravelPlanGenerationItemVO vo = new TravelPlanGenerationItemVO();
                        BeanUtils.copyProperties(item, vo);
                        return vo;
                    }).toList());
            return new TravelPlanRefineContextDTO(savedForm(planId), current, message);
        });
    }

    /** 模型耗时调用不持有 MySQL 事务；落库前再次校验容量、来源及表单是否变动。 */
    TravelPlanGenerateVO refine(String conversationId, TravelPlanRefineContextDTO context,
                                TravelPlanGenerateOptionsDTO options) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long planId = context.getCurrentGeneration().getPlanId();
        Long generationId = context.getCurrentGeneration().getId();
        String formFingerprint = fingerprint(context.getForm());
        applySearchOptions(context.getForm(), options);
        TravelPlanGenerationVO generated;
        try {
            generated = planApp.adjustStrategyGenerate(conversationId, context);
        } catch (RuntimeException exception) {
            log.warn("攻略微调模型调用或结构化解析失败", exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "攻略微调失败，请稍后重试");
        }
        validateResult(context.getForm(), generated);
        TravelPlanGenerationVO saved = transaction.execute(status -> {
            lockOwnedPlan(planId, userId);
            requireCapacity(planId, userId);
            requireOwnedGeneration(planId, generationId, userId);
            if (!formFingerprint.equals(fingerprint(savedForm(planId)))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "微调期间表单已修改，请按最新表单重新操作");
            }
            // 微调可保留相同地点组合，不执行“换一个”的历史候选排除逻辑。
            return save(userId, planId, formFingerprint, generated, GenerationAction.REFINE, generationId);
        });
        return new TravelPlanGenerateVO(saved, cache.save(userId, formFingerprint, saved));
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

    private TravelPlanGenerationVO callChangeModel(TravelPlanChangeDTO request) {
        try {
            TravelPlanGenerationVO result = planApp.changeStrategy(request);
            validateResult(request.getForm(), result);
            return result;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("换一套攻略模型调用或结构化解析失败", exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "换方案失败，请稍后重试");
        }
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
            if (!Integer.valueOf(1).equals(form.getXhsEnabled())) {
                item.setXhsSearchQuery(null);
                item.setXhsSearchUrl(null);
            }
        }
        items.getFirst().setName(form.getStartPlace());
        items.getFirst().setImageUrl(null);
        items.getLast().setName(form.getEndPlace());
        items.getLast().setImageUrl(null);
    }

    private void invalidResult() {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "模型返回的攻略不完整或不符合表单约束，请重新生成");
    }

    private TravelPlanGenerationVO save(Long userId, Long planId, String formFingerprint,
                                        TravelPlanGenerationVO result, GenerationAction action) {
        return save(userId, planId, formFingerprint, result, action, null);
    }

    private TravelPlanGenerationVO save(Long userId, Long planId, String formFingerprint,
                                        TravelPlanGenerationVO result, GenerationAction action,
                                        Long parentGenerationId) {
        TravelPlanGeneration generation = new TravelPlanGeneration();
        BeanUtils.copyProperties(result, generation);
        generation.setId(null);
        generation.setUserId(userId);
        generation.setPlanId(planId);
        generation.setAction(action);
        generation.setParentGenerationId(parentGenerationId);
        generation.setFormFingerprint(formFingerprint);
        generation.setRecommendationFingerprint(recommendationFingerprint(result));
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

    private String recommendationFingerprint(TravelPlanGenerationVO result) {
        return fingerprint(result.getItems().stream()
                .filter(item -> item.getType() == GenerationItemType.DESTINATION
                        || item.getType() == GenerationItemType.MEAL)
                .map(item -> List.of(item.getType().name(), canonical(item.getPoiSource()),
                        StringUtils.hasText(item.getPoiId()) && StringUtils.hasText(item.getPoiSource())
                                ? canonical(item.getPoiId())
                                : canonical(item.getName()) + "|" + canonical(item.getAddress())))
                .toList());
    }

    private TravelPlanGeneration requireOwnedGeneration(Long planId, Long generationId, Long userId) {
        TravelPlanGeneration generation = generationMapper.selectOne(new LambdaQueryWrapper<TravelPlanGeneration>()
                .eq(TravelPlanGeneration::getId, generationId)
                .eq(TravelPlanGeneration::getPlanId, planId)
                .eq(TravelPlanGeneration::getUserId, userId));
        if (generation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "当前攻略不存在或不属于该表单");
        }
        return generation;
    }

    private List<TravelPlanChangeItemDTO> excludedItems(Long planId, TravelPlanSaveDTO form) {
        List<TravelPlanGeneration> generations = generationMapper.selectList(new LambdaQueryWrapper<TravelPlanGeneration>()
                .eq(TravelPlanGeneration::getPlanId, planId)
                .eq(TravelPlanGeneration::getUserId, StpUtil.getLoginIdAsLong())
                .orderByAsc(TravelPlanGeneration::getCreatedAt, TravelPlanGeneration::getId));
        Map<String, TravelPlanChangeItemDTO> unique = new LinkedHashMap<>();
        for (TravelPlanGeneration generation : generations) {
            List<TravelPlanGenerationItem> items = itemMapper.selectList(new LambdaQueryWrapper<TravelPlanGenerationItem>()
                    .eq(TravelPlanGenerationItem::getGenerationId, generation.getId())
                    .in(TravelPlanGenerationItem::getType, GenerationItemType.DESTINATION, GenerationItemType.MEAL)
                    .orderByAsc(TravelPlanGenerationItem::getSortOrder));
            for (TravelPlanGenerationItem item : items) {
                if (isFixedByForm(item, form)) {
                    continue;
                }
                TravelPlanChangeItemDTO excluded = new TravelPlanChangeItemDTO(
                        item.getType(), item.getName(), item.getAddress(), item.getPoiId());
                String key = canonical(item.getType() == null ? null : item.getType().name()) + "|"
                        + canonical(item.getPoiId()) + "|" + canonical(item.getName()) + "|"
                        + canonical(item.getAddress());
                unique.putIfAbsent(key, excluded);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private boolean isFixedByForm(TravelPlanGenerationItem item, TravelPlanSaveDTO form) {
        int index = item.getSortOrder() == null ? -1 : item.getSortOrder() - 1;
        if (index < 0 || index >= form.getItems().size()) {
            return false;
        }
        String formName = form.getItems().get(index).getName();
        return StringUtils.hasText(formName) && formName.trim().equals(item.getName());
    }

    private boolean isDuplicateRecommendation(Long planId, String recommendationFingerprint) {
        return generationMapper.selectCount(new LambdaQueryWrapper<TravelPlanGeneration>()
                .eq(TravelPlanGeneration::getPlanId, planId)
                .eq(TravelPlanGeneration::getRecommendationFingerprint, recommendationFingerprint)) > 0;
    }

    private boolean containsExcludedCandidate(TravelPlanGenerationVO result,
                                              List<TravelPlanChangeItemDTO> excludedItems,
                                              TravelPlanSaveDTO form) {
        for (TravelPlanGenerationItemVO item : result.getItems()) {
            if ((item.getType() != GenerationItemType.DESTINATION
                    && item.getType() != GenerationItemType.MEAL)
                    || isFixedByForm(item, form)) {
                continue;
            }
            for (TravelPlanChangeItemDTO excluded : excludedItems) {
                if (excluded.getType() != item.getType()) {
                    continue;
                }
                boolean samePoi = StringUtils.hasText(item.getPoiId())
                        && StringUtils.hasText(excluded.getPoiId())
                        && canonical(item.getPoiId()).equals(canonical(excluded.getPoiId()));
                boolean sameNameAndAddress = canonical(item.getName()).equals(canonical(excluded.getName()))
                        && canonical(item.getAddress()).equals(canonical(excluded.getAddress()));
                if (samePoi || sameNameAndAddress) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isFixedByForm(TravelPlanGenerationItemVO item, TravelPlanSaveDTO form) {
        int index = item.getSortOrder() == null ? -1 : item.getSortOrder() - 1;
        if (index < 0 || index >= form.getItems().size()) {
            return false;
        }
        String formName = form.getItems().get(index).getName();
        return StringUtils.hasText(formName) && formName.trim().equals(item.getName());
    }

    private record ChangeContext(TravelPlanSaveDTO form,
                                 List<TravelPlanChangeItemDTO> excludedItems,
                                 String formFingerprint) {
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
        form.setTravelModes(detail.getTravelModes() == null ? new ArrayList<>() : detail.getTravelModes());
        form.setTravelMode(detail.getTravelMode());
        form.setItems(detail.getItems().stream().map(item -> {
            TravelPlanItemDTO dto = new TravelPlanItemDTO();
            BeanUtils.copyProperties(item, dto);
            return dto;
        }).toList());
        return normalize(form);
    }

    private void applySearchOptions(TravelPlanSaveDTO form, TravelPlanGenerateOptionsDTO options) {
        if (options == null) {
            return;
        }
        if (options.getXhsEnabled() != null) {
            form.setXhsEnabled(options.getXhsEnabled());
        }
        if (options.getDianpingEnabled() != null) {
            form.setDianpingEnabled(options.getDianpingEnabled());
        }
    }

    private TravelPlanSaveDTO normalize(TravelPlanSaveDTO request) {
        TravelPlanSaveDTO form = objectMapper.convertValue(request, TravelPlanSaveDTO.class);
        form.setStartPlace(form.getStartPlace().trim());
        form.setEndPlace(form.getEndPlace().trim());
        List<TravelMode> modes = form.getTravelModes() == null
                ? new ArrayList<>() : new ArrayList<>(form.getTravelModes());
        if (modes.isEmpty() && form.getTravelMode() != null) {
            modes.add(form.getTravelMode());
        }
        if (modes.isEmpty()) {
            modes.add(TravelMode.AUTO);
        }
        form.setTravelModes(modes.stream().distinct().toList());
        form.setTravelMode(form.getTravelModes().getFirst());
        form.setDianpingEnabled(form.getDianpingEnabled() == null ? 0 : form.getDianpingEnabled());
        form.setXhsEnabled(form.getXhsEnabled() == null ? 0 : form.getXhsEnabled());
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
            // 标题由 AI 生成，不属于用户输入条件；标题变化不应让同一套表单失去一致性校验。
            Object fingerprintValue = value;
            if (value instanceof TravelPlanSaveDTO form) {
                TravelPlanSaveDTO copy = objectMapper.convertValue(form, TravelPlanSaveDTO.class);
                copy.setTitle(null);
                fingerprintValue = copy;
            }
            byte[] bytes = objectMapper.writeValueAsString(fingerprintValue).getBytes(StandardCharsets.UTF_8);
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
