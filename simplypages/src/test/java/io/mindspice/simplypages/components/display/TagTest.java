package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TagTest {

    @Test
    @DisplayName("Tag should render color and removable classes")
    void testTagRendering() {
        Tag colored = Tag.create("Topic")
            .withColor("blue");

        Tag removable = Tag.create("Topic")
            .removable();

        String coloredHtml = colored.render();
        String removableHtml = removable.render();

        HtmlAssert.assertThat(coloredHtml)
            .hasElement("span.tag.tag-blue")
            .elementTextEquals("span.tag.tag-blue", "Topic");

        HtmlAssert.assertThat(removableHtml)
            .hasElement("span.tag.tag-removable")
            .elementTextEquals("span.tag.tag-removable", "Topic");
    }
}
