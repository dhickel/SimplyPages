package io.mindspice.jhf.components.forms;

import io.mindspice.jhf.components.Div;
import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

/**
 * Checkbox input component.
 * Can be rendered standalone or with a label.
 */
public class Checkbox extends Div {

    private final HtmlTag input;
    private HtmlTag label;

    public Checkbox(String name, String value) {
        super();
        this.withClass("form-checkbox");

        this.input = new HtmlTag("input", true)
            .withAttribute("type", "checkbox")
            .withAttribute("name", name)
            .withAttribute("value", value)
            .withAttribute("class", "checkbox-input");
    }

    public static Checkbox create(String name, String value) {
        return new Checkbox(name, value);
    }

    public Checkbox withId(String id) {
        this.input.withAttribute("id", id);
        return this;
    }

    public Checkbox withLabel(String labelText) {
        String id = "checkbox-" + System.nanoTime();
        this.input.withAttribute("id", id);

        this.label = new HtmlTag("label")
            .withAttribute("for", id)
            .withAttribute("class", "checkbox-label")
            .withInnerText(labelText);
        return this;
    }

    public Checkbox checked() {
        this.input.withAttribute("checked", "");
        return this;
    }

    public Checkbox required() {
        this.input.withAttribute("required", "");
        return this;
    }

    public Checkbox disabled() {
        this.input.withAttribute("disabled", "");
        return this;
    }

    @Override
    public String render() {
        super.withChild(input);
        if (label != null) {
            super.withChild(label);
        }
        return super.render();
    }
}
