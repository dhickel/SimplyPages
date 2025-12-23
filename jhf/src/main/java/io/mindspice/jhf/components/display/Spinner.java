package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;
import java.util.stream.Stream;

/**
 * Spinner component for loading indicators.
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
        updateClasses();
    }

    public static Spinner create() {
        return new Spinner();
    }

    public Spinner small() {
        this.size = Size.SMALL;
        updateClasses();
        return this;
    }

    public Spinner medium() {
        this.size = Size.MEDIUM;
        updateClasses();
        return this;
    }

    public Spinner large() {
        this.size = Size.LARGE;
        updateClasses();
        return this;
    }

    public Spinner withMessage(String message) {
        this.message = message;
        return this;
    }

    public Spinner withColor(String color) {
        this.color = color;
        updateClasses();
        return this;
    }

    private void updateClasses() {
        String classes = "spinner " + size.getCssClass();
        if (color != null) {
            classes += " spinner-" + color;
        }
        this.withAttribute("class", classes);
    }

    @Override
    protected Stream<Component> getChildrenStream() {
        Stream.Builder<Component> builder = Stream.builder();

        // Spinner element
        HtmlTag spinnerElement = new HtmlTag("div")
            .withAttribute("class", "spinner-border");

        // Screen reader text
        HtmlTag srText = new HtmlTag("span")
            .withAttribute("class", "sr-only")
            .withInnerText("Loading...");
        spinnerElement.withChild(srText);

        builder.add(spinnerElement);

        // Optional message
        if (message != null && !message.isEmpty()) {
            HtmlTag messageDiv = new HtmlTag("div")
                .withAttribute("class", "spinner-message")
                .withInnerText(message);
            builder.add(messageDiv);
        }

        return Stream.concat(builder.build(), super.getChildrenStream());
    }
}
