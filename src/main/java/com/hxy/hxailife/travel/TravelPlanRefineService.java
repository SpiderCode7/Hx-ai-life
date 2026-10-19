package com.hxy.hxailife.travel;

import cn.dev33.satoken.stp.StpUtil;
import com.hxy.hxailife.app.PlanApp;
import com.hxy.hxailife.common.ChatMessageDTO;
import com.hxy.hxailife.common.ChatHistoryMessageVO;
import com.hxy.hxailife.travel.dto.TravelPlanGenerateOptionsDTO;
import com.hxy.hxailife.mongo.ConversationRepository;
import com.hxy.hxailife.mongo.MessageRepository;
import com.hxy.hxailife.mongo.entity.ConversationDoc;
import com.hxy.hxailife.mongo.entity.ConversationType;
import com.hxy.hxailife.mongo.entity.MessageDoc;
import com.hxy.hxailife.travel.vo.TravelPlanRefineChatVO;
import com.hxy.hxailife.travel.vo.TravelPlanRefineGenerateVO;
import com.hxy.hxailife.travel.vo.TravelPlanRefineSessionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 微调会话编排；MySQL 结果仍交给 GenerationService 保存。 */
@Service
@Slf4j
public class TravelPlanRefineService {

    private static final DefaultRedisScript<Long> UNLOCK = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end return 0", Long.class);

    private final TravelPlanGenerationService generationService;
    private final PlanApp planApp;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final StringRedisTemplate redis;

    public TravelPlanRefineService(TravelPlanGenerationService generationService, PlanApp planApp,
                                 ConversationRepository conversations, MessageRepository messages,
                                 StringRedisTemplate redis) {
        this.generationService = generationService;
        this.planApp = planApp;
        this.conversations = conversations;
        this.messages = messages;
        this.redis = redis;
    }

    /** 发送按钮：保存用户原话和 AI 回复，不生成新攻略。 */
    public TravelPlanRefineChatVO chat(Long planId, Long generationId, ChatMessageDTO request) {
        if (!StringUtils.hasText(request.getMessage())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "微调聊天消息不能为空");
        }
        var context = generationService.refineContext(planId, generationId, request.getMessage(), false);
        ConversationDoc conversation = conversation(request.getConversationId(), planId, generationId,
                context.getCurrentGeneration().getTitle());
        String lockToken = lock(conversation.getId());
        try {
            // 等待/竞争锁后重新检查，避免前端旧版本覆盖已微调成功的新版本。
            conversation = conversation(conversation.getId(), planId, generationId, conversation.getTitle());
            String reply;
            try {
                reply = planApp.adjustStrategyChat(conversation.getId(), context);
                if (!StringUtils.hasText(reply)) {
                    throw new IllegalStateException("模型返回空回复");
                }
            } catch (RuntimeException exception) {
                log.warn("微调聊天调用失败，conversationId={}", conversation.getId(), exception);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "微调聊天失败，请稍后重试");
            }
            saveMessages(conversation, generationId, request.getMessage(), reply);
            conversation.setUpdatedAt(LocalDateTime.now());
            conversations.save(conversation);
            return new TravelPlanRefineChatVO(conversation.getId(), generationId, reply);
        } finally {
            unlock(conversation.getId(), lockToken);
        }
    }

    /** 生成按钮：本次 message 可为空，但此时会话中必须已有用户消息。 */
    public TravelPlanRefineGenerateVO generate(Long planId, Long generationId, TravelPlanGenerateOptionsDTO request) {
        var context = generationService.refineContext(planId, generationId, request.getMessage(), true);
        ConversationDoc conversation = conversation(request.getConversationId(), planId, generationId,
                context.getCurrentGeneration().getTitle());
        String lockToken = lock(conversation.getId());
        try {
            conversation = conversation(conversation.getId(), planId, generationId, conversation.getTitle());
            if (!StringUtils.hasText(request.getMessage())
                    && messages.findByConversationIdOrderByTimestampAsc(conversation.getId()).stream()
                    .noneMatch(message -> "user".equals(message.getRole()) && StringUtils.hasText(message.getContent()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先聊天说明修改要求，或填写本次微调消息");
            }
            var result = generationService.refine(conversation.getId(), context, request);
            boolean memorySaved = true;
            try {
                // MySQL 已提交；先更新版本指针，再保存简短消息，不把完整结果 JSON 当聊天记忆。
                conversation.setCurrentGenerationId(result.getGeneration().getId());
                conversation.setUpdatedAt(LocalDateTime.now());
                conversations.save(conversation);
                saveMessages(conversation, generationId, request.getMessage(),
                        "已按聊天中的修改要求生成攻略：" + result.getGeneration().getTitle()
                                + "（攻略ID：" + result.getGeneration().getId() + "）。"
                                + Objects.toString(result.getGeneration().getRecommendationReason(), ""));
            } catch (RuntimeException exception) {
                // Mongo 和 MySQL 不属于同一个事务，不能将已落库结果误报成生成失败。
                memorySaved = false;
                log.warn("攻略 {} 已保存，但微调会话记忆更新失败", result.getGeneration().getId(), exception);
            }
            return new TravelPlanRefineGenerateVO(conversation.getId(), result.getGeneration(),
                    result.isCached(), memorySaved);
        } finally {
            unlock(conversation.getId(), lockToken);
        }
    }

    /**
     * 打开微调聊天框：校验当前攻略归属后读取已有会话和最近 20 条消息。
     * 没有 conversationId 时只返回空会话，不提前创建 MongoDB 文档。
     */
    public TravelPlanRefineSessionVO session(Long planId, Long generationId, String conversationId) {
        var context = generationService.refineContext(planId, generationId, null, false);
        ConversationDoc conversation = null;
        if (StringUtils.hasText(conversationId)) {
            conversation = findConversation(conversationId, planId, generationId);
        }

        List<ChatHistoryMessageVO> history = conversation == null
                ? List.of()
                : recentMessages(conversation.getId());
        return new TravelPlanRefineSessionVO(
                conversation == null ? null : conversation.getId(),
                planId,
                generationId,
                context.getCurrentGeneration().getTitle(),
                history
        );
    }

    private ConversationDoc conversation(String id, Long planId, Long generationId, String title) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!StringUtils.hasText(id)) {
            ConversationDoc doc = new ConversationDoc();
            doc.setId(UUID.randomUUID().toString());
            doc.setUserId(userId);
            doc.setType(ConversationType.PLAN_REFINE);
            doc.setPlanId(planId);
            doc.setInitialGenerationId(generationId);
            doc.setCurrentGenerationId(generationId);
            doc.setTitle(title);
            return conversations.insert(doc);
        }
        return findConversation(id, planId, generationId);
    }

    private ConversationDoc findConversation(String id, Long planId, Long generationId) {
        Long userId = StpUtil.getLoginIdAsLong();
        ConversationDoc doc = conversations.findById(id)
                .filter(value -> userId.equals(value.getUserId())
                        && value.getType() == ConversationType.PLAN_REFINE
                        && planId.equals(value.getPlanId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "微调会话不存在"));
        if (!generationId.equals(doc.getCurrentGenerationId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "会话已关联其他攻略版本，请使用最新 generationId 或新建会话");
        }
        return doc;
    }

    private List<ChatHistoryMessageVO> recentMessages(String conversationId) {
        List<MessageDoc> history = messages.findByConversationIdOrderByTimestampAsc(conversationId);
        int fromIndex = Math.max(0, history.size() - 20);
        return history.subList(fromIndex, history.size()).stream()
                .map(message -> new ChatHistoryMessageVO(
                        message.getRole(),
                        message.getGenerationId(),
                        message.getContent(),
                        message.getTimestamp()))
                .toList();
    }

    private void saveMessages(ConversationDoc conversation, Long generationId, String input, String reply) {
        LocalDateTime now = LocalDateTime.now();
        if (StringUtils.hasText(input)) {
            messages.insert(message(conversation, generationId, "user", input, now));
        }
        messages.insert(message(conversation, generationId, "assistant", reply, now.plusNanos(1_000_000)));
    }

    private MessageDoc message(ConversationDoc conversation, Long generationId, String role,
                               String content, LocalDateTime timestamp) {
        MessageDoc doc = new MessageDoc();
        doc.setConversationId(conversation.getId());
        doc.setUserId(conversation.getUserId());
        // 一轮微调消息绑定来源版本；新结果版本由 ConversationDoc.currentGenerationId 指向。
        doc.setGenerationId(generationId);
        doc.setRole(role);
        doc.setContent(content);
        doc.setTimestamp(timestamp);
        return doc;
    }

    private String lock(String conversationId) {
        String token = UUID.randomUUID().toString();
        // 同一会话禁止重复点击并发调用，避免交错记忆和新版本指针相互覆盖。
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(
                "plan:refine:lock:" + conversationId, token, Duration.ofMinutes(30)))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该会话正在处理，请等待完成后再操作");
        }
        return token;
    }

    private void unlock(String conversationId, String token) {
        try {
            redis.execute(UNLOCK, List.of("plan:refine:lock:" + conversationId), token);
        } catch (RuntimeException exception) {
            log.warn("微调会话锁释放失败，将由 TTL 自动清理，conversationId={}", conversationId, exception);
        }
    }
}
