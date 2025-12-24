package io.mindspice.jhf.components.navigation;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Navigation bar component.
 * Can be used for top navigation or side navigation.
 */
public class NavBar extends HtmlTag {

    private final List<NavItem> items = new ArrayList<>();
    private String brand;

    public NavBar() {
        super("nav");
        this.withAttribute("class", "navbar");
    }

    public static NavBar create() {
        return new NavBar();
    }

    public NavBar withBrand(String brand) {
        this.brand = brand;
        return this;
    }

    public NavBar addItem(String text, String href) {
        items.add(new NavItem(text, href));
        return this;
    }

    public NavBar addItem(String text, String href, boolean active) {
        items.add(new NavItem(text, href, active));
        return this;
    }

    public NavBar vertical() {
        this.withAttribute("class", "navbar navbar-vertical");
        return this;
    }

    public NavBar horizontal() {
        this.withAttribute("class", "navbar navbar-horizontal");
        return this;
    }

    public NavBar withClass(String className) {
        String currentClass = "navbar";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    protected void build() {
        // Add brand if present
        if (brand != null) {
            HtmlTag brandDiv = new HtmlTag("div")
                .withAttribute("class", "navbar-brand")
                .withInnerText(brand);
            super.withChild(brandDiv);
        }

        // Add items container
        HtmlTag itemsContainer = new HtmlTag("div").withAttribute("class", "navbar-items");
        items.forEach(item -> itemsContainer.withChild(item));
        super.withChild(itemsContainer);
    }

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
        public String render() {
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
    }
}
