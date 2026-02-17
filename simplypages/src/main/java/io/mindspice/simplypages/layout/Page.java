package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.Arrays;
import java.util.function.Consumer;

public class Page extends HtmlTag {

    private Page() {
        super("div");
        this.withAttribute("class", "page-content");
    }

    public static PageBuilder builder() {
        return new PageBuilder();
    }

    public static class PageBuilder {
        private final Page page = new Page();
        private boolean independentScrolling = false;
        private Component stickyComponent = null;
        private int stickyMainWidth = 8;
        private int stickySidebarWidth = 4;
        private HtmlTag mainContent = null;

        public PageBuilder addRow(Row row) {
            if (mainContent != null) {
                mainContent.withChild(row);
            } else {
                page.withChild(row);
            }
            return this;
        }

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

        public PageBuilder addComponents(Component... components) {
            if (mainContent != null) {
                Arrays.stream(components).forEach(mainContent::withChild);
            } else {
                Arrays.stream(components).forEach(page::withChild);
            }
            return this;
        }

        /**
         * Enable independent scrolling for this page.
         * The page content will have its own scrollbar and scroll independently from the shell/navbar.
         * Useful for lazy loading content or creating scrollable containers.
         *
         * @return this builder for method chaining
         */
        public PageBuilder withIndependentScrolling() {
            this.independentScrolling = true;
            return this;
        }

        /**
         * Add a sticky sidebar to this page layout.
         * The sticky component will appear on the right side and follow the user as they scroll.
         * It will have its own scrollbar if content exceeds the viewport height.
         *
         * <p>Note: Call this method BEFORE adding rows/components to ensure content is added to the main area.</p>
         *
         * @param stickyComponent the component to display in the sticky sidebar
         * @param mainWidth the width of the main content area (1-12 columns)
         * @param sidebarWidth the width of the sticky sidebar (1-12 columns)
         * @return this builder for method chaining
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
         * Add a sticky sidebar to this page layout with default widths (8 columns main, 4 columns sidebar).
         *
         * <p>Note: Call this method BEFORE adding rows/components to ensure content is added to the main area.</p>
         *
         * @param stickyComponent the component to display in the sticky sidebar
         * @return this builder for method chaining
         */
        public PageBuilder withStickySidebar(Component stickyComponent) {
            return withStickySidebar(stickyComponent, 8, 4);
        }

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
                HtmlTag sidebar = new HtmlTag("div")
                        .withAttribute("class", "sticky-sidebar-aside")
                        .withAttribute("style", "flex: 0 0 " + (stickySidebarWidth * 8.333333) + "%; max-width: " + (stickySidebarWidth * 8.333333) + "%;")
                        .withChild(stickyComponent);

                page.withChild(sidebar);
            }

            return page;
        }
    }
}
