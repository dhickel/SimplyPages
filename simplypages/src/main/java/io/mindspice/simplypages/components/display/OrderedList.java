package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Ordered list component with appended list items.
 *
 * <p>Mutable and not thread-safe. Items are accumulated and rendered in insertion order. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class OrderedList extends HtmlTag {

    private final List<ListItem> items = new ArrayList<>();

    /**
     * Creates an ordered list with base class {@code list}.
     */
    public OrderedList() {
        super("ol");
        this.withAttribute("class", "list");
    }

    /**
     * Creates a new ordered list.
     *
     * @return new ordered list
     */
    public static OrderedList create() {
        return new OrderedList();
    }

    /**
     * Appends a text list item.
     *
     * @param text item text
     * @return this list
     */
    public OrderedList addItem(String text) {
        items.add(new ListItem(text));
        return this;
    }

    /**
     * Appends a component list item.
     *
     * @param component item component
     * @return this list
     */
    public OrderedList addItem(Component component) {
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
    public OrderedList withClass(String className) {
        super.addClass(className);
        return this;
    }

    /**
     * Sets HTML {@code start} attribute.
     *
     * @param start starting number
     * @return this list
     */
    public OrderedList withStart(int start) {
        this.withAttribute("start", String.valueOf(start));
        return this;
    }

    /**
     * Enables reversed ordering.
     *
     * @return this list
     */
    public OrderedList reversed() {
        this.withAttribute("reversed", "");
        return this;
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
