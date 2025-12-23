package io.mindspice.jhf.components.navigation;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;
import io.mindspice.jhf.core.RenderContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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

    @Override
    public NavBar withClass(String className) {
        super.addClass(className);
        return this;
    }

    @Override
    protected Stream<Component> getChildrenStream() {
        Stream.Builder<Component> builder = Stream.builder();

        // Brand
        if (brand != null) {
            HtmlTag brandDiv = new HtmlTag("div")
                .withAttribute("class", "navbar-brand")
                .withInnerText(brand);
            builder.add(brandDiv);
        }

        // Items container
        HtmlTag itemsContainer = new HtmlTag("div").withAttribute("class", "navbar-items");
        items.forEach(itemsContainer::withChild);
        builder.add(itemsContainer);

        return Stream.concat(builder.build(), super.getChildrenStream());
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
