package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.HtmlTag;

/**
 * Spinner component for loading indicators.
 *
 * <p>Spinners provide visual feedback during loading operations.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Small spinner
 * Spinner.create().small();
 *
 * // Medium spinner (default)
 * Spinner.create();
 *
 * // Large spinner with custom message
 * Spinner.create().large().withMessage("Loading data...");
 *
 * // Spinner with custom color
 * Spinner.create().withColor("primary");
 * }</pre>
 */
public class Spinner extends HtmlTag {

    public enum Size {
        SMALL("spinner-sm"),
        MEDIUM("spinner-md"),
        LARGE("spinner-lg");

        private final String cssClass;

        Size(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    private Size size = Size.MEDIUM;
    private String message;
    private String color;

    public Spinner() {
        super("div");
        this.withAttribute("class", "spinner");
        this.withAttribute("role", "status");
    }

    public static Spinner create() {
        return new Spinner();
    }

    /**
     * Sets small size.
     */
    public Spinner small() {
        this.size = Size.SMALL;
        return this;
    }

    /**
     * Sets medium size (default).
     */
    public Spinner medium() {
        this.size = Size.MEDIUM;
        return this;
    }

    /**
     * Sets large size.
     */
    public Spinner large() {
        this.size = Size.LARGE;
        return this;
    }

    /**
     * Adds a loading message below the spinner.
     *
     * @param message the loading message
     */
    public Spinner withMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Sets the spinner color.
     *
     * @param color color name or CSS class
     */
    public Spinner withColor(String color) {
        this.color = color;
        return this;
    }

    @Override
    public String render() {
        String classes = "spinner " + size.getCssClass();
        if (color != null) {
            classes += " spinner-" + color;
        }
        this.withAttribute("class", classes);

        // Spinner element
        HtmlTag spinnerElement = new HtmlTag("div")
            .withAttribute("class", "spinner-border");

        // Screen reader text
        HtmlTag srText = new HtmlTag("span")
            .withAttribute("class", "sr-only")
            .withInnerText("Loading...");
        spinnerElement.withChild(srText);

        super.withChild(spinnerElement);

        // Optional message
        if (message != null && !message.isEmpty()) {
            HtmlTag messageDiv = new HtmlTag("div")
                .withAttribute("class", "spinner-message")
                .withInnerText(message);
            super.withChild(messageDiv);
        }

        return super.render();
    }
}
