package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.chat.ChatTransportMode;
import io.mindspice.simplypages.components.chat.ChatUiConfig;
import io.mindspice.simplypages.components.forms.Form;
import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatModuleTest {

    @Test
    @DisplayName("ChatModule should render shell with configured transport hooks")
    void rendersChatModuleWithHookAttributes() {
        Form composer = Form.create()
            .withId("chat-composer")
            .withHxPost("/chat/messages")
            .withHxTarget("#chat-history")
            .withHxSwap("outerHTML")
            .addField("Message", TextInput.create("message"));

        ChatModule module = ChatModule.create()
            .withTitle("Chat")
            .withDescription("Smoke test")
            .withTranscript(new HtmlTag("div").withId("chat-history").withInnerText("history"))
            .withComposer(composer)
            .withUiConfig(new ChatUiConfig(
                "conv-1",
                ChatTransportMode.SSE,
                "/chat/history",
                "/chat/stream",
                "#chat-history",
                "outerHTML",
                null
            ));

        String html = module.render();

        HtmlAssert.assertThat(html)
            .hasElement(".chat-module")
            .hasElement(".chat-module-shell")
            .attributeEquals(".chat-module-shell", "data-sp-chat-conversation-id", "conv-1")
            .attributeEquals(".chat-module-shell", "data-sp-chat-transport", "SSE")
            .attributeEquals(".chat-module-shell", "data-sp-chat-history-endpoint", "/chat/history")
            .attributeEquals(".chat-module-shell", "data-sp-chat-stream-endpoint", "/chat/stream")
            .hasElement("#chat-history")
            .hasElement("form#chat-composer");
    }

    @Test
    @DisplayName("ChatModule should require ChatUiConfig")
    void requiresUiConfig() {
        ChatModule module = ChatModule.create()
            .withTranscript(new HtmlTag("div").withId("chat-history").withInnerText("history"));

        assertThrows(IllegalStateException.class, module::render);
    }
}
