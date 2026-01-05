package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

/**
 * Simple list item component for list content.
 *
 * <p>Example usage in a container module:</p>
 * <pre>
 * SimpleListModule list = SimpleListModule.create()
 *     .addItem(ListItem.create("First item"))
 *     .addItem(ListItem.create("Second item"));
 * </pre>
 */
public class ListItem extends HtmlTag {

    private String id;  // Optional - only applied to DOM if set
    private String text;

    public ListItem() {
        super("li");
        this.text = "";
        this.withClass("list-item");
    }

    public static ListItem create() {
        return new ListItem();
    }

    public static ListItem create(String text) {
        return new ListItem().withText(text);
    }

    /**
     * Sets the HTML id attribute for this list item.
     *
     * @param id the HTML id attribute value
     * @return this ListItem for method chaining
     */
    public ListItem withId(String id) {
        this.id = id;
        if (id != null) {
            this.withAttribute("id", id);
        }
        return this;
    }

    public ListItem withText(String text) {
        this.text = text;
        return this;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    @Override
    public String render(RenderContext context) {
        // Build content inline for HtmlTag
        this.withInnerText(text);
        return super.render(context);
    }

    @Override
    public String render() {
        return render(RenderContext.empty());
    }

}
