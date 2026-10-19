package com.hxy.hxailife.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.hxy.hxailife.chat.ChatService;
import com.hxy.hxailife.chat.vo.ChatReplyVO;
import com.hxy.hxailife.chat.vo.ChatSessionVO;
import com.hxy.hxailife.chat.vo.ChatConversationSummaryVO;
import com.hxy.hxailife.common.ChatMessageDTO;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@SaCheckLogin
@Tag(name = "普通聊天")
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "发送普通聊天消息", description = "webSearchEnabled 启用联网搜索，deepThinkingEnabled 启用轻量 ReAct 深度模式；两者独立且默认关闭")
    public ChatReplyVO send(@Valid @RequestBody ChatMessageDTO request) {
        return chatService.send(request);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式发送普通聊天消息", description = "以 SSE 推送深度思考摘要、工具进度和最终回答")
    public SseEmitter stream(@Valid @RequestBody ChatMessageDTO request) {
        SseEmitter emitter = new SseEmitter(300_000L);
        chatService.stream(request, emitter);
        return emitter;
    }

    @GetMapping("/session")
    @Operation(summary = "打开普通聊天页面，读取会话历史")
    public ChatSessionVO session(@RequestParam(required = false) String conversationId) {
        return chatService.session(conversationId);
    }

    @GetMapping("/conversations")
    @Operation(summary = "查询普通聊天历史会话列表")
    public List<ChatConversationSummaryVO> conversations() {
        return chatService.listConversations();
    }

    @DeleteMapping("/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除普通聊天会话及其消息")
    public void deleteConversation(@PathVariable String conversationId) {
        chatService.deleteConversation(conversationId);
    }
}
