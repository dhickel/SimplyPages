package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.components.Image;
import io.mindspice.simplypages.components.Markdown;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Unified, generic helper API for rendering forum category/topic/comment views.
 *
 * <p>This helper keeps rendering deterministic and delegates data/authorization policy to the
 * application through adapters and decorators.</p>
 */
public final class ForumHelper<CATEGORY, TOPIC, COMMENT, VIEWER> {

    public interface CategoryAdapter<CATEGORY> {
        String id(CATEGORY category);
        String title(CATEGORY category);

        default String description(CATEGORY category) {
            return "";
        }

        default Integer topicCount(CATEGORY category) {
            return null;
        }
    }

    public interface TopicAdapter<TOPIC> {
        String id(TOPIC topic);
        String title(TOPIC topic);
        String body(TOPIC topic);

        default String author(TOPIC topic) {
            return "Anonymous";
        }

        default String timestamp(TOPIC topic) {
            return "";
        }

        default Integer likes(TOPIC topic) {
            return null;
        }

        default Integer replies(TOPIC topic) {
            return null;
        }
    }

    public interface CommentAdapter<COMMENT> {
        String id(COMMENT comment);
        String topicId(COMMENT comment);
        String body(COMMENT comment);

        default String parentId(COMMENT comment) {
            return null;
        }

        default String author(COMMENT comment) {
            return "Anonymous";
        }

        default String timestamp(COMMENT comment) {
            return "";
        }

        default int depth(COMMENT comment) {
            return 0;
        }

        default String avatarUrl(COMMENT comment) {
            return null;
        }

        default Integer likes(COMMENT comment) {
            return null;
        }

        default Integer replies(COMMENT comment) {
            return null;
        }
    }

    @FunctionalInterface
    public interface CategoryRenderer<CATEGORY, VIEWER> {
        Component render(CategoryView<CATEGORY, VIEWER> view);
    }

    @FunctionalInterface
    public interface TopicRenderer<TOPIC, VIEWER> {
        Component render(TopicView<TOPIC, VIEWER> view);
    }

    @FunctionalInterface
    public interface CommentRenderer<COMMENT, VIEWER> {
        Component render(CommentView<COMMENT, VIEWER> view);
    }

    @FunctionalInterface
    public interface CommentPaginationEndpointResolver {
        String endpoint(String topicId, int page, int commentsPerPage);
    }

    @FunctionalInterface
    public interface TopicPaginationEndpointResolver {
        String endpoint(String scopeId, int page, int topicsPerPage);
    }

    public record TopicPagination(
        String scopeId,
        int page,
        int topicsPerPage,
        int totalTopics
    ) {
        public TopicPagination {
            if (scopeId == null || scopeId.isBlank()) {
                throw new IllegalArgumentException("Topic pagination requires a non-blank scopeId");
            }
            if (page < 1) {
                throw new IllegalArgumentException("Topic pagination page must be >= 1");
            }
            if (topicsPerPage < 1) {
                throw new IllegalArgumentException("Topic pagination topicsPerPage must be >= 1");
            }
            if (totalTopics < 0) {
                throw new IllegalArgumentException("Topic pagination totalTopics must be >= 0");
            }
        }

        public int totalPages() {
            return Math.max(1, (int) Math.ceil((double) totalTopics / topicsPerPage));
        }

        public int currentPage() {
            return Math.min(page, totalPages());
        }

        public boolean hasPrevious() {
            return currentPage() > 1;
        }

        public boolean hasNext() {
            return currentPage() < totalPages();
        }
    }

    public record CommentPagination(
        String topicId,
        int page,
        int commentsPerPage,
        int totalComments
    ) {
        public CommentPagination {
            if (topicId == null || topicId.isBlank()) {
                throw new IllegalArgumentException("Comment pagination requires a non-blank topicId");
            }
            if (page < 1) {
                throw new IllegalArgumentException("Comment pagination page must be >= 1");
            }
            if (commentsPerPage < 1) {
                throw new IllegalArgumentException("Comment pagination commentsPerPage must be >= 1");
            }
            if (totalComments < 0) {
                throw new IllegalArgumentException("Comment pagination totalComments must be >= 0");
            }
        }

        public int totalPages() {
            return Math.max(1, (int) Math.ceil((double) totalComments / commentsPerPage));
        }

        public int currentPage() {
            return Math.min(page, totalPages());
        }

        public boolean hasPrevious() {
            return currentPage() > 1;
        }

        public boolean hasNext() {
            return currentPage() < totalPages();
        }
    }

    public record CategoryView<CATEGORY, VIEWER>(
        CATEGORY source,
        String id,
        String title,
        String description,
        Integer topicCount,
        VIEWER viewer
    ) {}

    public record TopicView<TOPIC, VIEWER>(
        TOPIC source,
        String id,
        String title,
        String author,
        String timestamp,
        Integer likes,
        Integer replies,
        Component body,
        List<Component> actions,
        VIEWER viewer
    ) {}

    public record CommentView<COMMENT, VIEWER>(
        COMMENT source,
        String id,
        String topicId,
        String parentId,
        int depth,
        String author,
        String avatarUrl,
        String timestamp,
        Integer likes,
        Integer replies,
        Component body,
        List<Component> actions,
        VIEWER viewer
    ) {}

    private final CategoryAdapter<CATEGORY> categoryAdapter;
    private final TopicAdapter<TOPIC> topicAdapter;
    private final CommentAdapter<COMMENT> commentAdapter;

    private final ForumTagParser tagParser;
    private final ForumTagResolverRegistry resolverRegistry;

    private final CategoryRenderer<CATEGORY, VIEWER> categoryRenderer;
    private final TopicRenderer<TOPIC, VIEWER> topicRenderer;
    private final CommentRenderer<COMMENT, VIEWER> commentRenderer;

    private final ForumActionDecorator<VIEWER> actionDecorator;

    private final Function<VIEWER, Component> topicComposerLauncher;
    private final Function<VIEWER, Component> commentComposer;
    private final TopicPaginationEndpointResolver topicPaginationEndpointResolver;
    private final String topicsPaginationHxTarget;
    private final String topicsPaginationHxSwap;
    private final CommentPaginationEndpointResolver commentPaginationEndpointResolver;
    private final String commentsPaginationHxTarget;
    private final String commentsPaginationHxSwap;

    private final boolean parseTopicBodies;
    private final boolean parseCommentBodies;

    private ForumHelper(Builder<CATEGORY, TOPIC, COMMENT, VIEWER> builder) {
        this.categoryAdapter = builder.categoryAdapter;
        this.topicAdapter = builder.topicAdapter;
        this.commentAdapter = builder.commentAdapter;
        this.tagParser = builder.tagParser;
        this.resolverRegistry = builder.resolverRegistry;
        this.categoryRenderer = builder.categoryRenderer;
        this.topicRenderer = builder.topicRenderer;
        this.commentRenderer = builder.commentRenderer;
        this.actionDecorator = builder.actionDecorator;
        this.topicComposerLauncher = builder.topicComposerLauncher;
        this.commentComposer = builder.commentComposer;
        this.topicPaginationEndpointResolver = builder.topicPaginationEndpointResolver;
        this.topicsPaginationHxTarget = builder.topicsPaginationHxTarget;
        this.topicsPaginationHxSwap = builder.topicsPaginationHxSwap;
        this.commentPaginationEndpointResolver = builder.commentPaginationEndpointResolver;
        this.commentsPaginationHxTarget = builder.commentsPaginationHxTarget;
        this.commentsPaginationHxSwap = builder.commentsPaginationHxSwap;
        this.parseTopicBodies = builder.parseTopicBodies;
        this.parseCommentBodies = builder.parseCommentBodies;
    }

    public static <CATEGORY, TOPIC, COMMENT, VIEWER> Builder<CATEGORY, TOPIC, COMMENT, VIEWER> builder(
            CategoryAdapter<CATEGORY> categoryAdapter,
            TopicAdapter<TOPIC> topicAdapter,
            CommentAdapter<COMMENT> commentAdapter
    ) {
        return new Builder<>(categoryAdapter, topicAdapter, commentAdapter);
    }

    public Component renderCategoriesView(Collection<CATEGORY> categories, VIEWER viewer) {
        HtmlTag root = new HtmlTag("div").withAttribute("class", "forum-categories-view");
        if (categories == null) {
            return root;
        }

        for (CATEGORY category : categories) {
            String id = requireId(categoryAdapter.id(category), "category id");
            CategoryView<CATEGORY, VIEWER> view = new CategoryView<>(
                category,
                id,
                safe(categoryAdapter.title(category)),
                safe(categoryAdapter.description(category)),
                categoryAdapter.topicCount(category),
                viewer
            );
            root.withChild(categoryRenderer.render(view));
        }
        return root;
    }

    public Component renderTopicsView(Collection<TOPIC> topics, VIEWER viewer) {
        return renderTopicsView(topics, viewer, null);
    }

    public Component renderTopicsView(
            Collection<TOPIC> topics,
            VIEWER viewer,
            TopicPagination pagination
    ) {
        HtmlTag root = new HtmlTag("div").withAttribute("class", "forum-topics-view");

        if (topicComposerLauncher != null) {
            Component launcher = topicComposerLauncher.apply(viewer);
            if (launcher != null) {
                root.withChild(new HtmlTag("div")
                    .withAttribute("class", "forum-topics-toolbar")
                    .withChild(launcher));
            }
        }

        if (topics == null || topics.isEmpty()) {
            appendTopicPaginationControls(root, pagination);
            return root;
        }

        List<TOPIC> topicList = topics == null ? List.of() : List.copyOf(topics);
        if (pagination != null) {
            topicList = paginateTopics(topicList, pagination);
        }

        if (topicList.isEmpty()) {
            appendTopicPaginationControls(root, pagination);
            return root;
        }

        List<List<ForumTagParser.Segment>> parsedBodies = new ArrayList<>(topicList.size());
        Map<String, Set<String>> requestedTags = new LinkedHashMap<>();

        for (TOPIC topic : topicList) {
            requireId(topicAdapter.id(topic), "topic id");
            List<ForumTagParser.Segment> segments = parseBody(topicAdapter.body(topic), parseTopicBodies);
            parsedBodies.add(segments);
            collectTagRequests(requestedTags, segments);
        }

        Map<String, Map<String, Component>> resolvedTags = resolverRegistry.resolveAll(requestedTags);

        for (int i = 0; i < topicList.size(); i++) {
            TOPIC topic = topicList.get(i);
            String topicId = requireId(topicAdapter.id(topic), "topic id");
            Component body = renderBody(parsedBodies.get(i), resolvedTags, "forum-topic-body-content");

            List<Component> actions = normalizeActions(actionDecorator.decorate(
                new ForumActionDecorator.ActionContext<>(
                    ForumActionDecorator.ItemType.TOPIC,
                    topicId,
                    topicId,
                    topic,
                    viewer
                )
            ));

            TopicView<TOPIC, VIEWER> view = new TopicView<>(
                topic,
                topicId,
                safe(topicAdapter.title(topic)),
                safe(topicAdapter.author(topic)),
                safe(topicAdapter.timestamp(topic)),
                topicAdapter.likes(topic),
                topicAdapter.replies(topic),
                body,
                actions,
                viewer
            );

            root.withChild(topicRenderer.render(view));
        }

        appendTopicPaginationControls(root, pagination);
        return root;
    }

    public Component renderCommentsView(Collection<COMMENT> comments, VIEWER viewer) {
        return renderCommentsView(comments, viewer, null);
    }

    public Component renderCommentsView(
            Collection<COMMENT> comments,
            VIEWER viewer,
            CommentPagination pagination
    ) {
        HtmlTag root = new HtmlTag("div").withAttribute("class", "forum-comments-view");

        List<COMMENT> commentList = comments == null ? List.of() : List.copyOf(comments);
        if (pagination != null) {
            commentList = paginateComments(commentList, pagination);
        }

        if (commentList.isEmpty()) {
            appendPaginationControls(root, pagination);
            appendCommentComposer(root, viewer);
            return root;
        }

        List<List<ForumTagParser.Segment>> parsedBodies = new ArrayList<>(commentList.size());
        Map<String, Set<String>> requestedTags = new LinkedHashMap<>();

        for (COMMENT comment : commentList) {
            requireId(commentAdapter.id(comment), "comment id");
            requireId(commentAdapter.topicId(comment), "comment topic id");
            List<ForumTagParser.Segment> segments = parseBody(commentAdapter.body(comment), parseCommentBodies);
            parsedBodies.add(segments);
            collectTagRequests(requestedTags, segments);
        }

        Map<String, Map<String, Component>> resolvedTags = resolverRegistry.resolveAll(requestedTags);

        for (int i = 0; i < commentList.size(); i++) {
            COMMENT comment = commentList.get(i);
            String commentId = requireId(commentAdapter.id(comment), "comment id");
            String topicId = requireId(commentAdapter.topicId(comment), "comment topic id");
            Component body = renderBody(parsedBodies.get(i), resolvedTags, "forum-comment-body-content");

            List<Component> actions = normalizeActions(actionDecorator.decorate(
                new ForumActionDecorator.ActionContext<>(
                    ForumActionDecorator.ItemType.COMMENT,
                    commentId,
                    topicId,
                    comment,
                    viewer
                )
            ));

            CommentView<COMMENT, VIEWER> view = new CommentView<>(
                comment,
                commentId,
                topicId,
                commentAdapter.parentId(comment),
                commentAdapter.depth(comment),
                safe(commentAdapter.author(comment)),
                safe(commentAdapter.avatarUrl(comment)),
                safe(commentAdapter.timestamp(comment)),
                commentAdapter.likes(comment),
                commentAdapter.replies(comment),
                body,
                actions,
                viewer
            );

            root.withChild(commentRenderer.render(view));
        }

        appendPaginationControls(root, pagination);
        appendCommentComposer(root, viewer);
        return root;
    }

    private List<COMMENT> paginateComments(List<COMMENT> comments, CommentPagination pagination) {
        List<COMMENT> scopedToTopic = new ArrayList<>();
        for (COMMENT comment : comments) {
            String topicId = requireId(commentAdapter.topicId(comment), "comment topic id");
            if (pagination.topicId().equals(topicId)) {
                scopedToTopic.add(comment);
            }
        }

        int startIndex = (pagination.currentPage() - 1) * pagination.commentsPerPage();
        if (startIndex >= scopedToTopic.size()) {
            return List.of();
        }

        int endIndex = Math.min(startIndex + pagination.commentsPerPage(), scopedToTopic.size());
        return List.copyOf(scopedToTopic.subList(startIndex, endIndex));
    }

    private List<TOPIC> paginateTopics(List<TOPIC> topics, TopicPagination pagination) {
        int startIndex = (pagination.currentPage() - 1) * pagination.topicsPerPage();
        if (startIndex >= topics.size()) {
            return List.of();
        }

        int endIndex = Math.min(startIndex + pagination.topicsPerPage(), topics.size());
        return List.copyOf(topics.subList(startIndex, endIndex));
    }

    private void appendTopicPaginationControls(HtmlTag root, TopicPagination pagination) {
        if (pagination == null) {
            return;
        }

        HtmlTag controls = new HtmlTag("div")
            .withAttribute("class", "forum-topics-pagination")
            .withAttribute("data-scope-id", pagination.scopeId())
            .withAttribute("data-page", String.valueOf(pagination.currentPage()))
            .withAttribute("data-total-pages", String.valueOf(pagination.totalPages()));

        controls.withChild(buildTopicPaginationButton(
            "forum-topics-page-prev",
            "Previous",
            pagination.hasPrevious(),
            pagination.currentPage() - 1,
            pagination
        ));

        controls.withChild(new HtmlTag("span")
            .withAttribute("class", "forum-topics-page-status")
            .withInnerText("Page " + pagination.currentPage() + " of " + pagination.totalPages()));

        controls.withChild(buildTopicPaginationButton(
            "forum-topics-page-next",
            "Next",
            pagination.hasNext(),
            pagination.currentPage() + 1,
            pagination
        ));

        root.withChild(controls);
    }

    private Component buildTopicPaginationButton(
            String cssClass,
            String label,
            boolean enabled,
            int targetPage,
            TopicPagination pagination
    ) {
        HtmlTag button = new HtmlTag("button")
            .withAttribute("type", "button")
            .withAttribute("class", "forum-topics-page-button " + cssClass)
            .withAttribute("data-scope-id", pagination.scopeId())
            .withAttribute("data-target-page", String.valueOf(targetPage))
            .withInnerText(label);

        if (enabled) {
            String endpoint = topicPaginationEndpointResolver.endpoint(
                pagination.scopeId(),
                targetPage,
                pagination.topicsPerPage()
            );
            button.withAttribute("hx-get", endpoint)
                .withAttribute("hx-target", topicsPaginationHxTarget)
                .withAttribute("hx-swap", topicsPaginationHxSwap);
        } else {
            button.withAttribute("disabled", "");
        }
        return button;
    }

    private void appendPaginationControls(HtmlTag root, CommentPagination pagination) {
        if (pagination == null) {
            return;
        }

        HtmlTag controls = new HtmlTag("div")
            .withAttribute("class", "forum-comments-pagination")
            .withAttribute("data-topic-id", pagination.topicId())
            .withAttribute("data-page", String.valueOf(pagination.currentPage()))
            .withAttribute("data-total-pages", String.valueOf(pagination.totalPages()));

        controls.withChild(buildPaginationButton(
            "forum-comments-page-prev",
            "Previous",
            pagination.hasPrevious(),
            pagination.currentPage() - 1,
            pagination
        ));

        controls.withChild(new HtmlTag("span")
            .withAttribute("class", "forum-comments-page-status")
            .withInnerText("Page " + pagination.currentPage() + " of " + pagination.totalPages()));

        controls.withChild(buildPaginationButton(
            "forum-comments-page-next",
            "Next",
            pagination.hasNext(),
            pagination.currentPage() + 1,
            pagination
        ));

        root.withChild(controls);
    }

    private Component buildPaginationButton(
            String cssClass,
            String label,
            boolean enabled,
            int targetPage,
            CommentPagination pagination
    ) {
        HtmlTag button = new HtmlTag("button")
            .withAttribute("type", "button")
            .withAttribute("class", "forum-comments-page-button " + cssClass)
            .withAttribute("data-topic-id", pagination.topicId())
            .withAttribute("data-target-page", String.valueOf(targetPage))
            .withInnerText(label);

        if (enabled) {
            String endpoint = commentPaginationEndpointResolver.endpoint(
                pagination.topicId(),
                targetPage,
                pagination.commentsPerPage()
            );
            button.withAttribute("hx-get", endpoint)
                .withAttribute("hx-target", commentsPaginationHxTarget)
                .withAttribute("hx-swap", commentsPaginationHxSwap);
        } else {
            button.withAttribute("disabled", "");
        }
        return button;
    }

    private void appendCommentComposer(HtmlTag root, VIEWER viewer) {
        if (commentComposer == null) {
            return;
        }
        Component composer = commentComposer.apply(viewer);
        if (composer != null) {
            root.withChild(new HtmlTag("div")
                .withAttribute("class", "forum-comments-composer")
                .withChild(composer));
        }
    }

    private List<Component> normalizeActions(List<Component> actions) {
        if (actions == null || actions.isEmpty()) {
            return List.of();
        }
        List<Component> normalized = new ArrayList<>();
        for (Component action : actions) {
            if (action != null) {
                normalized.add(action);
            }
        }
        return List.copyOf(normalized);
    }

    private List<ForumTagParser.Segment> parseBody(String body, boolean enabled) {
        if (!enabled) {
            return List.of(new ForumTagParser.TextSegment(safe(body)));
        }
        return tagParser.parse(safe(body));
    }

    private void collectTagRequests(Map<String, Set<String>> requestedTags, List<ForumTagParser.Segment> segments) {
        for (ForumTagParser.Segment segment : segments) {
            if (segment instanceof ForumTagParser.TagSegment tag) {
                requestedTags.computeIfAbsent(tag.key(), ignored -> new LinkedHashSet<>()).add(tag.value());
            }
        }
    }

    private Component renderBody(
            List<ForumTagParser.Segment> segments,
            Map<String, Map<String, Component>> resolvedTags,
            String cssClass
    ) {
        HtmlTag body = new HtmlTag("div").withAttribute("class", cssClass);

        for (ForumTagParser.Segment segment : segments) {
            if (segment instanceof ForumTagParser.TextSegment textSegment) {
                if (!textSegment.text().isEmpty()) {
                    body.withChild(new Markdown(textSegment.text()));
                }
            } else if (segment instanceof ForumTagParser.TagSegment tagSegment) {
                Component resolved = resolvedTags
                    .getOrDefault(tagSegment.key(), Map.of())
                    .get(tagSegment.value());
                if (resolved != null) {
                    body.withChild(resolved);
                } else {
                    body.withChild(new HtmlTag("span")
                        .withAttribute("class", "forum-tag-literal")
                        .withInnerText(tagSegment.rawToken()));
                }
            }
        }

        return body;
    }

    private String requireId(String id, String fieldName) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Missing required " + fieldName);
        }
        return id;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static <CATEGORY, VIEWER> Component renderDefaultCategory(CategoryView<CATEGORY, VIEWER> view) {
        HtmlTag category = new HtmlTag("div")
            .withAttribute("class", "forum-category")
            .withAttribute("data-category-id", view.id());

        category.withChild(new HtmlTag("h3")
            .withAttribute("class", "forum-category-title")
            .withInnerText(view.title()));

        if (!view.description().isBlank()) {
            category.withChild(new HtmlTag("p")
                .withAttribute("class", "forum-category-description")
                .withInnerText(view.description()));
        }

        if (view.topicCount() != null) {
            category.withChild(new HtmlTag("span")
                .withAttribute("class", "forum-category-topic-count")
                .withInnerText(view.topicCount() + " topics"));
        }

        return category;
    }

    private static <TOPIC, VIEWER> Component renderDefaultTopic(TopicView<TOPIC, VIEWER> view) {
        HtmlTag topic = new HtmlTag("div")
            .withAttribute("class", "forum-topic")
            .withAttribute("data-topic-id", view.id());

        if (!view.actions().isEmpty()) {
            HtmlTag actions = new HtmlTag("div").withAttribute("class", "forum-topic-annotations");
            view.actions().forEach(actions::withChild);
            topic.withChild(actions);
        }

        HtmlTag header = new HtmlTag("div").withAttribute("class", "forum-topic-header")
            .withChild(new HtmlTag("div").withAttribute("class", "forum-topic-author").withInnerText(view.author()))
            .withChild(new HtmlTag("div").withAttribute("class", "forum-topic-timestamp").withInnerText(view.timestamp()));

        topic.withChild(header);
        topic.withChild(new HtmlTag("h3").withAttribute("class", "forum-topic-title").withInnerText(view.title()));
        topic.withChild(view.body());

        HtmlTag footer = renderStatsFooter("forum-topic-footer", "forum-topic-likes", "forum-topic-replies", view.likes(), view.replies());
        if (footer != null) {
            topic.withChild(footer);
        }

        return topic;
    }

    private static <COMMENT, VIEWER> Component renderDefaultComment(CommentView<COMMENT, VIEWER> view) {
        HtmlTag comment = new HtmlTag("div")
            .withAttribute("class", "forum-comment")
            .withAttribute("data-comment-id", view.id())
            .withAttribute("data-topic-id", view.topicId());

        if (view.parentId() != null && !view.parentId().isBlank()) {
            comment.withAttribute("data-parent-id", view.parentId());
        }
        if (view.depth() > 0) {
            comment.addStyle("margin-left", (view.depth() * 20) + "px");
        }

        HtmlTag layout = new HtmlTag("div").withAttribute("class", "forum-comment-layout");

        HtmlTag identity = new HtmlTag("div").withAttribute("class", "forum-comment-identity")
            .withChild(new HtmlTag("span")
                .withAttribute("class", "forum-comment-author")
                .withInnerText(view.author()))
            .withChild(renderAvatarSlot(view.avatarUrl(), "forum-comment-avatar-slot", "forum-comment-avatar-image"));

        HtmlTag main = new HtmlTag("div").withAttribute("class", "forum-comment-main")
            .withChild(new HtmlTag("span")
                .withAttribute("class", "forum-comment-timestamp")
                .withInnerText(view.timestamp()));

        if (!view.actions().isEmpty()) {
            HtmlTag actions = new HtmlTag("div").withAttribute("class", "forum-comment-annotations");
            view.actions().forEach(actions::withChild);
            main.withChild(actions);
        }

        main.withChild(view.body());
        HtmlTag footer = renderStatsFooter("forum-comment-footer", "forum-comment-likes", "forum-comment-replies", view.likes(), view.replies());
        if (footer != null) {
            main.withChild(footer);
        }

        layout.withChild(identity).withChild(main);
        comment.withChild(layout);
        return comment;
    }

    private static HtmlTag renderAvatarSlot(String avatarUrl, String slotClass, String imageClass) {
        HtmlTag slot = new HtmlTag("div")
            .withAttribute("class", slotClass)
            .addStyle("width", "150px")
            .addStyle("height", "150px");

        if (avatarUrl != null && !avatarUrl.isBlank()) {
            try {
                slot.withChild(Image.create(avatarUrl, "avatar")
                    .withClass(imageClass)
                    .withSize("150", "150"));
            } catch (IllegalArgumentException ignored) {
                // Keep blank slot when avatar source is invalid.
            }
        }
        return slot;
    }

    private static HtmlTag renderStatsFooter(
            String footerClass,
            String likesClass,
            String repliesClass,
            Integer likes,
            Integer replies
    ) {
        if (likes == null && replies == null) {
            return null;
        }

        HtmlTag footer = new HtmlTag("div").withAttribute("class", footerClass);
        if (likes != null) {
            footer.withChild(new HtmlTag("span")
                .withAttribute("class", likesClass)
                .withInnerText(likes + " likes"));
        }
        if (replies != null) {
            footer.withChild(new HtmlTag("span")
                .withAttribute("class", repliesClass)
                .withInnerText(replies + " replies"));
        }
        return footer;
    }

    private static <VIEWER> Component defaultTopicComposerLauncher(VIEWER viewer) {
        return new HtmlTag("a")
            .withAttribute("class", "forum-new-topic-link")
            .withAttribute("href", "/forum/topics/new")
            .withInnerText("New Topic");
    }

    private static <VIEWER> Component defaultCommentComposer(VIEWER viewer) {
        HtmlTag form = new HtmlTag("form")
            .withAttribute("class", "forum-comment-composer-form")
            .withAttribute("hx-post", "/forum/comments")
            .withAttribute("hx-swap", "none");

        form.withChild(new HtmlTag("textarea")
            .withAttribute("class", "forum-comment-composer-input")
            .withAttribute("name", "comment")
            .withAttribute("rows", "4")
            .withAttribute("placeholder", "Write a comment..."));

        form.withChild(new HtmlTag("button")
            .withAttribute("type", "submit")
            .withAttribute("class", "forum-comment-composer-submit")
            .withInnerText("Post Comment"));

        return form;
    }

    private static String defaultCommentPaginationEndpoint(String topicId, int page, int commentsPerPage) {
        return "/forum/topics/" + topicId + "/comments?page=" + page + "&size=" + commentsPerPage;
    }

    private static String defaultTopicPaginationEndpoint(String scopeId, int page, int topicsPerPage) {
        return "/forum/topics?scope=" + scopeId + "&page=" + page + "&size=" + topicsPerPage;
    }

    public static final class Builder<CATEGORY, TOPIC, COMMENT, VIEWER> {
        private final CategoryAdapter<CATEGORY> categoryAdapter;
        private final TopicAdapter<TOPIC> topicAdapter;
        private final CommentAdapter<COMMENT> commentAdapter;

        private ForumTagParser tagParser = ForumTagParser.create();
        private ForumTagResolverRegistry resolverRegistry = ForumTagResolverRegistry.withBuiltIns();

        private CategoryRenderer<CATEGORY, VIEWER> categoryRenderer = ForumHelper::renderDefaultCategory;
        private TopicRenderer<TOPIC, VIEWER> topicRenderer = ForumHelper::renderDefaultTopic;
        private CommentRenderer<COMMENT, VIEWER> commentRenderer = ForumHelper::renderDefaultComment;

        private ForumActionDecorator<VIEWER> actionDecorator = DefaultForumActionDecorator.create();

        private Function<VIEWER, Component> topicComposerLauncher = ForumHelper::defaultTopicComposerLauncher;
        private Function<VIEWER, Component> commentComposer = ForumHelper::defaultCommentComposer;
        private TopicPaginationEndpointResolver topicPaginationEndpointResolver = ForumHelper::defaultTopicPaginationEndpoint;
        private String topicsPaginationHxTarget = "closest .forum-topics-view";
        private String topicsPaginationHxSwap = "outerHTML";
        private CommentPaginationEndpointResolver commentPaginationEndpointResolver = ForumHelper::defaultCommentPaginationEndpoint;
        private String commentsPaginationHxTarget = "closest .forum-comments-view";
        private String commentsPaginationHxSwap = "outerHTML";

        private boolean parseTopicBodies = true;
        private boolean parseCommentBodies = true;

        private Builder(
                CategoryAdapter<CATEGORY> categoryAdapter,
                TopicAdapter<TOPIC> topicAdapter,
                CommentAdapter<COMMENT> commentAdapter
        ) {
            this.categoryAdapter = Objects.requireNonNull(categoryAdapter, "categoryAdapter");
            this.topicAdapter = Objects.requireNonNull(topicAdapter, "topicAdapter");
            this.commentAdapter = Objects.requireNonNull(commentAdapter, "commentAdapter");
        }

        public Builder<CATEGORY, TOPIC, COMMENT, VIEWER> withTagParser(ForumTagParser tagParser) {
            this.tagParser = Objects.requireNonNull(tagParser, "tagParser");
            return this;
        }

        public Builder<CATEGORY, TOPIC, COMMENT, VIEWER> withResolverRegistry(ForumTagResolverRegistry resolverRegistry) {
            this.resolverRegistry = Objects.requireNonNull(resolverRegistry, "resolverRegistry");
            return this;
        }

        public Builder<CATEGORY, TOPIC, COMMENT, VIEWER> withCategoryRenderer(CategoryRenderer<CATEGORY, VIEWER> categoryRenderer) {
            this.categoryRenderer = Objects.requireNonNull(categoryRenderer, "categoryRenderer");
            return this;
        }

        public Builder<CATEGORY, TOPIC, COMMENT, VIEWER> withTopicRenderer(TopicRenderer<TOPIC, VIEWER> topicRenderer) {
            this.topicRenderer = Objects.requireNonNull(topicRenderer, "topicRenderer");
            return this;
        }

        public Builder<CATEGORY, TOPIC, COMMENT, VIEWER> withCommentRenderer(CommentRenderer<COMMENT, VIEWER> commentRenderer) {
            this.commentRenderer = Objects.requireNonNull(commentRenderer, "commentRenderer");
            return this;
        }

        public Builder<CATEGORY, TOPIC, COMMENT, VIEWER> withActionDecorator(ForumActionDecorator<VIEWER> actionDecorator) {
            this.actionDecorator = Objects.requireNonNull(actionDecorator, "actionDecorator");
            return this;
        }

        public Builder<CATEGORY, TOPIC, COMMENT, VIEWER> withTopicComposerLauncher(Function<VIEWER, Component> topicComposerLauncher) {
            this.topicComposerLauncher = topicComposerLauncher;
            return this;
        }

        public Builder<CATEGORY, TOPIC, COMMENT, VIEWER> withCommentComposer(Function<VIEWER, Component> commentComposer) {
            this.commentComposer = commentComposer;
            return this;
        }

        public Builder<CATEGORY, TOPIC, COMMENT, VIEWER> withTopicPaginationEndpointResolver(
                TopicPaginationEndpointResolver topicPaginationEndpointResolver
        ) {
            this.topicPaginationEndpointResolver = Objects.requireNonNull(
                topicPaginationEndpointResolver,
                "topicPaginationEndpointResolver"
            );
            return this;
        }

        public Builder<CATEGORY, TOPIC, COMMENT, VIEWER> withTopicsPaginationHxTarget(String topicsPaginationHxTarget) {
            this.topicsPaginationHxTarget = Objects.requireNonNull(topicsPaginationHxTarget, "topicsPaginationHxTarget");
            return this;
        }

        public Builder<CATEGORY, TOPIC, COMMENT, VIEWER> withTopicsPaginationHxSwap(String topicsPaginationHxSwap) {
            this.topicsPaginationHxSwap = Objects.requireNonNull(topicsPaginationHxSwap, "topicsPaginationHxSwap");
            return this;
        }

        public Builder<CATEGORY, TOPIC, COMMENT, VIEWER> withCommentPaginationEndpointResolver(
                CommentPaginationEndpointResolver commentPaginationEndpointResolver
        ) {
            this.commentPaginationEndpointResolver = Objects.requireNonNull(
                commentPaginationEndpointResolver,
                "commentPaginationEndpointResolver"
            );
            return this;
        }

        public Builder<CATEGORY, TOPIC, COMMENT, VIEWER> withCommentsPaginationHxTarget(String commentsPaginationHxTarget) {
            this.commentsPaginationHxTarget = Objects.requireNonNull(commentsPaginationHxTarget, "commentsPaginationHxTarget");
            return this;
        }

        public Builder<CATEGORY, TOPIC, COMMENT, VIEWER> withCommentsPaginationHxSwap(String commentsPaginationHxSwap) {
            this.commentsPaginationHxSwap = Objects.requireNonNull(commentsPaginationHxSwap, "commentsPaginationHxSwap");
            return this;
        }

        public Builder<CATEGORY, TOPIC, COMMENT, VIEWER> parseTopicBodies(boolean parseTopicBodies) {
            this.parseTopicBodies = parseTopicBodies;
            return this;
        }

        public Builder<CATEGORY, TOPIC, COMMENT, VIEWER> parseCommentBodies(boolean parseCommentBodies) {
            this.parseCommentBodies = parseCommentBodies;
            return this;
        }

        public ForumHelper<CATEGORY, TOPIC, COMMENT, VIEWER> build() {
            return new ForumHelper<>(this);
        }
    }
}
