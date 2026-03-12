package io.mindspice.simplypages.components.content;

import io.mindspice.simplypages.core.Component;

import java.util.List;

/**
 * Final list-item contract populated by {@link StaticContentSite} index rendering.
 */
public interface ContentListItemComponent extends Component {
    ContentListItemComponent withSlug(String slug);
    ContentListItemComponent withRoute(String route);
    ContentListItemComponent withTitle(String title);
    ContentListItemComponent withSummary(String summary);
    ContentListItemComponent withAuthor(String author);
    ContentListItemComponent withPublishedAt(String publishedAt);
    ContentListItemComponent withTags(List<String> tags);
}
