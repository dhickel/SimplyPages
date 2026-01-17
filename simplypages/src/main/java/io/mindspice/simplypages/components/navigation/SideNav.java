package io.mindspice.simplypages.components.navigation;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import org.owasp.encoder.Encode;

/**
 * Side navigation component specifically designed for vertical navigation.
 * Supports sections and nested items.
 */
public class SideNav extends HtmlTag {

    public SideNav() {
        super("nav");
        this.withAttribute("class", "sidenav");
    }

    public static SideNav create() {
        return new SideNav();
    }

    public SideNav addItem(String text, String href) {
        this.withChild(new NavItem(text, href, false));
        return this;
    }

    public SideNav addItem(String text, String href, boolean active) {
        this.withChild(new NavItem(text, href, active));
        return this;
    }

    public SideNav addItem(NavItem navItem) {
        this.withChild(navItem);
        return this;
    }

    public SideNav addSection(String title) {
        this.withChild(new Section(title));
        return this;
    }

    @Override
    public SideNav withClass(String className) {
        super.addClass(className);
        return this;
    }

    // Removed getChildrenStream override. We just add items/sections as children directly.

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
        public String render(RenderContext context) {
            String safeHref = href == null ? "" : Encode.forHtmlAttribute(href);
            String safeText = text == null ? "" : Encode.forHtml(text);
            StringBuilder sb = new StringBuilder("<a href=\"").append(safeHref).append("\"");
            sb.append(" class=\"sidenav-item");
            if (active) {
                sb.append(" active");
            }
            sb.append("\"");

            if (hxGet != null) {
                sb.append(" hx-get=\"").append(Encode.forHtmlAttribute(hxGet)).append("\"");
            }
            if (hxTarget != null) {
                sb.append(" hx-target=\"").append(Encode.forHtmlAttribute(hxTarget)).append("\"");
            }
            if (hxPushUrl) {
                sb.append(" hx-push-url=\"true\"");
            }

            sb.append(">");

            if (icon != null) {
                sb.append("<span class=\"sidenav-icon\">").append(Encode.forHtml(icon)).append("</span>");
            }

            sb.append(safeText).append("</a>");
            return sb.toString();
        }

        @Override
        public String render() {
            return render(RenderContext.empty());
        }
    }

    public static class Section implements Component {
        private final String title;

        public Section(String title) {
            this.title = title;
        }

        @Override
        public String render(RenderContext context) {
            String safeTitle = title == null ? "" : Encode.forHtml(title);
            return "<div class=\"sidenav-section\">" + safeTitle + "</div>";
        }

        @Override
        public String render() {
            return render(RenderContext.empty());
        }
    }
}
