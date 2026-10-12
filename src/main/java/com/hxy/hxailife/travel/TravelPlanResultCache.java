package com.hxy.hxailife.travel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxy.hxailife.travel.vo.TravelPlanGenerationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class TravelPlanResultCache {

    // 同一输入条件下，每个结果 ID 对应完整攻略 JSON；写入和续期原子完成。
    private static final DefaultRedisScript<Long> SAVE = new DefaultRedisScript<>("""
            redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
            redis.call('EXPIRE', KEYS[1], 604800)
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public TravelPlanResultCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public boolean save(Long userId, String formFingerprint, TravelPlanGenerationVO generation) {
        String key = "plan:results:" + userId + ":" + generation.getPlanId() + ":" + formFingerprint;
        try {
            return Long.valueOf(1).equals(redis.execute(SAVE, List.of(key),
                    generation.getId().toString(), objectMapper.writeValueAsString(generation)));
        } catch (JsonProcessingException | RuntimeException exception) {
            // 正式结果已在 MySQL 提交，不能因此向前端返回生成失败或诱发重复生成。
            log.warn("攻略 {} 已保存到 MySQL，但 Redis 缓存写入失败", generation.getId(), exception);
            return false;
        }
    }

    public void evict(TravelPlanGeneration generation) {
        // 表单已删除时不再使用原表单缓存；旧 key 按既有 TTL 自行过期。
        if (generation.getPlanId() == null) {
            return;
        }
        String key = "plan:results:" + generation.getUserId() + ":" + generation.getPlanId()
                + ":" + generation.getFormFingerprint();
        try {
            redis.opsForHash().delete(key, generation.getId().toString());
        } catch (RuntimeException exception) {
            log.warn("攻略 {} 的状态已更新到 MySQL，但 Redis 缓存清理失败", generation.getId(), exception);
        }
    }
}
