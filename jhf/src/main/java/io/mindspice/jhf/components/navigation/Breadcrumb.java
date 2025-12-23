package io.mindspice.jhf.components.navigation;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;
import io.mindspice.jhf.core.RenderContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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

    @Override
    public Breadcrumb withClass(String className) {
        super.addClass(className);
        return this;
    }

    @Override
    protected Stream<Component> getChildrenStream() {
        HtmlTag ol = new HtmlTag("ol").withAttribute("class", "breadcrumb-list");
        items.forEach(ol::withChild);

        return Stream.concat(Stream.of(ol), super.getChildrenStream());
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
        public String render(RenderContext context) {
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

        @Override
        public String render() {
            return render(RenderContext.empty());
        }
    }
}
