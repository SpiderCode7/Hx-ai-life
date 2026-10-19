package com.hxy.hxailife.chat;

import cn.dev33.satoken.stp.StpUtil;
import com.hxy.hxailife.app.LifeApp;
import com.hxy.hxailife.common.ChatMessageDTO;
import com.hxy.hxailife.common.ChatHistoryMessageVO;
import com.hxy.hxailife.mongo.ConversationRepository;
import com.hxy.hxailife.mongo.entity.ConversationDoc;
import com.hxy.hxailife.mongo.entity.ConversationType;
import com.hxy.hxailife.chat.vo.ChatReplyVO;
import com.hxy.hxailife.chat.vo.ChatSessionVO;
import com.hxy.hxailife.chat.vo.ChatConversationSummaryVO;
import com.hxy.hxailife.mongo.MessageRepository;
import com.hxy.hxailife.mongo.entity.MessageDoc;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** 普通“聊一聊”会话编排；普通模式用记忆 Advisor，深度模式只保存最终对话。 */
@Service
public class ChatService {

    private final LifeApp lifeApp;
    private final ConversationRepository conversations;
    private final MessageRepository messages;

    public ChatService(LifeApp lifeApp, ConversationRepository conversations,
                       MessageRepository messages) {
        this.lifeApp = lifeApp;
        this.conversations = conversations;
        this.messages = messages;
    }

    /** 发送普通聊天消息，按本次按钮状态选择深度模式和联网，保留 Mongo 多轮记忆。 */
    public ChatReplyVO send(ChatMessageDTO request) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "聊天消息不能为空");
        }

        ConversationDoc conversation = getOrCreateConversation(request.getConversationId(),request.getMessage());
        String reply;
        try {
            // 两个开关独立且只作用于本次请求；不传或 null 均视为关闭。
            boolean webSearchEnabled = Boolean.TRUE.equals(request.getWebSearchEnabled());
            reply = Boolean.TRUE.equals(request.getDeepThinkingEnabled())
                    ? lifeApp.deepChat(request.getMessage().trim(), conversation.getId(), webSearchEnabled)
                    : lifeApp.chat(request.getMessage().trim(), conversation.getId(), webSearchEnabled);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "聊天服务暂时不可用，请稍后重试", exception);
        }
        if (!StringUtils.hasText(reply)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "模型未返回有效回复");
        }

        conversation.setUpdatedAt(LocalDateTime.now());
        conversations.save(conversation);
        return new ChatReplyVO(conversation.getId(), reply);
    }

    /** 以 SSE 推送深度思考摘要、工具进度和最终回答。 */
    public void stream(ChatMessageDTO request, SseEmitter emitter) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "聊天消息不能为空");
        }
        ConversationDoc conversation = getOrCreateConversation(request.getConversationId(), request.getMessage());
        send(emitter, new ChatStreamEvent("meta", conversation.getId()));
        CompletableFuture.runAsync(() -> {
            try {
                boolean deep = Boolean.TRUE.equals(request.getDeepThinkingEnabled());
                boolean webSearch = Boolean.TRUE.equals(request.getWebSearchEnabled());
                send(emitter, new ChatStreamEvent("thinking", deep ? "正在思考…" : "正在生成回答…"));
                String reply = deep
                        ? lifeApp.deepChat(request.getMessage().trim(), conversation.getId(), webSearch,
                        text -> send(emitter, new ChatStreamEvent("thinking", text)))
                        : lifeApp.chat(request.getMessage().trim(), conversation.getId(), webSearch);
                if (!StringUtils.hasText(reply)) {
                    throw new IllegalStateException("模型未返回有效回复");
                }
                for (int index = 0; index < reply.length(); index += 24) {
                    send(emitter, new ChatStreamEvent("answer",
                            reply.substring(index, Math.min(index + 24, reply.length()))));
                }
                conversation.setUpdatedAt(LocalDateTime.now());
                conversations.save(conversation);
                send(emitter, new ChatStreamEvent("done", ""));
                emitter.complete();
            } catch (Exception exception) {
                send(emitter, new ChatStreamEvent("error", "聊天服务暂时不可用，请稍后重试"));
                emitter.completeWithError(exception);
            }
        });
    }

    private void send(SseEmitter emitter, ChatStreamEvent event) {
        try {
            emitter.send(SseEmitter.event().name(event.type()).data(event));
        } catch (Exception exception) {
            throw new IllegalStateException("SSE 推送失败", exception);
        }
    }

    /** 打开普通聊天页面时读取会话元数据和最近 20 条消息。 */
    public ChatSessionVO session(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return new ChatSessionVO(null, "普通聊天", List.of());
        }
        ConversationDoc conversation = findOwnedChat(conversationId);
        List<MessageDoc> history = messages.findByConversationIdOrderByTimestampAsc(conversationId);
        int fromIndex = Math.max(0, history.size() - 20);
        List<ChatHistoryMessageVO> result = history.subList(fromIndex, history.size()).stream()
                .map(message -> new ChatHistoryMessageVO(
                        message.getRole(), message.getGenerationId(),
                        message.getContent(), message.getTimestamp()))
                .toList();
        return new ChatSessionVO(conversation.getId(), conversation.getTitle(), result);
    }

    /** 查询当前用户的普通聊天会话，供左侧历史列表使用。 */
    public List<ChatConversationSummaryVO> listConversations() {
        Long userId = StpUtil.getLoginIdAsLong();
        return conversations.findByUserIdAndTypeOrderByUpdatedAtDesc(userId, ConversationType.CHAT).stream()
                .map(conversation -> new ChatConversationSummaryVO(
                        conversation.getId(), conversation.getTitle(), conversation.getUpdatedAt()))
                .toList();
    }

    /** 删除当前用户的一条普通聊天会话及其消息。 */
    public void deleteConversation(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "会话 ID 不能为空");
        }
        ConversationDoc conversation = findOwnedChat(conversationId);
        messages.deleteByConversationId(conversation.getId());
        conversations.delete(conversation);
    }

    private ConversationDoc getOrCreateConversation(String conversationId,String message) {
        if (!StringUtils.hasText(conversationId)) {
            ConversationDoc conversation = new ConversationDoc();
            conversation.setId(UUID.randomUUID().toString());
            conversation.setUserId(StpUtil.getLoginIdAsLong());
            conversation.setType(ConversationType.CHAT);
            conversation.setTitle(message);
            return conversations.insert(conversation);
        }
        return findOwnedChat(conversationId);
    }

    private ConversationDoc findOwnedChat(String conversationId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return conversations.findById(conversationId)
                .filter(conversation -> userId.equals(conversation.getUserId())
                        && conversation.getType() == ConversationType.CHAT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "普通聊天会话不存在"));
    }
}
