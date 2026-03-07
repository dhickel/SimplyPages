package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import java.util.stream.Stream;

/**
 * Checkbox wrapper with optional label.
 *
 * <p>Mutable and not thread-safe. Builder-style methods update the same input/label state. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class Checkbox extends Div {

    private final HtmlTag input;
    private HtmlTag label;

    /**
     * Creates a checkbox control.
     *
     * @param name input name
     * @param value input value
     */
    public Checkbox(String name, String value) {
        super();
        this.withClass("form-checkbox");

        this.input = new HtmlTag("input", true)
            .withAttribute("type", "checkbox")
            .withAttribute("name", name)
            .withAttribute("value", value)
            .withAttribute("class", "checkbox-input");
    }

    /**
     * Creates a checkbox control.
     *
     * @param name input name
     * @param value input value
     * @return new checkbox
     */
    public static Checkbox create(String name, String value) {
        return new Checkbox(name, value);
    }

    /**
     * Sets checkbox input id.
     *
     * @param id input id
     * @return this checkbox
     */
    public Checkbox withId(String id) {
        this.input.withAttribute("id", id);
        return this;
    }

    /**
     * Creates and attaches a label linked to this checkbox.
     *
     * <p>This method always assigns a generated id to the input and overwrites any previous id.</p>
     *
     * @param labelText label text
     * @return this checkbox
     */
    public Checkbox withLabel(String labelText) {
        String id = "checkbox-" + System.nanoTime();
        this.input.withAttribute("id", id);

        this.label = new HtmlTag("label")
            .withAttribute("for", id)
            .withAttribute("class", "checkbox-label")
            .withInnerText(labelText);
        return this;
    }

    /**
     * Marks checkbox as checked.
     *
     * @return this checkbox
     */
    public Checkbox checked() {
        this.input.withAttribute("checked", "");
        return this;
    }

    /**
     * Marks checkbox as required.
     *
     * @return this checkbox
     */
    public Checkbox required() {
        this.input.withAttribute("required", "");
        return this;
    }

    /**
     * Marks checkbox as disabled.
     *
     * @return this checkbox
     */
    public Checkbox disabled() {
        this.input.withAttribute("disabled", "");
        return this;
    }

    /**
     * Renders checkbox input first, optional label second, then inherited children.
     *
     * @return child stream
     */
    @Override
    protected Stream<Component> getChildrenStream() {
        Stream.Builder<Component> builder = Stream.builder();
        builder.add(input);
        if (label != null) {
            builder.add(label);
        }
        return Stream.concat(builder.build(), super.getChildrenStream());
    }
}
