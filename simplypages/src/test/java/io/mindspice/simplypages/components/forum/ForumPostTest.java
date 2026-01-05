package io.mindspice.simplypages.components.forum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForumPostTest {

    @Test
    @DisplayName("ForumPost should render author, title, and content")
    void testForumPostRendering() {
        ForumPost post = ForumPost.create()
            .withAuthor("Alice")
            .withTitle("Title")
            .withContent("Body")
            .withLikes(3)
            .withReplies(2)
            .disableMarkdown();

        String html = post.render();

        assertTrue(html.contains("Alice"));
        assertTrue(html.contains("Title"));
        assertTrue(html.contains("Body"));
        assertTrue(html.contains("3 likes"));
        assertTrue(html.contains("2 replies"));
    }
}
