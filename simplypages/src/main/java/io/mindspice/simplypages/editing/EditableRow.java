package io.mindspice.simplypages.editing;


import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.Module;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.modules.EditableModule;

import java.util.ArrayList;
import java.util.List;


/**
 * Row wrapper that adds page-building UI for managing modules within a row.
 * <p>
 * EditableRow allows users to add, edit, and remove modules in a row,
 * with automatic layout calculation and a maximum module limit to maintain
 * layout integrity.
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><strong>Module Management:</strong> Add/edit/delete modules within the row</li>
 *   <li><strong>Auto Column Sizing:</strong> Automatically calculates equal column widths</li>
 *   <li><strong>Module Limit:</strong> Enforces max modules per row (default 3)</li>
 *   <li><strong>Add Module UI:</strong> Shows "Add Module" button when space available</li>
 *   <li><strong>HTMX Integration:</strong> Modal-based module addition workflow</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // Create empty row
 * Row row = new Row();
 * EditableRow editableRow = EditableRow.wrap(row, "row-1", "page-123");
 *
 * // Add modules (automatically wrapped in EditableModule)
 * editableRow.addEditableModule(
 *     ContentModule.create().withTitle("Module 1"),
 *     "module-1"
 * );
 *
 * editableRow.addEditableModule(
 *     GalleryModule.create().withTitle("Gallery"),
 *     "module-2"
 * );
 *
 * // Render - includes row + modules + "Add Module" button
 * String html = editableRow.render();
 * }</pre>
 *
 * <h2>Usage in Page Builder</h2>
 * <pre>{@code
 * @GetMapping("/vendor/page/{pageId}/edit")
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
 * <h2>Add Module Flow</h2>
 * <ol>
 *   <li>User clicks "+ Add Module" button</li>
 *   <li>HTMX GET to `/api/pages/{pageId}/rows/{rowId}/add-module-form`</li>
 *   <li>Server returns modal with module type selector</li>
 *   <li>User selects type, fills form</li>
 *   <li>Submit POST to `/api/pages/{pageId}/rows/{rowId}/add-module`</li>
 *   <li>Server returns updated EditableRow HTML</li>
 *   <li>HTMX replaces entire row</li>
 * </ol>
 *
 * <h2>Customization</h2>
 * <pre>{@code
 * // Custom max modules
 * editableRow.withMaxModules(4);  // Allow up to 4 modules
 *
 * // Custom edit mode
 * editableRow.withEditMode(EditMode.OWNER_EDIT);
 * }</pre>
 *
 * @see EditableModule
 * @see EditablePage
 */
public class EditableRow extends HtmlTag {

    private final Row wrappedRow;
    private final String rowId;
    private final String pageId;
    private int maxModulesPerRow = 3;
    private final List<ModuleInfo> modules = new ArrayList<>();
    private EditMode editMode = EditMode.OWNER_EDIT;  // Default for page builder

    // Permission flags (Phase 6.5)
    private boolean canAddModule = true;

    /**
     * Internal class to track module information.
     */
    private static class ModuleInfo {
        final Module module;
        final String moduleId;

        ModuleInfo(Module module, String moduleId) {
            this.module = module;
            this.moduleId = moduleId;
        }
    }

    /**
     * Private constructor - use {@link #wrap(Row, String, String)} instead.
     *
     * @param row the row to wrap
     * @param rowId unique identifier for this row
     * @param pageId the parent page identifier
     */
    private EditableRow(Row row, String rowId, String pageId) {
        super("div");
        this.wrappedRow = row;
        this.rowId = rowId;
        this.pageId = pageId;
        this.withAttribute("id", "row-" + rowId);
        this.withClass("editable-row-wrapper");
    }

    /**
     * Creates an EditableRow that wraps the given row.
     *
     * @param row the row to make editable
     * @param rowId unique identifier for this row
     * @param pageId the parent page identifier
     * @return new EditableRow instance
     */
    public static EditableRow wrap(Row row, String rowId, String pageId) {
        return new EditableRow(row, rowId, pageId);
    }

    /**
     * Sets the maximum number of modules allowed in this row.
     *
     * @param max maximum modules (default 3)
     * @return this EditableRow for method chaining
     */
    public EditableRow withMaxModules(int max) {
        if (max < 1) {
            throw new IllegalArgumentException("Max modules must be at least 1");
        }
        this.maxModulesPerRow = max;
        return this;
    }

    /**
     * Sets the edit mode for all modules in this row.
     *
     * @param mode the edit mode to use
     * @return this EditableRow for method chaining
     */
    public EditableRow withEditMode(EditMode mode) {
        this.editMode = mode;
        return this;
    }

    /**
     * Sets whether modules can be added to this row (Phase 6.5).
     * If false, the "Add Module" button will not be displayed.
     *
     * @param canAddModule true to allow adding modules, false to lock row
     * @return this EditableRow for method chaining
     */
    public EditableRow withCanAddModule(boolean canAddModule) {
        this.canAddModule = canAddModule;
        return this;
    }

    /**
     * Adds an editable module to this row.
     * <p>
     * The module is tracked and will be wrapped in EditableModule at render time
     * with appropriate edit/delete URLs. Column width is calculated during render
     * to ensure all modules share the 12-column grid evenly.
     * </p>
     * <p>
     * <strong>Note:</strong> For simplicity, this implementation uses
     * fixed equal-width columns. If you need more control over individual
     * column widths, build the row manually with Column components.
     * </p>
     *
     * @param module the module to add
     * @param moduleId unique identifier for this module
     * @return this EditableRow for method chaining
     * @throws IllegalStateException if max modules limit reached
     */
    public EditableRow addEditableModule(Module module, String moduleId) {
        if (modules.size() >= maxModulesPerRow) {
            throw new IllegalStateException("Maximum modules per row (" + maxModulesPerRow + ") reached");
        }

        // Track the module (columns built at render time for correct width calculation)
        modules.add(new ModuleInfo(module, moduleId));

        // Set module ID on the wrapped module
        module.withModuleId(moduleId);

        return this;
    }

    /**
     * Renders the editable row with modules and add module UI.
     */
    @Override
    public String render(RenderContext context) {
        children.clear();

        // Build a fresh row with all modules at proper column widths
        Row row = new Row();

        if (!modules.isEmpty()) {
            // Calculate equal column width for all modules
            int colWidth = 12 / modules.size();

            for (ModuleInfo info : modules) {
                // Wrap module in EditableModule
                EditableModule editableModule = EditableModule.wrap(info.module)
                        .withEditUrl("/api/pages/" + pageId + "/modules/" + info.moduleId + "/edit")
                        .withDeleteUrl("/api/pages/" + pageId + "/modules/" + info.moduleId + "/delete")
                        .withEditMode(editMode);

                // Create column with calculated width
                Column col = Column.create()
                        .withWidth(colWidth)
                        .withChild(editableModule);

                row.addColumn(col);
            }
        }

        super.withChild(row);

        // Add "Add Module" button if space available AND adding is permitted
        if (canAddModule && modules.size() < maxModulesPerRow) {
            Div addModuleSection = new Div()
                    .withClass("add-module-section");

            Button addBtn = Button.create("+ Add Module")
                    .withStyle(Button.ButtonStyle.SECONDARY);

            addBtn.withAttribute("hx-get", "/api/pages/" + pageId + "/rows/" + rowId + "/add-module-form");
            addBtn.withAttribute("hx-target", "#edit-modal-container");
            addBtn.withAttribute("hx-swap", "innerHTML");

            addModuleSection.withChild(addBtn);
            super.withChild(addModuleSection);
        }

        return super.render(context);
    }

    @Override
    public String render() {
        return render(RenderContext.empty());
    }
}
