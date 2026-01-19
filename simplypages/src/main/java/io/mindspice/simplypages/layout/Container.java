package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Container component for constraining content width.
 */
public class Container extends HtmlTag {

    public enum ContainerSize {
        SMALL("container-sm"),
        MEDIUM("container-md"),
        LARGE("container-lg"),
        EXTRA_LARGE("container-xl"),
        FLUID("container-fluid");

        private final String cssClass;

        ContainerSize(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    public Container() {
        super("div");
        this.withAttribute("class", "container");
    }

    public static Container create() {
        return new Container();
    }

    public static Container fluid() {
        return new Container().withSize(ContainerSize.FLUID);
    }

    public Container withSize(ContainerSize size) {
        this.withAttribute("class", "container " + size.getCssClass());
        return this;
    }

    @Override
    public Container withId(String id) {
        super.withId(id);
        return this;
    }

    public Container withClass(String className) {
        String currentClass = "container";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    public Container withChild(Component component) {
        super.withChild(component);
        return this;
    }
}
