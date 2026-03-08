package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ForumHelperTest {

    private record Category(String id, String title, String description, Integer topicCount) {}
    private record Topic(String id, String title, String body, String author, String timestamp, Integer likes, Integer replies) {}
    private record Comment(String id, String topicId, String parentId, int depth, String body, String author, String avatarUrl, String timestamp, Integer likes, Integer replies) {}

    @Test
    @DisplayName("ForumHelper should render categories, topics, comments, tags, actions, and composer hooks")
    void rendersUnifiedViews() {
        ForumTagResolverRegistry resolvers = ForumTagResolverRegistry.withBuiltIns()
            .register(ForumTagResolvers.of("custom.tag", values -> Map.of(
                "abc", new HtmlTag("span").withAttribute("class", "resolved-custom").withInnerText("custom:abc")
            )));

        ForumHelper<Category, Topic, Comment, String> helper = ForumHelper
            .<Category, Topic, Comment, String>builder(
                new ForumHelper.CategoryAdapter<Category>() {
                    @Override
                    public String id(Category category) { return category.id(); }

                    @Override
                    public String title(Category category) { return category.title(); }

                    @Override
                    public String description(Category category) { return category.description(); }

                    @Override
                    public Integer topicCount(Category category) { return category.topicCount(); }
                },
                new ForumHelper.TopicAdapter<Topic>() {
                    @Override
                    public String id(Topic topic) { return topic.id(); }

                    @Override
                    public String title(Topic topic) { return topic.title(); }

                    @Override
                    public String body(Topic topic) { return topic.body(); }

                    @Override
                    public String author(Topic topic) { return topic.author(); }

                    @Override
                    public String timestamp(Topic topic) { return topic.timestamp(); }

                    @Override
                    public Integer likes(Topic topic) { return topic.likes(); }

                    @Override
                    public Integer replies(Topic topic) { return topic.replies(); }
                },
                new ForumHelper.CommentAdapter<Comment>() {
                    @Override
                    public String id(Comment comment) { return comment.id(); }

                    @Override
                    public String topicId(Comment comment) { return comment.topicId(); }

                    @Override
                    public String body(Comment comment) { return comment.body(); }

                    @Override
                    public String parentId(Comment comment) { return comment.parentId(); }

                    @Override
                    public int depth(Comment comment) { return comment.depth(); }

                    @Override
                    public String author(Comment comment) { return comment.author(); }

                    @Override
                    public String avatarUrl(Comment comment) { return comment.avatarUrl(); }

                    @Override
                    public String timestamp(Comment comment) { return comment.timestamp(); }

                    @Override
                    public Integer likes(Comment comment) { return comment.likes(); }

                    @Override
                    public Integer replies(Comment comment) { return comment.replies(); }
                }
            )
            .withResolverRegistry(resolvers)
            .withActionDecorator(ctx -> List.of(new HtmlTag("button")
                .withAttribute("type", "button")
                .withAttribute("class", "custom-action")
                .withAttribute("data-item-id", ctx.itemId())
                .withInnerText("Act")))
            .withCommentComposer(viewer -> new HtmlTag("div").withAttribute("class", "custom-comment-composer").withInnerText("Compose"))
            .build();

        String categoriesHtml = helper.renderCategoriesView(List.of(
            new Category("cat-1", "General", "General discussion", 2)
        ), "viewer").render();

        HtmlAssert.assertThat(categoriesHtml)
            .hasElement("div.forum-categories-view > div.forum-category[data-category-id=cat-1]")
            .hasElement("div.forum-category > h3.forum-category-title")
            .hasElement("div.forum-category > p.forum-category-description")
            .hasElement("div.forum-category > span.forum-category-topic-count");

        String topicsHtml = helper.renderTopicsView(List.of(
            new Topic("topic-1", "Release", "hello [[custom.tag::abc]] and [[unknown.tag::xyz]]", "alice", "now", null, null),
            new Topic("topic-2", "Replies", "[[quote::comment-9]]", "bob", "later", 3, 4)
        ), "viewer").render();

        HtmlAssert.assertThat(topicsHtml)
            .hasElement("div.forum-topics-view > div.forum-topics-toolbar > a.forum-new-topic-link")
            .hasElement("div.forum-topic[data-topic-id=topic-1] .resolved-custom")
            .hasElement("div.forum-topic[data-topic-id=topic-1] span.forum-tag-literal")
            .hasElement("div.forum-topic[data-topic-id=topic-1] div.forum-topic-annotations > button.custom-action[data-item-id=topic-1]")
            .doesNotHaveElement("div.forum-topic[data-topic-id=topic-1] div.forum-topic-footer")
            .hasElement("div.forum-topic[data-topic-id=topic-2] div.forum-topic-footer > span.forum-topic-likes")
            .hasElement("div.forum-topic[data-topic-id=topic-2] div.forum-topic-body-content blockquote.forum-tag-quote");

        String commentsHtml = helper.renderCommentsView(List.of(
            new Comment("comment-1", "topic-1", null, 2, "check [[mention::sam]]", "eve", null, "1m", 1, null),
            new Comment("comment-2", "topic-1", null, 0, "plain", "max", "https://example.com/avatar.png", "2m", null, null)
        ), "viewer").render();

        HtmlAssert.assertThat(commentsHtml)
            .hasElement("div.forum-comments-view > div.forum-comment[data-comment-id=comment-1][data-topic-id=topic-1]")
            .attributeEquals("div.forum-comment[data-comment-id=comment-1]", "style", "margin-left: 40px;")
            .attributeEquals("div.forum-comment[data-comment-id=comment-1] .forum-comment-avatar-slot", "style", "width: 150px; height: 150px;")
            .doesNotHaveElement("div.forum-comment[data-comment-id=comment-1] .forum-comment-avatar-image")
            .hasElement("div.forum-comment[data-comment-id=comment-1] .forum-tag-mention")
            .hasElement("div.forum-comment[data-comment-id=comment-1] .custom-action[data-item-id=comment-1]")
            .hasElement("div.forum-comment[data-comment-id=comment-2] .forum-comment-avatar-image")
            .hasElement("div.forum-comments-view > div.forum-comments-composer > div.custom-comment-composer");
    }

    @Test
    @DisplayName("ForumHelper should batch resolver calls per tag key")
    void batchesResolverCalls() {
        AtomicInteger calls = new AtomicInteger();
        ForumTagResolverRegistry registry = ForumTagResolverRegistry.create()
            .register(ForumTagResolvers.of("custom.tag", values -> {
                calls.incrementAndGet();
                return Map.of(
                    "a", new HtmlTag("span").withInnerText("A"),
                    "b", new HtmlTag("span").withInnerText("B")
                );
            }));

        ForumHelper<Category, Topic, Comment, String> helper = minimalHelper(registry);
        helper.renderTopicsView(List.of(
            new Topic("topic-1", "One", "[[custom.tag::a]] [[custom.tag::b]]", "a", "t", null, null),
            new Topic("topic-2", "Two", "[[custom.tag::a]]", "b", "t2", null, null)
        ), "viewer").render();

        org.junit.jupiter.api.Assertions.assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("ForumHelper should paginate comments and wire previous/next HTMX controls")
    void paginatesCommentsWithHtmxControls() {
        ForumHelper<Category, Topic, Comment, String> helper = ForumHelper
            .<Category, Topic, Comment, String>builder(
                new ForumHelper.CategoryAdapter<>() {
                    @Override
                    public String id(Category category) { return category.id(); }

                    @Override
                    public String title(Category category) { return category.title(); }
                },
                new ForumHelper.TopicAdapter<>() {
                    @Override
                    public String id(Topic topic) { return topic.id(); }

                    @Override
                    public String title(Topic topic) { return topic.title(); }

                    @Override
                    public String body(Topic topic) { return topic.body(); }
                },
                new ForumHelper.CommentAdapter<>() {
                    @Override
                    public String id(Comment comment) { return comment.id(); }

                    @Override
                    public String topicId(Comment comment) { return comment.topicId(); }

                    @Override
                    public String body(Comment comment) { return comment.body(); }
                }
            )
            .withCommentPaginationEndpointResolver((topicId, page, size) ->
                "/api/forum/topics/" + topicId + "/comments?page=" + page + "&size=" + size)
            .withCommentsPaginationHxTarget("#topic-comments")
            .withCommentsPaginationHxSwap("outerHTML")
            .withActionDecorator(ForumActionDecorator.none())
            .withCommentComposer(viewer -> null)
            .build();

        List<Comment> comments = List.of(
            new Comment("c1", "topic-1", null, 0, "one", "a", null, "t1", null, null),
            new Comment("c2", "topic-1", null, 0, "two", "a", null, "t2", null, null),
            new Comment("c3", "topic-1", null, 0, "three", "a", null, "t3", null, null),
            new Comment("c4", "topic-1", null, 0, "four", "a", null, "t4", null, null),
            new Comment("c5", "topic-1", null, 0, "five", "a", null, "t5", null, null),
            new Comment("c99", "topic-2", null, 0, "other", "a", null, "t6", null, null)
        );

        String page1Html = helper.renderCommentsView(
            comments,
            "viewer",
            new ForumHelper.CommentPagination("topic-1", 1, 2, 5)
        ).render();

        HtmlAssert.assertThat(page1Html)
            .hasElementCount("div.forum-comments-view > div.forum-comment", 2)
            .hasElement("div.forum-comment[data-comment-id=c1]")
            .hasElement("div.forum-comment[data-comment-id=c2]")
            .doesNotHaveElement("div.forum-comment[data-comment-id=c99]")
            .hasElement("div.forum-comments-pagination[data-topic-id=topic-1][data-page=1][data-total-pages=3]")
            .hasElement("button.forum-comments-page-prev[disabled]")
            .hasElement("button.forum-comments-page-next")
            .attributeEquals("button.forum-comments-page-next", "hx-get", "/api/forum/topics/topic-1/comments?page=2&size=2")
            .attributeEquals("button.forum-comments-page-next", "hx-target", "#topic-comments")
            .attributeEquals("button.forum-comments-page-next", "hx-swap", "outerHTML");

        String page3Html = helper.renderCommentsView(
            comments,
            "viewer",
            new ForumHelper.CommentPagination("topic-1", 3, 2, 5)
        ).render();

        HtmlAssert.assertThat(page3Html)
            .hasElementCount("div.forum-comments-view > div.forum-comment", 1)
            .hasElement("div.forum-comment[data-comment-id=c5]")
            .hasElement("button.forum-comments-page-next[disabled]")
            .hasElement("button.forum-comments-page-prev")
            .attributeEquals("button.forum-comments-page-prev", "hx-get", "/api/forum/topics/topic-1/comments?page=2&size=2")
            .elementTextEquals("span.forum-comments-page-status", "Page 3 of 3");
    }

    @Test
    @DisplayName("ForumHelper should paginate topics and wire previous/next HTMX controls")
    void paginatesTopicsWithHtmxControls() {
        ForumHelper<Category, Topic, Comment, String> helper = ForumHelper
            .<Category, Topic, Comment, String>builder(
                new ForumHelper.CategoryAdapter<>() {
                    @Override
                    public String id(Category category) { return category.id(); }

                    @Override
                    public String title(Category category) { return category.title(); }
                },
                new ForumHelper.TopicAdapter<>() {
                    @Override
                    public String id(Topic topic) { return topic.id(); }

                    @Override
                    public String title(Topic topic) { return topic.title(); }

                    @Override
                    public String body(Topic topic) { return topic.body(); }
                },
                new ForumHelper.CommentAdapter<>() {
                    @Override
                    public String id(Comment comment) { return comment.id(); }

                    @Override
                    public String topicId(Comment comment) { return comment.topicId(); }

                    @Override
                    public String body(Comment comment) { return comment.body(); }
                }
            )
            .withTopicPaginationEndpointResolver((scopeId, page, size) ->
                "/api/forum/topics?scope=" + scopeId + "&page=" + page + "&size=" + size)
            .withTopicsPaginationHxTarget("#topics-view")
            .withTopicsPaginationHxSwap("outerHTML")
            .withActionDecorator(ForumActionDecorator.none())
            .withTopicComposerLauncher(viewer -> null)
            .build();

        List<Topic> topics = List.of(
            new Topic("t1", "one", "b1", "a", "t1", null, null),
            new Topic("t2", "two", "b2", "a", "t2", null, null),
            new Topic("t3", "three", "b3", "a", "t3", null, null),
            new Topic("t4", "four", "b4", "a", "t4", null, null),
            new Topic("t5", "five", "b5", "a", "t5", null, null)
        );

        String page1Html = helper.renderTopicsView(
            topics,
            "viewer",
            new ForumHelper.TopicPagination("cat-1", 1, 2, 5)
        ).render();

        HtmlAssert.assertThat(page1Html)
            .hasElementCount("div.forum-topics-view > div.forum-topic", 2)
            .hasElement("div.forum-topic[data-topic-id=t1]")
            .hasElement("div.forum-topic[data-topic-id=t2]")
            .hasElement("div.forum-topics-pagination[data-scope-id=cat-1][data-page=1][data-total-pages=3]")
            .hasElement("button.forum-topics-page-prev[disabled]")
            .hasElement("button.forum-topics-page-next")
            .attributeEquals("button.forum-topics-page-next", "hx-get", "/api/forum/topics?scope=cat-1&page=2&size=2")
            .attributeEquals("button.forum-topics-page-next", "hx-target", "#topics-view")
            .attributeEquals("button.forum-topics-page-next", "hx-swap", "outerHTML");

        String page3Html = helper.renderTopicsView(
            topics,
            "viewer",
            new ForumHelper.TopicPagination("cat-1", 3, 2, 5)
        ).render();

        HtmlAssert.assertThat(page3Html)
            .hasElementCount("div.forum-topics-view > div.forum-topic", 1)
            .hasElement("div.forum-topic[data-topic-id=t5]")
            .hasElement("button.forum-topics-page-next[disabled]")
            .hasElement("button.forum-topics-page-prev")
            .attributeEquals("button.forum-topics-page-prev", "hx-get", "/api/forum/topics?scope=cat-1&page=2&size=2")
            .elementTextEquals("span.forum-topics-page-status", "Page 3 of 3");
    }

    @Test
    @DisplayName("ForumHelper pagination should default target to the current comments view container")
    void defaultPaginationTargetUsesClosestCommentsContainer() {
        ForumHelper<Category, Topic, Comment, String> helper = minimalHelper(ForumTagResolverRegistry.withBuiltIns());

        String html = helper.renderCommentsView(
            List.of(
                new Comment("c1", "topic-1", null, 0, "one", "a", null, "t1", null, null),
                new Comment("c2", "topic-1", null, 0, "two", "a", null, "t2", null, null)
            ),
            "viewer",
            new ForumHelper.CommentPagination("topic-1", 1, 1, 2)
        ).render();

        HtmlAssert.assertThat(html)
            .attributeEquals("button.forum-comments-page-next", "hx-target", "closest .forum-comments-view");
    }

    @Test
    @DisplayName("ForumHelper topic pagination should default target to the current topics view container")
    void defaultTopicPaginationTargetUsesClosestTopicsContainer() {
        ForumHelper<Category, Topic, Comment, String> helper = minimalHelper(ForumTagResolverRegistry.withBuiltIns());

        String html = helper.renderTopicsView(
            List.of(
                new Topic("t1", "one", "body", "a", "t1", null, null),
                new Topic("t2", "two", "body", "a", "t2", null, null)
            ),
            "viewer",
            new ForumHelper.TopicPagination("cat-1", 1, 1, 2)
        ).render();

        HtmlAssert.assertThat(html)
            .attributeEquals("button.forum-topics-page-next", "hx-target", "closest .forum-topics-view");
    }

    @Test
    @DisplayName("ForumHelper should require IDs for topics and comments")
    void requiresIds() {
        ForumHelper<Category, Topic, Comment, String> helper = minimalHelper(ForumTagResolverRegistry.withBuiltIns());

        assertThrows(IllegalArgumentException.class,
            () -> helper.renderTopicsView(List.of(new Topic("", "bad", "body", "a", "t", null, null)), "viewer").render());

        assertThrows(IllegalArgumentException.class,
            () -> helper.renderCommentsView(List.of(new Comment("c", "", null, 0, "body", "a", null, "t", null, null)), "viewer").render());
    }

    private ForumHelper<Category, Topic, Comment, String> minimalHelper(ForumTagResolverRegistry registry) {
        return ForumHelper
            .<Category, Topic, Comment, String>builder(
                new ForumHelper.CategoryAdapter<Category>() {
                    @Override
                    public String id(Category category) { return category.id(); }

                    @Override
                    public String title(Category category) { return category.title(); }
                },
                new ForumHelper.TopicAdapter<Topic>() {
                    @Override
                    public String id(Topic topic) { return topic.id(); }

                    @Override
                    public String title(Topic topic) { return topic.title(); }

                    @Override
                    public String body(Topic topic) { return topic.body(); }
                },
                new ForumHelper.CommentAdapter<Comment>() {
                    @Override
                    public String id(Comment comment) { return comment.id(); }

                    @Override
                    public String topicId(Comment comment) { return comment.topicId(); }

                    @Override
                    public String body(Comment comment) { return comment.body(); }
                }
            )
            .withResolverRegistry(registry)
            .withActionDecorator(ForumActionDecorator.none())
            .withCommentComposer(viewer -> null)
            .build();
    }
}
