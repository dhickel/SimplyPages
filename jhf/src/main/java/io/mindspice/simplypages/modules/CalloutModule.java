package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.core.Module;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Markdown;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Callout module for highlighted informational boxes.
 *
 * <p>Callouts draw attention to important information with visual styling.
 * Supports different types (info, warning, success, error) for different contexts.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Info callout
 * CalloutModule.create()
 *     .info()
 *     .withTitle("Did you know?")
 *     .withContent("Scientific research is rapidly expanding.");
 *
 * // Warning callout
 * CalloutModule.create()
 *     .warning()
 *     .withTitle("Important")
 *     .withContent("Always verify data sources before citing.");
 *
 * // Success callout
 * CalloutModule.create()
 *     .success()
 *     .withContent("Your submission was successful!");
 *
 * // Error callout
 * CalloutModule.create()
 *     .error()
 *     .withTitle("Error")
 *     .withContent("Failed to load data. Please try again.");
 * }</pre>
 */
public class CalloutModule extends Module {

    public enum CalloutType {
        INFO("callout-info"),
        WARNING("callout-warning"),
        SUCCESS("callout-success"),
        ERROR("callout-error"),
        NOTE("callout-note");

        private final String cssClass;

        CalloutType(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    private String content;
    private Component customContent;
    private CalloutType type = CalloutType.INFO;
    private String icon;
    private boolean dismissible = false;

    public CalloutModule() {
        super("div");
        this.withClass("callout-module");
    }

    public static CalloutModule create() {
        return new CalloutModule();
    }

    @Override
    public CalloutModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public CalloutModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    /**
     * Sets the callout content.
     *
     * @param content the text content
     */
    public CalloutModule withContent(String content) {
        this.content = content;
        return this;
    }

    /**
     * Sets custom component content.
     *
     * @param content custom component
     */
    public CalloutModule withCustomContent(Component content) {
        this.customContent = content;
        return this;
    }

    /**
     * Sets info type (blue, informational).
     */
    public CalloutModule info() {
        this.type = CalloutType.INFO;
        return this;
    }

    /**
     * Sets warning type (yellow, caution).
     */
    public CalloutModule warning() {
        this.type = CalloutType.WARNING;
        return this;
    }

    /**
     * Sets success type (green, positive).
     */
    public CalloutModule success() {
        this.type = CalloutType.SUCCESS;
        return this;
    }

    /**
     * Sets error type (red, negative).
     */
    public CalloutModule error() {
        this.type = CalloutType.ERROR;
        return this;
    }

    /**
     * Sets note type (gray, neutral).
     */
    public CalloutModule note() {
        this.type = CalloutType.NOTE;
        return this;
    }

    /**
     * Sets a custom icon for the callout.
     *
     * @param icon icon class or HTML
     */
    public CalloutModule withIcon(String icon) {
        this.icon = icon;
        return this;
    }

    /**
     * Makes the callout dismissible with a close button.
     */
    public CalloutModule dismissible() {
        this.dismissible = true;
        return this;
    }

    @Override
    protected void buildContent() {
        Div callout = new Div().withClass("callout " + type.getCssClass());

        // Close button for dismissible callouts
        if (dismissible) {
            HtmlTag closeBtn = new HtmlTag("button")
                .withAttribute("class", "callout-close")
                .withAttribute("type", "button")
                .withAttribute("aria-label", "Close")
                .withInnerText("×");
            callout.withChild(closeBtn);
        }

        // Icon (if provided)
        if (icon != null && !icon.isEmpty()) {
            HtmlTag iconSpan = new HtmlTag("span")
                .withAttribute("class", "callout-icon")
                .withInnerText(icon);
            callout.withChild(iconSpan);
        }

        // Content wrapper
        Div contentWrapper = new Div().withClass("callout-content");

        // Title
        if (title != null && !title.isEmpty()) {
            contentWrapper.withChild(Header.H3(title).withClass("callout-title"));
        }

        // Content
        if (customContent != null) {
            contentWrapper.withChild(customContent);
        } else if (content != null && !content.isEmpty()) {
            HtmlTag contentText = new HtmlTag("div")
                .withAttribute("class", "callout-text")
                .withInnerText(content);
            contentWrapper.withChild(contentText);
        }

        callout.withChild(contentWrapper);
        super.withChild(callout);
    }
}
