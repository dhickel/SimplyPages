package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.core.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultForumActionDecoratorTest {

    @Test
    @DisplayName("DefaultForumActionDecorator should render quote action by default")
    void defaultQuoteAction() {
        DefaultForumActionDecorator<String> decorator = DefaultForumActionDecorator.create();

        List<Component> actions = decorator.decorate(new ForumActionDecorator.ActionContext<>(
            ForumActionDecorator.ItemType.COMMENT,
            "c-1",
            "t-1",
            "source",
            "viewer"
        ));

        assertEquals(1, actions.size());
        String html = actions.getFirst().render();
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("forum-action-quote"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("hx-post=\"/forum/comments/c-1/quote\""));
    }

    @Test
    @DisplayName("DefaultForumActionDecorator should support edit and delete visibility")
    void editDeleteVisibility() {
        DefaultForumActionDecorator<String> decorator = DefaultForumActionDecorator.<String>create()
            .showEditWhen(ctx -> true)
            .showDeleteWhen(ctx -> true);

        List<Component> actions = decorator.decorate(new ForumActionDecorator.ActionContext<>(
            ForumActionDecorator.ItemType.TOPIC,
            "t-2",
            "t-2",
            "source",
            "viewer"
        ));

        assertEquals(3, actions.size());
        String html = actions.get(1).render() + actions.get(2).render();
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("forum-action-edit"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("forum-action-delete"));
    }
}
