package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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

    @Override
    public UnorderedList withClass(String className) {
        super.addClass(className);
        return this;
    }

    public UnorderedList unstyled() {
        return (UnorderedList) this.withClass("list-unstyled");
    }

    public UnorderedList inline() {
        return (UnorderedList) this.withClass("list-inline");
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
            String content = text != null ? text : component.render(context);
            return "<li>" + content + "</li>";
        }

        @Override
        public String render() {
            return render(RenderContext.empty());
        }
    }
}
