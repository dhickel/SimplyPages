package io.mindspice.simplypages.components.forum.topics;

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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Renderer for forum topics.
 */
public final class ForumTopicRenderer<TOPIC extends ForumTopicData, CTX> {

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

    private final Supplier<? extends ForumTopicComponent> componentSupplier;
    private final ForumActionProvider<TOPIC, CTX> actionProvider;
    private final ForumTagParser tagParser;
    private final ForumTagResolverRegistry resolverRegistry;
    private final TopicPaginationEndpointResolver paginationEndpointResolver;
    private final String paginationHxTarget;
    private final String paginationHxSwap;
    private final Function<TOPIC, String> topicScopeExtractor;
    private final BiFunction<TOPIC, CTX, String> bodyTextResolver;
    private final BiFunction<TOPIC, CTX, ForumTopicTitleLink> titleLinkResolver;

    private ForumTopicRenderer(Builder<TOPIC, CTX> builder) {
        this.componentSupplier = builder.componentSupplier;
        this.actionProvider = builder.actionProvider;
        this.tagParser = builder.tagParser;
        this.resolverRegistry = builder.resolverRegistry;
        this.paginationEndpointResolver = builder.paginationEndpointResolver;
        this.paginationHxTarget = builder.paginationHxTarget;
        this.paginationHxSwap = builder.paginationHxSwap;
        this.topicScopeExtractor = builder.topicScopeExtractor;
        this.bodyTextResolver = builder.bodyTextResolver;
        this.titleLinkResolver = builder.titleLinkResolver;
    }

    public static <TOPIC extends ForumTopicData, CTX> Builder<TOPIC, CTX> builder() {
        return new Builder<>();
    }

    public Component render(Collection<TOPIC> topics, CTX context) {
        return render(topics, context, null);
    }

    public Component render(Collection<TOPIC> topics, CTX context, TopicPagination pagination) {
        HtmlTag root = new HtmlTag("div").withAttribute("class", "forum-topics-view");
        if (topics == null || topics.isEmpty()) {
            appendPagination(root, pagination);
            return root;
        }

        List<TOPIC> topicList = List.copyOf(topics);
        if (pagination != null) {
            if (topicScopeExtractor != null) {
                topicList = filterByScope(topicList, pagination.scopeId());
            }
            topicList = paginate(topicList, pagination);
        }

        if (topicList.isEmpty()) {
            appendPagination(root, pagination);
            return root;
        }

        List<List<ForumTagParser.Segment>> parsed = new ArrayList<>(topicList.size());
        Map<TagType, Set<Tag>> requested = new LinkedHashMap<>();

        for (TOPIC topic : topicList) {
            requireId(topic.id(), "topic id");
            List<ForumTagParser.Segment> segments = parseBody(bodyTextResolver.apply(topic, context));
            parsed.add(segments);
            collectTagRequests(requested, segments);
        }

        Map<Tag, Component> resolved = resolverRegistry.resolveAll(requested);

        for (int i = 0; i < topicList.size(); i++) {
            TOPIC topic = topicList.get(i);
            String topicId = requireId(topic.id(), "topic id");

            Component body = renderBody(parsed.get(i), resolved, "forum-topic-body-content");
            List<Component> actions = normalizeActions(actionProvider.provide(new ForumActionContext<>(
                ForumActionType.TOPIC,
                topicId,
                topicId,
                topic,
                context
            )));

            ForumTopicComponent rendered = Objects.requireNonNull(
                componentSupplier.get(),
                "topic component supplier returned null"
            );

            root.withChild(rendered
                .withTopicId(topicId)
                .withTitle(safe(topic.title()))
                .withTitleLink(titleLinkResolver.apply(topic, context))
                .withAuthor(safe(topic.author()))
                .withTimestamp(safe(topic.timestamp()))
                .withBody(body)
                .withActions(actions)
                .withLikes(topic.likes())
                .withReplies(topic.replies()));
        }

        appendPagination(root, pagination);
        return root;
    }

    private void appendPagination(HtmlTag root, TopicPagination pagination) {
        if (pagination == null) {
            return;
        }

        HtmlTag controls = new HtmlTag("div")
            .withAttribute("class", "forum-topics-pagination")
            .withAttribute("data-scope-id", pagination.scopeId())
            .withAttribute("data-page", String.valueOf(pagination.currentPage()))
            .withAttribute("data-total-pages", String.valueOf(pagination.totalPages()));

        controls.withChild(buildPaginationButton(
            "forum-topics-page-prev",
            "Previous",
            pagination.hasPrevious(),
            pagination.currentPage() - 1,
            pagination
        ));

        controls.withChild(new HtmlTag("span")
            .withAttribute("class", "forum-topics-page-status")
            .withInnerText("Page " + pagination.currentPage() + " of " + pagination.totalPages()));

        controls.withChild(buildPaginationButton(
            "forum-topics-page-next",
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
            TopicPagination pagination
    ) {
        HtmlTag button = new HtmlTag("button")
            .withAttribute("type", "button")
            .withAttribute("class", "forum-topics-page-button " + cssClass)
            .withAttribute("data-scope-id", pagination.scopeId())
            .withAttribute("data-target-page", String.valueOf(targetPage))
            .withInnerText(label);

        if (enabled) {
            button.withAttribute("hx-get", paginationEndpointResolver.endpoint(
                    pagination.scopeId(),
                    targetPage,
                    pagination.topicsPerPage()
                ))
                .withAttribute("hx-target", paginationHxTarget)
                .withAttribute("hx-swap", paginationHxSwap)
                .hxScrollTargetTop();
        } else {
            button.withAttribute("disabled", "");
        }

        return button;
    }

    private List<TOPIC> paginate(List<TOPIC> topics, TopicPagination pagination) {
        int startIndex = (pagination.currentPage() - 1) * pagination.topicsPerPage();
        if (startIndex >= topics.size()) {
            return List.of();
        }

        int endIndex = Math.min(startIndex + pagination.topicsPerPage(), topics.size());
        return List.copyOf(topics.subList(startIndex, endIndex));
    }

    private List<TOPIC> filterByScope(List<TOPIC> topics, String scopeId) {
        List<TOPIC> scoped = new ArrayList<>();
        for (TOPIC topic : topics) {
            String extractedScope = topicScopeExtractor.apply(topic);
            if (scopeId.equals(extractedScope)) {
                scoped.add(topic);
            }
        }
        return List.copyOf(scoped);
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

    private static String defaultPaginationEndpoint(String scopeId, int page, int topicsPerPage) {
        return "/forum/topics?scope=" + encodeQueryValue(scopeId) + "&page=" + page + "&size=" + topicsPerPage;
    }

    private static String encodeQueryValue(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static final class Builder<TOPIC extends ForumTopicData, CTX> {
        private Supplier<? extends ForumTopicComponent> componentSupplier = DefaultForumTopicComponent::create;
        private ForumActionProvider<TOPIC, CTX> actionProvider = ForumActionProvider.none();
        private ForumTagParser tagParser = ForumTagParser.create();
        private ForumTagResolverRegistry resolverRegistry = ForumTagResolverRegistry.withBuiltIns();
        private TopicPaginationEndpointResolver paginationEndpointResolver = ForumTopicRenderer::defaultPaginationEndpoint;
        private String paginationHxTarget = "closest .forum-topics-view";
        private String paginationHxSwap = "outerHTML";
        private Function<TOPIC, String> topicScopeExtractor;
        private BiFunction<TOPIC, CTX, String> bodyTextResolver = (topic, context) -> topic.body();
        private BiFunction<TOPIC, CTX, ForumTopicTitleLink> titleLinkResolver = (topic, context) -> null;

        public Builder<TOPIC, CTX> withTopicComponentSupplier(
                Supplier<? extends ForumTopicComponent> componentSupplier
        ) {
            this.componentSupplier = Objects.requireNonNull(componentSupplier, "componentSupplier");
            return this;
        }

        public Builder<TOPIC, CTX> withActionProvider(ForumActionProvider<TOPIC, CTX> actionProvider) {
            this.actionProvider = Objects.requireNonNull(actionProvider, "actionProvider");
            return this;
        }

        public Builder<TOPIC, CTX> withTagParser(ForumTagParser tagParser) {
            this.tagParser = Objects.requireNonNull(tagParser, "tagParser");
            return this;
        }

        public Builder<TOPIC, CTX> withResolverRegistry(ForumTagResolverRegistry resolverRegistry) {
            this.resolverRegistry = Objects.requireNonNull(resolverRegistry, "resolverRegistry");
            return this;
        }

        public Builder<TOPIC, CTX> withPaginationEndpointResolver(TopicPaginationEndpointResolver paginationEndpointResolver) {
            this.paginationEndpointResolver = Objects.requireNonNull(paginationEndpointResolver, "paginationEndpointResolver");
            return this;
        }

        public Builder<TOPIC, CTX> withPaginationHxTarget(String paginationHxTarget) {
            this.paginationHxTarget = Objects.requireNonNull(paginationHxTarget, "paginationHxTarget");
            return this;
        }

        public Builder<TOPIC, CTX> withPaginationHxSwap(String paginationHxSwap) {
            this.paginationHxSwap = Objects.requireNonNull(paginationHxSwap, "paginationHxSwap");
            return this;
        }

        public Builder<TOPIC, CTX> withTopicScopeExtractor(Function<TOPIC, String> topicScopeExtractor) {
            this.topicScopeExtractor = Objects.requireNonNull(topicScopeExtractor, "topicScopeExtractor");
            return this;
        }

        public Builder<TOPIC, CTX> withBodyTextResolver(BiFunction<TOPIC, CTX, String> bodyTextResolver) {
            this.bodyTextResolver = Objects.requireNonNull(bodyTextResolver, "bodyTextResolver");
            return this;
        }

        public Builder<TOPIC, CTX> withTitleLinkResolver(
            BiFunction<TOPIC, CTX, ForumTopicTitleLink> titleLinkResolver
        ) {
            this.titleLinkResolver = Objects.requireNonNull(titleLinkResolver, "titleLinkResolver");
            return this;
        }

        public ForumTopicRenderer<TOPIC, CTX> build() {
            return new ForumTopicRenderer<>(this);
        }
    }
}
