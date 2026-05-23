package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.Test;

class AssistantChatModuleTest {

    @Test
    void rendersAssistantChatComposition() {
        String html = AssistantChatModule.create()
            .withModuleId("assistant")
            .withTitle("Assistant")
            .withRoomList(new HtmlTag("div").withAttribute("class", "rooms").withInnerText("rooms"))
            .withToolbar(new HtmlTag("div").withAttribute("class", "toolbar").withInnerText("tools"))
            .withChat(new HtmlTag("div").withAttribute("class", "chat-module").withInnerText("chat"))
            .withSidePanel(new HtmlTag("div").withAttribute("class", "side").withInnerText("side"))
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("section#assistant.assistant-chat-module")
            .hasElement(".assistant-chat-shell")
            .hasElement(".assistant-chat-side .rooms")
            .hasElement(".assistant-chat-side .side")
            .hasElement(".assistant-chat-main .toolbar")
            .hasElement(".assistant-chat-main .chat-module");
    }
}
