package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.components.forum.actions.DefaultForumActionProvider;
import io.mindspice.simplypages.components.forum.actions.ForumActionContext;
import io.mindspice.simplypages.components.forum.actions.ForumActionType;
import io.mindspice.simplypages.core.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultForumActionProviderTest {

    @Test
    @DisplayName("DefaultForumActionProvider should render quote action by default")
    void defaultQuoteAction() {
        DefaultForumActionProvider<String, String> provider = DefaultForumActionProvider.create();

        List<Component> actions = provider.provide(new ForumActionContext<>(
            ForumActionType.COMMENT,
            "c-1",
            "t-1",
            "source",
            "ctx"
        ));

        assertEquals(1, actions.size());
        String html = actions.getFirst().render();
        assertTrue(html.contains("forum-action-quote"));
        assertTrue(html.contains("hx-post=\"/forum/comments/c-1/quote\""));
    }

    @Test
    @DisplayName("DefaultForumActionProvider should support edit and delete visibility")
    void editDeleteVisibility() {
        DefaultForumActionProvider<String, String> provider = DefaultForumActionProvider.<String, String>create()
            .showEditWhen(ctx -> true)
            .showDeleteWhen(ctx -> true);

        List<Component> actions = provider.provide(new ForumActionContext<>(
            ForumActionType.TOPIC,
            "t-2",
            "t-2",
            "source",
            "ctx"
        ));

        assertEquals(3, actions.size());
        String html = actions.get(1).render() + actions.get(2).render();
        assertTrue(html.contains("forum-action-edit"));
        assertTrue(html.contains("forum-action-delete"));
    }

    @Test
    @DisplayName("DefaultForumActionProvider should not emit actions when endpoints are blank")
    void blankEndpointsSuppressActions() {
        DefaultForumActionProvider<String, String> provider = DefaultForumActionProvider.<String, String>create()
            .withQuoteEndpoint(ctx -> "  ");

        List<Component> actions = provider.provide(new ForumActionContext<>(
            ForumActionType.COMMENT,
            "c-1",
            "t-1",
            "source",
            "ctx"
        ));

        assertTrue(actions.isEmpty());
    }

    @Test
    @DisplayName("DefaultForumActionProvider should default topic edit endpoint correctly")
    void defaultTopicEndpointShape() {
        DefaultForumActionProvider<String, String> provider = DefaultForumActionProvider.<String, String>create()
            .showEditWhen(ctx -> true)
            .showQuoteWhen(ctx -> false)
            .showDeleteWhen(ctx -> false);

        List<Component> actions = provider.provide(new ForumActionContext<>(
            ForumActionType.TOPIC,
            "t-99",
            "t-99",
            "source",
            "ctx"
        ));

        assertEquals(1, actions.size());
        String html = actions.getFirst().render();
        assertTrue(html.contains("hx-post=\"/forum/topics/t-99/edit\""));
    }

    @Test
    @DisplayName("DefaultForumActionProvider should use label-only text when icon is blank")
    void blankIconUsesLabelOnly() {
        DefaultForumActionProvider<String, String> provider = DefaultForumActionProvider.<String, String>create()
            .withQuoteIcon(" ")
            .showEditWhen(ctx -> false)
            .showDeleteWhen(ctx -> false);

        List<Component> actions = provider.provide(new ForumActionContext<>(
            ForumActionType.COMMENT,
            "c-7",
            "t-7",
            "source",
            "ctx"
        ));

        String html = actions.getFirst().render();
        assertTrue(html.contains(">Quote<"));
        assertFalse(html.contains("↩"));
    }

    @Test
    @DisplayName("DefaultForumActionProvider should apply optional hx-target and hx-swap attributes")
    void optionalHxTargetAndSwap() {
        DefaultForumActionProvider<String, String> provider = DefaultForumActionProvider.<String, String>create()
            .withHxTarget("#comment-thread")
            .withHxSwap("outerHTML")
            .showEditWhen(ctx -> false)
            .showDeleteWhen(ctx -> false);

        List<Component> actions = provider.provide(new ForumActionContext<>(
            ForumActionType.COMMENT,
            "c-8",
            "t-8",
            "source",
            "ctx"
        ));

        assertEquals(1, actions.size());
        String html = actions.getFirst().render();
        assertTrue(html.contains("hx-target=\"#comment-thread\""));
        assertTrue(html.contains("hx-swap=\"outerHTML\""));
    }
}
