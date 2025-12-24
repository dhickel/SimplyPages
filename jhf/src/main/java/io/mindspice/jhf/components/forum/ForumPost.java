package io.mindspice.jhf.components.forum;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;
import io.mindspice.jhf.components.Markdown;

/**
 * Forum post component for displaying individual forum posts.
 */
public class ForumPost extends HtmlTag {

    private String author;
    private String timestamp;
    private String title;
    private String content;
    private boolean useMarkdown = true;
    private int replies = 0;
    private int likes = 0;

    public ForumPost() {
        super("div");
        this.withAttribute("class", "forum-post");
    }

    public static ForumPost create() {
        return new ForumPost();
    }

    public ForumPost withAuthor(String author) {
        this.author = author;
        return this;
    }

    public ForumPost withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public ForumPost withTitle(String title) {
        this.title = title;
        return this;
    }

    public ForumPost withContent(String content) {
        this.content = content;
        return this;
    }

    public ForumPost disableMarkdown() {
        this.useMarkdown = false;
        return this;
    }

    public ForumPost withReplies(int replies) {
        this.replies = replies;
        return this;
    }

    public ForumPost withLikes(int likes) {
        this.likes = likes;
        return this;
    }

    public ForumPost withClass(String className) {
        String currentClass = "forum-post";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    protected void build() {
        // Header section
        HtmlTag header = new HtmlTag("div").withAttribute("class", "post-header");

        HtmlTag authorDiv = new HtmlTag("div")
            .withAttribute("class", "post-author")
            .withInnerText(author != null ? author : "Anonymous");

        HtmlTag timestampDiv = new HtmlTag("div")
            .withAttribute("class", "post-timestamp")
            .withInnerText(timestamp != null ? timestamp : "");

        header.withChild(authorDiv).withChild(timestampDiv);

        // Title section
        Component titleComponent = null;
        if (title != null && !title.isEmpty()) {
            titleComponent = new HtmlTag("h3")
                .withAttribute("class", "post-title")
                .withInnerText(title);
        }

        // Content section
        Component contentComponent;
        if (useMarkdown && content != null) {
            contentComponent = new HtmlTag("div")
                .withAttribute("class", "post-content")
                .withChild(new Markdown(content));
        } else {
            contentComponent = new HtmlTag("div")
                .withAttribute("class", "post-content")
                .withInnerText(content != null ? content : "");
        }

        // Footer section
        HtmlTag footer = new HtmlTag("div").withAttribute("class", "post-footer");

        HtmlTag likesSpan = new HtmlTag("span")
            .withAttribute("class", "post-likes")
            .withInnerText(likes + " likes");

        HtmlTag repliesSpan = new HtmlTag("span")
            .withAttribute("class", "post-replies")
            .withInnerText(replies + " replies");

        footer.withChild(likesSpan).withChild(repliesSpan);

        // Assemble
        super.withChild(header);
        if (titleComponent != null) {
            super.withChild(titleComponent);
        }
        super.withChild(contentComponent);
        super.withChild(footer);
    }
}
