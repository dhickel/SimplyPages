package io.mindspice.simplypages.testutil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class HtmlAssertTest {

    @Test
    @DisplayName("HtmlAssert should verify structure and attributes")
    void testStructuralAssertions() {
        String html = "<div class=\"card\"><h2 class=\"title\">Hello</h2><p data-k=\"1\">Body</p></div>";

        HtmlAssert.assertThat(html)
            .hasElement("div.card")
            .hasElementCount("div.card > *", 2)
            .elementTextEquals("h2.title", "Hello")
            .attributeEquals("p", "data-k", "1")
            .childOrder("div.card", "h2.title", "p");
    }

    @Test
    @DisplayName("HtmlAssert should fail when selectors are not present")
    void testMissingSelectorFailure() {
        String html = "<div><span>Value</span></div>";
        assertThrows(AssertionError.class, () -> HtmlAssert.assertThat(html).hasElement(".missing"));
    }
}
