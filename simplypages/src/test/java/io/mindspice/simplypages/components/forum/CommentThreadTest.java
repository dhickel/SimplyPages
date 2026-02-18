package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommentThreadTest {

    @Test
    @DisplayName("CommentThread should render child comments")
    void testCommentThreadRendering() {
        CommentThread thread = CommentThread.create()
            .addComment(Comment.create().withContent("First").disableMarkdown());

        String html = thread.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.comment-thread")
            .hasElement("div.comment-thread > div.comment")
            .elementTextEquals("div.comment-thread > div.comment .comment-content", "First");
    }

    @Test
    @DisplayName("CommentThread should preserve 3-level nested thread order")
    void testCommentThreadDeepNestingOrder() {
        Comment level3 = Comment.create()
            .withAuthor("L3")
            .withContent("Third level")
            .disableMarkdown()
            .withDepth(3);

        Comment level2 = Comment.create()
            .withAuthor("L2")
            .withContent("Second level")
            .disableMarkdown()
            .withDepth(2);
        level2.withChild(CommentThread.create().addComment(level3));

        Comment level1 = Comment.create()
            .withAuthor("L1")
            .withContent("First level")
            .disableMarkdown()
            .withDepth(1);
        level1.withChild(CommentThread.create().addComment(level2));

        String html = CommentThread.create()
            .addComment(level1)
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("div.comment-thread > div.comment.comment-depth-1")
            .hasElement("div.comment-depth-1 > div.comment-thread > div.comment.comment-depth-2")
            .hasElement("div.comment-depth-2 > div.comment-thread > div.comment.comment-depth-3")
            .elementTextEquals("div.comment-depth-1 > div.comment-content", "First level")
            .elementTextEquals("div.comment-depth-2 > div.comment-content", "Second level")
            .elementTextEquals("div.comment-depth-3 > div.comment-content", "Third level");
    }
}
