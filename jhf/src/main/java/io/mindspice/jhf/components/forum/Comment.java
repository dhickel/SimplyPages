package io.mindspice.jhf.components.forum;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;
import io.mindspice.jhf.components.Markdown;

/**
 * Comment component for displaying individual comments.
 */
public class Comment extends HtmlTag {

    private String author;
    private String timestamp;
    private String content;
    private boolean useMarkdown = true;
    private int depth = 0;

    public Comment() {
        super("div");
        this.withAttribute("class", "comment");
    }

    public static Comment create() {
        return new Comment();
    }

    public Comment withAuthor(String author) {
        this.author = author;
        return this;
    }

    public Comment withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Comment withContent(String content) {
        this.content = content;
        return this;
    }

    public Comment disableMarkdown() {
        this.useMarkdown = false;
        return this;
    }

    public Comment withDepth(int depth) {
        this.depth = depth;
        return this;
    }

    public Comment withClass(String className) {
        String currentClass = "comment";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    protected void build() {
        if (depth > 0) {
            this.withAttribute("class", "comment comment-depth-" + depth);
            this.withAttribute("style", "margin-left: " + (depth * 20) + "px");
        }

        // Header
        HtmlTag header = new HtmlTag("div").withAttribute("class", "comment-header");

        HtmlTag authorSpan = new HtmlTag("span")
            .withAttribute("class", "comment-author")
            .withInnerText(author != null ? author : "Anonymous");

        HtmlTag timestampSpan = new HtmlTag("span")
            .withAttribute("class", "comment-timestamp")
            .withInnerText(timestamp != null ? timestamp : "");

        header.withChild(authorSpan).withChild(timestampSpan);

        // Content
        Component contentComponent;
        if (useMarkdown && content != null) {
            contentComponent = new HtmlTag("div")
                .withAttribute("class", "comment-content")
                .withChild(new Markdown(content));
        } else {
            contentComponent = new HtmlTag("div")
                .withAttribute("class", "comment-content")
                .withInnerText(content != null ? content : "");
        }

        super.withChild(header);
        super.withChild(contentComponent);
    }
}
