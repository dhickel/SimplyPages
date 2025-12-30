package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.components.Markdown;

import java.util.stream.Stream;

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

    @Override
    public Comment withClass(String className) {
        super.addClass(className);
        return this;
    }

    @Override
    protected Stream<Component> getChildrenStream() {
        Stream.Builder<Component> builder = Stream.builder();

        // Header
        HtmlTag header = new HtmlTag("div").withAttribute("class", "comment-header");

        HtmlTag authorSpan = new HtmlTag("span")
            .withAttribute("class", "comment-author")
            .withInnerText(author != null ? author : "Anonymous");

        HtmlTag timestampSpan = new HtmlTag("span")
            .withAttribute("class", "comment-timestamp")
            .withInnerText(timestamp != null ? timestamp : "");

        header.withChild(authorSpan).withChild(timestampSpan);
        builder.add(header);

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
        builder.add(contentComponent);

        return Stream.concat(builder.build(), super.getChildrenStream());
    }

    @Override
    public String render(RenderContext context) {
        if (depth > 0) {
            // We need to append the depth class and style.
            // Using addClass is safe.
            this.addClass("comment-depth-" + depth);
            this.addStyle("margin-left", (depth * 20) + "px");
        }
        return super.render(context);
    }
}
