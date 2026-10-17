package io.mindspice.simplypages.editing;


import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

import java.util.ArrayList;
import java.util.List;


/**
 * Editable page wrapper that emits rows plus row-insert controls.
 *
 * <p>Lifecycle: row state is mutated via {@link #addEditableRow(EditableRow)} and markup is
 * rebuilt on each render call.</p>
 *
 * <p>Rendering composes temporary markup without changing stored attributes or children.
 * Generic inherited children remain stored but are not emitted. Templates retain this renderer
 * live, including later row and URL configuration. Finish configuration before sharing; row
 * delegates, descendants and request contexts retain their own sharing obligations.</p>
 */
public class EditablePage extends HtmlTag {

    private final String pageId;
    private String insertRowUrlTemplate = "/api/pages/{pageId}/rows/insert?position={position}";
    private final List<EditableRow> rows = new ArrayList<>();
    private final Div pageContainer;

    /** Internal constructor; use {@link #create(String)}. */
    private EditablePage(String pageId) {
        super("div");
        this.pageId = pageId;
        this.pageContainer = new Div().withClass("editable-page");
        this.withClass("editable-page-wrapper");
        this.withAttribute("id", "page-" + this.pageId);
    }

    /** Creates an editable page wrapper for the given page id. */
    public static EditablePage create(String pageId) {
        return new EditablePage(pageId);
    }

    /**
     * Sets the insert-control URL template (default
     * {@code /api/pages/{pageId}/rows/insert?position={position}}).
     *
     * <p>Only {@code {pageId}} and {@code {position}} are supported, as whole path segments or
     * whole query values. Repeated/omitted tokens and fixed URLs are allowed. Literal URL text is
     * preserved. In browser-relevant structure, token-bearing absolute URLs require
     * {@code scheme://nonempty-literal-authority};
     * protocol-relative, root-relative, relative-path and query-only references are supported.
     * Placement is also checked after browser-style control/space and path-backslash handling,
     * without rewriting the output. Scheme shorthands, empty authorities and opaque templated
     * URLs are unsupported; fixed URLs do not acquire structural validation.
     * This configures only this page's insert controls, not supplied rows.</p>
     *
     * <p>The raw page id is UTF-8 percent-encoded when interpolated during render, not at creation.
     * It must be non-null, nonempty, nonblank, not exactly {@code .} or {@code ..}, and valid UTF-16.
     * It is not trimmed, normalized or decoded; a literal percent is encoded too. Omitted tokens
     * do not validate their ids. DOM ids are unchanged. Applications own routes, server decoding,
     * authorization and CSRF enforcement.</p>
     *
     * @param template caller-owned URL template
     * @return this page
     * @throws IllegalArgumentException for null templates, unsupported tokens or token placements
     */
    public EditablePage withInsertRowUrlTemplate(String template) {
        this.insertRowUrlTemplate = EditingUrlTemplate.validate(template, "pageId", "position");
        return this;
    }

    private String insertRowUrl(int position) {
        return EditingUrlTemplate.expand(insertRowUrlTemplate,
                token -> token.equals("pageId") ? pageId : Integer.toString(position));
    }

    /** Appends an editable row to this page. */
    public EditablePage addEditableRow(EditableRow row) {
        rows.add(row);
        return this;
    }

    /**
     * Renders temporary page-builder markup for current rows. Empty pages emit position zero;
     * nonempty pages emit positions one through the row count, after their respective rows.
     */
    @Override
    public String render(RenderContext context) {
        RenderTag output = new RenderTag();

        Div content = new Div().withClass("editable-page");

        if (rows.isEmpty()) {
            // Empty page - show a single "Add First Row" button
            Div insertRowSection = new Div()
                    .withClass("insert-row-section empty-page-insert");

            Button insertBtn = Button.create("+ Add First Row")
                    .withStyle(Button.ButtonStyle.SECONDARY);

            // Position 0 means insert at the beginning (first row)
            insertBtn.withAttribute("hx-post", insertRowUrl(0));
            insertBtn.withAttribute("hx-target", "closest .insert-row-section");
            insertBtn.withAttribute("hx-swap", "beforebegin");

            insertRowSection.withChild(insertBtn);
            content.withChild(insertRowSection);
        } else {
            // Add each row with insert button after it
            for (int i = 0; i < rows.size(); i++) {
                EditableRow row = rows.get(i);

                // Add the row
                content.withChild(row);

                // Add "Insert Row Below" button after each row (including last)
                // Include position context so server knows where to insert
                Div insertRowSection = new Div()
                        .withClass("insert-row-section");

                Button insertBtn = Button.create("+ Insert Row Below")
                        .withStyle(Button.ButtonStyle.LINK);

                // Position is the index after which to insert (i+1 means insert after row i)
                int insertPosition = i + 1;
                insertBtn.withAttribute("hx-post", insertRowUrl(insertPosition));
                insertBtn.withAttribute("hx-target", "closest .insert-row-section");
                insertBtn.withAttribute("hx-swap", "beforebegin");

                insertRowSection.withChild(insertBtn);
                content.withChild(insertRowSection);
            }
        }

        output.withChild(content);

        return output.render(context);
    }

    private final class RenderTag extends HtmlTag {
        private RenderTag() {
            super(EditablePage.this.tagName, EditablePage.this.selfClosing);
            attributes.addAll(EditablePage.this.attributes);
            id = EditablePage.this.id;
            innerText = EditablePage.this.innerText;
            innerTextSlot = EditablePage.this.innerTextSlot;
            trustedHtml = EditablePage.this.trustedHtml;
        }
    }

    @Override
    public String render() {
        return render(RenderContext.empty());
    }

    /** Returns the page identifier backing this wrapper. */
    public String getPageId() {
        return pageId;
    }

    /** Returns current editable-row count. */
    public int getRowCount() {
        return rows.size();
    }
}
