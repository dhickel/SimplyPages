package io.mindspice.simplypages.components.forum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentThreadTest {

    @Test
    @DisplayName("CommentThread should render child comments")
    void testCommentThreadRendering() {
        CommentThread thread = CommentThread.create()
            .addComment(Comment.create().withContent("First").disableMarkdown());

        String html = thread.render();

        assertTrue(html.contains("comment-thread"));
        assertTrue(html.contains("First"));
    }
}
