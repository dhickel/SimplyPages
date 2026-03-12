package io.mindspice.demo.pages;

import io.mindspice.demo.chat.ChatDemoService;
import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Markdown;
import io.mindspice.simplypages.components.chat.ChatTranscriptRenderer;
import io.mindspice.simplypages.components.chat.ChatTransportMode;
import io.mindspice.simplypages.components.chat.ChatUiConfig;
import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.forms.Form;
import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.modules.ChatModule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatDemoPage {

    private final ChatTranscriptRenderer<ChatDemoService.ChatMessageView, Void> transcriptRenderer =
        ChatTranscriptRenderer.<ChatDemoService.ChatMessageView, Void>builder()
            .withEmptyStateText("No messages yet. Start by sending one below.")
            .build();

    public String renderMain(String conversationId, List<ChatDemoService.ChatMessageView> history, String flashText, boolean warning) {
        ChatModule module = ChatModule.create()
            .withModuleId("chat-module")
            .withTitle("Chat Smoke Test")
            .withDescription("Forum-style pluggable rendering contracts with app-owned history/session/networking.")
            .withTranscript(buildTranscript(conversationId, history))
            .withComposer(buildComposer(conversationId))
            .withUiConfig(new ChatUiConfig(
                conversationId,
                ChatTransportMode.SSE,
                "/chat/history",
                "/chat/stream",
                "#chat-history",
                "outerHTML",
                null
            ));

        Div main = new Div().withId("chat-main")
            .withChild(Header.H1("Chat Demo"))
            .withChild(new Markdown("""
                ## Notes
                - History/session ownership remains in the demo app layer.
                - The framework module renders transcript/composer + transport hook attributes.
                - SSE emits lightweight update signals; HTMX refreshes transcript fragments.
                """));

        if (flashText != null && !flashText.isBlank()) {
            main.withChild(warning ? Alert.warning(flashText) : Alert.success(flashText));
        }

        main.withChild(module);

        return Page.builder()
            .addComponents(main)
            .build()
            .render();
    }

    public String renderTranscript(String conversationId, List<ChatDemoService.ChatMessageView> history) {
        return buildTranscript(conversationId, history).render();
    }

    private HtmlTag buildTranscript(String conversationId, List<ChatDemoService.ChatMessageView> history) {
        return new HtmlTag("div")
            .withId("chat-history")
            .withAttribute("data-chat-conversation-id", conversationId)
            .withChild(transcriptRenderer.render(history, null));
    }

    private Form buildComposer(String conversationId) {
        TextInput messageInput = TextInput.create("message")
            .withPlaceholder("Type a message")
            .required();
        messageInput.withAttribute("autocomplete", "off");
        messageInput.withAttribute("autocorrect", "off");
        messageInput.withAttribute("autocapitalize", "off");
        messageInput.withAttribute("spellcheck", "false");

        Form form = Form.create()
            .withId("chat-composer")
            .withHxPost("/chat/messages")
            .withHxTarget("#chat-history")
            .withHxSwap("outerHTML")
            .addField("Message", messageInput);
        form.withAttribute("autocomplete", "off");

        form.withChild(new HtmlTag("input", true)
            .withAttribute("type", "hidden")
            .withAttribute("name", "conversationId")
            .withAttribute("value", conversationId));

        form.withChild(Button.submit("Send"));
        return form;
    }
}
