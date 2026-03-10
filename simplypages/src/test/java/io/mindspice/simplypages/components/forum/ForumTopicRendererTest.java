package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.components.forum.tags.ForumTagResolverRegistry;
import io.mindspice.simplypages.components.forum.tags.ForumTagResolvers;
import io.mindspice.simplypages.components.forum.tags.Tag;
import io.mindspice.simplypages.components.forum.tags.TagType;
import io.mindspice.simplypages.components.forum.topics.ForumTopicComponent;
import io.mindspice.simplypages.components.forum.topics.ForumTopicData;
import io.mindspice.simplypages.components.forum.topics.ForumTopicRenderer;
import io.mindspice.simplypages.components.forum.topics.ForumTopicTitleLink;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ForumTopicRendererTest {

    private record Topic(String id, String title, String body, String author, String timestamp, Integer likes, Integer replies)
        implements ForumTopicData {}

    private static final class ProbeTopicComponent implements ForumTopicComponent {
        private String id = "";
        private String title = "";

        @Override
        public ForumTopicComponent withTopicId(String id) {
            this.id = id == null ? "" : id;
            return this;
        }

        @Override
        public ForumTopicComponent withTitle(String title) {
            this.title = title == null ? "" : title;
            return this;
        }

        @Override
        public ForumTopicComponent withAuthor(String author) {
            return this;
        }

        @Override
        public ForumTopicComponent withTimestamp(String timestamp) {
            return this;
        }

        @Override
        public ForumTopicComponent withBody(Component body) {
            return this;
        }

        @Override
        public ForumTopicComponent withActions(List<Component> actions) {
            return this;
        }

        @Override
        public ForumTopicComponent withLikes(Integer likes) {
            return this;
        }

        @Override
        public ForumTopicComponent withReplies(Integer replies) {
            return this;
        }

        @Override
        public String render(RenderContext context) {
            return new HtmlTag("div")
                .withAttribute("class", "probe-topic")
                .withAttribute("data-topic-id", id)
                .withChild(new HtmlTag("span").withAttribute("class", "probe-title").withInnerText(title))
                .render(context);
        }
    }

    @Test
    @DisplayName("ForumTopicRenderer should render tags, actions, and pagination")
    void rendersTopicsWithFeatures() {
        ForumTagResolverRegistry registry = ForumTagResolverRegistry.withBuiltIns()
            .register(ForumTagResolvers.of("custom.tag", tags -> Map.of(
                new Tag(TagType.of("custom.tag"), "abc"),
                new HtmlTag("span").withAttribute("class", "resolved-custom").withInnerText("custom:abc")
            )));

        ForumTopicRenderer<Topic, String> renderer = ForumTopicRenderer.<Topic, String>builder()
            .withResolverRegistry(registry)
            .withActionProvider(ctx -> List.of(new HtmlTag("button")
                .withAttribute("type", "button")
                .withAttribute("class", "custom-action")
                .withAttribute("data-item-id", ctx.itemId())
                .withInnerText("Act")))
            .build();

        String html = renderer.render(
            List.of(
                new Topic("topic-1", "Release", "hello [[custom.tag::abc]] and [[unknown.tag::xyz]]", "alice", "now", null, null),
                new Topic("topic-2", "Replies", "[[quote::comment-9]]", "bob", "later", 3, 4)
            ),
            "ctx",
            new ForumTopicRenderer.TopicPagination("cat-1", 1, 2, 5)
        ).render();

        HtmlAssert.assertThat(html)
            .hasElement("div.forum-topic[data-topic-id=topic-1] .resolved-custom")
            .hasElement("div.forum-topic[data-topic-id=topic-1] span.forum-tag-literal")
            .hasElement("div.forum-topic[data-topic-id=topic-1] .forum-topic-header .forum-topic-actions > button.custom-action[data-item-id=topic-1]")
            .doesNotHaveElement("div.forum-topic[data-topic-id=topic-1] .forum-topic-title-link")
            .hasElement("div.forum-topic[data-topic-id=topic-2] div.forum-topic-footer > span.forum-topic-likes")
            .hasElement("div.forum-topic[data-topic-id=topic-2] div.forum-topic-body-content blockquote.forum-tag-quote")
            .attributeEquals("button.forum-topics-page-next", "data-sp-scroll-top", "target")
            .attributeEquals("button.forum-topics-page-next", "hx-target", "closest .forum-topics-view");
    }

    @Test
    @DisplayName("ForumTopicRenderer should batch resolver calls per tag type")
    void batchesResolverCalls() {
        AtomicInteger calls = new AtomicInteger();
        ForumTagResolverRegistry registry = ForumTagResolverRegistry.create()
            .register(ForumTagResolvers.of("custom.tag", tags -> {
                calls.incrementAndGet();
                return tags.stream().collect(java.util.stream.Collectors.toMap(
                    tag -> tag,
                    tag -> new HtmlTag("span").withInnerText(tag.value())
                ));
            }));

        ForumTopicRenderer<Topic, String> renderer = ForumTopicRenderer.<Topic, String>builder()
            .withResolverRegistry(registry)
            .build();

        renderer.render(List.of(
            new Topic("topic-1", "One", "[[custom.tag::a]] [[custom.tag::b]]", "a", "t", null, null),
            new Topic("topic-2", "Two", "[[custom.tag::a]]", "b", "t2", null, null)
        ), "ctx").render();

        org.junit.jupiter.api.Assertions.assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("ForumTopicRenderer should require topic id")
    void requiresTopicId() {
        ForumTopicRenderer<Topic, String> renderer = ForumTopicRenderer.<Topic, String>builder().build();

        assertThrows(IllegalArgumentException.class,
            () -> renderer.render(List.of(new Topic("", "bad", "body", "a", "t", null, null)), "ctx").render());
    }

    @Test
    @DisplayName("ForumTopicRenderer should invoke supplied component once per rendered item")
    void topicSupplierIsUsedPerItem() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<ForumTopicComponent> supplier = () -> {
            calls.incrementAndGet();
            return new ProbeTopicComponent();
        };

        ForumTopicRenderer<Topic, String> renderer = ForumTopicRenderer.<Topic, String>builder()
            .withTopicComponentSupplier(supplier)
            .build();

        String html = renderer.render(List.of(
            new Topic("t1", "One", "body", "a", "t", null, null),
            new Topic("t2", "Two", "body", "b", "t2", null, null)
        ), "ctx").render();

        assertEquals(2, calls.get());
        HtmlAssert.assertThat(html)
            .hasElement("div.probe-topic[data-topic-id=t1] .probe-title")
            .hasElement("div.probe-topic[data-topic-id=t2] .probe-title")
            .elementTextEquals("div.probe-topic[data-topic-id=t1] .probe-title", "One")
            .elementTextEquals("div.probe-topic[data-topic-id=t2] .probe-title", "Two");
    }

    @Test
    @DisplayName("ForumTopicRenderer should not invoke resolvers when bodies contain no tags")
    void noTagBodiesSkipResolverCalls() {
        AtomicInteger calls = new AtomicInteger();
        ForumTagResolverRegistry registry = ForumTagResolverRegistry.create()
            .register(ForumTagResolvers.of("custom.tag", tags -> {
                calls.incrementAndGet();
                return Map.of();
            }));

        ForumTopicRenderer<Topic, String> renderer = ForumTopicRenderer.<Topic, String>builder()
            .withResolverRegistry(registry)
            .build();

        renderer.render(List.of(
            new Topic("t1", "One", "plain body", "a", "t", null, null),
            new Topic("t2", "Two", "still plain", "b", "t2", null, null)
        ), "ctx").render();

        assertEquals(0, calls.get());
    }

    @Test
    @DisplayName("ForumTopicRenderer pagination should wire first and last page controls correctly")
    void topicPaginationBoundaryControls() {
        ForumTopicRenderer<Topic, String> renderer = ForumTopicRenderer.<Topic, String>builder()
            .withPaginationEndpointResolver((scopeId, page, size) ->
                "/api/forum/topics?scope=" + scopeId + "&page=" + page + "&size=" + size)
            .build();

        List<Topic> topics = List.of(
            new Topic("t1", "One", "b1", "a", "t1", null, null),
            new Topic("t2", "Two", "b2", "a", "t2", null, null),
            new Topic("t3", "Three", "b3", "a", "t3", null, null),
            new Topic("t4", "Four", "b4", "a", "t4", null, null),
            new Topic("t5", "Five", "b5", "a", "t5", null, null)
        );

        String page1 = renderer.render(
            topics,
            "ctx",
            new ForumTopicRenderer.TopicPagination("cat-1", 1, 2, 5)
        ).render();

        HtmlAssert.assertThat(page1)
            .hasElement("button.forum-topics-page-prev[disabled]")
            .attributeEquals("button.forum-topics-page-next", "hx-get", "/api/forum/topics?scope=cat-1&page=2&size=2")
            .elementTextEquals("span.forum-topics-page-status", "Page 1 of 3");

        String page3 = renderer.render(
            topics,
            "ctx",
            new ForumTopicRenderer.TopicPagination("cat-1", 3, 2, 5)
        ).render();

        HtmlAssert.assertThat(page3)
            .hasElement("button.forum-topics-page-next[disabled]")
            .attributeEquals("button.forum-topics-page-prev", "hx-get", "/api/forum/topics?scope=cat-1&page=2&size=2")
            .elementTextEquals("span.forum-topics-page-status", "Page 3 of 3");
    }

    @Test
    @DisplayName("ForumTopicRenderer should optionally filter topics by pagination scope before slicing")
    void topicScopeExtractorFiltersBeforePagination() {
        record ScopedTopic(String id, String scopeId, String title, String body) implements ForumTopicData {}

        ForumTopicRenderer<ScopedTopic, String> renderer = ForumTopicRenderer.<ScopedTopic, String>builder()
            .withTopicScopeExtractor(ScopedTopic::scopeId)
            .build();

        List<ScopedTopic> topics = List.of(
            new ScopedTopic("t1", "cat-1", "One", "one"),
            new ScopedTopic("t2", "cat-2", "Two", "two"),
            new ScopedTopic("t3", "cat-1", "Three", "three")
        );

        String page1 = renderer.render(
            topics,
            "ctx",
            new ForumTopicRenderer.TopicPagination("cat-1", 1, 1, 2)
        ).render();

        HtmlAssert.assertThat(page1)
            .hasElement("div.forum-topic[data-topic-id=t1]")
            .doesNotHaveElement("div.forum-topic[data-topic-id=t2]")
            .doesNotHaveElement("div.forum-topic[data-topic-id=t3]");

        String page2 = renderer.render(
            topics,
            "ctx",
            new ForumTopicRenderer.TopicPagination("cat-1", 2, 1, 2)
        ).render();

        HtmlAssert.assertThat(page2)
            .doesNotHaveElement("div.forum-topic[data-topic-id=t1]")
            .doesNotHaveElement("div.forum-topic[data-topic-id=t2]")
            .hasElement("div.forum-topic[data-topic-id=t3]");
    }

    @Test
    @DisplayName("ForumTopicRenderer should use body text resolver when provided")
    void bodyTextResolverOverridesSourceBody() {
        ForumTopicRenderer<Topic, String> renderer = ForumTopicRenderer.<Topic, String>builder()
            .withBodyTextResolver((topic, context) -> "Preview for " + topic.id())
            .build();

        String html = renderer.render(List.of(
            new Topic("topic-9", "Resolver", "Original body text", "a", "t", null, null)
        ), "ctx").render();

        HtmlAssert.assertThat(html)
            .hasElement("div.forum-topic[data-topic-id=topic-9] .forum-topic-body-content");
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("Preview for topic-9"));
        org.junit.jupiter.api.Assertions.assertFalse(html.contains("Original body text"));
    }

    @Test
    @DisplayName("ForumTopicRenderer should apply topic title link metadata from resolver")
    void titleLinkResolverRendersLinkedTitle() {
        ForumTopicRenderer<Topic, String> renderer = ForumTopicRenderer.<Topic, String>builder()
            .withTitleLinkResolver((topic, context) -> ForumTopicTitleLink.htmx(
                "/forum?topic=" + topic.id(),
                "/forum/topics/" + topic.id() + "/comments",
                "#forum-main",
                "outerHTML",
                "/forum?view=comments&topic=" + topic.id()
            ))
            .build();

        String html = renderer.render(List.of(
            new Topic("topic-10", "Linked Title", "Body", "a", "t", null, null)
        ), "ctx").render();

        HtmlAssert.assertThat(html)
            .attributeEquals("div.forum-topic[data-topic-id=topic-10] .forum-topic-title-link", "href", "/forum?topic=topic-10")
            .attributeEquals("div.forum-topic[data-topic-id=topic-10] .forum-topic-title-link", "hx-get", "/forum/topics/topic-10/comments")
            .attributeEquals("div.forum-topic[data-topic-id=topic-10] .forum-topic-title-link", "hx-target", "#forum-main")
            .attributeEquals("div.forum-topic[data-topic-id=topic-10] .forum-topic-title-link", "hx-swap", "outerHTML")
            .attributeEquals("div.forum-topic[data-topic-id=topic-10] .forum-topic-title-link", "hx-push-url", "/forum?view=comments&topic=topic-10");
    }
}
