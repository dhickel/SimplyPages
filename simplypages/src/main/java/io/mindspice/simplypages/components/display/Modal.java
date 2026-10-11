package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

/**
 * Backdrop-based modal renderer with configurable close behavior.
 *
 * <p>Mutable and not thread-safe. Configure and render within a request-scoped lifecycle. For reuse, stop mutating shared instances and render stable structures with per-request context data.</p>
 *
 * <p>Load the framework JavaScript for focus entry/containment, close actions and focus restoration
 * on direct or HTMX insertion. The container is a named modal dialog; untitled dialogs use
 * {@code Dialog} as their accessible name without adding a visible header.</p>
 *
 * <p>Security boundary: {@link #withModalId(String)} validates generated identifiers.
 * Body/footer components are rendered with the supplied context.</p>
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
        this.modalId = "modal-" + java.util.UUID.randomUUID();
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
     * Sets backdrop id and the prefix used for the dialog title id.
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
     * Renders complete modal/backdrop markup with configured behavior metadata and content.
     *
     * @return modal HTML
     */
    @Override
    public String render(RenderContext context) {
        boolean hasTitle = title != null && !title.isBlank();
        StringBuilder html = new StringBuilder();

        html.append("<div class=\"modal-backdrop\" id=\"").append(modalId)
            .append("\" data-close-on-backdrop=\"").append(closeOnBackdrop)
            .append("\" data-close-on-escape=\"").append(closeOnEscape).append("\">");

        html.append("<div class=\"modal-container\" role=\"dialog\" aria-modal=\"true\" tabindex=\"-1\"");
        if (title != null && !title.isBlank()) {
            html.append(" aria-labelledby=\"").append(modalId).append("-title\"");
        } else {
            html.append(" aria-label=\"Dialog\"");
        }
        html.append(">");

        // Header
        if (hasTitle || showCloseButton) {
            html.append("<div class=\"modal-header\">");

            // Title
            if (hasTitle) {
                html.append("<h3 class=\"modal-title\" id=\"").append(modalId)
                    .append("-title\" style=\"margin: 0; font-size: 1.25rem; font-weight: 600; color: #1a202c;\">")
                    .append(escapeHtml(title))
                    .append("</h3>");
            } else {
                html.append("<div></div>"); // Spacer for flexbox
            }

            // Close button
            if (showCloseButton) {
                html.append("<button type=\"button\" class=\"modal-close\" ")
                    .append("style=\"background: none; border: none; font-size: 1.5rem; ")
                    .append("color: #718096; cursor: pointer; padding: 0; line-height: 1;\" ")
                    .append("aria-label=\"Close\">&times;</button>");
            }

            html.append("</div>");
        }

        // Body
        if (body != null) {
            html.append("<div class=\"modal-body\">");
            html.append(body.render(context));
            html.append("</div>");
        }

        // Footer
        if (footer != null) {
            html.append("<div class=\"modal-footer\">");
            html.append(footer.render(context));
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
     * @return modal HTML
     */
    @Override
    public String render() {
        return render(RenderContext.empty());
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
