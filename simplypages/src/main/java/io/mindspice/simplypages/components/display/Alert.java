package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Alert message component with severity style variants.
 *
 * <p>Severity controls styling only; this component does not assign a role or live-region
 * announcement policy. Application-supplied attributes are retained.</p>
 *
 * <p>Mutable and not thread-safe while being configured. Finish configuration before sharing
 * a stable structure or compiling a template; use slots/context for per-request values.</p>
 */
public class Alert extends HtmlTag {
    private boolean dismissControlConfigured;

    public enum AlertType {
        INFO("alert-info"),
        SUCCESS("alert-success"),
        WARNING("alert-warning"),
        DANGER("alert-danger");

        private final String cssClass;

        AlertType(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    public Alert(String message, AlertType type) {
        super("div");
        this.withAttribute("class", "alert " + type.getCssClass());
        this.withInnerText(message);
    }

    public static Alert info(String message) {
        return new Alert(message, AlertType.INFO);
    }

    public static Alert success(String message) {
        return new Alert(message, AlertType.SUCCESS);
    }

    public static Alert warning(String message) {
        return new Alert(message, AlertType.WARNING);
    }

    public static Alert danger(String message) {
        return new Alert(message, AlertType.DANGER);
    }

    /**
     * Configures one native, non-submit close button named {@code Dismiss alert}.
     * Repeated calls retain the dismissible class without adding another control.
     *
     * <p>Load {@code /css/framework.css} and {@code /js/framework.js} once in the page shell.
     * Native activation hides the nearest usable dismissible alert with {@code hidden};
     * the DOM is retained and dismissal is not persisted. Cancelled, disabled, unavailable
     * or covered-modal controls do not dismiss an alert. Later inserted alerts need no
     * reinitialization.</p>
     *
     * <p>If the alert held focus, an application-selected replacement is respected. In the
     * top Modal, focus stays within that dialog. On ordinary pages, surviving containing
     * elements are tried with native focus first; only an absent {@code tabindex} may be
     * borrowed as {@code -1} while focus needs it. Caller attributes, including malformed
     * values and later same-value writes, are not overwritten or removed. Borrowed state
     * is released on blur, failed focus or disconnection and can be restored when that
     * containing element was a modal opener.</p>
     *
     * <p>Custom themes/layouts, application focus flows and role/live-region semantics
     * remain application responsibilities.</p>
     *
     * @return this alert
     */
    public Alert dismissible() {
        super.addClass("alert-dismissible");
        if (!dismissControlConfigured) {
            super.withChild(new HtmlTag("button")
                    .withAttribute("type", "button")
                    .withAttribute("class", "alert-close")
                    .withAttribute("aria-label", "Dismiss alert")
                    .withInnerText("×"));
            dismissControlConfigured = true;
        }
        return this;
    }

    public Alert withClass(String className) {
        super.addClass(className);
        return this;
    }
}
