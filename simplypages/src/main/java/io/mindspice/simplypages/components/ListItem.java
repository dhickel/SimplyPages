package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

/**
 * Mutable list item wrapper that stores text then writes it during render.
 *
 * <p>Not thread-safe. Reusing across concurrent renders can race on internal text state.</p>
 */
public class ListItem extends HtmlTag {

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
    @Override
    public ListItem withId(String id) {
        super.withId(id);
        return this;
    }

    public ListItem withText(String text) {
        this.text = text;
        return this;
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
