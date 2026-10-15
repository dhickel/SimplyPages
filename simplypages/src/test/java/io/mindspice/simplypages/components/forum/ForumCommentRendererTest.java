package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.components.forum.comments.ForumCommentComponent;
import io.mindspice.simplypages.components.forum.comments.ForumCommentData;
import io.mindspice.simplypages.components.forum.comments.ForumCommentRenderer;
import io.mindspice.simplypages.components.forum.tags.ForumTagResolverRegistry;
import io.mindspice.simplypages.components.forum.tags.ForumTagResolvers;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ForumCommentRendererTest {

    private record Comment(
        String id,
        String topicId,
        String parentId,
        int depth,
        String body,
        String author,
        String avatarUrl,
        String timestamp,
        Integer likes,
        Integer replies
    ) implements ForumCommentData {}

    private static final class ProbeCommentComponent implements ForumCommentComponent {
        private String id = "";
        private String topicId = "";
        private String parentId;
        private int depth;
        private int parentWrites;
        private int depthWrites;
        private int renders;

        @Override
        public ForumCommentComponent withCommentId(String id) {
            this.id = id == null ? "" : id;
            return this;
        }

        @Override
        public ForumCommentComponent withTopicId(String topicId) {
            this.topicId = topicId == null ? "" : topicId;
            return this;
        }

        @Override
        public ForumCommentComponent withParentId(String parentId) {
            this.parentId = parentId;
            parentWrites++;
            return this;
        }

        @Override
        public ForumCommentComponent withDepth(int depth) {
            this.depth = depth;
            depthWrites++;
            return this;
        }

        @Override
        public ForumCommentComponent withAuthor(String author) {
            return this;
        }

        @Override
        public ForumCommentComponent withAvatarUrl(String avatarUrl) {
            return this;
        }

        @Override
        public ForumCommentComponent withTimestamp(String timestamp) {
            return this;
        }

        @Override
        public ForumCommentComponent withBody(io.mindspice.simplypages.core.Component body) {
            return this;
        }

        @Override
        public ForumCommentComponent withActions(List<io.mindspice.simplypages.core.Component> actions) {
            return this;
        }

        @Override
        public ForumCommentComponent withLikes(Integer likes) {
            return this;
        }

        @Override
        public ForumCommentComponent withReplies(Integer replies) {
            return this;
        }

        @Override
        public String render(RenderContext context) {
            renders++;
            return new HtmlTag("div")
                .withAttribute("class", "probe-comment")
                .withAttribute("data-comment-id", id)
                .withAttribute("data-topic-id", topicId)
                .withAttribute("data-parent-raw", String.valueOf(parentId))
                .withAttribute("data-depth-raw", String.valueOf(depth))
                .render(context);
        }
    }

    @Test
    @DisplayName("ForumCommentRenderer should render comments, actions, tags, and pagination")
    void rendersCommentsWithFeatures() {
        ForumCommentRenderer<Comment, String> renderer = ForumCommentRenderer.<Comment, String>builder()
            .withActionProvider(ctx -> List.of(new HtmlTag("button")
                .withAttribute("type", "button")
                .withAttribute("class", "custom-action")
                .withAttribute("data-item-id", ctx.itemId())
                .withInnerText("Act")))
            .build();

        String html = renderer.render(
            List.of(
                new Comment("comment-1", "topic-1", null, 2, "check [[mention::sam]]", "eve", null, "1m", 1, null),
                new Comment("comment-2", "topic-1", null, 0, "plain", "max", "https://example.com/avatar.png", "2m", null, null)
            ),
            "ctx",
            new ForumCommentRenderer.CommentPagination("topic-1", 1, 2, 5)
        ).render();

        HtmlAssert.assertThat(html)
            .hasElement("div.forum-comments-view > div.forum-comment[data-comment-id=comment-1][data-topic-id=topic-1]")
            .attributeEquals("div.forum-comment[data-comment-id=comment-1]", "style", "")
            .attributeEquals("div.forum-comment[data-comment-id=comment-1] .forum-comment-avatar-slot", "style", "width: 150px; height: 150px;")
            .doesNotHaveElement("div.forum-comment[data-comment-id=comment-1] .forum-comment-avatar-image")
            .hasElement("div.forum-comment[data-comment-id=comment-1] .forum-tag-mention")
            .hasElement("div.forum-comment[data-comment-id=comment-1] .forum-comment-meta .forum-comment-timestamp")
            .hasElement("div.forum-comment[data-comment-id=comment-1] .forum-comment-meta .forum-comment-actions > .custom-action[data-item-id=comment-1]")
            .hasElement("div.forum-comment[data-comment-id=comment-2] .forum-comment-avatar-image")
            .attributeEquals("button.forum-comments-page-next", "data-sp-scroll-top", "target")
            .attributeEquals("button.forum-comments-page-next", "hx-target", "closest .forum-comments-view");
    }

    @Test
    @DisplayName("ForumCommentRenderer should paginate comments and wire previous/next controls")
    void paginatesComments() {
        ForumCommentRenderer<Comment, String> renderer = ForumCommentRenderer.<Comment, String>builder()
            .withPaginationEndpointResolver((topicId, page, size) ->
                "/api/forum/topics/" + topicId + "/comments?page=" + page + "&size=" + size)
            .build();

        List<Comment> comments = List.of(
            new Comment("c1", "topic-1", null, 0, "one", "a", null, "t1", null, null),
            new Comment("c2", "topic-1", null, 0, "two", "a", null, "t2", null, null),
            new Comment("c3", "topic-1", null, 0, "three", "a", null, "t3", null, null),
            new Comment("c99", "topic-2", null, 0, "other", "a", null, "t4", null, null)
        );

        String html = renderer.render(
            comments,
            "ctx",
            new ForumCommentRenderer.CommentPagination("topic-1", 1, 2, 3)
        ).render();

        HtmlAssert.assertThat(html)
            .hasElementCount("div.forum-comments-view > div.forum-comment", 2)
            .doesNotHaveElement("div.forum-comment[data-comment-id=c99]")
            .attributeEquals("button.forum-comments-page-next", "hx-get", "/api/forum/topics/topic-1/comments?page=2&size=2");
    }

    @Test
    @DisplayName("ForumCommentRenderer should require comment ids and topic ids")
    void requiresIds() {
        ForumCommentRenderer<Comment, String> renderer = ForumCommentRenderer.<Comment, String>builder().build();

        assertThrows(IllegalArgumentException.class,
            () -> renderer.render(List.of(new Comment("", "topic-1", null, 0, "body", "a", null, "t", null, null)), "ctx").render());

        assertThrows(IllegalArgumentException.class,
            () -> renderer.render(List.of(new Comment("c1", "", null, 0, "body", "a", null, "t", null, null)), "ctx").render());
    }

    @Test
    @DisplayName("ForumCommentRenderer should invoke supplied component once per rendered item")
    void commentSupplierIsUsedPerItem() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<ForumCommentComponent> supplier = () -> {
            calls.incrementAndGet();
            return new ProbeCommentComponent();
        };

        ForumCommentRenderer<Comment, String> renderer = ForumCommentRenderer.<Comment, String>builder()
            .withCommentComponentSupplier(supplier)
            .build();

        String html = renderer.render(List.of(
            new Comment("c1", "topic-1", null, 0, "one", "a", null, "t1", null, null),
            new Comment("c2", "topic-1", null, 0, "two", "b", null, "t2", null, null)
        ), "ctx").render();

        assertEquals(2, calls.get());
        HtmlAssert.assertThat(html)
            .hasElement("div.probe-comment[data-comment-id=c1][data-topic-id=topic-1]")
            .hasElement("div.probe-comment[data-comment-id=c2][data-topic-id=topic-1]");
    }

    @Test
    @DisplayName("ForumCommentRenderer should forward raw parent and depth to custom components")
    void customComponentsReceiveRawParentAndDepth() {
        AtomicInteger calls = new AtomicInteger();
        ForumCommentRenderer<Comment, String> renderer = ForumCommentRenderer.<Comment, String>builder()
            .withCommentComponentSupplier(() -> {
                calls.incrementAndGet();
                return new ProbeCommentComponent();
            })
            .build();

        String html = renderer.render(List.of(
            new Comment("child", "topic-1", "parent<&", Integer.MIN_VALUE, "child", "a", null, "t1", null, null),
            new Comment("deep", "topic-1", null, Integer.MAX_VALUE, "deep", "b", null, "t2", null, null)
        ), "ctx").render();

        assertEquals(2, calls.get());
        assertFalse(html.contains("forum-comment-depth-"));
        HtmlAssert.assertThat(html)
            .attributeEquals("div.probe-comment[data-comment-id=child]", "data-parent-raw", "parent<&")
            .attributeEquals("div.probe-comment[data-comment-id=child]", "data-depth-raw", String.valueOf(Integer.MIN_VALUE))
            .attributeEquals("div.probe-comment[data-comment-id=deep]", "data-parent-raw", "null")
            .attributeEquals("div.probe-comment[data-comment-id=deep]", "data-depth-raw", String.valueOf(Integer.MAX_VALUE));
    }

    @Test
    void parentGraphsRemainUninterpretedFlatAndSourcePreservedForDefaultAndCustomCards() {
        var source = new ArrayList<>(List.of(
            comment("child", "one", "root", 0),
            comment("absent", "one", "missing", 1),
            comment("self", "one", "self", 2),
            comment("cycle-a", "one", "cycle-b", 4),
            comment("cycle-b", "one", "cycle-a", 5),
            comment("cross", "one", "foreign", 9),
            comment("foreign", "two", " \t", Integer.MIN_VALUE),
            comment("root", "one", null, Integer.MAX_VALUE)
        ));
        var before = List.copyOf(source);
        for (boolean custom : List.of(false, true)) {
            var probes = new ArrayList<ProbeCommentComponent>();
            var builder = ForumCommentRenderer.<Comment, String>builder();
            if (custom) builder.withCommentComponentSupplier(() -> {
                var probe = new ProbeCommentComponent();
                probes.add(probe);
                return probe;
            });
            var renderer = builder.build();
            var result = Jsoup.parseBodyFragment(renderer.render(source, "ctx").render());
            var cards = result.select(".forum-comments-view > " + (custom ? ".probe-comment" : ".forum-comment"));
            assertEquals(source.stream().map(Comment::id).toList(), cards.eachAttr("data-comment-id"));
            for (int index = 0; index < source.size(); index++) {
                Comment item = source.get(index);
                var card = cards.get(index);
                assertEquals(String.valueOf(item.depth()), card.attr(custom ? "data-depth-raw" : "data-depth"));
                if (custom) {
                    assertEquals(String.valueOf(item.parentId()), card.attr("data-parent-raw"));
                    assertFalse(card.hasClass("forum-comment"));
                    assertEquals(1, probes.get(index).parentWrites);
                    assertEquals(1, probes.get(index).depthWrites);
                    assertEquals(1, probes.get(index).renders);
                    assertEquals(item.parentId(), probes.get(index).parentId);
                    assertEquals(item.depth(), probes.get(index).depth);
                } else {
                    assertEquals(item.parentId() == null || item.parentId().isBlank() ? "" : item.parentId(),
                        card.attr("data-parent-id"));
                    assertEquals(0, card.select(".forum-comment .forum-comment").size());
                }
            }
            if (custom) assertEquals(source.size(), probes.size());
            assertEquals(before, source);
        }
    }

    @Test
    void filteringStrictSlicesAndControlEndpointsCanSeparateChildAndParent() {
        var source = new ArrayList<>(List.of(
            comment("child", "one", "root", Integer.MAX_VALUE),
            comment("foreign", "two", "child", Integer.MIN_VALUE),
            comment("root", "one", null, 0),
            comment("tail", "one", "absent", -1)
        ));
        var before = List.copyOf(source);
        for (boolean custom : List.of(false, true)) {
            var probes = new ArrayList<ProbeCommentComponent>();
            var builder = ForumCommentRenderer.<Comment, String>builder()
                .withPaginationEndpointResolver((topic, page, size) -> "/comments/" + topic + "?p=" + page + "&n=" + size)
                .withPaginationHxTarget("#thread").withPaginationHxSwap("outerHTML");
            if (custom) builder.withCommentComponentSupplier(() -> {
                var probe = new ProbeCommentComponent();
                probes.add(probe);
                return probe;
            });
            var renderer = builder.build();
            for (int page : List.of(1, 2, 3, 99)) {
                probes.clear();
                var result = Jsoup.parseBodyFragment(renderer.render(source, "ctx",
                    new ForumCommentRenderer.CommentPagination("one", page, 1, 3)).render());
                int selected = Math.min(page, 3);
                var cards = result.select(custom ? ".probe-comment" : ".forum-comment");
                assertEquals(List.of(List.of("child", "root", "tail").get(selected - 1)), cards.eachAttr("data-comment-id"));
                assertEquals("Page " + selected + " of 3", result.selectFirst(".forum-comments-page-status").text());
                for (String side : List.of("prev", "next")) {
                    boolean enabled = side.equals("prev") ? selected > 1 : selected < 3;
                    var control = result.selectFirst(".forum-comments-page-" + side);
                    assertEquals(!enabled, control.hasAttr("disabled"));
                    if (enabled) {
                        int target = selected + (side.equals("prev") ? -1 : 1);
                        assertEquals("/comments/one?p=" + target + "&n=1", control.attr("hx-get"));
                        assertEquals("#thread", control.attr("hx-target"));
                        assertEquals("outerHTML", control.attr("hx-swap"));
                        assertEquals("target", control.attr("data-sp-scroll-top"));
                    } else assertFalse(control.hasAttr("hx-get"));
                }
                if (custom) {
                    assertEquals(1, probes.size());
                    assertEquals(1, probes.getFirst().depthWrites);
                    assertEquals(1, probes.getFirst().parentWrites);
                    assertEquals(1, probes.getFirst().renders);
                    assertEquals(selected == 1 ? "root" : selected == 2 ? null : "absent", probes.getFirst().parentId);
                    assertEquals(selected == 1 ? Integer.MAX_VALUE : selected == 2 ? 0 : -1, probes.getFirst().depth);
                }
                assertEquals(before, source);
            }
        }
    }

    private static Comment comment(String id, String topic, String parent, int depth) {
        return new Comment(id, topic, parent, depth, "body", "author", null, "now", null, null);
    }

    @Test
    @DisplayName("ForumCommentRenderer should not invoke resolvers when bodies contain no tags")
    void noTagBodiesSkipResolverCalls() {
        AtomicInteger calls = new AtomicInteger();
        ForumTagResolverRegistry registry = ForumTagResolverRegistry.create()
            .register(ForumTagResolvers.of("custom.tag", tags -> {
                calls.incrementAndGet();
                return Map.of();
            }));

        ForumCommentRenderer<Comment, String> renderer = ForumCommentRenderer.<Comment, String>builder()
            .withResolverRegistry(registry)
            .build();

        renderer.render(List.of(
            new Comment("c1", "topic-1", null, 0, "plain", "a", null, "t1", null, null),
            new Comment("c2", "topic-1", null, 0, "still plain", "b", null, "t2", null, null)
        ), "ctx").render();

        assertEquals(0, calls.get());
    }

    @Test
    @DisplayName("ForumCommentRenderer pagination should wire first and last page controls correctly")
    void commentPaginationBoundaryControls() {
        ForumCommentRenderer<Comment, String> renderer = ForumCommentRenderer.<Comment, String>builder()
            .withPaginationEndpointResolver((topicId, page, size) ->
                "/api/forum/topics/" + topicId + "/comments?page=" + page + "&size=" + size)
            .build();

        List<Comment> comments = List.of(
            new Comment("c1", "topic-1", null, 0, "one", "a", null, "t1", null, null),
            new Comment("c2", "topic-1", null, 0, "two", "a", null, "t2", null, null),
            new Comment("c3", "topic-1", null, 0, "three", "a", null, "t3", null, null),
            new Comment("c4", "topic-1", null, 0, "four", "a", null, "t4", null, null),
            new Comment("c5", "topic-1", null, 0, "five", "a", null, "t5", null, null)
        );

        String page1 = renderer.render(
            comments,
            "ctx",
            new ForumCommentRenderer.CommentPagination("topic-1", 1, 2, 5)
        ).render();

        HtmlAssert.assertThat(page1)
            .hasElement("button.forum-comments-page-prev[disabled]")
            .attributeEquals("button.forum-comments-page-next", "hx-get", "/api/forum/topics/topic-1/comments?page=2&size=2")
            .elementTextEquals("span.forum-comments-page-status", "Page 1 of 3");

        String page3 = renderer.render(
            comments,
            "ctx",
            new ForumCommentRenderer.CommentPagination("topic-1", 3, 2, 5)
        ).render();

        HtmlAssert.assertThat(page3)
            .hasElement("button.forum-comments-page-next[disabled]")
            .attributeEquals("button.forum-comments-page-prev", "hx-get", "/api/forum/topics/topic-1/comments?page=2&size=2")
            .elementTextEquals("span.forum-comments-page-status", "Page 3 of 3");
    }

    @Test
    @DisplayName("ForumCommentRenderer default pagination endpoint should URL-encode topic ids")
    void defaultPaginationEndpointEncodesTopicId() {
        ForumCommentRenderer<Comment, String> renderer = ForumCommentRenderer.<Comment, String>builder()
            .build();

        String html = renderer.render(
            List.of(new Comment("c1", "id/a ?#&b", null, 0, "body", "a", null, "t", null, null)),
            "ctx",
            new ForumCommentRenderer.CommentPagination("id/a ?#&b", 1, 2, 3)
        ).render();

        HtmlAssert.assertThat(html)
            .attributeEquals(
                "button.forum-comments-page-next",
                "hx-get",
                "/forum/topics/id%2Fa%20%3F%23%26b/comments?page=2&size=2"
            );
    }

    @Test
    @DisplayName("ForumCommentRenderer should ignore null actions from providers")
    void nullActionsAreFiltered() {
        ForumCommentRenderer<Comment, String> renderer = ForumCommentRenderer.<Comment, String>builder()
            .withActionProvider(ctx -> {
                List<io.mindspice.simplypages.core.Component> actions = new ArrayList<>();
                actions.add(null);
                actions.add(new HtmlTag("button").withAttribute("class", "valid-action").withInnerText("ok"));
                return actions;
            })
            .build();

        String html = renderer.render(List.of(
            new Comment("c1", "topic-1", null, 0, "plain", "a", null, "t1", null, null)
        ), "ctx").render();

        HtmlAssert.assertThat(html)
            .hasElement("div.forum-comment[data-comment-id=c1] .valid-action")
            .hasElementCount("div.forum-comment[data-comment-id=c1] .forum-comment-annotations > *", 1);
    }
}
