package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
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
    public String render() {
        // If there's a message, wrap in a container
        if (message != null && !message.isEmpty()) {
            HtmlTag wrapper = new HtmlTag("div").withAttribute("class", "spinner-wrapper");

            // The spinner itself
            HtmlTag spinner = new HtmlTag("div");
            String classes = "spinner " + size.getCssClass();
            if (color != null) {
                classes += " spinner-" + color;
            }
            spinner.withAttribute("class", classes);
            spinner.withAttribute("role", "status");
            spinner.withAttribute("aria-label", "Loading");

            // Message
            HtmlTag messageDiv = new HtmlTag("div")
                .withAttribute("class", "spinner-message")
                .withInnerText(message);

            wrapper.withChild(spinner);
            wrapper.withChild(messageDiv);
            return wrapper.render();
        } else {
            // Just the spinner
            return super.render();
        }
    }
}
