package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.Module;

/**
 * Composed assistant chat/workspace shell built around the existing {@link ChatModule}.
 */
public class AssistantChatModule extends Module {

    private String description;
    private Component roomList;
    private Component toolbar;
    private Component chat;
    private Component sidePanel;

    public AssistantChatModule() {
        super("section");
        this.withClass("assistant-chat-module");
    }

    public static AssistantChatModule create() {
        return new AssistantChatModule();
    }

    @Override
    public AssistantChatModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public AssistantChatModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    public AssistantChatModule withDescription(String description) {
        this.description = description;
        return this;
    }

    public AssistantChatModule withRoomList(Component roomList) {
        this.roomList = roomList;
        return this;
    }

    public AssistantChatModule withToolbar(Component toolbar) {
        this.toolbar = toolbar;
        return this;
    }

    public AssistantChatModule withChat(Component chat) {
        this.chat = chat;
        return this;
    }

    public AssistantChatModule withSidePanel(Component sidePanel) {
        this.sidePanel = sidePanel;
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }
        if (description != null && !description.isEmpty()) {
            super.withChild(new HtmlTag("p")
                .withAttribute("class", "module-description")
                .withInnerText(description));
        }

        HtmlTag shell = new HtmlTag("div").withAttribute("class", "assistant-chat-shell");
        if (roomList != null || sidePanel != null) {
            HtmlTag side = new HtmlTag("aside").withAttribute("class", "assistant-chat-side");
            if (roomList != null) {
                side.withChild(roomList);
            }
            if (sidePanel != null) {
                side.withChild(sidePanel);
            }
            shell.withChild(side);
        }

        HtmlTag main = new HtmlTag("div").withAttribute("class", "assistant-chat-main");
        if (toolbar != null) {
            main.withChild(new HtmlTag("div")
                .withAttribute("class", "assistant-chat-toolbar")
                .withChild(toolbar));
        }
        main.withChild(chat == null
            ? new HtmlTag("div").withAttribute("class", "assistant-chat-empty").withInnerText("No chat configured.")
            : chat);
        shell.withChild(main);

        super.withChild(shell);
    }
}
