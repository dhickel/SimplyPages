package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommentTest {

    @Test
    @DisplayName("Comment should render author, content, and depth styles on exact comment node")
    void testCommentRendering() {
        Comment comment = Comment.create()
            .withAuthor("Bob")
            .withContent("Reply")
            .disableMarkdown()
            .withDepth(1);

        String html = comment.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.comment.comment-depth-1")
            .hasElement("div.comment > div.comment-header > span.comment-author")
            .hasElement("div.comment > div.comment-content")
            .elementTextEquals("span.comment-author", "Bob")
            .elementTextEquals("div.comment-content", "Reply")
            .attributeEquals("div.comment.comment-depth-1", "style", "margin-left: 20px;");
    }

    @Test
    @DisplayName("Comment should render markdown content in structured containers")
    void testCommentMarkdownStructure() {
        String markdown = "# Heading\n\nParagraph with **bold** text\n\n- One\n- Two\n\n| A | B |\n|---|---|\n| 1 | 2 |";

        String html = Comment.create()
            .withAuthor("Writer")
            .withContent(markdown)
            .withDepth(2)
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("div.comment.comment-depth-2 > div.comment-content > h1")
            .hasElement("div.comment.comment-depth-2 > div.comment-content > p")
            .hasElement("div.comment.comment-depth-2 > div.comment-content > ul > li")
            .hasElement("div.comment.comment-depth-2 > div.comment-content > table")
            .attributeEquals("div.comment.comment-depth-2", "style", "margin-left: 40px;");
    }
}
