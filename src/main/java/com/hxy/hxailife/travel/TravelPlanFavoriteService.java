package com.hxy.hxailife.travel;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hxy.hxailife.mapper.TravelPlanGenerationItemMapper;
import com.hxy.hxailife.mapper.TravelPlanGenerationMapper;
import com.hxy.hxailife.travel.vo.TravelPlanGenerationItemVO;
import com.hxy.hxailife.travel.vo.TravelPlanGenerationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TravelPlanFavoriteService {

    private final TravelPlanGenerationMapper generationMapper;
    private final TravelPlanGenerationItemMapper itemMapper;
    private final TravelPlanResultCache cache;

    public TravelPlanFavoriteService(TravelPlanGenerationMapper generationMapper,
                                     TravelPlanGenerationItemMapper itemMapper,
                                     TravelPlanResultCache cache) {
        this.generationMapper = generationMapper;
        this.itemMapper = itemMapper;
        this.cache = cache;
    }

    @Transactional
    public TravelPlanGenerationVO setFavorite(Long generationId, boolean favorite) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 锁住结果行，收藏与表单删除按实际写入顺序生效，不会在结果已删除后仍返回收藏成功。
        TravelPlanGeneration generation = generationMapper.selectOne(new LambdaQueryWrapper<TravelPlanGeneration>()
                .eq(TravelPlanGeneration::getId, generationId)
                .eq(TravelPlanGeneration::getUserId, userId)
                .last("FOR UPDATE"));
        if (generation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "攻略不存在");
        }
        // 只更新状态，重复操作不新增攻略、不修改创建时间，也不改变 5 套配额。
        if (!Boolean.valueOf(favorite).equals(generation.getFavorite())) {
            generationMapper.update(null, new LambdaUpdateWrapper<TravelPlanGeneration>()
                    .eq(TravelPlanGeneration::getId, generationId)
                    .eq(TravelPlanGeneration::getUserId, userId)
                    .set(TravelPlanGeneration::getFavorite, favorite));
            generation.setFavorite(favorite);
        }
        // 事务提交后才清理缓存；Redis 不可用不撤销 MySQL 的收藏状态。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cache.evict(generation);
            }
        });
        return toVO(generation);
    }

    @Transactional(readOnly = true)
    public List<TravelPlanGenerationVO> list() {
        return generationMapper.selectList(new LambdaQueryWrapper<TravelPlanGeneration>()
                        .eq(TravelPlanGeneration::getUserId, StpUtil.getLoginIdAsLong())
                        .eq(TravelPlanGeneration::getFavorite, true)
                        .orderByDesc(TravelPlanGeneration::getCreatedAt, TravelPlanGeneration::getId))
                .stream().map(this::toVO).toList();
    }

    @Transactional(readOnly = true)
    public TravelPlanGenerationVO detail(Long generationId) {
        // 不查询来源表单，planId 为空的独立收藏攻略也能正常返回。
        TravelPlanGeneration generation = generationMapper.selectOne(new LambdaQueryWrapper<TravelPlanGeneration>()
                .eq(TravelPlanGeneration::getId, generationId)
                .eq(TravelPlanGeneration::getUserId, StpUtil.getLoginIdAsLong())
                .eq(TravelPlanGeneration::getFavorite, true));
        if (generation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "收藏攻略不存在");
        }
        return toVO(generation);
    }

    private TravelPlanGenerationVO toVO(TravelPlanGeneration generation) {
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
    }
}
