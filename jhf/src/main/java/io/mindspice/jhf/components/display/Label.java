package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.HtmlTag;

/**
 * Label component for form labels and general text labels.
 */
public class Label extends HtmlTag {

    public Label(String text) {
        super("label");
        this.withAttribute("class", "label");
        this.withInnerText(text);
    }

    public static Label create(String text) {
        return new Label(text);
    }

    public Label forInput(String inputId) {
        this.withAttribute("for", inputId);
        return this;
    }

    public Label withClass(String className) {
        String currentClass = "label";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    public Label required() {
        return this.withClass("label-required");
    }
}
