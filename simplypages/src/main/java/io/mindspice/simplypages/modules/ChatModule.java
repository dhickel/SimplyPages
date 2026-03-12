package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.chat.ChatUiConfig;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.Module;

/**
 * Module for rendering an embeddable chat transcript and composer shell.
 *
 * <p>History, session identity, and networking are application-owned. This module only renders
 * structure and transport hook attributes.</p>
 */
public class ChatModule extends Module {

    private String description;
    private Component transcript;
    private Component composer;
    private ChatUiConfig uiConfig;

    public ChatModule() {
        super("div");
        this.withClass("chat-module");
    }

    public static ChatModule create() {
        return new ChatModule();
    }

    public ChatModule withDescription(String description) {
        this.description = description;
        return this;
    }

    public ChatModule withTranscript(Component transcript) {
        this.transcript = transcript;
        return this;
    }

    public ChatModule withComposer(Component composer) {
        this.composer = composer;
        return this;
    }

    public ChatModule withUiConfig(ChatUiConfig uiConfig) {
        this.uiConfig = uiConfig;
        return this;
    }

    @Override
    public ChatModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public ChatModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    @Override
    protected void buildContent() {
        if (uiConfig == null) {
            throw new IllegalStateException("ChatModule requires ChatUiConfig via withUiConfig(...)");
        }

        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }

        if (description != null && !description.isEmpty()) {
            super.withChild(new HtmlTag("p")
                .withAttribute("class", "module-description")
                .withInnerText(description));
        }

        HtmlTag shell = new HtmlTag("div")
            .withAttribute("class", "chat-module-shell")
            .withAttribute("data-sp-chat", "true")
            .withAttribute("data-sp-chat-conversation-id", uiConfig.conversationId())
            .withAttribute("data-sp-chat-transport", uiConfig.transportMode().name())
            .withAttribute("data-sp-chat-history-endpoint", uiConfig.historyEndpoint())
            .withAttribute("data-sp-chat-history-target", uiConfig.historyTargetSelector())
            .withAttribute("data-sp-chat-history-swap", uiConfig.historySwap());

        if (uiConfig.streamEndpoint() != null && !uiConfig.streamEndpoint().isBlank()) {
            shell.withAttribute("data-sp-chat-stream-endpoint", uiConfig.streamEndpoint());
        }

        if (uiConfig.pollingIntervalMs() != null) {
            shell.withAttribute("data-sp-chat-polling-ms", String.valueOf(uiConfig.pollingIntervalMs()));
        }

        shell.withChild(new HtmlTag("div")
            .withAttribute("class", "chat-module-transcript-region")
            .withChild(transcript == null ? new HtmlTag("div")
                .withAttribute("class", "chat-module-empty-transcript")
                .withInnerText("No messages yet.") : transcript));

        if (composer != null) {
            shell.withChild(new HtmlTag("div")
                .withAttribute("class", "chat-module-composer-region")
                .withChild(composer));
        }

        super.withChild(shell);
    }
}
