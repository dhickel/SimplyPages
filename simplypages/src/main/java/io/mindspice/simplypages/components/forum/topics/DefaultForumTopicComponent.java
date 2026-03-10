package io.mindspice.simplypages.components.forum.topics;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.components.navigation.Link;

import java.util.List;

/**
 * Default topic component implementation.
 */
public class DefaultForumTopicComponent implements ForumTopicComponent {
    private String id;
    private String title;
    private String author;
    private String timestamp;
    private ForumTopicTitleLink titleLink;
    private Component body;
    private List<Component> actions = List.of();
    private Integer likes;
    private Integer replies;

    public static DefaultForumTopicComponent create() {
        return new DefaultForumTopicComponent();
    }

    @Override
    public ForumTopicComponent withTopicId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public ForumTopicComponent withTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    public ForumTopicComponent withAuthor(String author) {
        this.author = author;
        return this;
    }

    @Override
    public ForumTopicComponent withTitleLink(ForumTopicTitleLink titleLink) {
        this.titleLink = titleLink;
        return this;
    }

    @Override
    public ForumTopicComponent withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @Override
    public ForumTopicComponent withBody(Component body) {
        this.body = body;
        return this;
    }

    @Override
    public ForumTopicComponent withActions(List<Component> actions) {
        this.actions = actions == null ? List.of() : List.copyOf(actions);
        return this;
    }

    @Override
    public ForumTopicComponent withLikes(Integer likes) {
        this.likes = likes;
        return this;
    }

    @Override
    public ForumTopicComponent withReplies(Integer replies) {
        this.replies = replies;
        return this;
    }

    @Override
    public String render(RenderContext context) {
        HtmlTag topic = new HtmlTag("div")
            .withAttribute("class", "forum-topic")
            .withAttribute("data-topic-id", id == null ? "" : id);

        HtmlTag header = new HtmlTag("div")
            .withAttribute("class", "forum-topic-header");
        HtmlTag meta = new HtmlTag("div")
            .withAttribute("class", "forum-topic-meta-left")
            .withChild(new HtmlTag("span").withAttribute("class", "forum-topic-author").withInnerText(author == null ? "" : author))
            .withChild(new HtmlTag("span").withAttribute("class", "forum-topic-timestamp").withInnerText(timestamp == null ? "" : timestamp));
        header.withChild(meta);

        if (!actions.isEmpty()) {
            HtmlTag actionContainer = new HtmlTag("div")
                .withAttribute("class", "forum-topic-actions forum-topic-annotations");
            for (Component action : actions) {
                actionContainer.withChild(action);
            }
            header.withChild(actionContainer);
        }

        topic.withChild(header);
        String safeTitle = title == null ? "" : title;
        HtmlTag titleNode = new HtmlTag("h3").withAttribute("class", "forum-topic-title");
        if (titleLink != null) {
            Link linkedTitle = Link.create(titleLink.href(), safeTitle).withClass("forum-topic-title-link");
            if (titleLink.hxGet() != null) {
                linkedTitle.withHxGet(titleLink.hxGet());
            }
            if (titleLink.hxTarget() != null) {
                linkedTitle.withHxTarget(titleLink.hxTarget());
            }
            if (titleLink.hxSwap() != null) {
                linkedTitle.withHxSwap(titleLink.hxSwap());
            }
            if (titleLink.hxPushUrl() != null) {
                linkedTitle.withAttribute("hx-push-url", titleLink.hxPushUrl());
            }
            titleNode.withChild(linkedTitle);
        } else {
            titleNode.withInnerText(safeTitle);
        }
        topic.withChild(titleNode);
        if (body != null) {
            topic.withChild(body);
        }

        if (likes != null || replies != null) {
            HtmlTag footer = new HtmlTag("div").withAttribute("class", "forum-topic-footer");
            if (likes != null) {
                footer.withChild(new HtmlTag("span").withAttribute("class", "forum-topic-likes").withInnerText(likes + " likes"));
            }
            if (replies != null) {
                footer.withChild(new HtmlTag("span").withAttribute("class", "forum-topic-replies").withInnerText(replies + " replies"));
            }
            topic.withChild(footer);
        }

        return topic.render(context);
    }
}
