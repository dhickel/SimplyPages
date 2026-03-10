package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ForumCollapsibleComposerTest {

    @Test
    @DisplayName("ForumCollapsibleComposer should default to collapsed details")
    void defaultsToCollapsed() {
        String html = ForumCollapsibleComposer.create(
            "New Topic",
            new HtmlTag("form").withAttribute("id", "topic-compose-form")
        ).render();

        HtmlAssert.assertThat(html)
            .hasElement("details.forum-composer")
            .doesNotHaveElement("details.forum-composer[open]")
            .elementTextEquals("details.forum-composer > summary.forum-composer-summary", "New Topic")
            .hasElement("details.forum-composer > div.forum-composer-content > form#topic-compose-form");
    }

    @Test
    @DisplayName("ForumCollapsibleComposer should render open details when expanded")
    void expandedStateRendersOpen() {
        String html = ForumCollapsibleComposer.create(
            "New Comment",
            new HtmlTag("div").withAttribute("id", "comment-compose")
        ).expandedByDefault().render();

        HtmlAssert.assertThat(html)
            .hasElement("details.forum-composer[open]")
            .elementTextEquals("details.forum-composer > summary.forum-composer-summary", "New Comment")
            .hasElement("details.forum-composer > div.forum-composer-content > div#comment-compose");
    }
}
