package io.mindspice.simplypages.components.forum.actions;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Default quote/edit/delete action provider with configurable visibility and endpoints.
 */
public class DefaultForumActionProvider<SOURCE, CTX> implements ForumActionProvider<SOURCE, CTX> {

    private Predicate<ForumActionContext<SOURCE, CTX>> showQuote = context -> true;
    private Predicate<ForumActionContext<SOURCE, CTX>> showEdit = context -> false;
    private Predicate<ForumActionContext<SOURCE, CTX>> showDelete = context -> false;

    private Function<ForumActionContext<SOURCE, CTX>, String> quoteEndpoint =
        context -> defaultEndpoint(context, "quote");
    private Function<ForumActionContext<SOURCE, CTX>, String> editEndpoint =
        context -> defaultEndpoint(context, "edit");
    private Function<ForumActionContext<SOURCE, CTX>, String> deleteEndpoint =
        context -> defaultEndpoint(context, "delete");

    private String quoteIcon = "↩";
    private String editIcon = "✎";
    private String deleteIcon = "🗑";
    private String hxTarget;
    private String hxSwap;
    private String quoteHxInclude;

    public static <SOURCE, CTX> DefaultForumActionProvider<SOURCE, CTX> create() {
        return new DefaultForumActionProvider<>();
    }

    public DefaultForumActionProvider<SOURCE, CTX> showQuoteWhen(Predicate<ForumActionContext<SOURCE, CTX>> predicate) {
        this.showQuote = Objects.requireNonNull(predicate, "predicate");
        return this;
    }

    public DefaultForumActionProvider<SOURCE, CTX> showEditWhen(Predicate<ForumActionContext<SOURCE, CTX>> predicate) {
        this.showEdit = Objects.requireNonNull(predicate, "predicate");
        return this;
    }

    public DefaultForumActionProvider<SOURCE, CTX> showDeleteWhen(Predicate<ForumActionContext<SOURCE, CTX>> predicate) {
        this.showDelete = Objects.requireNonNull(predicate, "predicate");
        return this;
    }

    public DefaultForumActionProvider<SOURCE, CTX> withQuoteEndpoint(Function<ForumActionContext<SOURCE, CTX>, String> endpoint) {
        this.quoteEndpoint = Objects.requireNonNull(endpoint, "endpoint");
        return this;
    }

    public DefaultForumActionProvider<SOURCE, CTX> withEditEndpoint(Function<ForumActionContext<SOURCE, CTX>, String> endpoint) {
        this.editEndpoint = Objects.requireNonNull(endpoint, "endpoint");
        return this;
    }

    public DefaultForumActionProvider<SOURCE, CTX> withDeleteEndpoint(Function<ForumActionContext<SOURCE, CTX>, String> endpoint) {
        this.deleteEndpoint = Objects.requireNonNull(endpoint, "endpoint");
        return this;
    }

    public DefaultForumActionProvider<SOURCE, CTX> withQuoteIcon(String icon) {
        this.quoteIcon = icon == null ? "" : icon;
        return this;
    }

    public DefaultForumActionProvider<SOURCE, CTX> withEditIcon(String icon) {
        this.editIcon = icon == null ? "" : icon;
        return this;
    }

    public DefaultForumActionProvider<SOURCE, CTX> withDeleteIcon(String icon) {
        this.deleteIcon = icon == null ? "" : icon;
        return this;
    }

    public DefaultForumActionProvider<SOURCE, CTX> withHxTarget(String hxTarget) {
        this.hxTarget = normalizeAttributeValue(hxTarget);
        return this;
    }

    public DefaultForumActionProvider<SOURCE, CTX> withHxSwap(String hxSwap) {
        this.hxSwap = normalizeAttributeValue(hxSwap);
        return this;
    }

    public DefaultForumActionProvider<SOURCE, CTX> withQuoteHxInclude(String hxIncludeSelector) {
        this.quoteHxInclude = normalizeAttributeValue(hxIncludeSelector);
        return this;
    }

    @Override
    public List<Component> provide(ForumActionContext<SOURCE, CTX> context) {
        List<Component> actions = new ArrayList<>();

        if (showQuote.test(context)) {
            String endpoint = quoteEndpoint.apply(context);
            if (endpoint != null && !endpoint.isBlank()) {
                actions.add(buildAction(
                    "forum-action-quote",
                    quoteIcon,
                    "Quote",
                    endpoint,
                    context,
                    quoteHxInclude
                ));
            }
        }

        if (showEdit.test(context)) {
            String endpoint = editEndpoint.apply(context);
            if (endpoint != null && !endpoint.isBlank()) {
                actions.add(buildAction("forum-action-edit", editIcon, "Edit", endpoint, context, null));
            }
        }

        if (showDelete.test(context)) {
            String endpoint = deleteEndpoint.apply(context);
            if (endpoint != null && !endpoint.isBlank()) {
                actions.add(buildAction("forum-action-delete", deleteIcon, "Delete", endpoint, context, null));
            }
        }

        return List.copyOf(actions);
    }

    private Component buildAction(
            String cssClass,
            String icon,
            String label,
            String endpoint,
            ForumActionContext<SOURCE, CTX> context,
            String hxInclude
    ) {
        String text = (icon == null || icon.isBlank()) ? label : (icon + " " + label);
        HtmlTag button = new HtmlTag("button")
            .withAttribute("type", "button")
            .withAttribute("class", "forum-action " + cssClass)
            .withAttribute("data-item-id", context.itemId())
            .withAttribute("data-item-type", context.itemType().name().toLowerCase())
            .withAttribute("data-topic-id", context.topicId() == null ? "" : context.topicId())
            .withAttribute("hx-post", endpoint)
            .withInnerText(text);

        if (hxTarget != null) {
            button.withAttribute("hx-target", hxTarget);
        }
        if (hxSwap != null) {
            button.withAttribute("hx-swap", hxSwap);
        }
        if (hxInclude != null) {
            button.withAttribute("hx-include", hxInclude);
        }
        return button;
    }

    private static <SOURCE, CTX> String defaultEndpoint(ForumActionContext<SOURCE, CTX> context, String action) {
        String kind = context.itemType() == ForumActionType.TOPIC ? "topics" : "comments";
        return "/forum/" + kind + "/" + context.itemId() + "/" + action;
    }

    private static String normalizeAttributeValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
