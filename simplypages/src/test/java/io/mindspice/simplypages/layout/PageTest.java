package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageTest {

    @Test
    @DisplayName("Page should render base class")
    void testPageBaseClass() {
        Page page = Page.builder()
            .addRow(new Row())
            .build();

        String html = page.render();

        assertTrue(html.contains("class=\"page-content\""));
    }

    @Test
    @DisplayName("Page should render scrollable class when enabled")
    void testIndependentScrolling() {
        Page page = Page.builder()
            .withIndependentScrolling()
            .build();

        String html = page.render();

        assertTrue(html.contains("scrollable-page"));
    }

    @Test
    @DisplayName("Page should reject invalid sticky sidebar widths")
    void testStickySidebarWidthValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            Page.builder().withStickySidebar(new Div(), 10, 5);
        });
    }

    @Test
    @DisplayName("Page sticky sidebar should render mobile collapse structure")
    void testStickySidebarMobileCollapseMarkup() {
        Page page = Page.builder()
            .withStickySidebar(new Div().withInnerText("Sidebar"), 8, 4)
            .addComponents(new Div().withInnerText("Main"))
            .build();

        String html = page.render();

        HtmlAssert.assertThat(html)
            .hasElement(".sticky-sidebar-mobile-collapse")
            .doesNotHaveElement(".sticky-sidebar-mobile-collapse[open]")
            .hasElement(".sticky-sidebar-mobile-summary")
            .hasElement(".sticky-sidebar-content");
    }
}
