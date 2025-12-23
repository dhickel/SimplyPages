package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;
import java.util.stream.Stream;

/**
 * Progress bar component for showing completion progress.
 */
public class ProgressBar extends HtmlTag {

    public enum ColorType {
        DEFAULT(""),
        PRIMARY("progress-primary"),
        SUCCESS("progress-success"),
        WARNING("progress-warning"),
        ERROR("progress-error"),
        INFO("progress-info");

        private final String cssClass;

        ColorType(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    private int value;
    private int max = 100;
    private String label;
    private ColorType colorType = ColorType.DEFAULT;
    private boolean striped = false;
    private boolean animated = false;
    private String height;

    public ProgressBar(int value) {
        super("div");
        this.value = Math.max(0, Math.min(100, value));
        this.withAttribute("class", "progress");
        this.withAttribute("role", "progressbar");
    }

    public static ProgressBar create(int value) {
        return new ProgressBar(value);
    }

    public ProgressBar withLabel(String label) {
        this.label = label;
        return this;
    }

    public ProgressBar withMax(int max) {
        this.max = max;
        return this;
    }

    public ProgressBar primary() {
        this.colorType = ColorType.PRIMARY;
        return this;
    }

    public ProgressBar success() {
        this.colorType = ColorType.SUCCESS;
        return this;
    }

    public ProgressBar warning() {
        this.colorType = ColorType.WARNING;
        return this;
    }

    public ProgressBar error() {
        this.colorType = ColorType.ERROR;
        return this;
    }

    public ProgressBar info() {
        this.colorType = ColorType.INFO;
        return this;
    }

    public ProgressBar striped() {
        this.striped = true;
        return this;
    }

    public ProgressBar animated() {
        this.animated = true;
        this.striped = true; // Animated requires striped
        return this;
    }

    public ProgressBar withHeight(String height) {
        this.height = height;
        this.withAttribute("style", "height: " + height + ";");
        return this;
    }

    @Override
    protected Stream<Component> getChildrenStream() {
        // Create progress bar fill
        String barClasses = "progress-bar";
        if (!colorType.getCssClass().isEmpty()) {
            barClasses += " " + colorType.getCssClass();
        }
        if (striped) {
            barClasses += " progress-bar-striped";
        }
        if (animated) {
            barClasses += " progress-bar-animated";
        }

        int percentage = (max > 0) ? (int) ((double) value / max * 100) : 0;

        HtmlTag bar = new HtmlTag("div")
            .withAttribute("class", barClasses)
            .withAttribute("style", "width: " + percentage + "%;")
            .withAttribute("aria-valuenow", String.valueOf(value))
            .withAttribute("aria-valuemin", "0")
            .withAttribute("aria-valuemax", String.valueOf(max));

        // Add label if specified
        if (label != null && !label.isEmpty()) {
            bar.withInnerText(label);
        }

        return Stream.concat(Stream.of(bar), super.getChildrenStream());
    }
}
