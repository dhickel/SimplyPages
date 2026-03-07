package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Unordered list component with appended list items.
 *
 * <p>Mutable and not thread-safe. Items are accumulated and rendered in insertion order. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class UnorderedList extends HtmlTag {

    private final List<ListItem> items = new ArrayList<>();

    /**
     * Creates an unordered list with base class {@code list}.
     */
    public UnorderedList() {
        super("ul");
        this.withAttribute("class", "list");
    }

    /**
     * Creates a new unordered list.
     *
     * @return new unordered list
     */
    public static UnorderedList create() {
        return new UnorderedList();
    }

    /**
     * Appends a text list item.
     *
     * @param text item text
     * @return this list
     */
    public UnorderedList addItem(String text) {
        items.add(new ListItem(text));
        return this;
    }

    /**
     * Appends a component list item.
     *
     * @param component item component
     * @return this list
     */
    public UnorderedList addItem(Component component) {
        items.add(new ListItem(component));
        return this;
    }

    /**
     * Appends class token(s).
     *
     * @param className class token(s)
     * @return this list
     */
    @Override
    public UnorderedList withClass(String className) {
        super.addClass(className);
        return this;
    }

    /**
     * Adds {@code list-unstyled} class.
     *
     * @return this list
     */
    public UnorderedList unstyled() {
        return (UnorderedList) this.withClass("list-unstyled");
    }

    /**
     * Adds {@code list-inline} class.
     *
     * @return this list
     */
    public UnorderedList inline() {
        return (UnorderedList) this.withClass("list-inline");
    }

    /**
     * Returns inherited children plus appended list items.
     *
     * @return child stream
     */
    @Override
    protected Stream<Component> getChildrenStream() {
        return Stream.concat(
            super.getChildrenStream(),
            items.stream().map(item -> (Component) item)
        );
    }

    /**
     * Internal list-item model.
     */
    private static class ListItem implements Component {
        private final String text;
        private final Component component;

        /**
         * Creates text item.
         *
         * @param text item text
         */
        ListItem(String text) {
            this.text = text;
            this.component = null;
        }

        /**
         * Creates component item.
         *
         * @param component item component
         */
        ListItem(Component component) {
            this.text = null;
            this.component = component;
        }

        /**
         * Renders escaped text items or delegated component output.
         *
         * @param context render context
         * @return list item HTML
         */
        @Override
        public String render(RenderContext context) {
            String content = text != null ? Encode.forHtml(text) : component.render(context);
            return "<li>" + content + "</li>";
        }

        /**
         * Renders with empty context.
         *
         * @return list item HTML
         */
        @Override
        public String render() {
            return render(RenderContext.empty());
        }
    }
}
