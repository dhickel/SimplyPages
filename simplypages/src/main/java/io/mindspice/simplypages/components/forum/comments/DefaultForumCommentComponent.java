package io.mindspice.simplypages.components.forum.comments;

import io.mindspice.simplypages.components.Image;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

import java.util.List;

/**
 * Default flat comment card with raw depth metadata and bounded CSS indentation.
 *
 * <p>Every card emits the configured signed {@code int} as {@code data-depth}. Only the
 * visual class is clamped to {@code forum-comment-depth-0} through
 * {@code forum-comment-depth-4}; negative depths look like zero. Framework CSS indents
 * from the logical start by 16px per level (8px at viewport widths up to 920px), limited
 * to 8% of the containing inline size. Host CSS can override these defaults.</p>
 *
 * <p>Depth and parent ID are independent metadata, not a validated hierarchy. Rendering
 * keeps configured state unchanged and dispatches body/actions with the supplied context.
 * Templates retain live rendering, including later configuration and subclass overrides.
 * Finish configuration before sharing; contexts and child delegates have their own
 * sharing requirements.</p>
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

    /**
     * Stores the raw depth unchanged; only its default visual class is clamped to 0..4.
     *
     * @param depth any signed int, independent of the parent ID
     * @return this component
     */
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
        int visualDepth = Math.max(0, Math.min(depth, 4));
        HtmlTag comment = new HtmlTag("div")
            .withAttribute("class", "forum-comment forum-comment-depth-" + visualDepth)
            .withAttribute("data-comment-id", id == null ? "" : id)
            .withAttribute("data-topic-id", topicId == null ? "" : topicId)
            .withAttribute("data-depth", String.valueOf(depth));

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
