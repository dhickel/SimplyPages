package io.mindspice.demo;

import io.mindspice.demo.chat.ChatDemoService;
import io.mindspice.demo.pages.ChatDemoPage;
import io.mindspice.simplypages.builders.BannerBuilder;
import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.builders.TopNavBuilder;
import io.mindspice.simplypages.components.RawHtml;
import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.core.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Controller
@RequestMapping("/chat")
public class ChatDemoController {
    private final ChatDemoService chatDemoService;
    private final ChatDemoPage chatDemoPage;

    public ChatDemoController(ChatDemoService chatDemoService, ChatDemoPage chatDemoPage) {
        this.chatDemoService = chatDemoService;
        this.chatDemoPage = chatDemoPage;
    }

    @ModelAttribute
    private void privateResponse(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        for (String header : response.getHeaders("Vary")) {
            for (String token : header.split(",")) if ("HX-Request".equalsIgnoreCase(token.trim())) return;
        }
        response.addHeader("Vary", "HX-Request");
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String chatPage(@RequestHeader(value = "HX-Request", required = false) String hxRequest,
                           HttpServletRequest request, HttpServletResponse response) {
        if ("true".equalsIgnoreCase(hxRequest)) {
            response.setHeader("HX-Redirect", "/chat");
            return ""; // No session or conversation allocation for navigation fragments.
        }
        HttpSession session = request.getSession();
        String main;
        try {
            ChatDemoService.Snapshot snapshot = chatDemoService.initialize(session, false);
            main = chatDemoPage.renderMain(snapshot.conversationId(), snapshot.messages(), null, false);
        } catch (ChatDemoService.ChatFailure failure) {
            if (failure.status() != 410) throw failure;
            response.setStatus(410);
            main = chatDemoPage.renderMain((String) session.getAttribute(ChatDemoService.SESSION_CONVERSATION),
                List.of(), failure.getMessage(), true);
        }
        return ShellBuilder.create()
            .withPageTitle("SimplyPages Chat Demo")
            .withTopBanner(BannerBuilder.create().withLayout(BannerBuilder.BannerLayout.HORIZONTAL)
                .withTitle("SimplyPages").withSubtitle("Private chat + finite SSE sandbox").build())
            .withTopNav(buildGlobalTopNav()).withContent(new RawHtml(main))
            .addCustomJs("/js/chat-demo.js").build();
    }

    @PostMapping(value = "/new", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String startNew(HttpServletRequest request) {
        ChatDemoService.Snapshot snapshot = chatDemoService.initialize(request.getSession(), true);
        return chatDemoPage.renderMain(snapshot.conversationId(), snapshot.messages(), null, false);
    }

    @GetMapping(value = "/history", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String historyFragment(@RequestParam(value = "conversationId", required = false) String id,
                                  HttpServletRequest request) {
        ChatDemoService.Snapshot snapshot = chatDemoService.history(request.getSession(false), id);
        return chatDemoPage.renderTranscript(snapshot.conversationId(), snapshot.messages());
    }

    @PostMapping(value = "/messages", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String postMessage(@RequestParam(value = "conversationId", required = false) String id,
                              @RequestParam(value = "message", required = false) String message,
                              HttpServletRequest request) {
        // Fixed bounded display fallback: session scope is not authenticated identity.
        ChatDemoService.Snapshot snapshot = chatDemoService.appendExchange(request.getSession(false), id, message, "Demo User");
        return chatDemoPage.renderTranscript(snapshot.conversationId(), snapshot.messages());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter stream(@RequestParam(value = "conversationId", required = false) String id,
                             HttpServletRequest request) {
        return chatDemoService.stream(request.getSession(false), id);
    }

    @ExceptionHandler(ChatDemoService.ChatFailure.class)
    public ResponseEntity<String> chatFailure(ChatDemoService.ChatFailure failure) {
        return ResponseEntity.status(failure.status()).contentType(MediaType.TEXT_HTML)
            .body(Alert.warning(failure.getMessage()).render());
    }

    private Component buildGlobalTopNav() {
        return TopNavBuilder.create().withHtmxNavigation(false)
            .addPrimaryLink("Home", "/home").addPrimaryLink("Demos", "/demos")
            .addPrimaryLink("Javadocs", "/javadocs-view").addPrimaryLink("Forum", "/forum")
            .addPrimaryLink("Chat", "/chat").addPrimaryLink("Blog", "/blog")
            .addPrimaryLink("Docs", "/docs").build();
    }
}
