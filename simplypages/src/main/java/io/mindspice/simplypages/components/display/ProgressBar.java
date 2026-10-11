package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import java.util.stream.Stream;

/**
 * Progress bar component with configurable max, label, color, and animation flags.
 *
 * <p>Mutable and not thread-safe. Value/style flags are read when child markup is built. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
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

    public ProgressBar(int value) {
        super("div");
        this.value = Math.max(0, value);
        this.withAttribute("class", "progress");
        this.withAttribute("role", "progressbar");
        this.withAttribute("aria-valuemin", "0");
        this.withAttribute("aria-label", "Progress");
        updateValueAttributes();
    }

    public static ProgressBar create(int value) {
        return new ProgressBar(value);
    }

    /**
     * Sets visible fill text and the accessible name. For an external label or a different name,
     * set {@code aria-labelledby} or {@code aria-label} with {@code withAttribute}.
     */
    public ProgressBar withLabel(String label) {
        this.label = label;
        this.withAttribute("aria-label", label == null || label.isBlank() ? "Progress" : label);
        return this;
    }

    public ProgressBar withMax(int max) {
        if (max < 1) throw new IllegalArgumentException("max must be positive");
        this.max = max;
        updateValueAttributes();
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
        this.withAttribute("style", "height: " + height + ";");
        return this;
    }

    private void updateValueAttributes() {
        this.withAttribute("aria-valuenow", String.valueOf(Math.min(value, max)));
        this.withAttribute("aria-valuemax", String.valueOf(max));
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

        int renderedValue = Math.min(value, max);
        int percentage = (int) ((double) renderedValue / max * 100);

        HtmlTag bar = new HtmlTag("div")
            .withAttribute("class", barClasses)
            .withAttribute("style", "width: " + percentage + "%;");

        // Add label if specified
        if (label != null && !label.isEmpty()) {
            bar.withInnerText(label);
        }

        return Stream.concat(Stream.of(bar), super.getChildrenStream());
    }
}
