package io.mindspice.demo.forum;

import io.mindspice.simplypages.components.forum.categories.ForumCategoryComponent;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Demo category component that links each category card to HTMX topic loading.
 */
public class ForumDemoCategoryComponent implements ForumCategoryComponent {

    private final int topicsPerPage;
    private final int commentsPerPage;

    private String id;
    private String title;
    private String description;
    private Integer topicCount;

    private ForumDemoCategoryComponent(int topicsPerPage, int commentsPerPage) {
        this.topicsPerPage = Math.max(1, topicsPerPage);
        this.commentsPerPage = Math.max(1, commentsPerPage);
    }

    public static ForumDemoCategoryComponent create(int topicsPerPage, int commentsPerPage) {
        return new ForumDemoCategoryComponent(topicsPerPage, commentsPerPage);
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
        String categoryId = id == null ? "" : id;
        String encodedCategoryId = URLEncoder.encode(categoryId, StandardCharsets.UTF_8);
        String topicEndpoint = "/forum/topics?view=topics&scope=" + encodedCategoryId
            + "&page=1&size=" + topicsPerPage
            + "&commentPage=1&commentSize=" + commentsPerPage;
        String pushUrl = "/forum?view=topics&scope=" + encodedCategoryId
            + "&topicPage=1&topicSize=" + topicsPerPage
            + "&commentPage=1&commentSize=" + commentsPerPage;

        HtmlTag root = new HtmlTag("div")
            .withAttribute("class", "forum-category")
            .withAttribute("data-category-id", categoryId);

        HtmlTag link = new HtmlTag("a")
            .withAttribute("class", "forum-category-link")
            .withAttribute("href", pushUrl)
            .withAttribute("hx-get", topicEndpoint)
            .withAttribute("hx-target", "#forum-main")
            .withAttribute("hx-swap", "outerHTML")
            .withAttribute("hx-push-url", pushUrl)
            .withInnerText(title == null ? "" : title);

        root.withChild(new HtmlTag("h3")
            .withAttribute("class", "forum-category-title")
            .withChild(link));

        if (description != null && !description.isBlank()) {
            root.withChild(new HtmlTag("p")
                .withAttribute("class", "forum-category-description")
                .withInnerText(description));
        }

        if (topicCount != null) {
            root.withChild(new HtmlTag("span")
                .withAttribute("class", "forum-category-topic-count")
                .withInnerText(topicCount + " topics"));
        }

        return root.render(context);
    }
}
