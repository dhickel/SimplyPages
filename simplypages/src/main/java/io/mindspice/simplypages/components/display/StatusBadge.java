package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Semantic status badge for operational state labels.
 *
 * <p>Mutable and not thread-safe. Configure and render within a request-scoped lifecycle.</p>
 */
public class StatusBadge extends HtmlTag {

    public enum Status {
        NEUTRAL("status-neutral"),
        INFO("status-info"),
        SUCCESS("status-success"),
        WARNING("status-warning"),
        DANGER("status-danger"),
        BUSY("status-busy");

        private final String cssClass;

        Status(String cssClass) {
            this.cssClass = cssClass;
        }

        public String cssClass() {
            return cssClass;
        }
    }

    public StatusBadge(String label, Status status) {
        super("span");
        Status resolvedStatus = status == null ? Status.NEUTRAL : status;
        this.withAttribute("class", "status-badge " + resolvedStatus.cssClass());
        this.withAttribute("data-status", resolvedStatus.name().toLowerCase());
        this.withAttribute("role", "status");
        this.withInnerText(label == null ? "" : label);
    }

    public static StatusBadge create(String label, Status status) {
        return new StatusBadge(label, status);
    }

    public static StatusBadge neutral(String label) {
        return create(label, Status.NEUTRAL);
    }

    public static StatusBadge info(String label) {
        return create(label, Status.INFO);
    }

    public static StatusBadge success(String label) {
        return create(label, Status.SUCCESS);
    }

    public static StatusBadge warning(String label) {
        return create(label, Status.WARNING);
    }

    public static StatusBadge danger(String label) {
        return create(label, Status.DANGER);
    }

    public static StatusBadge busy(String label) {
        return create(label, Status.BUSY);
    }

    public StatusBadge withAriaLabel(String ariaLabel) {
        this.withAttribute("aria-label", ariaLabel);
        return this;
    }

    public StatusBadge withClass(String className) {
        super.addClass(className);
        return this;
    }
}
