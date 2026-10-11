package io.mindspice.simplypages.editing;


import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.Module;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.modules.EditableModule;

import java.util.ArrayList;
import java.util.List;


/**
 * Composes a supplied row with a separate row of registered editable modules and add-module controls.
 *
 * <p>The supplied row retains its own attributes, content and renderer. Registrations are independent
 * content, not edits to that row; callers must avoid registering content already present there.</p>
 *
 * <p>Lifecycle: module list is mutable via configuration methods; the registered-module row/column
 * structure is rebuilt on each render call with equal-width columns (fill columns above twelve).</p>
 *
 * <p>Mutability and thread-safety: mutable and not thread-safe. Intended for request-scoped page
 * builder usage.</p>
 */
public class EditableRow extends HtmlTag {

    private final Row wrappedRow;
    private final String rowId;
    private final String pageId;
    private String moduleEditUrlTemplate = "/api/pages/{pageId}/modules/{moduleId}/edit";
    private String moduleDeleteUrlTemplate = "/api/pages/{pageId}/modules/{moduleId}/delete";
    private String addModuleFormUrlTemplate = "/api/pages/{pageId}/rows/{rowId}/add-module-form";
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

    /**
     * Creates a wrapper containing the supplied row, then a separate registered-module row and an
     * optional add-module control.
     *
     * <p>A non-null {@code row} is rendered once per wrapper render with the same render context,
     * including custom {@link Row#render(RenderContext)} overrides and rendering through a template.
     * This wrapper does not copy or mutate the supplied row's structure. Its id, classes and other
     * attributes remain on the supplied row, distinct from the outer wrapper's attributes.</p>
     *
     * <p>Modules registered with {@link #addEditableModule(Module, String)} belong only to the separate
     * generated row. No merging or deduplication occurs; callers must not register content already
     * present in the supplied row. Registration still assigns the supplied module's module id.</p>
     *
     * @param row independent source row; an empty row is retained, while {@code null} omits it
     * @param rowId identifier used for the outer wrapper id and add-module endpoint
     * @param pageId identifier used for generated editing endpoints
     * @return a new mutable, non-thread-safe wrapper
     */
    public static EditableRow wrap(Row row, String rowId, String pageId) {
        return new EditableRow(row, rowId, pageId);
    }

    /**
     * Sets max registrations allowed in the generated row; source-row children do not count.
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

    /**
     * Sets the module-edit URL template (default {@code /api/pages/{pageId}/modules/{moduleId}/edit}).
     * Supports {@code {pageId}}, {@code {rowId}} and {@code {moduleId}} only as whole path segments
     * or whole query values; repeated/omitted tokens and fixed URLs are allowed.
     * In browser-relevant structure, token-bearing absolute URLs require
     * {@code scheme://nonempty-literal-authority};
     * protocol-relative, root-relative, relative-path and query-only references are supported.
     * Placement is also checked after browser-style control/space and path-backslash handling,
     * without rewriting the output. Scheme shorthands, empty authorities and opaque templated
     * URLs are unsupported; fixed URLs do not acquire structural validation.
     *
     * <p>Templates are validated here; raw ids are validated and UTF-8 percent-encoded only when
     * interpolated during render. Used ids must be non-null, nonempty, nonblank, not exactly
     * {@code .} or {@code ..}, and valid UTF-16. No trimming, normalization or decoding occurs;
     * literal percent signs are encoded. Unused ids and DOM ids are unaffected. Literal URL text
     * is preserved. Applications own routing, server decoding, authorization and CSRF.</p>
     *
     * @param template caller-owned module-edit URL template
     * @return this row
     * @throws IllegalArgumentException for null templates, unsupported tokens or token placements
     */
    public EditableRow withModuleEditUrlTemplate(String template) {
        this.moduleEditUrlTemplate = EditingUrlTemplate.validate(template, "pageId", "rowId", "moduleId");
        return this;
    }

    /**
     * Sets the module-delete URL template (default
     * {@code /api/pages/{pageId}/modules/{moduleId}/delete}).
     * Supports {@code {pageId}}, {@code {rowId}} and {@code {moduleId}} with the placement,
     * encoding and render-time id validation described in {@link #withModuleEditUrlTemplate(String)}.
     * Configuring this operation does not change the edit or add-form template.
     *
     * @param template caller-owned module-delete URL template
     * @return this row
     * @throws IllegalArgumentException for null templates, unsupported tokens or token placements
     */
    public EditableRow withModuleDeleteUrlTemplate(String template) {
        this.moduleDeleteUrlTemplate = EditingUrlTemplate.validate(template, "pageId", "rowId", "moduleId");
        return this;
    }

    /**
     * Sets the add-module-form URL template (default
     * {@code /api/pages/{pageId}/rows/{rowId}/add-module-form}).
     * Supports only {@code {pageId}} and {@code {rowId}}, using the placement, encoding and
     * render-time id validation described in {@link #withModuleEditUrlTemplate(String)}.
     * This template is expanded only when the add control is eligible to render.
     *
     * @param template caller-owned add-form URL template
     * @return this row
     * @throws IllegalArgumentException for null templates, unsupported tokens or token placements
     */
    public EditableRow withAddModuleFormUrlTemplate(String template) {
        this.addModuleFormUrlTemplate = EditingUrlTemplate.validate(template, "pageId", "rowId");
        return this;
    }

    private String editingUrl(String template, String moduleId) {
        return EditingUrlTemplate.expand(template, token -> switch (token) {
            case "pageId" -> pageId;
            case "rowId" -> rowId;
            default -> moduleId;
        });
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
     * Adds independent content to the generated row, wrapped at render time, and assigns its module id.
     * This does not make existing source-row content editable or deduplicate it.
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

    /** Renders the supplied row, then rebuilt editable-module columns and an optional add control. */
    @Override
    public String render(RenderContext context) {
        children.clear();

        if (wrappedRow != null) {
            super.withChild(wrappedRow);
        }

        // Build a fresh row with all modules at proper column widths
        Row row = new Row();

        if (!modules.isEmpty()) {
            // Calculate equal column width for all modules
            int colWidth = 12 / modules.size();

            for (ModuleInfo info : modules) {
                // Wrap module in EditableModule
                EditableModule editableModule = EditableModule.wrap(info.module)
                        .withEditUrl(editingUrl(moduleEditUrlTemplate, info.moduleId))
                        .withDeleteUrl(editingUrl(moduleDeleteUrlTemplate, info.moduleId))
                        .withEditMode(editMode);

                // Create column with calculated width
                Column col = colWidth > 0 ? Column.create().withWidth(colWidth) : Column.create().fill();
                col.withChild(editableModule);

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

            addBtn.withAttribute("hx-get", editingUrl(addModuleFormUrlTemplate, null));
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
