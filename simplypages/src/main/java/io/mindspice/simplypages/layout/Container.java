package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Width-constrained content container.
 *
 * <p>Mutable and not thread-safe. Use within a single request/render lifecycle. For reuse, stop mutating shared instances and render stable structures with per-request context data.</p>
 */
public class Container extends HtmlTag {

    /**
     * Predefined container width variants.
     */
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

        /**
         * Returns framework CSS class token for this size.
         *
         * @return class token
         */
        public String getCssClass() {
            return cssClass;
        }
    }

    /**
     * Creates a fixed-width container with class {@code container}.
     */
    public Container() {
        super("div");
        this.withAttribute("class", "container");
    }

    /**
     * Creates a fixed-width container.
     *
     * @return new container
     */
    public static Container create() {
        return new Container();
    }

    /**
     * Creates a fluid container.
     *
     * @return new fluid container
     */
    public static Container fluid() {
        return new Container().withSize(ContainerSize.FLUID);
    }

    /**
     * Replaces class attribute with {@code container <sizeClass>}.
     *
     * @param size container size enum
     * @return this container
     */
    public Container withSize(ContainerSize size) {
        this.withAttribute("class", "container " + size.getCssClass());
        return this;
    }

    /**
     * Sets id attribute.
     *
     * @param id element id
     * @return this container
     */
    @Override
    public Container withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Replaces class attribute with {@code container <className>}.
     *
     * @param className additional classes
     * @return this container
     */
    public Container withClass(String className) {
        String currentClass = "container";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    /**
     * Appends a child component.
     *
     * @param component child component
     * @return this container
     */
    @Override
    public Container withChild(Component component) {
        super.withChild(component);
        return this;
    }
}
