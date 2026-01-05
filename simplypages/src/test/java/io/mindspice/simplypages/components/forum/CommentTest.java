package io.mindspice.simplypages.components.forum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentTest {

    @Test
    @DisplayName("Comment should render author, content, and depth")
    void testCommentRendering() {
        Comment comment = Comment.create()
            .withAuthor("Bob")
            .withContent("Reply")
            .disableMarkdown()
            .withDepth(1);

        String html = comment.render();

        assertTrue(html.contains("Bob"));
        assertTrue(html.contains("Reply"));
        assertTrue(html.contains("comment-depth-1"));
        assertTrue(html.contains("margin-left: 20px"));
    }
}
