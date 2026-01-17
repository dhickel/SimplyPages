package io.mindspice.simplypages.components.navigation;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

/**
 * Navigation bar component.
 * Can be used for top navigation or side navigation.
 */
public class NavBar extends HtmlTag {

    private final HtmlTag itemsContainer;
    private HtmlTag brandDiv;

    public NavBar() {
        super("nav");
        this.withAttribute("class", "navbar");
        this.itemsContainer = new HtmlTag("div").withAttribute("class", "navbar-items");
        // We add itemsContainer to children. Brand will be added before it if set.
        // But since we want brand first, we can manage order in constructor or addBrand logic.
        // Let's add itemsContainer now, and if brand is added, we insert it at 0?
        // HtmlTag doesn't support insert.
        // So we'll add itemsContainer.
        this.withChild(itemsContainer);
    }

    public static NavBar create() {
        return new NavBar();
    }

    public NavBar withBrand(String brand) {
        if (brandDiv == null) {
            brandDiv = new HtmlTag("div")
                .withAttribute("class", "navbar-brand")
                .withInnerText(brand);
            // We need brand to be first.
            // Since we can't insert, we'll cheat:
            // Re-add itemsContainer after brand?
            // Accessing protected children via inheritance.
            this.children.add(0, brandDiv);
        } else {
            brandDiv.withInnerText(brand);
        }
        return this;
    }

    public NavBar addItem(String text, String href) {
        itemsContainer.withChild(new NavItem(text, href));
        return this;
    }

    public NavBar addItem(String text, String href, boolean active) {
        itemsContainer.withChild(new NavItem(text, href, active));
        return this;
    }

    public NavBar vertical() {
        this.addClass("navbar-vertical");
        return this;
    }

    public NavBar horizontal() {
        this.addClass("navbar-horizontal");
        return this;
    }

    @Override
    public NavBar withClass(String className) {
        super.addClass(className);
        return this;
    }

    // Removed getChildrenStream override

    public static class NavItem implements Component {
        private final String text;
        private final String href;
        private final boolean active;
        private String hxGet;
        private String hxTarget;
        private boolean hxPushUrl;

        public NavItem(String text, String href) {
            this(text, href, false);
        }

        public NavItem(String text, String href, boolean active) {
            this.text = text;
            this.href = href;
            this.active = active;
        }

        public NavItem withHxGet(String url) {
            this.hxGet = url;
            return this;
        }

        public NavItem withHxTarget(String target) {
            this.hxTarget = target;
            return this;
        }

        public NavItem withHxPushUrl() {
            this.hxPushUrl = true;
            return this;
        }

        @Override
        public String render(RenderContext context) {
            StringBuilder sb = new StringBuilder("<a href=\"").append(href).append("\"");
            sb.append(" class=\"navbar-item");
            if (active) {
                sb.append(" active");
            }
            sb.append("\"");

            if (hxGet != null) {
                sb.append(" hx-get=\"").append(hxGet).append("\"");
            }
            if (hxTarget != null) {
                sb.append(" hx-target=\"").append(hxTarget).append("\"");
            }
            if (hxPushUrl) {
                sb.append(" hx-push-url=\"true\"");
            }

            sb.append(">").append(text).append("</a>");
            return sb.toString();
        }

        @Override
        public String render() {
            return render(RenderContext.empty());
        }
    }
}
