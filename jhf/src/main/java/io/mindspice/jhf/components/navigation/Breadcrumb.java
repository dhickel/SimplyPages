package io.mindspice.jhf.components.navigation;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Breadcrumb navigation component for showing hierarchical navigation.
 */
public class Breadcrumb extends HtmlTag {

    private final List<BreadcrumbItem> items = new ArrayList<>();

    public Breadcrumb() {
        super("nav");
        this.withAttribute("class", "breadcrumb");
        this.withAttribute("aria-label", "breadcrumb");
    }

    public static Breadcrumb create() {
        return new Breadcrumb();
    }

    public Breadcrumb addItem(String text, String href) {
        items.add(new BreadcrumbItem(text, href, false));
        return this;
    }

    public Breadcrumb addActiveItem(String text) {
        items.add(new BreadcrumbItem(text, null, true));
        return this;
    }

    public Breadcrumb withClass(String className) {
        String currentClass = "breadcrumb";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    public String render() {
        HtmlTag ol = new HtmlTag("ol").withAttribute("class", "breadcrumb-list");
        items.forEach(item -> ol.withChild(item));
        super.withChild(ol);
        return super.render();
    }

    private static class BreadcrumbItem implements Component {
        private final String text;
        private final String href;
        private final boolean active;

        BreadcrumbItem(String text, String href, boolean active) {
            this.text = text;
            this.href = href;
            this.active = active;
        }

        @Override
        public String render() {
            StringBuilder sb = new StringBuilder("<li class=\"breadcrumb-item");
            if (active) {
                sb.append(" active\" aria-current=\"page");
            }
            sb.append("\">");

            if (active || href == null) {
                sb.append(text);
            } else {
                sb.append("<a href=\"").append(href).append("\">").append(text).append("</a>");
            }

            sb.append("</li>");
            return sb.toString();
        }
    }
}
