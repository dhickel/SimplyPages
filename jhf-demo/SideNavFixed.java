package io.mindspice.jhf.components.navigation;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Side navigation component specifically designed for vertical navigation.
 * Supports sections and nested items.
 */
public class SideNav extends HtmlTag {

    private final List<Object> items = new ArrayList<>(); // Can be NavItem or Section

    public SideNav() {
        super("nav");
        this.withAttribute("class", "sidenav");
    }

    public static SideNav create() {
        return new SideNav();
    }

    public SideNav addItem(String text, String href) {
        items.add(new NavItem(text, href, false));
        return this;
    }

    public SideNav addItem(String text, String href, boolean active) {
        items.add(new NavItem(text, href, active));
        return this;
    }

    public SideNav addItem(NavItem navItem) {
        items.add(navItem);
        return this;
    }

    public SideNav addSection(String title) {
        items.add(new Section(title));
        return this;
    }

    public SideNav withClass(String className) {
        String currentClass = "sidenav";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    public String render() {
        // Create a temporary list to hold our rendered items as components
        List<Component> tempChildren = new ArrayList<>();

        // Convert each item to a Component and add to our temporary list
        for (Object item : items) {
            if (item instanceof Component component) {
                // Wrap the rendered string in an HtmlTag component
                tempChildren.add(new HtmlTag("div") {
                    @Override
                    public String render() {
                        return component.render();
                    }
                });
            }
        }

        // Clear existing children to avoid duplication
        // Since we can't directly clear the children list, we'll work around this
        // by creating a new HtmlTag with the same attributes and our components as children

        HtmlTag navTag = new HtmlTag("nav");
        navTag.withAttribute("class", "sidenav");

        // Add each component as a child
        for (Component component : tempChildren) {
            navTag.withChild(component);
        }

        return navTag.render();
    }

    public static class NavItem implements Component {
        private final String text;
        private final String href;
        private final boolean active;
        private String icon;
        private String hxGet;
        private String hxTarget;
        private boolean hxPushUrl;

        public NavItem(String text, String href, boolean active) {
            this.text = text;
            this.href = href;
            this.active = active;
        }

        public NavItem withIcon(String icon) {
            this.icon = icon;
            return this;
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
            sb.append(" class=\"sidenav-item");
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

            sb.append(">");

            if (icon != null) {
                sb.append("<span class=\"sidenav-icon\">").append(icon).append("</span>");
            }

            sb.append(text).append("</a>");
            return sb.toString();
        }
    }

    public static class Section implements Component {
        private final String title;

        public Section(String title) {
            this.title = title;
        }

        @Override
        public String render() {
            return "<div class=\"sidenav-section\">" + title + "</div>";
        }
    }
}
