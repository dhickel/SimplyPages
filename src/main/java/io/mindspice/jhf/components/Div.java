package io.mindspice.jhf.components;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

public class Div extends HtmlTag {

    public Div() {
        super("div");
    }

    public Div withClass(String className) {
        this.withAttribute("class", className);
        return this;
    }

    @Override
    public Div withAttribute(String name, String value) {
        super.withAttribute(name, value);
        return this;
    }

    @Override
    public Div withChild(Component component) {
        super.withChild(component);
        return this;
    }
}
