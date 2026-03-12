package io.mindspice.simplypages.components.content;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.display.Tag;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

import java.util.List;

/**
 * Default section index card component for static content entries.
 */
public class DefaultContentListItemComponent implements ContentListItemComponent {

    private String slug = "";
    private String route = "";
    private String title = "";
    private String summary = "";
    private String author = "";
    private String publishedAt = "";
    private List<String> tags = List.of();

    public static DefaultContentListItemComponent create() {
        return new DefaultContentListItemComponent();
    }

    @Override
    public ContentListItemComponent withSlug(String slug) {
        this.slug = slug == null ? "" : slug;
        return this;
    }

    @Override
    public ContentListItemComponent withRoute(String route) {
        this.route = route == null ? "" : route;
        return this;
    }

    @Override
    public ContentListItemComponent withTitle(String title) {
        this.title = title == null ? "" : title;
        return this;
    }

    @Override
    public ContentListItemComponent withSummary(String summary) {
        this.summary = summary == null ? "" : summary;
        return this;
    }

    @Override
    public ContentListItemComponent withAuthor(String author) {
        this.author = author == null ? "" : author.trim();
        return this;
    }

    @Override
    public ContentListItemComponent withPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt == null ? "" : publishedAt;
        return this;
    }

    @Override
    public ContentListItemComponent withTags(List<String> tags) {
        this.tags = tags == null ? List.of() : List.copyOf(tags);
        return this;
    }

    @Override
    public String render(RenderContext context) {
        HtmlTag article = new HtmlTag("article")
            .withAttribute("class", "sp-content-list-item")
            .withAttribute("data-content-slug", slug);

        if (!tags.isEmpty()) {
            article.withChild(buildTagGroup("sp-content-list-tags sp-content-list-tags-top sp-content-list-top-meta"));
        }

        HtmlTag main = new HtmlTag("section").withAttribute("class", "sp-content-list-main-box");
        main.withChild(new HtmlTag("h2")
            .withAttribute("class", "sp-content-list-title")
            .withChild(Link.create(route, title).withClass("sp-content-list-link")));

        if (!summary.isBlank()) {
            main.withChild(new Paragraph(summary).withClass("sp-content-list-summary"));
        }

        article.withChild(main);

        String byline = buildByline(author, publishedAt);
        if (!byline.isBlank()) {
            article.withChild(new HtmlTag("footer")
                .withAttribute("class", "sp-content-list-footer")
                .withChild(new HtmlTag("span")
                    .withAttribute("class", "sp-content-list-byline")
                    .withInnerText(byline)));
        }

        return article.render(context);
    }

    private HtmlTag buildTagGroup(String className) {
        HtmlTag tagGroup = new HtmlTag("div").withAttribute("class", className);
        for (String tagValue : tags) {
            if (tagValue == null || tagValue.isBlank()) {
                continue;
            }
            tagGroup.withChild(Tag.create(tagValue));
        }
        return tagGroup;
    }

    private String buildByline(String author, String publishedAt) {
        boolean hasAuthor = author != null && !author.isBlank();
        boolean hasDate = publishedAt != null && !publishedAt.isBlank();

        if (hasAuthor && hasDate) {
            return "By " + author + " on " + publishedAt;
        }
        if (hasAuthor) {
            return "By " + author;
        }
        return hasDate ? publishedAt : "";
    }
}
