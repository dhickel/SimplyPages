package io.mindspice.simplypages.components.forum.comments;

import io.mindspice.simplypages.components.Image;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

import java.util.List;

/**
 * Default comment component implementation.
 */
public class DefaultForumCommentComponent implements ForumCommentComponent {
    private String id;
    private String topicId;
    private String parentId;
    private int depth;
    private String author;
    private String avatarUrl;
    private String timestamp;
    private Component body;
    private List<Component> actions = List.of();
    private Integer likes;
    private Integer replies;

    public static DefaultForumCommentComponent create() {
        return new DefaultForumCommentComponent();
    }

    @Override
    public ForumCommentComponent withCommentId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public ForumCommentComponent withTopicId(String topicId) {
        this.topicId = topicId;
        return this;
    }

    @Override
    public ForumCommentComponent withParentId(String parentId) {
        this.parentId = parentId;
        return this;
    }

    @Override
    public ForumCommentComponent withDepth(int depth) {
        this.depth = depth;
        return this;
    }

    @Override
    public ForumCommentComponent withAuthor(String author) {
        this.author = author;
        return this;
    }

    @Override
    public ForumCommentComponent withAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    @Override
    public ForumCommentComponent withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @Override
    public ForumCommentComponent withBody(Component body) {
        this.body = body;
        return this;
    }

    @Override
    public ForumCommentComponent withActions(List<Component> actions) {
        this.actions = actions == null ? List.of() : List.copyOf(actions);
        return this;
    }

    @Override
    public ForumCommentComponent withLikes(Integer likes) {
        this.likes = likes;
        return this;
    }

    @Override
    public ForumCommentComponent withReplies(Integer replies) {
        this.replies = replies;
        return this;
    }

    @Override
    public String render(RenderContext context) {
        HtmlTag comment = new HtmlTag("div")
            .withAttribute("class", "forum-comment")
            .withAttribute("data-comment-id", id == null ? "" : id)
            .withAttribute("data-topic-id", topicId == null ? "" : topicId);

        if (parentId != null && !parentId.isBlank()) {
            comment.withAttribute("data-parent-id", parentId);
        }

        HtmlTag layout = new HtmlTag("div").withAttribute("class", "forum-comment-layout");
        HtmlTag identity = new HtmlTag("div").withAttribute("class", "forum-comment-identity");

        identity.withChild(new HtmlTag("span")
            .withAttribute("class", "forum-comment-author")
            .withInnerText(author == null ? "" : author));

        HtmlTag avatarSlot = new HtmlTag("div")
            .withAttribute("class", "forum-comment-avatar-slot")
            .addStyle("width", "150px")
            .addStyle("height", "150px");
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            try {
                avatarSlot.withChild(Image.create(avatarUrl, "avatar")
                    .withClass("forum-comment-avatar-image")
                    .withSize("150", "150"));
            } catch (IllegalArgumentException ignored) {
                // Keep slot blank when avatar URL is invalid.
            }
        }
        identity.withChild(avatarSlot);

        HtmlTag main = new HtmlTag("div").withAttribute("class", "forum-comment-main");
        HtmlTag meta = new HtmlTag("div").withAttribute("class", "forum-comment-meta");
        meta.withChild(new HtmlTag("span")
            .withAttribute("class", "forum-comment-timestamp")
            .withInnerText(timestamp == null ? "" : timestamp));

        if (!actions.isEmpty()) {
            HtmlTag actionContainer = new HtmlTag("div")
                .withAttribute("class", "forum-comment-actions forum-comment-annotations");
            for (Component action : actions) {
                actionContainer.withChild(action);
            }
            meta.withChild(actionContainer);
        }
        main.withChild(meta);

        if (body != null) {
            main.withChild(body);
        }

        if (likes != null || replies != null) {
            HtmlTag footer = new HtmlTag("div").withAttribute("class", "forum-comment-footer");
            if (likes != null) {
                footer.withChild(new HtmlTag("span")
                    .withAttribute("class", "forum-comment-likes")
                    .withInnerText(likes + " likes"));
            }
            if (replies != null) {
                footer.withChild(new HtmlTag("span")
                    .withAttribute("class", "forum-comment-replies")
                    .withInnerText(replies + " replies"));
            }
            main.withChild(footer);
        }

        layout.withChild(identity).withChild(main);
        comment.withChild(layout);

        return comment.render(context);
    }
}
