package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.components.forum.comments.ForumCommentComponent;
import io.mindspice.simplypages.components.forum.comments.ForumCommentData;
import io.mindspice.simplypages.components.forum.comments.ForumCommentRenderer;
import io.mindspice.simplypages.components.forum.tags.ForumTagResolverRegistry;
import io.mindspice.simplypages.components.forum.tags.ForumTagResolvers;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            return this;
        }

        @Override
        public ForumCommentComponent withDepth(int depth) {
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
            return new HtmlTag("div")
                .withAttribute("class", "probe-comment")
                .withAttribute("data-comment-id", id)
                .withAttribute("data-topic-id", topicId)
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
            .attributeEquals("div.forum-comment[data-comment-id=comment-1]", "style", "margin-left: 40px;")
            .attributeEquals("div.forum-comment[data-comment-id=comment-1] .forum-comment-avatar-slot", "style", "width: 150px; height: 150px;")
            .doesNotHaveElement("div.forum-comment[data-comment-id=comment-1] .forum-comment-avatar-image")
            .hasElement("div.forum-comment[data-comment-id=comment-1] .forum-tag-mention")
            .hasElement("div.forum-comment[data-comment-id=comment-1] .custom-action[data-item-id=comment-1]")
            .hasElement("div.forum-comment[data-comment-id=comment-2] .forum-comment-avatar-image")
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
