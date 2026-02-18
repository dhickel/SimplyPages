package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ForumPostTest {

    @Test
    @DisplayName("ForumPost should render author, title, content, and stats containers")
    void testForumPostRendering() {
        ForumPost post = ForumPost.create()
            .withAuthor("Alice")
            .withTitle("Title")
            .withContent("Body")
            .withLikes(3)
            .withReplies(2)
            .disableMarkdown();

        String html = post.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.forum-post > div.post-header > div.post-author")
            .hasElement("div.forum-post > h3.post-title")
            .hasElement("div.forum-post > div.post-content")
            .hasElement("div.forum-post > div.post-footer > span.post-likes")
            .hasElement("div.forum-post > div.post-footer > span.post-replies")
            .elementTextEquals("div.post-author", "Alice")
            .elementTextEquals("h3.post-title", "Title")
            .elementTextEquals("div.post-content", "Body")
            .elementTextEquals("span.post-likes", "3 likes")
            .elementTextEquals("span.post-replies", "2 replies")
            .childOrder("div.forum-post", "div.post-header", "h3.post-title", "div.post-content", "div.post-footer");
    }
}
