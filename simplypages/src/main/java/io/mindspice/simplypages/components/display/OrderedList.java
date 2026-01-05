package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Ordered list component.
 */
public class OrderedList extends HtmlTag {

    private final List<ListItem> items = new ArrayList<>();

    public OrderedList() {
        super("ol");
        this.withAttribute("class", "list");
    }

    public static OrderedList create() {
        return new OrderedList();
    }

    public OrderedList addItem(String text) {
        items.add(new ListItem(text));
        return this;
    }

    public OrderedList addItem(Component component) {
        items.add(new ListItem(component));
        return this;
    }

    @Override
    public OrderedList withClass(String className) {
        super.addClass(className);
        return this;
    }

    public OrderedList withStart(int start) {
        this.withAttribute("start", String.valueOf(start));
        return this;
    }

    public OrderedList reversed() {
        this.withAttribute("reversed", "");
        return this;
    }

    @Override
    protected Stream<Component> getChildrenStream() {
        return Stream.concat(
            super.getChildrenStream(),
            items.stream().map(item -> (Component) item)
        );
    }

    private static class ListItem implements Component {
        private final String text;
        private final Component component;

        ListItem(String text) {
            this.text = text;
            this.component = null;
        }

        ListItem(Component component) {
            this.text = null;
            this.component = component;
        }

        @Override
        public String render(RenderContext context) {
            String content = text != null ? Encode.forHtml(text) : component.render(context);
            return "<li>" + content + "</li>";
        }

        @Override
        public String render() {
            return render(RenderContext.empty());
        }
    }
}
