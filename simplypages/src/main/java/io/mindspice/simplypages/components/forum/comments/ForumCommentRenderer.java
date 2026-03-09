package io.mindspice.simplypages.components.forum.comments;

import io.mindspice.simplypages.components.Markdown;
import io.mindspice.simplypages.components.forum.actions.ForumActionContext;
import io.mindspice.simplypages.components.forum.actions.ForumActionProvider;
import io.mindspice.simplypages.components.forum.actions.ForumActionType;
import io.mindspice.simplypages.components.forum.tags.ForumTagParser;
import io.mindspice.simplypages.components.forum.tags.ForumTagResolverRegistry;
import io.mindspice.simplypages.components.forum.tags.Tag;
import io.mindspice.simplypages.components.forum.tags.TagType;
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
import java.util.function.Supplier;

/**
 * Renderer for forum comments.
 */
public final class ForumCommentRenderer<COMMENT extends ForumCommentData, CTX> {

    @FunctionalInterface
    public interface CommentPaginationEndpointResolver {
        String endpoint(String topicId, int page, int commentsPerPage);
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

    private final Supplier<? extends ForumCommentComponent> componentSupplier;
    private final ForumActionProvider<COMMENT, CTX> actionProvider;
    private final ForumTagParser tagParser;
    private final ForumTagResolverRegistry resolverRegistry;
    private final CommentPaginationEndpointResolver paginationEndpointResolver;
    private final String paginationHxTarget;
    private final String paginationHxSwap;

    private ForumCommentRenderer(Builder<COMMENT, CTX> builder) {
        this.componentSupplier = builder.componentSupplier;
        this.actionProvider = builder.actionProvider;
        this.tagParser = builder.tagParser;
        this.resolverRegistry = builder.resolverRegistry;
        this.paginationEndpointResolver = builder.paginationEndpointResolver;
        this.paginationHxTarget = builder.paginationHxTarget;
        this.paginationHxSwap = builder.paginationHxSwap;
    }

    public static <COMMENT extends ForumCommentData, CTX> Builder<COMMENT, CTX> builder() {
        return new Builder<>();
    }

    public Component render(Collection<COMMENT> comments, CTX context) {
        return render(comments, context, null);
    }

    public Component render(Collection<COMMENT> comments, CTX context, CommentPagination pagination) {
        HtmlTag root = new HtmlTag("div").withAttribute("class", "forum-comments-view");

        List<COMMENT> commentList = comments == null ? List.of() : List.copyOf(comments);
        if (pagination != null) {
            commentList = paginate(commentList, pagination);
        }

        if (commentList.isEmpty()) {
            appendPagination(root, pagination);
            return root;
        }

        List<List<ForumTagParser.Segment>> parsed = new ArrayList<>(commentList.size());
        Map<TagType, Set<Tag>> requested = new LinkedHashMap<>();

        for (COMMENT comment : commentList) {
            requireId(comment.id(), "comment id");
            requireId(comment.topicId(), "comment topic id");

            List<ForumTagParser.Segment> segments = parseBody(comment.body());
            parsed.add(segments);
            collectTagRequests(requested, segments);
        }

        Map<Tag, Component> resolved = resolverRegistry.resolveAll(requested);

        for (int i = 0; i < commentList.size(); i++) {
            COMMENT comment = commentList.get(i);
            String commentId = requireId(comment.id(), "comment id");
            String topicId = requireId(comment.topicId(), "comment topic id");

            Component body = renderBody(parsed.get(i), resolved, "forum-comment-body-content");
            List<Component> actions = normalizeActions(actionProvider.provide(new ForumActionContext<>(
                ForumActionType.COMMENT,
                commentId,
                topicId,
                comment,
                context
            )));

            ForumCommentComponent rendered = Objects.requireNonNull(
                componentSupplier.get(),
                "comment component supplier returned null"
            );

            root.withChild(rendered
                .withCommentId(commentId)
                .withTopicId(topicId)
                .withParentId(comment.parentId())
                .withDepth(comment.depth())
                .withAuthor(safe(comment.author()))
                .withAvatarUrl(safe(comment.avatarUrl()))
                .withTimestamp(safe(comment.timestamp()))
                .withBody(body)
                .withActions(actions)
                .withLikes(comment.likes())
                .withReplies(comment.replies()));
        }

        appendPagination(root, pagination);
        return root;
    }

    private List<COMMENT> paginate(List<COMMENT> comments, CommentPagination pagination) {
        List<COMMENT> scoped = new ArrayList<>();
        for (COMMENT comment : comments) {
            if (pagination.topicId().equals(requireId(comment.topicId(), "comment topic id"))) {
                scoped.add(comment);
            }
        }

        int startIndex = (pagination.currentPage() - 1) * pagination.commentsPerPage();
        if (startIndex >= scoped.size()) {
            return List.of();
        }

        int endIndex = Math.min(startIndex + pagination.commentsPerPage(), scoped.size());
        return List.copyOf(scoped.subList(startIndex, endIndex));
    }

    private void appendPagination(HtmlTag root, CommentPagination pagination) {
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
            button.withAttribute("hx-get", paginationEndpointResolver.endpoint(
                    pagination.topicId(),
                    targetPage,
                    pagination.commentsPerPage()
                ))
                .withAttribute("hx-target", paginationHxTarget)
                .withAttribute("hx-swap", paginationHxSwap);
        } else {
            button.withAttribute("disabled", "");
        }

        return button;
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

    private List<ForumTagParser.Segment> parseBody(String body) {
        String safeBody = safe(body);
        if (!safeBody.contains("[[")) {
            return List.of(new ForumTagParser.TextSegment(safeBody));
        }
        return tagParser.parse(safeBody);
    }

    private void collectTagRequests(Map<TagType, Set<Tag>> requested, List<ForumTagParser.Segment> segments) {
        for (ForumTagParser.Segment segment : segments) {
            if (segment instanceof ForumTagParser.TagSegment tagSegment) {
                TagType type = TagType.of(tagSegment.key());
                Tag tag = new Tag(type, tagSegment.value());
                requested.computeIfAbsent(type, ignored -> new LinkedHashSet<>()).add(tag);
            }
        }
    }

    private Component renderBody(List<ForumTagParser.Segment> segments, Map<Tag, Component> resolved, String cssClass) {
        HtmlTag body = new HtmlTag("div").withAttribute("class", cssClass);

        for (ForumTagParser.Segment segment : segments) {
            if (segment instanceof ForumTagParser.TextSegment textSegment) {
                if (!textSegment.text().isEmpty()) {
                    body.withChild(new Markdown(textSegment.text()));
                }
            } else if (segment instanceof ForumTagParser.TagSegment tagSegment) {
                Component component = resolved.get(new Tag(TagType.of(tagSegment.key()), tagSegment.value()));
                if (component != null) {
                    body.withChild(component);
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

    private static String defaultPaginationEndpoint(String topicId, int page, int commentsPerPage) {
        return "/forum/topics/" + topicId + "/comments?page=" + page + "&size=" + commentsPerPage;
    }

    public static final class Builder<COMMENT extends ForumCommentData, CTX> {
        private Supplier<? extends ForumCommentComponent> componentSupplier = DefaultForumCommentComponent::create;
        private ForumActionProvider<COMMENT, CTX> actionProvider = ForumActionProvider.none();
        private ForumTagParser tagParser = ForumTagParser.create();
        private ForumTagResolverRegistry resolverRegistry = ForumTagResolverRegistry.withBuiltIns();
        private CommentPaginationEndpointResolver paginationEndpointResolver = ForumCommentRenderer::defaultPaginationEndpoint;
        private String paginationHxTarget = "closest .forum-comments-view";
        private String paginationHxSwap = "outerHTML";

        public Builder<COMMENT, CTX> withCommentComponentSupplier(
                Supplier<? extends ForumCommentComponent> componentSupplier
        ) {
            this.componentSupplier = Objects.requireNonNull(componentSupplier, "componentSupplier");
            return this;
        }

        public Builder<COMMENT, CTX> withActionProvider(ForumActionProvider<COMMENT, CTX> actionProvider) {
            this.actionProvider = Objects.requireNonNull(actionProvider, "actionProvider");
            return this;
        }

        public Builder<COMMENT, CTX> withTagParser(ForumTagParser tagParser) {
            this.tagParser = Objects.requireNonNull(tagParser, "tagParser");
            return this;
        }

        public Builder<COMMENT, CTX> withResolverRegistry(ForumTagResolverRegistry resolverRegistry) {
            this.resolverRegistry = Objects.requireNonNull(resolverRegistry, "resolverRegistry");
            return this;
        }

        public Builder<COMMENT, CTX> withPaginationEndpointResolver(
                CommentPaginationEndpointResolver paginationEndpointResolver
        ) {
            this.paginationEndpointResolver = Objects.requireNonNull(paginationEndpointResolver, "paginationEndpointResolver");
            return this;
        }

        public Builder<COMMENT, CTX> withPaginationHxTarget(String paginationHxTarget) {
            this.paginationHxTarget = Objects.requireNonNull(paginationHxTarget, "paginationHxTarget");
            return this;
        }

        public Builder<COMMENT, CTX> withPaginationHxSwap(String paginationHxSwap) {
            this.paginationHxSwap = Objects.requireNonNull(paginationHxSwap, "paginationHxSwap");
            return this;
        }

        public ForumCommentRenderer<COMMENT, CTX> build() {
            return new ForumCommentRenderer<>(this);
        }
    }
}
