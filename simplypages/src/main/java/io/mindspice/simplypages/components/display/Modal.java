package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.components.forms.Button;

/**
 * Overlay modal component with backdrop.
 *
 * Features:
 * - Semi-transparent backdrop overlay
 * - Centered on desktop, full-screen on mobile
 * - ESC key to close
 * - Click backdrop to close (optional)
 * - Z-index layering (backdrop: 1000, modal: 1001)
 *
 * Usage:
 * <pre>
 * Modal modal = Modal.create()
 *     .withTitle("Edit Module")
 *     .withBody(formComponent)
 *     .withFooter(buttonsComponent)
 *     .closeOnBackdrop(true);
 * </pre>
 */
public class Modal extends HtmlTag {

    private String modalId;
    private String title;
    private Component body;
    private Component footer;
    private boolean closeOnBackdrop = true;
    private boolean closeOnEscape = true;
    private boolean showCloseButton = true;

    // Pattern for valid modal IDs: alphanumeric, hyphens, underscores only
    private static final java.util.regex.Pattern VALID_ID_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]*$");

    /**
     * Private constructor. Use create() factory method.
     */
    private Modal() {
        super("div");
        this.withClass("modal-backdrop");
        this.modalId = "modal-" + System.currentTimeMillis();
    }

    /**
     * Factory method to create a new Modal instance.
     * @return A new Modal instance
     */
    public static Modal create() {
        return new Modal();
    }

    /**
     * Set the modal ID.
     * <p>
     * The ID must start with a letter and contain only letters, numbers,
     * hyphens, and underscores. This is required for security as the ID
     * is used in JavaScript.
     * </p>
     *
     * @param modalId The modal identifier
     * @return this for chaining
     * @throws IllegalArgumentException if the ID contains invalid characters
     */
    public Modal withModalId(String modalId) {
        if (modalId == null || !VALID_ID_PATTERN.matcher(modalId).matches()) {
            throw new IllegalArgumentException(
                    "Modal ID must start with a letter and contain only letters, numbers, hyphens, and underscores. Got: " + modalId);
        }
        this.modalId = modalId;
        return this;
    }

    /**
     * Set the modal title.
     * @param title The modal title
     * @return this for chaining
     */
    public Modal withTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Set the modal body content.
     * @param body The body component
     * @return this for chaining
     */
    public Modal withBody(Component body) {
        this.body = body;
        return this;
    }

    /**
     * Set the modal footer content.
     * @param footer The footer component
     * @return this for chaining
     */
    public Modal withFooter(Component footer) {
        this.footer = footer;
        return this;
    }

    /**
     * Enable or disable closing modal when clicking backdrop.
     * @param enabled true to enable backdrop click to close
     * @return this for chaining
     */
    public Modal closeOnBackdrop(boolean enabled) {
        this.closeOnBackdrop = enabled;
        return this;
    }

    /**
     * Enable or disable closing modal when pressing Escape.
     *
     * @param enabled true to enable ESC key to close
     * @return this for chaining
     */
    public Modal closeOnEscape(boolean enabled) {
        this.closeOnEscape = enabled;
        return this;
    }

    /**
     * Show or hide the close button in header.
     * @param show true to show close button
     * @return this for chaining
     */
    public Modal showCloseButton(boolean show) {
        this.showCloseButton = show;
        return this;
    }

    @Override
    public String render() {
        StringBuilder html = new StringBuilder();

        // Backdrop wrapper
        html.append("<div class=\"modal-backdrop\" id=\"").append(modalId).append("\"");

        // Add onclick to close if enabled
        if (closeOnBackdrop) {
            html.append(" onclick=\"document.getElementById('")
                .append(modalId)
                .append("').remove()\"");
        }

        // ESC key support
        if (closeOnEscape) {
            html.append(" onkeydown=\"if(event.key === 'Escape') this.remove()\"");
        }
        html.append(" tabindex=\"0\""); // Make focusable for keyboard events
        html.append(">");

        // Modal container (stops propagation so clicking inside doesn't close)
        html.append("<div class=\"modal-container\" onclick=\"event.stopPropagation()\">");

        // Header
        if (title != null || showCloseButton) {
            html.append("<div class=\"modal-header\">");

            // Title
            if (title != null) {
                html.append("<h3 class=\"modal-title\" style=\"margin: 0; font-size: 1.25rem; font-weight: 600; color: #1a202c;\">")
                    .append(escapeHtml(title))
                    .append("</h3>");
            } else {
                html.append("<div></div>"); // Spacer for flexbox
            }

            // Close button
            if (showCloseButton) {
                html.append("<button type=\"button\" class=\"modal-close\" ")
                    .append("onclick=\"document.getElementById('")
                    .append(modalId)
                    .append("').remove()\" ")
                    .append("style=\"background: none; border: none; font-size: 1.5rem; ")
                    .append("color: #718096; cursor: pointer; padding: 0; line-height: 1;\" ")
                    .append("aria-label=\"Close\">&times;</button>");
            }

            html.append("</div>");
        }

        // Body
        if (body != null) {
            html.append("<div class=\"modal-body\">");
            html.append(body.render());
            html.append("</div>");
        }

        // Footer
        if (footer != null) {
            html.append("<div class=\"modal-footer\">");
            html.append(footer.render());
            html.append("</div>");
        }

        // Close container and backdrop
        html.append("</div>"); // modal-container
        html.append("</div>"); // modal-backdrop

        return html.toString();
    }

    /**
     * Escape HTML special characters to prevent XSS.
     * @param text The text to escape
     * @return Escaped text
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
