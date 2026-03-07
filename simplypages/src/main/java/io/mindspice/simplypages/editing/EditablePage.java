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
 * <p>Mutability and thread-safety: mutable and not thread-safe. Mutate in request-scoped
 * page-builder flows; for reuse, stop mutating shared instances and render stable structures with
 * per-request context data.</p>
 */
public class EditablePage extends HtmlTag {

    private final String pageId;
    private final List<EditableRow> rows = new ArrayList<>();
    private final Div pageContainer;

    /** Internal constructor; use {@link #create(String)}. */
    private EditablePage(String pageId) {
        super("div");
        this.pageId = pageId;
        this.pageContainer = new Div().withClass("editable-page");
        this.withClass("editable-page-wrapper");
        this.withAttribute("id", "page-" + pageId);
    }

    /** Creates an editable page wrapper for the given page id. */
    public static EditablePage create(String pageId) {
        return new EditablePage(pageId);
    }

    /** Appends an editable row to this page. */
    public EditablePage addEditableRow(EditableRow row) {
        rows.add(row);
        return this;
    }

    /**
     * Rebuilds and renders page-builder markup for current rows.
     */
    @Override
    public String render(RenderContext context) {
        children.clear();

        Div content = new Div().withClass("editable-page");

        if (rows.isEmpty()) {
            // Empty page - show a single "Add First Row" button
            Div insertRowSection = new Div()
                    .withClass("insert-row-section empty-page-insert");

            Button insertBtn = Button.create("+ Add First Row")
                    .withStyle(Button.ButtonStyle.SECONDARY);

            // Position 0 means insert at the beginning (first row)
            insertBtn.withAttribute("hx-post", "/api/pages/" + pageId + "/rows/insert?position=0");
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
                insertBtn.withAttribute("hx-post", "/api/pages/" + pageId + "/rows/insert?position=" + insertPosition);
                insertBtn.withAttribute("hx-target", "closest .insert-row-section");
                insertBtn.withAttribute("hx-swap", "beforebegin");

                insertRowSection.withChild(insertBtn);
                content.withChild(insertRowSection);
            }
        }

        super.withChild(content);

        return super.render(context);
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
