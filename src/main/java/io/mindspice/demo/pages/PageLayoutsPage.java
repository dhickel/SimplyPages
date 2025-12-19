package io.mindspice.demo.pages;

import io.mindspice.jhf.components.*;
import io.mindspice.jhf.components.display.*;
import io.mindspice.jhf.components.navigation.*;
import io.mindspice.jhf.layout.Page;
import org.springframework.stereotype.Component;

/**
 * Demo page for special page layout features.
 * Demonstrates independent scrolling and sticky sidebar layouts.
 */
@Component
public class PageLayoutsPage implements DemoPage {

    @Override
    public String render() {
        return Page.builder()
                .addComponents(
                        Header.H1("Special Page Layouts").withClass("mb-4"),
                        new Paragraph("This page demonstrates the new page layout features: independent scrolling and sticky sidebar layouts."),

                        Header.H2("1. Independent Scrolling Page").withClass("mt-5 mb-3"),
                        new Paragraph("An independent scrolling page has its own scrollbar and scrolls independently from the shell and navbar. " +
                                "This is useful for creating contained scrollable areas or implementing lazy loading patterns."),

                        Header.H3("Features:").withClass("mt-4 mb-3"),
                        new Div()
                                .withAttribute("class", "list mb-4")
                                .withChild(new Div().withAttribute("class", "mb-2").withInnerText("• Page content scrolls independently from shell/navbar"))
                                .withChild(new Div().withAttribute("class", "mb-2").withInnerText("• Fixed height container with overflow scrolling"))
                                .withChild(new Div().withAttribute("class", "mb-2").withInnerText("• Ideal for lazy loading content on scroll"))
                                .withChild(new Div().withAttribute("class", "mb-2").withInnerText("• Useful for creating app-like experiences")),

                        Header.H3("Usage:").withClass("mt-4 mb-3"),
                        Code.block("""
                                Page page = Page.builder()
                                    .withIndependentScrolling()
                                    .addComponents(
                                        // Your page content here
                                    )
                                    .build();
                                """).withLanguage("java"),

                        Alert.info("The independent scrolling feature can be combined with HTMX for infinite scroll or lazy loading patterns."),

                        Divider.horizontal().withClass("my-5"),

                        Header.H2("2. Sticky Sidebar Layout").withClass("mb-3"),
                        new Paragraph("A sticky sidebar layout splits the page into main content and a persistent sidebar that follows the user as they scroll. " +
                                "The sidebar has its own scrollbar if content exceeds viewport height."),

                        Header.H3("Features:").withClass("mt-4 mb-3"),
                        new Div()
                                .withAttribute("class", "list mb-4")
                                .withChild(new Div().withAttribute("class", "mb-2").withInnerText("• Sidebar sticks to the viewport as user scrolls"))
                                .withChild(new Div().withAttribute("class", "mb-2").withInnerText("• Independent scrollbar for sidebar content"))
                                .withChild(new Div().withAttribute("class", "mb-2").withInnerText("• Configurable column widths (main vs sidebar)"))
                                .withChild(new Div().withAttribute("class", "mb-2").withInnerText("• Responsive: stacks vertically on mobile"))
                                .withChild(new Div().withAttribute("class", "mb-2").withInnerText("• Perfect for table of contents, navigation, or related links")),

                        Header.H3("Usage:").withClass("mt-4 mb-3"),
                        Code.block("""
                                // Default widths (8 columns main, 4 columns sidebar)
                                Page page = Page.builder()
                                    .withStickySidebar(
                                        // Sidebar component here
                                        new Card()
                                            .withHeader("Table of Contents")
                                            .withBody(/* navigation links */)
                                    )
                                    .addComponents(
                                        // Main content here
                                    )
                                    .build();

                                // Custom widths
                                Page page = Page.builder()
                                    .withStickySidebar(sidebarComponent, 9, 3)  // 9 cols main, 3 cols sidebar
                                    .addComponents(
                                        // Main content here
                                    )
                                    .build();
                                """).withLanguage("java"),

                        Alert.warning("Important: Call withStickySidebar() BEFORE adding rows/components to ensure content is added to the main area, not the sidebar."),

                        Divider.horizontal().withClass("my-5"),

                        Header.H2("3. Live Example").withClass("mb-3"),
                        new Paragraph("See the live example pages to experience these layouts in action:"),

                        new Div()
                                .withAttribute("class", "d-flex gap-3 mt-4")
                                .withChild(
                                        new Link("/page-layouts/scrolling-demo", "View Scrolling Demo →")
                                                .withClass("btn btn-primary")
                                )
                                .withChild(
                                        new Link("/page-layouts/sticky-demo", "View Sticky Sidebar Demo →")
                                                .withClass("btn btn-primary")
                                ),

                        Divider.horizontal().withClass("my-5"),

                        Header.H2("4. Combining Features").withClass("mb-3"),
                        new Paragraph("You can combine both features for advanced layouts:"),

                        Code.block("""
                                Page page = Page.builder()
                                    .withIndependentScrolling()
                                    .withStickySidebar(sidebarComponent, 8, 4)
                                    .addComponents(
                                        // Main content here
                                    )
                                    .build();
                                """).withLanguage("java"),

                        Alert.info("When combining features, the entire page (including both main and sidebar) will have independent scrolling, while the sidebar will also stick within that scrollable container.")
                )
                .build()
                .render();
    }
}
