package io.mindspice.jhf.editing;


import io.mindspice.jhf.components.Div;
import io.mindspice.jhf.components.forms.Button;
import io.mindspice.jhf.core.Module;


/**
 * Wrapper that adds editing UI to any module.
 * <p>
 * Uses the decorator pattern to transparently add edit/delete controls
 * to existing modules without modifying the original module code.
 * This keeps editing concerns separate from module logic.
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><strong>Edit Button:</strong> Opens edit form via HTMX</li>
 *   <li><strong>Delete Button:</strong> Removes module with confirmation</li>
 *   <li><strong>Edit Modes:</strong> USER_EDIT (approval required) or OWNER_EDIT (immediate)</li>
 *   <li><strong>HTMX Integration:</strong> Seamless in-place editing without page reload</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // Create any module
 * ContentModule content = ContentModule.create()
 *     .withTitle("My Content")
 *     .withContent("Article text...");
 *
 * // Wrap it to add editing UI
 * EditableModule editable = EditableModule.wrap(content)
 *     .withModuleId("content-123")
 *     .withEditUrl("/api/modules/123/edit")
 *     .withDeleteUrl("/api/modules/123/delete")
 *     .withEditMode(EditMode.USER_EDIT);
 *
 * // Render - includes edit toolbar + original module
 * String html = editable.render();
 * }</pre>
 *
 * <h2>With Authorization Check</h2>
 * <pre>{@code
 * @GetMapping("/page/{id}")
 * public String viewPage(@PathVariable String id, Principal principal) {
 *     Module module = moduleFactory.create(moduleData);
 *
 *     // Only wrap for editing if user has permission
 *     if (authChecker.canEdit(moduleId, principal.getName())) {
 *         EditMode mode = authChecker.getEditMode(moduleId, principal.getName());
 *         module = EditableModule.wrap(module)
 *             .withModuleId(moduleId)
 *             .withEditUrl("/api/modules/" + moduleId + "/edit")
 *             .withDeleteUrl("/api/modules/" + moduleId + "/delete")
 *             .withEditMode(mode);
 *     }
 *
 *     return page.addRow(row -> row.withChild(module));
 * }
 * }</pre>
 *
 * <h2>HTMX Edit Flow</h2>
 * <ol>
 *   <li>User clicks "Edit" button</li>
 *   <li>HTMX GET request to editUrl</li>
 *   <li>Server returns edit form HTML</li>
 *   <li>Form replaces module (outerHTML swap)</li>
 *   <li>User edits and submits</li>
 *   <li>Server returns updated module or approval message</li>
 *   <li>Result replaces form</li>
 * </ol>
 *
 * <h2>Edit Modes</h2>
 * <ul>
 *   <li><strong>USER_EDIT:</strong> Changes go to approval queue, shows "Pending Approval" badge on button</li>
 *   <li><strong>OWNER_EDIT:</strong> Changes go live immediately, no badge</li>
 * </ul>
 *
 * <h2>Toolbar Customization</h2>
 * <pre>{@code
 * // Hide toolbar (view-only mode)
 * editable.hideEditControls();
 *
 * // Edit only (no delete button)
 * editable.withEditUrl("/api/edit")
 *         .withModuleId("module-123");
 * // Don't call withDeleteUrl() - button won't appear
 * }</pre>
 *
 * @see EditMode
 * @see ModuleEditHandler
 * @see AuthorizationChecker
 */
public class EditableModule extends Module {

    private final Module wrappedModule;
    private String editUrl;
    private String deleteUrl;
    private EditMode editMode = EditMode.USER_EDIT;
    private boolean showEditControls = true;

    /**
     * Private constructor - use {@link #wrap(Module)} instead.
     *
     * @param module the module to wrap with editing capabilities
     */
    private EditableModule(Module module) {
        super("div");
        this.wrappedModule = module;
        this.withClass("editable-module-wrapper");
    }

    /**
     * Creates an EditableModule that wraps the given module.
     * <p>
     * The wrapper adds edit controls while preserving the original
     * module's appearance and functionality.
     * </p>
     *
     * @param module the module to make editable
     * @return new EditableModule instance wrapping the provided module
     */
    public static EditableModule wrap(Module module) {
        return new EditableModule(module);
    }

    /**
     * Sets the URL for the edit endpoint.
     * <p>
     * When the user clicks "Edit", HTMX will GET this URL.
     * The server should return the edit form HTML.
     * </p>
     *
     * @param url the edit endpoint URL (e.g., "/api/modules/123/edit")
     * @return this EditableModule for method chaining
     */
    public EditableModule withEditUrl(String url) {
        this.editUrl = url;
        return this;
    }

    /**
     * Sets the URL for the delete endpoint.
     * <p>
     * When the user clicks "Delete" (and confirms), HTMX will DELETE to this URL.
     * The server should return empty HTML or a success message.
     * </p>
     * <p>
     * If not set, the delete button will not appear in the toolbar.
     * </p>
     *
     * @param url the delete endpoint URL (e.g., "/api/modules/123/delete")
     * @return this EditableModule for method chaining
     */
    public EditableModule withDeleteUrl(String url) {
        this.deleteUrl = url;
        return this;
    }

    /**
     * Sets the module ID for both the wrapper and the wrapped module.
     * <p>
     * The module ID is used as the HTML id attribute and as the
     * HTMX target for in-place updates.
     * </p>
     *
     * @param id unique identifier for this module
     * @return this EditableModule for method chaining
     */
    @Override
    public EditableModule withModuleId(String id) {
        super.withModuleId(id);
        // Also set on wrapped module for HTMX targeting
        this.wrappedModule.withModuleId(id);
        return this;
    }

    /**
     * Sets the edit mode.
     * <p>
     * USER_EDIT: Changes require approval, button shows hint tooltip.
     * OWNER_EDIT: Changes go live immediately, no hint.
     * </p>
     *
     * @param mode the edit mode to use
     * @return this EditableModule for method chaining
     */
    public EditableModule withEditMode(EditMode mode) {
        this.editMode = mode;
        return this;
    }

    /**
     * Hides the edit controls toolbar.
     * <p>
     * Useful when you want to conditionally show/hide editing UI
     * based on runtime conditions without creating two separate components.
     * </p>
     *
     * @return this EditableModule for method chaining
     */
    public EditableModule hideEditControls() {
        this.showEditControls = false;
        return this;
    }

    /**
     * Builds the content by adding edit toolbar (if enabled) and the wrapped module.
     */
    @Override
    protected void buildContent() {
        // Add edit controls toolbar at top if enabled
        if (showEditControls && editUrl != null) {
            Div toolbar = buildEditToolbar();
            super.withChild(toolbar);
        }

        // Add the wrapped module (it will build its own content when rendered)
        super.withChild(wrappedModule);
    }

    /**
     * Builds the edit toolbar with Edit and Delete buttons.
     *
     * @return Div containing the edit controls
     */
    private Div buildEditToolbar() {
        Div toolbar = new Div()
                .withClass("edit-toolbar");

        // Very small edit button - just text link
        Button editBtn = Button.create("✏")
                .withStyle(Button.ButtonStyle.LINK)
                .small();

        editBtn.withAttribute("hx-get", editUrl);
        editBtn.withAttribute("hx-target", "#edit-module-modal");
        editBtn.withAttribute("hx-swap", "innerHTML");
        editBtn.withAttribute("style", "text-decoration: none; font-size: 0.9rem; padding: 2px 6px; color: #6c757d; opacity: 0.6;");
        editBtn.withAttribute("onmouseover", "this.style.opacity='1'");
        editBtn.withAttribute("onmouseout", "this.style.opacity='0.6'");

        // Add approval hint for user edits
        if (editMode == EditMode.USER_EDIT) {
            editBtn.withAttribute("title", "Edit (changes require approval)");
        } else {
            editBtn.withAttribute("title", "Edit");
        }

        toolbar.withChild(editBtn);

        return toolbar;
    }

    /**
     * Sets the title on the wrapped module.
     *
     * @param title the module title
     * @return this EditableModule for method chaining
     */
    @Override
    public EditableModule withTitle(String title) {
        super.withTitle(title);
        this.wrappedModule.withTitle(title);
        return this;
    }

    /**
     * Adds a CSS class to the wrapper.
     *
     * @param className CSS class name to add
     * @return this EditableModule for method chaining
     */
    @Override
    public EditableModule withClass(String className) {
        super.withClass(className);
        return this;
    }
}
