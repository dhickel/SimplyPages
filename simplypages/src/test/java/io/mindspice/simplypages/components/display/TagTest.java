package io.mindspice.simplypages.components.display;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertTrue(coloredHtml.contains("tag-blue"));
        assertTrue(removableHtml.contains("tag-removable"));
        assertTrue(removableHtml.contains(">Topic</span>"));
    }
}
