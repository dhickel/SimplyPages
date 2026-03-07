package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

/**
 * Backdrop-based modal renderer with configurable close behavior.
 *
 * <p>Mutable and not thread-safe. Configure and render within a request-scoped lifecycle. For reuse, stop mutating shared instances and render stable structures with per-request context data.</p>
 *
 * <p>Security boundary: {@link #withModalId(String)} validates identifiers used in inline JS.
 * Body/footer components are rendered as provided by caller.</p>
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
     * Creates a modal with generated id and default close behavior enabled.
     */
    private Modal() {
        super("div");
        this.withClass("modal-backdrop");
        this.modalId = "modal-" + System.currentTimeMillis();
    }

    /**
     * Creates a new modal instance.
     *
     * @return new modal
     */
    public static Modal create() {
        return new Modal();
    }

    /**
     * Sets modal id used by generated close handlers.
     *
     * @param modalId id matching {@code ^[a-zA-Z][a-zA-Z0-9_-]*$}
     * @return this modal
     * @throws IllegalArgumentException when id format is invalid
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
     * Sets modal title text.
     *
     * @param title title text
     * @return this modal
     */
    public Modal withTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets modal body component.
     *
     * @param body body component
     * @return this modal
     */
    public Modal withBody(Component body) {
        this.body = body;
        return this;
    }

    /**
     * Sets modal footer component.
     *
     * @param footer footer component
     * @return this modal
     */
    public Modal withFooter(Component footer) {
        this.footer = footer;
        return this;
    }

    /**
     * Toggles close-on-backdrop behavior.
     *
     * @param enabled true to enable backdrop click to close
     * @return this modal
     */
    public Modal closeOnBackdrop(boolean enabled) {
        this.closeOnBackdrop = enabled;
        return this;
    }

    /**
     * Toggles close-on-escape behavior.
     *
     * @param enabled true to enable ESC key to close
     * @return this modal
     */
    public Modal closeOnEscape(boolean enabled) {
        this.closeOnEscape = enabled;
        return this;
    }

    /**
     * Toggles close button rendering in modal header.
     *
     * @param show true to show close button
     * @return this modal
     */
    public Modal showCloseButton(boolean show) {
        this.showCloseButton = show;
        return this;
    }

    /**
     * Renders complete modal/backdrop markup with configured handlers/content.
     *
     * @return modal HTML
     */
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
     * Ensures modal markup rendering is identical for context-aware parent render paths.
     *
     * @param context render context (unused; modal currently renders static content tree)
     * @return modal HTML
     */
    @Override
    public String render(RenderContext context) {
        return render();
    }

    /**
     * Escapes title text for safe inline HTML insertion.
     *
     * @param text raw title text
     * @return escaped title text
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
