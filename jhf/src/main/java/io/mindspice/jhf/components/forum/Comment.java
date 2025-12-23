package io.mindspice.jhf.components.forum;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;
import io.mindspice.jhf.core.RenderContext;
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

    // Internal components
    private final HtmlTag header;
    private final HtmlTag authorSpan;
    private final HtmlTag timestampSpan;
    private HtmlTag contentDiv;

    public Comment() {
        super("div");
        this.withAttribute("class", "comment");

        // Header
        header = new HtmlTag("div").withAttribute("class", "comment-header");
        authorSpan = new HtmlTag("span").withAttribute("class", "comment-author").withInnerText("Anonymous");
        timestampSpan = new HtmlTag("span").withAttribute("class", "comment-timestamp").withInnerText("");
        header.withChild(authorSpan).withChild(timestampSpan);
        this.children.add(header);

        // Content (initialized empty)
        contentDiv = new HtmlTag("div").withAttribute("class", "comment-content");
        this.children.add(contentDiv);
    }

    public static Comment create() {
        return new Comment();
    }

    public Comment withAuthor(String author) {
        this.author = author;
        this.authorSpan.withInnerText(author != null ? author : "Anonymous");
        return this;
    }

    public Comment withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        this.timestampSpan.withInnerText(timestamp != null ? timestamp : "");
        return this;
    }

    public Comment withContent(String content) {
        this.content = content;
        updateContent();
        return this;
    }

    public Comment disableMarkdown() {
        this.useMarkdown = false;
        updateContent();
        return this;
    }

    private void updateContent() {
        int index = this.children.indexOf(contentDiv);

        HtmlTag newContentDiv = new HtmlTag("div").withAttribute("class", "comment-content");

        if (useMarkdown && content != null) {
            newContentDiv.withChild(new Markdown(content));
        } else {
            newContentDiv.withInnerText(content != null ? content : "");
        }

        if (index != -1) {
            this.children.set(index, newContentDiv);
        } else {
            this.children.add(newContentDiv);
        }

        this.contentDiv = newContentDiv;
    }

    public Comment withDepth(int depth) {
        this.depth = depth;
        if (depth > 0) {
            this.addClass("comment-depth-" + depth);
            this.addStyle("margin-left", (depth * 20) + "px");
        }
        return this;
    }

    @Override
    public Comment withClass(String className) {
        super.addClass(className);
        return this;
    }
}
