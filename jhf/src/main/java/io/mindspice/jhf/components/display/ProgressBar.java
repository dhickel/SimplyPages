package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.HtmlTag;

/**
 * Progress bar component for showing completion progress.
 *
 * <p>Progress bars visually represent the completion status of a task or operation.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Basic progress bar at 75%
 * ProgressBar.create(75);
 *
 * // Progress bar with label
 * ProgressBar.create(60).withLabel("60% Complete");
 *
 * // Colored progress bar
 * ProgressBar.create(90).success();
 *
 * // Striped animated progress bar
 * ProgressBar.create(45).striped().animated();
 *
 * // Custom height
 * ProgressBar.create(33).withHeight("30px");
 * }</pre>
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

    /**
     * Sets a custom label to display on the progress bar.
     *
     * @param label the label text
     */
    public ProgressBar withLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the maximum value (default 100).
     *
     * @param max the maximum value
     */
    public ProgressBar withMax(int max) {
        this.max = max;
        return this;
    }

    /**
     * Sets primary color style.
     */
    public ProgressBar primary() {
        this.colorType = ColorType.PRIMARY;
        return this;
    }

    /**
     * Sets success color style (green).
     */
    public ProgressBar success() {
        this.colorType = ColorType.SUCCESS;
        return this;
    }

    /**
     * Sets warning color style (yellow).
     */
    public ProgressBar warning() {
        this.colorType = ColorType.WARNING;
        return this;
    }

    /**
     * Sets error color style (red).
     */
    public ProgressBar error() {
        this.colorType = ColorType.ERROR;
        return this;
    }

    /**
     * Sets info color style (blue).
     */
    public ProgressBar info() {
        this.colorType = ColorType.INFO;
        return this;
    }

    /**
     * Adds striped pattern to the progress bar.
     */
    public ProgressBar striped() {
        this.striped = true;
        return this;
    }

    /**
     * Animates the striped pattern.
     */
    public ProgressBar animated() {
        this.animated = true;
        this.striped = true; // Animated requires striped
        return this;
    }

    /**
     * Sets custom height for the progress bar.
     *
     * @param height CSS height value (e.g., "20px", "2rem")
     */
    public ProgressBar withHeight(String height) {
        this.height = height;
        return this;
    }

    @Override
    public String render() {
        // Apply height if specified
        if (height != null) {
            this.withAttribute("style", "height: " + height + ";");
        }

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

        int percentage = (int) ((double) value / max * 100);

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

        super.withChild(bar);
        return super.render();
    }
}
