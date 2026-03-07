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
 * Editable row wrapper that manages module wrappers and add-module controls.
 *
 * <p>Lifecycle: module list is mutable via configuration methods; row/column structure is rebuilt
 * on each render call with equal-width columns.</p>
 *
 * <p>Mutability and thread-safety: mutable and not thread-safe. Intended for request-scoped page
 * builder usage.</p>
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

    /** Internal immutable module registration entry. */
    private static class ModuleInfo {
        final Module module;
        final String moduleId;

        ModuleInfo(Module module, String moduleId) {
            this.module = module;
            this.moduleId = moduleId;
        }
    }

    /** Internal constructor; use {@link #wrap(Row, String, String)}. */
    private EditableRow(Row row, String rowId, String pageId) {
        super("div");
        this.wrappedRow = row;
        this.rowId = rowId;
        this.pageId = pageId;
        this.withAttribute("id", "row-" + rowId);
        this.withClass("editable-row-wrapper");
    }

    /** Creates an editable wrapper around a row id scoped to a page id. */
    public static EditableRow wrap(Row row, String rowId, String pageId) {
        return new EditableRow(row, rowId, pageId);
    }

    /**
     * Sets max modules allowed in the row.
     *
     * @throws IllegalArgumentException when {@code max < 1}
     */
    public EditableRow withMaxModules(int max) {
        if (max < 1) {
            throw new IllegalArgumentException("Max modules must be at least 1");
        }
        this.maxModulesPerRow = max;
        return this;
    }

    /** Sets edit mode applied to generated {@link EditableModule} wrappers. */
    public EditableRow withEditMode(EditMode mode) {
        this.editMode = mode;
        return this;
    }

    /** Sets whether the add-module control is rendered. */
    public EditableRow withCanAddModule(boolean canAddModule) {
        this.canAddModule = canAddModule;
        return this;
    }

    /**
     * Adds a module registration that will be wrapped at render time.
     *
     * @throws IllegalStateException when module limit is reached
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

    /** Rebuilds and renders the row with editable wrappers and optional add control. */
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
