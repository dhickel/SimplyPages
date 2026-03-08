package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Default action decorator with configurable quote/edit/delete action rendering.
 */
public class DefaultForumActionDecorator<VIEWER> implements ForumActionDecorator<VIEWER> {

    private Predicate<ActionContext<VIEWER>> showQuote = context -> true;
    private Predicate<ActionContext<VIEWER>> showEdit = context -> false;
    private Predicate<ActionContext<VIEWER>> showDelete = context -> false;

    private Function<ActionContext<VIEWER>, String> quoteEndpoint =
        context -> defaultEndpoint(context, "quote");
    private Function<ActionContext<VIEWER>, String> editEndpoint =
        context -> defaultEndpoint(context, "edit");
    private Function<ActionContext<VIEWER>, String> deleteEndpoint =
        context -> defaultEndpoint(context, "delete");

    private String quoteIcon = "↩";
    private String editIcon = "✎";
    private String deleteIcon = "🗑";

    public static <VIEWER> DefaultForumActionDecorator<VIEWER> create() {
        return new DefaultForumActionDecorator<>();
    }

    public DefaultForumActionDecorator<VIEWER> showQuoteWhen(Predicate<ActionContext<VIEWER>> predicate) {
        this.showQuote = Objects.requireNonNull(predicate, "predicate");
        return this;
    }

    public DefaultForumActionDecorator<VIEWER> showEditWhen(Predicate<ActionContext<VIEWER>> predicate) {
        this.showEdit = Objects.requireNonNull(predicate, "predicate");
        return this;
    }

    public DefaultForumActionDecorator<VIEWER> showDeleteWhen(Predicate<ActionContext<VIEWER>> predicate) {
        this.showDelete = Objects.requireNonNull(predicate, "predicate");
        return this;
    }

    public DefaultForumActionDecorator<VIEWER> withQuoteEndpoint(Function<ActionContext<VIEWER>, String> endpoint) {
        this.quoteEndpoint = Objects.requireNonNull(endpoint, "endpoint");
        return this;
    }

    public DefaultForumActionDecorator<VIEWER> withEditEndpoint(Function<ActionContext<VIEWER>, String> endpoint) {
        this.editEndpoint = Objects.requireNonNull(endpoint, "endpoint");
        return this;
    }

    public DefaultForumActionDecorator<VIEWER> withDeleteEndpoint(Function<ActionContext<VIEWER>, String> endpoint) {
        this.deleteEndpoint = Objects.requireNonNull(endpoint, "endpoint");
        return this;
    }

    public DefaultForumActionDecorator<VIEWER> withQuoteIcon(String icon) {
        this.quoteIcon = icon == null ? "" : icon;
        return this;
    }

    public DefaultForumActionDecorator<VIEWER> withEditIcon(String icon) {
        this.editIcon = icon == null ? "" : icon;
        return this;
    }

    public DefaultForumActionDecorator<VIEWER> withDeleteIcon(String icon) {
        this.deleteIcon = icon == null ? "" : icon;
        return this;
    }

    @Override
    public List<Component> decorate(ActionContext<VIEWER> context) {
        List<Component> actions = new ArrayList<>();

        if (showQuote.test(context)) {
            String endpoint = quoteEndpoint.apply(context);
            if (endpoint != null && !endpoint.isBlank()) {
                actions.add(buildAction("forum-action-quote", quoteIcon, "Quote", endpoint, context));
            }
        }

        if (showEdit.test(context)) {
            String endpoint = editEndpoint.apply(context);
            if (endpoint != null && !endpoint.isBlank()) {
                actions.add(buildAction("forum-action-edit", editIcon, "Edit", endpoint, context));
            }
        }

        if (showDelete.test(context)) {
            String endpoint = deleteEndpoint.apply(context);
            if (endpoint != null && !endpoint.isBlank()) {
                actions.add(buildAction("forum-action-delete", deleteIcon, "Delete", endpoint, context));
            }
        }

        return List.copyOf(actions);
    }

    private Component buildAction(
            String cssClass,
            String icon,
            String label,
            String endpoint,
            ActionContext<VIEWER> context
    ) {
        String text = (icon == null || icon.isBlank()) ? label : (icon + " " + label);
        return new HtmlTag("button")
            .withAttribute("type", "button")
            .withAttribute("class", "forum-action " + cssClass)
            .withAttribute("data-item-id", context.itemId())
            .withAttribute("data-item-type", context.itemType().name().toLowerCase())
            .withAttribute("data-topic-id", context.topicId() == null ? "" : context.topicId())
            .withAttribute("hx-post", endpoint)
            .withInnerText(text);
    }

    private static <VIEWER> String defaultEndpoint(ActionContext<VIEWER> context, String action) {
        String kind = context.itemType() == ItemType.TOPIC ? "topics" : "comments";
        return "/forum/" + kind + "/" + context.itemId() + "/" + action;
    }
}
