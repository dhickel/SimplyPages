package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Unordered list component.
 */
public class UnorderedList extends HtmlTag {

    private final List<ListItem> items = new ArrayList<>();

    public UnorderedList() {
        super("ul");
        this.withAttribute("class", "list");
    }

    public static UnorderedList create() {
        return new UnorderedList();
    }

    public UnorderedList addItem(String text) {
        items.add(new ListItem(text));
        return this;
    }

    public UnorderedList addItem(Component component) {
        items.add(new ListItem(component));
        return this;
    }

    public UnorderedList withClass(String className) {
        String currentClass = "list";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    public UnorderedList unstyled() {
        return this.withClass("list-unstyled");
    }

    public UnorderedList inline() {
        return this.withClass("list-inline");
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
