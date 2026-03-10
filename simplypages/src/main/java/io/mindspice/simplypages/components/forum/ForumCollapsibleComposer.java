package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

import java.util.Objects;

/**
 * Reusable collapsible wrapper for forum composer content.
 * Renders as a details/summary block and defaults to collapsed.
 */
public final class ForumCollapsibleComposer implements Component {
    private String summaryText;
    private Component content;
    private boolean expanded;
    private String additionalClass;

    private ForumCollapsibleComposer(String summaryText, Component content) {
        this.summaryText = normalizeSummaryText(summaryText);
        this.content = Objects.requireNonNull(content, "content");
    }

    public static ForumCollapsibleComposer create(String summaryText, Component content) {
        return new ForumCollapsibleComposer(summaryText, content);
    }

    public ForumCollapsibleComposer withSummaryText(String summaryText) {
        this.summaryText = normalizeSummaryText(summaryText);
        return this;
    }

    public ForumCollapsibleComposer withContent(Component content) {
        this.content = Objects.requireNonNull(content, "content");
        return this;
    }

    public ForumCollapsibleComposer withExpanded(boolean expanded) {
        this.expanded = expanded;
        return this;
    }

    public ForumCollapsibleComposer withClass(String cssClass) {
        this.additionalClass = (cssClass == null || cssClass.isBlank()) ? null : cssClass.trim();
        return this;
    }

    public ForumCollapsibleComposer expandedByDefault() {
        this.expanded = true;
        return this;
    }

    @Override
    public String render(RenderContext context) {
        HtmlTag details = new HtmlTag("details")
            .withAttribute("class", detailsClass());
        if (expanded) {
            details.withAttribute("open", "");
        }

        details.withChild(new HtmlTag("summary")
            .withAttribute("class", "forum-composer-summary")
            .withInnerText(summaryText));

        details.withChild(new HtmlTag("div")
            .withAttribute("class", "forum-composer-content")
            .withChild(content));

        return details.render(context);
    }

    private static String normalizeSummaryText(String summaryText) {
        if (summaryText == null || summaryText.isBlank()) {
            return "Compose";
        }
        return summaryText;
    }

    private String detailsClass() {
        if (additionalClass == null) {
            return "forum-composer";
        }
        return "forum-composer " + additionalClass;
    }
}
