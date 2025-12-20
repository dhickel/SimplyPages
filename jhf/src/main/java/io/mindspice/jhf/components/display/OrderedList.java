package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

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

    public OrderedList withClass(String className) {
        String currentClass = "list";
        this.withAttribute("class", currentClass + " " + className);
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
    public String render() {
        items.forEach(item -> super.withChild(item));
        return super.render();
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
        public String render() {
            String content = text != null ? text : component.render();
            return "<li>" + content + "</li>";
        }
    }
}
