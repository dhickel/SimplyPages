package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * High-level page layout container built through {@link PageBuilder}.
 *
 * <p>{@code Page} and its builder are mutable and not thread-safe. Build and render within a
 * single request lifecycle; do not reuse the same instances concurrently.</p>
 */
public class Page extends HtmlTag {

    /**
     * Creates the underlying page root element with base class {@code page-content}.
     */
    private Page() {
        super("div");
        this.withAttribute("class", "page-content");
    }

    /**
     * Creates a new page builder.
     *
     * @return fresh builder instance
     */
    public static PageBuilder builder() {
        return new PageBuilder();
    }

    /**
     * Fluent builder for {@link Page} layouts.
     *
     * <p>Ordering matters: if sticky sidebar mode is enabled, rows/components added afterward are
     * routed to the dedicated main-content container.</p>
     */
    public static class PageBuilder {
        private final Page page = new Page();
        private boolean independentScrolling = false;
        private Component stickyComponent = null;
        private int stickyMainWidth = 8;
        private int stickySidebarWidth = 4;
        private HtmlTag mainContent = null;

        /**
         * Appends a row to page content. When sticky mode is enabled, appends to sticky main area.
         *
         * @param row row to append
         * @return this builder
         */
        public PageBuilder addRow(Row row) {
            if (mainContent != null) {
                mainContent.withChild(row);
            } else {
                page.withChild(row);
            }
            return this;
        }

        /**
         * Builds a new row via consumer and appends it using the same routing as {@link #addRow(Row)}.
         *
         * @param rowConsumer consumer that mutates a fresh row
         * @return this builder
         */
        public PageBuilder addRow(Consumer<Row> rowConsumer) {
            Row row = new Row();
            rowConsumer.accept(row);
            if (mainContent != null) {
                mainContent.withChild(row);
            } else {
                page.withChild(row);
            }
            return this;
        }

        /**
         * Appends multiple components in argument order.
         *
         * <p>Components are routed to sticky main content when sticky mode is active.</p>
         *
         * @param components components to append
         * @return this builder
         */
        public PageBuilder addComponents(Component... components) {
            if (mainContent != null) {
                Arrays.stream(components).forEach(mainContent::withChild);
            } else {
                Arrays.stream(components).forEach(page::withChild);
            }
            return this;
        }

        /**
         * Enables page-area independent scrolling by adding framework class hooks.
         *
         * @return this builder
         */
        public PageBuilder withIndependentScrolling() {
            this.independentScrolling = true;
            return this;
        }

        /**
         * Enables sticky-sidebar layout with explicit 12-grid widths.
         *
         * <p>Calling this method also enables independent scrolling. Call before
         * {@code addRow}/{@code addComponents} so subsequent content is routed to the sticky main area.</p>
         *
         * @param stickyComponent component rendered in the sidebar
         * @param mainWidth main-area width in [1,12]
         * @param sidebarWidth sidebar width in [1,12]
         * @return this builder
         * @throws IllegalArgumentException when widths are out of range or sum above 12
         */
        public PageBuilder withStickySidebar(Component stickyComponent, int mainWidth, int sidebarWidth) {
            if (mainWidth < 1 || mainWidth > 12 || sidebarWidth < 1 || sidebarWidth > 12) {
                throw new IllegalArgumentException("Column widths must be between 1 and 12");
            }
            if (mainWidth + sidebarWidth > 12) {
                throw new IllegalArgumentException("Combined column widths cannot exceed 12");
            }

            // Automatically enable independent scrolling for sticky sidebar
            this.independentScrolling = true;

            this.stickyComponent = stickyComponent;
            this.stickyMainWidth = mainWidth;
            this.stickySidebarWidth = sidebarWidth;

            // Initialize main content container
            this.mainContent = new HtmlTag("div")
                    .withAttribute("class", "sticky-sidebar-main")
                    .withAttribute("style", "flex: 0 0 " + (mainWidth * 8.333333) + "%; max-width: " + (mainWidth * 8.333333) + "%;");

            return this;
        }

        /**
         * Enables sticky-sidebar layout with default widths (8 main, 4 sidebar).
         *
         * @param stickyComponent component rendered in the sidebar
         * @return this builder
         */
        public PageBuilder withStickySidebar(Component stickyComponent) {
            return withStickySidebar(stickyComponent, 8, 4);
        }

        /**
         * Finalizes and returns the configured page.
         *
         * <p>Build is idempotent for unchanged builder state but mutates the underlying {@link Page}
         * by appending layout containers/classes as needed.</p>
         *
         * @return configured page instance
         */
        public Page build() {
            // Apply independent scrolling class if enabled
            if (independentScrolling) {
                page.addClass("scrollable-page");
            }

            // Build sticky sidebar layout if configured
            if (stickyComponent != null) {
                page.addClass("with-sticky-sidebar");

                // Add main content to page
                page.withChild(mainContent);

                // Create and add sidebar
                HtmlTag sidebarDetails = new HtmlTag("details")
                        .withAttribute("class", "sticky-sidebar-mobile-collapse")
                        .withChild(new HtmlTag("summary")
                                .withAttribute("class", "sticky-sidebar-mobile-summary")
                                .withInnerText("Page navigation"))
                        .withChild(new HtmlTag("div")
                                .withAttribute("class", "sticky-sidebar-content")
                                .withChild(stickyComponent));

                HtmlTag sidebar = new HtmlTag("div")
                        .withAttribute("class", "sticky-sidebar-aside")
                        .withAttribute("style", "flex: 0 0 " + (stickySidebarWidth * 8.333333) + "%; max-width: " + (stickySidebarWidth * 8.333333) + "%;")
                        .withChild(sidebarDetails);

                page.withChild(sidebar);
            }

            return page;
        }
    }
}
