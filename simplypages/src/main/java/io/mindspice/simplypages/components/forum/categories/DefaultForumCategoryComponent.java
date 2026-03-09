package io.mindspice.simplypages.components.forum.categories;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

/**
 * Default category component implementation.
 */
public class DefaultForumCategoryComponent implements ForumCategoryComponent {
    private String id;
    private String title;
    private String description;
    private Integer topicCount;

    public static DefaultForumCategoryComponent create() {
        return new DefaultForumCategoryComponent();
    }

    @Override
    public ForumCategoryComponent withCategoryId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public ForumCategoryComponent withTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    public ForumCategoryComponent withDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public ForumCategoryComponent withTopicCount(Integer topicCount) {
        this.topicCount = topicCount;
        return this;
    }

    @Override
    public String render(RenderContext context) {
        HtmlTag category = new HtmlTag("div")
            .withAttribute("class", "forum-category")
            .withAttribute("data-category-id", id == null ? "" : id);

        category.withChild(new HtmlTag("h3")
            .withAttribute("class", "forum-category-title")
            .withInnerText(title == null ? "" : title));

        if (description != null && !description.isBlank()) {
            category.withChild(new HtmlTag("p")
                .withAttribute("class", "forum-category-description")
                .withInnerText(description));
        }

        if (topicCount != null) {
            category.withChild(new HtmlTag("span")
                .withAttribute("class", "forum-category-topic-count")
                .withInnerText(topicCount + " topics"));
        }

        return category.render(context);
    }
}
