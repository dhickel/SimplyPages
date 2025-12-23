package io.mindspice.jhf.components.forum;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;
import io.mindspice.jhf.components.Markdown;
import io.mindspice.jhf.core.RenderContext;

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

    // Internal components
    private final HtmlTag header;
    private final HtmlTag authorDiv;
    private final HtmlTag timestampDiv;
    private HtmlTag titleComponent;
    private HtmlTag contentDiv;
    private final HtmlTag footer;
    private final HtmlTag likesSpan;
    private final HtmlTag repliesSpan;

    public ForumPost() {
        super("div");
        this.withAttribute("class", "forum-post");

        // Header section
        header = new HtmlTag("div").withAttribute("class", "post-header");
        authorDiv = new HtmlTag("div").withAttribute("class", "post-author").withInnerText("Anonymous");
        timestampDiv = new HtmlTag("div").withAttribute("class", "post-timestamp");
        header.withChild(authorDiv).withChild(timestampDiv);
        this.children.add(header);

        // Content section (initialized empty)
        contentDiv = new HtmlTag("div").withAttribute("class", "post-content");
        this.children.add(contentDiv);

        // Footer section
        footer = new HtmlTag("div").withAttribute("class", "post-footer");
        likesSpan = new HtmlTag("span").withAttribute("class", "post-likes").withInnerText("0 likes");
        repliesSpan = new HtmlTag("span").withAttribute("class", "post-replies").withInnerText("0 replies");
        footer.withChild(likesSpan).withChild(repliesSpan);
        this.children.add(footer);
    }

    public static ForumPost create() {
        return new ForumPost();
    }

    public ForumPost withAuthor(String author) {
        this.author = author;
        this.authorDiv.withInnerText(author != null ? author : "Anonymous");
        return this;
    }

    public ForumPost withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        this.timestampDiv.withInnerText(timestamp != null ? timestamp : "");
        return this;
    }

    public ForumPost withTitle(String title) {
        this.title = title;
        if (title != null && !title.isEmpty()) {
            if (titleComponent == null) {
                titleComponent = new HtmlTag("h3")
                        .withAttribute("class", "post-title");
                // Insert after header (index of header + 1)
                int headerIndex = this.children.indexOf(header);
                if (headerIndex != -1) {
                    this.children.add(headerIndex + 1, titleComponent);
                } else {
                    // Fallback to beginning if header missing (unlikely)
                    this.children.add(0, titleComponent);
                }
            }
            titleComponent.withInnerText(title);
        } else {
            if (titleComponent != null) {
                this.children.remove(titleComponent);
                titleComponent = null;
            }
        }
        return this;
    }

    public ForumPost withContent(String content) {
        this.content = content;
        updateContent();
        return this;
    }

    public ForumPost disableMarkdown() {
        this.useMarkdown = false;
        updateContent();
        return this;
    }

    private void updateContent() {
        // Find existing contentDiv position
        int index = this.children.indexOf(contentDiv);

        // Create new content div
        HtmlTag newContentDiv = new HtmlTag("div").withAttribute("class", "post-content");

        if (useMarkdown && content != null) {
            newContentDiv.withChild(new Markdown(content));
        } else {
            newContentDiv.withInnerText(content != null ? content : "");
        }

        // Replace in children list
        if (index != -1) {
            this.children.set(index, newContentDiv);
        } else {
            // Fallback: append before footer?
            int footerIndex = this.children.indexOf(footer);
            if (footerIndex != -1) {
                this.children.add(footerIndex, newContentDiv);
            } else {
                this.children.add(newContentDiv);
            }
        }

        this.contentDiv = newContentDiv;
    }

    public ForumPost withReplies(int replies) {
        this.replies = replies;
        this.repliesSpan.withInnerText(replies + " replies");
        return this;
    }

    public ForumPost withLikes(int likes) {
        this.likes = likes;
        this.likesSpan.withInnerText(likes + " likes");
        return this;
    }

    @Override
    public ForumPost withClass(String className) {
        super.addClass(className);
        return this;
    }
}
