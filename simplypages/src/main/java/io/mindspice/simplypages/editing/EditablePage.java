package io.mindspice.simplypages.editing;


import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

import java.util.ArrayList;
import java.util.List;


/**
 * Page wrapper that adds page-building UI for managing rows and modules.
 * <p>
 * EditablePage allows users to add, edit, and remove rows and modules,
 * providing a complete page builder experience with insert controls
 * between rows and at the bottom of the page.
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><strong>Row Management:</strong> Insert rows at any position using "Insert Row Below" pattern</li>
 *   <li><strong>Visual Separators:</strong> Clean borders between rows with insert buttons</li>
 *   <li><strong>HTMX Integration:</strong> Dynamic row insertion without page reload</li>
 *   <li><strong>Clean UI:</strong> Professional page builder interface</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // Create editable page
 * EditablePage page = EditablePage.create("page-123");
 *
 * // Add editable rows
 * Row row1 = new Row();
 * EditableRow editableRow1 = EditableRow.wrap(row1, "row-1", "page-123")
 *     .addEditableModule(ContentModule.create().withTitle("Content"), "module-1");
 *
 * page.addEditableRow(editableRow1);
 *
 * Row row2 = new Row();
 * EditableRow editableRow2 = EditableRow.wrap(row2, "row-2", "page-123")
 *     .addEditableModule(GalleryModule.create().withTitle("Gallery"), "module-2");
 *
 * page.addEditableRow(editableRow2);
 *
 * // Render complete editable page
 * String html = page.render();
 * }</pre>
 *
 * <h2>Controller Example</h2>
 * <pre>{@code
 * @GetMapping("/vendor/page/{pageId}/edit")
 * @PreAuthorize("@authChecker.canEdit(#pageId, authentication.name)")
 * public String editPage(@PathVariable String pageId) {
 *     PageData data = pageService.load(pageId);
 *
 *     EditablePage editablePage = EditablePage.create(pageId);
 *
 *     for (RowData rowData : data.getRows()) {
 *         Row row = new Row();
 *         EditableRow editableRow = EditableRow.wrap(row, rowData.getId(), pageId);
 *
 *         for (ModuleData moduleData : rowData.getModules()) {
 *             Module module = moduleFactory.create(moduleData);
 *             editableRow.addEditableModule(module, moduleData.getId());
 *         }
 *
 *         editablePage.addEditableRow(editableRow);
 *     }
 *
 *     return renderWithShell(editablePage);
 * }
 * }</pre>
 *
 * <h2>Insert Row Flow</h2>
 * <ol>
 *   <li>User clicks "+ Insert Row Below"</li>
 *   <li>HTMX POST to `/api/pages/{pageId}/rows/insert`</li>
 *   <li>Server creates new empty row, saves to database</li>
 *   <li>Server returns new EditableRow HTML</li>
 *   <li>HTMX inserts row before the clicked button's section</li>
 * </ol>
 *
 * <h2>Page Structure</h2>
 * <pre>
 * [EditablePage]
 *   [EditableRow 1]
 *     [Module 1] [Edit] [Delete]
 *     [Module 2] [Edit] [Delete]
 *     [+ Add Module]
 *   [+ Insert Row Below]
 *
 *   [EditableRow 2]
 *     [Module 3] [Edit] [Delete]
 *     [+ Add Module]
 *   [+ Insert Row Below]
 * </pre>
 *
 * @see EditableRow
 * @see EditableModule
 */
public class EditablePage extends HtmlTag {

    private final String pageId;
    private final List<EditableRow> rows = new ArrayList<>();
    private final Div pageContainer;

    /**
     * Private constructor - use {@link #create(String)} instead.
     *
     * @param pageId the unique identifier for this page
     */
    private EditablePage(String pageId) {
        super("div");
        this.pageId = pageId;
        this.pageContainer = new Div().withClass("editable-page");
        this.withClass("editable-page-wrapper");
        this.withAttribute("id", "page-" + pageId);
    }

    /**
     * Creates a new EditablePage.
     *
     * @param pageId unique identifier for the page
     * @return new EditablePage instance
     */
    public static EditablePage create(String pageId) {
        return new EditablePage(pageId);
    }

    /**
     * Adds an editable row to the page.
     * <p>
     * An "Insert Row Below" button is automatically added after each row.
     * </p>
     *
     * @param row the EditableRow to add
     * @return this EditablePage for method chaining
     */
    public EditablePage addEditableRow(EditableRow row) {
        rows.add(row);
        return this;
    }

    /**
     * Renders the complete editable page with all rows and insert controls.
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

    /**
     * Gets the page ID.
     *
     * @return the page identifier
     */
    public String getPageId() {
        return pageId;
    }

    /**
     * Gets the number of rows in this page.
     *
     * @return row count
     */
    public int getRowCount() {
        return rows.size();
    }
}
