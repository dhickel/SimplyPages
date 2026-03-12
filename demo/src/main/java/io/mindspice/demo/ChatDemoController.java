package io.mindspice.demo;

import io.mindspice.demo.chat.ChatDemoService;
import io.mindspice.demo.pages.ChatDemoPage;
import io.mindspice.simplypages.builders.BannerBuilder;
import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.builders.TopNavBuilder;
import io.mindspice.simplypages.components.RawHtml;
import io.mindspice.simplypages.core.Component;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Controller
@RequestMapping("/chat")
public class ChatDemoController {

    private static final String SESSION_CHAT_CONVERSATION_ID = "chat.demo.conversation.id";
    private static final String SESSION_CHAT_DISPLAY_NAME = "chat.demo.viewer.name";

    private final ChatDemoService chatDemoService;
    private final ChatDemoPage chatDemoPage;
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emittersByConversation = new ConcurrentHashMap<>();

    public ChatDemoController(ChatDemoService chatDemoService, ChatDemoPage chatDemoPage) {
        this.chatDemoService = chatDemoService;
        this.chatDemoPage = chatDemoPage;
    }

    @GetMapping
    @ResponseBody
    public String chatPage(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpSession session,
        HttpServletResponse response
    ) {
        String conversationId = resolveConversationId(session);
        String main = chatDemoPage.renderMain(
            conversationId,
            chatDemoService.history(conversationId),
            null,
            false
        );

        response.setHeader("Vary", "HX-Request");
        if (hxRequest != null) {
            return main;
        }

        return ShellBuilder.create()
            .withPageTitle("SimplyPages Chat Demo")
            .withTopBanner(BannerBuilder.create()
                .withLayout(BannerBuilder.BannerLayout.HORIZONTAL)
                .withTitle("SimplyPages")
                .withSubtitle("Chat module + SSE smoke test")
                .build())
            .withTopNav(buildGlobalTopNav())
            .withContent(new RawHtml(main))
            .addCustomJs("/js/chat-demo.js")
            .build();
    }

    @GetMapping("/history")
    @ResponseBody
    public String historyFragment(
        @RequestParam("conversationId") String conversationId,
        HttpServletResponse response
    ) {
        response.setHeader("Vary", "HX-Request");
        return chatDemoPage.renderTranscript(conversationId, chatDemoService.history(conversationId));
    }

    @PostMapping("/messages")
    @ResponseBody
    public String postMessage(
        @RequestParam("conversationId") String conversationId,
        @RequestParam("message") String message,
        HttpSession session,
        HttpServletResponse response
    ) {
        String displayName = resolveDisplayName(session);
        Optional<Long> cursor = chatDemoService.appendExchange(conversationId, message, displayName);

        if (cursor.isEmpty()) {
            response.setStatus(400);
            response.setHeader("Vary", "HX-Request");
            return chatDemoPage.renderTranscript(conversationId, chatDemoService.history(conversationId));
        }

        publishUpdate(conversationId, cursor.get());
        response.setHeader("Vary", "HX-Request");
        return chatDemoPage.renderTranscript(conversationId, chatDemoService.history(conversationId));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter stream(@RequestParam("conversationId") String conversationId) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByConversation
            .computeIfAbsent(conversationId, ignored -> new CopyOnWriteArrayList<>())
            .add(emitter);

        emitter.onCompletion(() -> removeEmitter(conversationId, emitter));
        emitter.onTimeout(() -> removeEmitter(conversationId, emitter));
        emitter.onError(ex -> removeEmitter(conversationId, emitter));

        long currentCursor = chatDemoService.currentCursor(conversationId);
        try {
            emitter.send(SseEmitter.event()
                .name("chat-updated")
                .id(String.valueOf(currentCursor))
                .data(currentCursor));
        } catch (IOException e) {
            removeEmitter(conversationId, emitter);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private void publishUpdate(String conversationId, long cursor) {
        List<SseEmitter> emitters = emittersByConversation.get(conversationId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("chat-updated")
                    .id(String.valueOf(cursor))
                    .data(cursor));
            } catch (IOException e) {
                removeEmitter(conversationId, emitter);
                emitter.completeWithError(e);
            }
        }
    }

    private void removeEmitter(String conversationId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByConversation.get(conversationId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByConversation.remove(conversationId);
        }
    }

    private String resolveConversationId(HttpSession session) {
        Object existing = session.getAttribute(SESSION_CHAT_CONVERSATION_ID);
        if (existing instanceof String existingId && !existingId.isBlank()) {
            return existingId;
        }

        String generated = "conv-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 6);
        session.setAttribute(SESSION_CHAT_CONVERSATION_ID, generated);
        return generated;
    }

    private String resolveDisplayName(HttpSession session) {
        Object existing = session.getAttribute(SESSION_CHAT_DISPLAY_NAME);
        if (existing instanceof String name && !name.isBlank()) {
            return name;
        }
        String fallback = "Demo User";
        session.setAttribute(SESSION_CHAT_DISPLAY_NAME, fallback);
        return fallback;
    }

    private Component buildGlobalTopNav() {
        return TopNavBuilder.create()
            .withHtmxNavigation(false)
            .addPrimaryLink("Home", "/home")
            .addPrimaryLink("Demos", "/demos")
            .addPrimaryLink("Javadocs", "/javadocs-view")
            .addPrimaryLink("Forum", "/forum")
            .addPrimaryLink("Chat", "/chat")
            .addPrimaryLink("Blog", "/blog")
            .addPrimaryLink("Docs", "/docs")
            .build();
    }
}
