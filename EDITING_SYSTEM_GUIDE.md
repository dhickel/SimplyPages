# SimplyPages Editing System - Complete Implementation Guide

**Comprehensive guide to implementing inline editing in your SimplyPages application**

---

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Core Concepts](#core-concepts)
4. [Step-by-Step Implementation](#step-by-step-implementation)
5. [Authorization & Permissions](#authorization--permissions)
6. [HTMX Integration Patterns](#htmx-integration-patterns)
7. [Complete Working Example](#complete-working-example)
8. [Best Practices](#best-practices)
9. [Troubleshooting](#troubleshooting)

---

## Overview

The SimplyPages editing system provides **production-ready inline editing** for web applications with:

- ✅ **Modal-based editing** - Professional overlay modals with backdrop
- ✅ **Auto-save** - Changes save immediately (no manual save/load buttons)
- ✅ **Permission system** - Fine-grained control over edit/delete permissions
- ✅ **Authorization wrapper** - Optional auth checks for protected operations
- ✅ **HTMX integration** - Seamless dynamic updates without page reloads
- ✅ **Framework-level styling** - Consistent edit buttons with proper z-index
- ✅ **Module constraints** - Rows require modules, auto-delete empty rows

### Architecture

```
User clicks Edit Button
         ↓
HTMX GET request to /edit/{id}
         ↓
Controller returns Modal HTML
         ↓
Modal displays in overlay
         ↓
User edits and clicks Save
         ↓
HTMX POST request to /save/{id}
         ↓
Controller validates & saves
         ↓
Returns OOB swaps (clear modal + refresh page)
         ↓
Page updates without reload
```

---

## Quick Start

### Minimal Working Example

```java
@Controller
@RequestMapping("/my-page")
public class MyEditableController {

    private Map<String, String> content = new ConcurrentHashMap<>();

    @GetMapping
    @ResponseBody
    public String showPage() {
        // 1. Modal container (required!)
        Div modalContainer = new Div().withAttribute("id", "edit-modal-container");

        // 2. Create editable module
        ContentModule module = ContentModule.create()
            .withModuleId("module-1")
            .withTitle("Welcome")
            .withContent(content.getOrDefault("module-1", "Click edit to update!"));

        EditableModule editable = EditableModule.wrap(module)
            .withEditUrl("/my-page/edit/module-1")
            .withDeleteUrl("/my-page/delete/module-1");

        // 3. Build page
        Container page = Container.create()
            .withChild(editable);

        // 4. Return HTML with modal container
        return buildHtml("My Page",
            "<div id=\"page-content\">" + page.render() + "</div>",
            modalContainer.render());
    }

    @GetMapping("/edit/{id}")
    @ResponseBody
    public String editModule(@PathVariable String id) {
        ContentModule module = ContentModule.create()
            .withModuleId(id)
            .withTitle("Welcome")
            .withContent(content.getOrDefault(id, ""));

        EditAdapter<ContentModule> adapter = module;

        return EditModalBuilder.create()
            .withTitle("Edit Module")
            .withEditView(adapter.buildEditView())
            .withSaveUrl("/my-page/save/" + id)
            .withDeleteUrl("/my-page/delete/" + id)
            .build()
            .render();
    }

    @PostMapping("/save/{id}")
    @ResponseBody
    public String saveModule(@PathVariable String id, @RequestParam Map<String, String> formData) {
        content.put(id, formData.get("content"));

        // OOB swap pattern: clear modal + refresh page
        String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
        String updatePage = /* render page content with OOB */;

        return clearModal + updatePage;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public String deleteModule(@PathVariable String id) {
        content.remove(id);
        return /* render updated page */;
    }

    private String buildHtml(String title, String body, String modalHtml) {
        return "<!DOCTYPE html>\n<html>\n<head>\n" +
               "  <title>" + title + "</title>\n" +
               "  <link rel=\"stylesheet\" href=\"/css/framework.css\">\n" +
               "  <script src=\"/webjars/htmx.org/dist/htmx.min.js\" defer></script>\n" +
               "</head>\n<body>\n" +
               body + "\n" + modalHtml + "\n" +
               "</body>\n</html>";
    }
}
```

---

## Core Concepts

### 1. EditableModule - The Wrapper

`EditableModule` wraps any module and adds edit/delete buttons with proper styling.

**Key Features:**
- Framework-level CSS styling (no manual CSS required)
- Z-index management (buttons always visible)
- HTMX integration (automatic dynamic loading)
- Permission support (show/hide buttons conditionally)

**Usage:**
```java
EditableModule editableModule = EditableModule.wrap(contentModule)
    .withEditUrl("/edit/module-123")          // GET endpoint for edit modal
    .withDeleteUrl("/delete/module-123")      // DELETE endpoint
    .withDeleteTarget("#page-content")        // HTMX target for delete
    .withDeleteConfirm("Delete this?")        // Confirmation dialog
    .withCanEdit(true)                        // Permission flags
    .withCanDelete(true);
```

### 2. Modal - Overlay Component

`Modal` creates professional overlay modals with backdrop, animations, and ESC key support.

**Usage:**
```java
Modal modal = Modal.create()
    .withTitle("Edit Content")
    .withBody(formComponents)
    .withFooter(buttons)
    .render();
```

**Critical:** Page MUST have exactly ONE modal container:
```html
<div id="edit-modal-container"></div>
```

### 3. EditModalBuilder - Standardized Edit Modals

`EditModalBuilder` creates consistent edit modals with standard layout and OOB swap support.

**Features:**
- Standard footer layout (delete left, cancel/save right)
- HTMX integration with OOB swaps
- Automatic form field inclusion
- Configurable save/delete URLs

**Usage:**
```java
EditModalBuilder.create()
    .withTitle("Edit Module")
    .withModuleId(moduleId)
    .withEditView(formComponents)
    .withSaveUrl("/save/" + moduleId)
    .withDeleteUrl("/delete/" + moduleId)
    .withPageContainerId("page-content")      // For OOB refresh
    .withModalContainerId("edit-modal-container")
    .build()
    .render();
```

### 4. EditAdapter Interface - Module Edit Contract

Modules implement `EditAdapter<T>` to support inline editing.

**Interface Methods:**
```java
public interface EditAdapter<T extends Module> {
    Component buildEditView();                    // Build edit form UI
    T applyEdits(Map<String, String> formData);  // Apply changes to module
    default ValidationResult validate(Map<String, String> formData) { return ValidationResult.valid(); }
}
```

**Modules with EditAdapter:**
- ✅ `ContentModule` - Title, content, markdown toggle

**To implement for custom modules:**
```java
public class MyModule extends Module implements EditAdapter<MyModule> {
    private String customField;

    @Override
    public Component buildEditView() {
        Div form = new Div();
        form.withChild(new Div().withClass("form-field")
            .withChild(new Paragraph("Custom Field:").withClass("form-label"))
            .withChild(TextInput.create("customField").withValue(customField)));
        return form;
    }

    @Override
    public ValidationResult validate(Map<String, String> formData) {
        if (formData.get("customField").isEmpty()) {
            return ValidationResult.invalid("Custom field is required");
        }
        return ValidationResult.valid();
    }

    @Override
    public MyModule applyEdits(Map<String, String> formData) {
        this.customField = formData.get("customField");
        // IMPORTANT: If module is build-once, rebuild content
        children.clear();
        buildContent();
        return this;
    }
}
```

### 5. AuthWrapper - Authorization Pattern

`AuthWrapper` provides optional authorization checks for edit operations.

**Usage:**
```java
@GetMapping("/edit/{id}")
@ResponseBody
public String editModule(@PathVariable String id, Principal principal) {
    return AuthWrapper.requireForEdit(
        () -> canUserEdit(id, principal.getName()),  // Auth check
        () -> {
            // Authorized action - return edit modal
            return EditModalBuilder.create()...render();
        },
        "You don't have permission"  // Custom error message (optional)
    );
}
```

**Available Methods:**
- `require(authCheck, action, unauthorizedHandler)` - Generic wrapper
- `requireForEdit(authCheck, action)` - Edit operations
- `requireForEdit(authCheck, action, message)` - With custom message
- `requireForDelete(authCheck, action)` - Delete operations
- `requireForCreate(authCheck, action)` - Create operations

---

## Step-by-Step Implementation

### Step 1: Set Up Data Storage

Define your data structures for modules, rows, and pages:

```java
@Controller
@RequestMapping("/my-editable-page")
public class EditablePageController {

    // Data classes
    private static class ModuleData {
        String id;
        String title;
        String content;
        int width;  // Grid width (1-12)
        boolean canEdit = true;
        boolean canDelete = true;

        ModuleData(String id, String title, String content, int width) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.width = width;
        }
    }

    private static class RowData {
        String id;
        int position;
        boolean canAddModule = true;
        List<ModuleData> modules = new ArrayList<>();

        RowData(String id, int position) {
            this.id = id;
            this.position = position;
        }
    }

    private static class PageData {
        String pageId;
        List<RowData> rows = new ArrayList<>();

        PageData(String pageId) {
            this.pageId = pageId;
        }
    }

    // Storage (use database in production)
    private final Map<String, PageData> pages = new ConcurrentHashMap<>();
    private int idCounter = 100;

    public EditablePageController() {
        initializeTestData();
    }

    private void initializeTestData() {
        PageData page = new PageData("my-page");

        RowData row1 = new RowData("row-1", 0);
        row1.modules.add(new ModuleData("module-1", "Welcome",
            "# Welcome to My Site\n\nEdit me!", 12));
        page.rows.add(row1);

        pages.put("my-page", page);
    }
}
```

### Step 2: Create Main Page Endpoint

**Critical Requirements:**
1. Must have `<div id="edit-modal-container"></div>`
2. Page content should be wrapped in identifiable container (e.g., `id="page-content"`)
3. Include HTMX script
4. Include framework.css

```java
@GetMapping
@ResponseBody
public String showPage() {
    Div modalContainer = new Div().withAttribute("id", "edit-modal-container");

    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
    html.append("  <meta charset=\"UTF-8\">\n");
    html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
    html.append("  <title>My Editable Page</title>\n");
    html.append("  <link rel=\"stylesheet\" href=\"/css/framework.css\">\n");
    html.append("  <script src=\"/webjars/htmx.org/dist/htmx.min.js\" defer></script>\n");
    html.append("</head>\n<body>\n");
    html.append(renderPageContent());
    html.append(modalContainer.render());
    html.append("</body>\n</html>\n");

    return html.toString();
}
```

### Step 3: Implement Page Rendering

Render rows and modules with `EditableModule` wrapper:

```java
private String renderPageContent() {
    PageData page = pages.get("my-page");
    Container content = Container.create();

    content.withChild(Header.H1("My Editable Page"));

    // Render each row
    for (RowData row : page.rows) {
        Div rowWrapper = new Div().withClass("editable-row-wrapper");

        // Create Row with modules
        Row moduleRow = new Row();
        for (ModuleData module : row.modules) {
            // Create module
            ContentModule contentMod = ContentModule.create()
                .withModuleId(module.id)
                .withTitle(module.title)
                .withContent(module.content);

            // Wrap with EditableModule
            EditableModule editableModule = EditableModule.wrap(contentMod)
                .withEditUrl("/my-editable-page/edit/" + module.id)
                .withDeleteUrl("/my-editable-page/delete/" + module.id)
                .withDeleteTarget("#page-content")
                .withDeleteConfirm("Delete this module?")
                .withCanEdit(module.canEdit)
                .withCanDelete(module.canDelete);

            // Add to row with grid width
            Column col = Column.create().withWidth(module.width)
                .withChild(editableModule);
            moduleRow.addColumn(col);
        }

        rowWrapper.withChild(moduleRow);

        // Add "Add Module" button (if row permits)
        if (row.canAddModule && row.modules.size() < 3) {
            Div addModuleSection = new Div().withClass("add-module-section");
            Button addModuleBtn = Button.create("+ Add Module to Row")
                .withStyle(Button.ButtonStyle.SECONDARY);
            addModuleBtn.withAttribute("hx-get", "/my-editable-page/add-module-modal/" + row.id);
            addModuleBtn.withAttribute("hx-target", "#edit-modal-container");
            addModuleBtn.withAttribute("hx-swap", "innerHTML");
            addModuleSection.withChild(addModuleBtn);
            rowWrapper.withChild(addModuleSection);
        }

        content.withChild(rowWrapper);
    }

    return "<div id=\"page-content\">" + content.render() + "</div>";
}
```

### Step 4: Implement Edit Endpoint

**Pattern:** GET endpoint that returns Modal HTML via HTMX

```java
@GetMapping("/edit/{moduleId}")
@ResponseBody
public String editModule(@PathVariable String moduleId) {
    // Find module data
    ModuleData module = findModule(moduleId);
    if (module == null) {
        return Modal.create()
            .withTitle("Error")
            .withBody(Alert.danger("Module not found"))
            .render();
    }

    // Create module instance
    ContentModule contentMod = ContentModule.create()
        .withModuleId(moduleId)
        .withTitle(module.title)
        .withContent(module.content);

    // Get EditAdapter interface
    EditAdapter<ContentModule> adapter = contentMod;

    // Build combined form (module fields + layout fields)
    Div combinedForm = new Div();
    combinedForm.withChild(adapter.buildEditView());  // Title, content, markdown

    // Add width selector (layout concern, not module concern)
    Div widthGroup = new Div().withClass("form-field mt-4");
    widthGroup.withChild(new Paragraph("Module Width:").withClass("form-label"));
    widthGroup.withChild(Select.create("width")
        .addOption("3", "1/4 (3/12)", module.width == 3)
        .addOption("4", "1/3 (4/12)", module.width == 4)
        .addOption("6", "1/2 (6/12)", module.width == 6)
        .addOption("8", "2/3 (8/12)", module.width == 8)
        .addOption("12", "Full (12/12)", module.width == 12));
    combinedForm.withChild(widthGroup);

    // Return edit modal
    return EditModalBuilder.create()
        .withTitle("Edit Module")
        .withModuleId(moduleId)
        .withEditView(combinedForm)
        .withSaveUrl("/my-editable-page/save/" + moduleId)
        .withDeleteUrl("/my-editable-page/delete/" + moduleId)
        .withPageContainerId("page-content")
        .withModalContainerId("edit-modal-container")
        .build()
        .render();
}
```

### Step 5: Implement Save Endpoint

**Pattern:** POST endpoint that returns OOB swaps (clear modal + refresh page)

```java
@PostMapping("/save/{moduleId}")
@ResponseBody
public String saveModule(@PathVariable String moduleId,
                        @RequestParam Map<String, String> formData) {
    ModuleData module = findModule(moduleId);
    if (module == null) {
        return Modal.create()
            .withTitle("Error")
            .withBody(Alert.danger("Module not found"))
            .render();
    }

    // Update module data
    module.title = formData.getOrDefault("title", module.title);
    module.content = formData.getOrDefault("content", module.content);

    if (formData.containsKey("width")) {
        try {
            module.width = Integer.parseInt(formData.get("width"));
        } catch (NumberFormatException e) {
            // Keep existing
        }
    }

    // Return OOB swaps: clear modal + update page
    String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
    String updatePage = renderPageContent().replace("<div id=\"page-content\">",
            "<div hx-swap-oob=\"true\" id=\"page-content\">");

    return clearModal + updatePage;
}
```

### Step 6: Implement Delete Endpoint

**Pattern:** DELETE endpoint that removes module and refreshes page

```java
@DeleteMapping("/delete/{moduleId}")
@ResponseBody
public String deleteModule(@PathVariable String moduleId) {
    PageData page = pages.get("my-page");
    RowData containingRow = null;

    // Find and remove module
    for (RowData row : page.rows) {
        if (row.modules.removeIf(m -> m.id.equals(moduleId))) {
            containingRow = row;
            break;
        }
    }

    // Auto-delete empty rows
    if (containingRow != null && containingRow.modules.isEmpty()) {
        page.rows.remove(containingRow);

        // Renumber remaining rows
        for (int i = 0; i < page.rows.size(); i++) {
            page.rows.get(i).position = i;
        }
    }

    // Return updated page (EditableModule handles OOB swap)
    return renderPageContent();
}
```

### Step 7: Implement Add Module Modal

**Pattern:** GET endpoint that shows modal with form for new module

```java
@GetMapping("/add-module-modal/{rowId}")
@ResponseBody
public String showAddModuleModal(@PathVariable String rowId) {
    Div body = new Div();

    // Title field
    Div titleGroup = new Div().withClass("form-field");
    titleGroup.withChild(new Paragraph("Title:").withClass("form-label"));
    titleGroup.withChild(TextInput.create("title").withPlaceholder("Module title"));
    body.withChild(titleGroup);

    // Content field
    Div contentGroup = new Div().withClass("form-field");
    contentGroup.withChild(new Paragraph("Content (Markdown):").withClass("form-label"));
    contentGroup.withChild(TextArea.create("content")
        .withPlaceholder("# Heading\n\nYour content...")
        .withRows(8));
    body.withChild(contentGroup);

    // Width selector
    Div widthGroup = new Div().withClass("form-field");
    widthGroup.withChild(new Paragraph("Module Width:").withClass("form-label"));
    widthGroup.withChild(Select.create("width")
        .addOption("6", "1/2 (6/12)", true)
        .addOption("12", "Full (12/12)", false));
    body.withChild(widthGroup);

    // Footer buttons
    Div footer = new Div().withClass("d-flex justify-content-end gap-2");

    Button cancelBtn = Button.create("Cancel").withStyle(Button.ButtonStyle.SECONDARY);
    cancelBtn.withAttribute("onclick",
        "document.getElementById('edit-modal-container').innerHTML = ''");
    footer.withChild(cancelBtn);

    Button addBtn = Button.create("Add Module").withStyle(Button.ButtonStyle.PRIMARY);
    addBtn.withAttribute("hx-post", "/my-editable-page/add-module/" + rowId);
    addBtn.withAttribute("hx-swap", "none");  // OOB swaps only
    addBtn.withAttribute("hx-include", ".modal-body input, .modal-body textarea, .modal-body select");
    footer.withChild(addBtn);

    return Modal.create()
        .withTitle("Add Module")
        .withBody(body)
        .withFooter(footer)
        .render();
}
```

### Step 8: Implement Add Module Endpoint

**Pattern:** POST endpoint that adds module and returns OOB swaps

```java
@PostMapping("/add-module/{rowId}")
@ResponseBody
public String addModule(@PathVariable String rowId,
                       @RequestParam Map<String, String> formData) {
    PageData page = pages.get("my-page");
    RowData row = page.rows.stream()
        .filter(r -> r.id.equals(rowId))
        .findFirst()
        .orElse(null);

    if (row == null) {
        return Modal.create()
            .withTitle("Error")
            .withBody(Alert.danger("Row not found"))
            .render();
    }

    // Create new module
    String moduleId = "module-" + (++idCounter);
    String title = formData.getOrDefault("title", "New Module");
    String content = formData.getOrDefault("content", "New content");
    int width = Integer.parseInt(formData.getOrDefault("width", "6"));

    ModuleData newModule = new ModuleData(moduleId, title, content, width);
    row.modules.add(newModule);

    // Return OOB swaps: clear modal + update page
    String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
    String updatePage = renderPageContent().replace("<div id=\"page-content\">",
            "<div hx-swap-oob=\"true\" id=\"page-content\">");

    return clearModal + updatePage;
}
```

### Step 9: Helper Method - Find Module

```java
private ModuleData findModule(String id) {
    PageData page = pages.get("my-page");
    for (RowData row : page.rows) {
        for (ModuleData module : row.modules) {
            if (module.id.equals(id)) {
                return module;
            }
        }
    }
    return null;
}
```

---

## Authorization & Permissions

### Using AuthWrapper

Wrap edit/delete/create operations with authorization checks:

```java
@GetMapping("/edit/{moduleId}")
@ResponseBody
public String editModule(@PathVariable String moduleId, Principal principal) {
    return AuthWrapper.requireForEdit(
        () -> canUserEdit(moduleId, principal.getName()),
        () -> {
            // Return edit modal (same as before)
            return EditModalBuilder.create()...render();
        },
        "You don't have permission to edit this content"
    );
}

@DeleteMapping("/delete/{moduleId}")
@ResponseBody
public String deleteModule(@PathVariable String moduleId, Principal principal) {
    return AuthWrapper.requireForDelete(
        () -> canUserDelete(moduleId, principal.getName()),
        () -> {
            // Perform deletion (same as before)
            return renderPageContent();
        }
    );
}

@PostMapping("/add-module/{rowId}")
@ResponseBody
public String addModule(@PathVariable String rowId, Principal principal) {
    return AuthWrapper.requireForCreate(
        () -> canUserCreate(rowId, principal.getName()),
        () -> {
            // Add module (same as before)
            return clearModal + updatePage;
        }
    );
}

// Authorization logic
private boolean canUserEdit(String moduleId, String username) {
    // Check user permissions (Spring Security, database, etc.)
    UserRole role = getUserRole(username);
    return role == UserRole.ADMIN || role == UserRole.EDITOR;
}
```

### Module-Level Permissions

Control edit/delete buttons via `EditableModule`:

```java
EditableModule editable = EditableModule.wrap(module)
    .withEditUrl("/edit/module-123")
    .withDeleteUrl("/delete/module-123")
    .withCanEdit(userCanEdit)     // Hide edit button if false
    .withCanDelete(userCanDelete);  // Hide delete button if false
```

**Common Patterns:**

**1. Locked Module (No Edit/Delete):**
```java
.withCanEdit(false)
.withCanDelete(false)
// Use case: Site branding, legal text, required content
```

**2. Edit-Only Module (No Delete):**
```java
.withCanEdit(true)
.withCanDelete(false)
// Use case: Core content that must stay but can be updated
```

**3. Role-Based Permissions:**
```java
boolean isAdmin = user.hasRole("ADMIN");
boolean isEditor = user.hasRole("EDITOR");

.withCanEdit(isAdmin || isEditor)
.withCanDelete(isAdmin)  // Only admins can delete
```

### Row-Level Permissions

Control "Add Module" button visibility:

```java
// In data storage
rowData.canAddModule = false;  // Prevent adding modules to this row

// In renderPageContent()
if (row.canAddModule && row.modules.size() < 3) {
    // Show "Add Module" button
}
```

### Combining Authorization & Permissions

**Best Practice:** Use both for defense in depth

```java
// 1. Hide UI elements (permissions)
EditableModule editable = EditableModule.wrap(module)
    .withCanEdit(userCanEdit)
    .withCanDelete(userCanDelete);

// 2. Protect endpoints (authorization)
@GetMapping("/edit/{id}")
@ResponseBody
public String editModule(@PathVariable String id, Principal principal) {
    return AuthWrapper.requireForEdit(
        () -> canUserEdit(id, principal.getName()),
        () -> /* return edit modal */
    );
}
```

**Why Both?**
- **Permissions**: UX optimization (don't show disabled buttons)
- **Authorization**: Security enforcement (prevent direct endpoint access)

---

## HTMX Integration Patterns

### Out-of-Band (OOB) Swaps

**Critical Pattern:** Use OOB swaps to update multiple page sections simultaneously.

**Standard Response Pattern:**
```java
@PostMapping("/save/{id}")
@ResponseBody
public String save(@PathVariable String id, @RequestParam Map<String, String> formData) {
    // Save data...

    // OOB swap 1: Clear modal
    String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";

    // OOB swap 2: Update page content
    String updatePage = renderPageContent().replace(
        "<div id=\"page-content\">",
        "<div hx-swap-oob=\"true\" id=\"page-content\">"
    );

    return clearModal + updatePage;
}
```

**Important:** Save button should use `hx-swap="none"` when using OOB swaps:
```java
Button saveBtn = Button.create("Save");
saveBtn.withAttribute("hx-post", "/save/123");
saveBtn.withAttribute("hx-swap", "none");  // OOB swaps only
```

### Modal Target Pattern

Edit buttons target the modal container:

```java
EditableModule editable = EditableModule.wrap(module)
    .withEditUrl("/edit/123")
    .withEditTarget("#edit-modal-container")  // Default
    .withEditSwap("innerHTML");  // Default
```

**Generated HTMX:**
```html
<button hx-get="/edit/123"
        hx-target="#edit-modal-container"
        hx-swap="innerHTML">✏</button>
```

### Delete Target Pattern

Delete buttons can target page content directly or use OOB:

**Option 1: Direct target (simple)**
```java
EditableModule editable = EditableModule.wrap(module)
    .withDeleteUrl("/delete/123")
    .withDeleteTarget("#page-content")
    .withDeleteSwap("outerHTML");
```

**Option 2: OOB swap (more flexible)**
```java
// EditableModule doesn't specify target (uses default OOB)
EditableModule editable = EditableModule.wrap(module)
    .withDeleteUrl("/delete/123");

// Controller returns OOB swaps
@DeleteMapping("/delete/{id}")
@ResponseBody
public String delete(@PathVariable String id) {
    // Delete logic...
    String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
    String updatePage = renderPageContent().replace(...);
    return clearModal + updatePage;
}
```

### Form Field Inclusion

Include form fields in HTMX requests:

```java
Button saveBtn = Button.create("Save");
saveBtn.withAttribute("hx-post", "/save/123");
saveBtn.withAttribute("hx-include", ".modal-body input, .modal-body textarea, .modal-body select");
```

**Important:** Use direct CSS selector, NOT `closest .modal-body` (causes HTMX errors).

---

## Complete Working Example

See `/demo/src/main/java/io/mindspice/demo/Phase6_5TestController.java` for a complete, production-ready reference implementation with:

✅ Full CRUD operations (Create, Read, Update, Delete)
✅ Module and row permissions
✅ OOB swap patterns
✅ Modal-based editing
✅ Width editing (layout concerns)
✅ Auto-delete empty rows
✅ Insert row functionality

**Test it:** Run the demo and visit `http://localhost:8080/test/phase6-5`

---

## Best Practices

### 1. Always Use EditModalBuilder

**✅ GOOD:**
```java
return EditModalBuilder.create()
    .withTitle("Edit Module")
    .withEditView(formComponents)
    .withSaveUrl("/save/123")
    .build()
    .render();
```

**❌ BAD:**
```java
// Don't manually construct modals for editing
return Modal.create()
    .withTitle("Edit")
    .withBody(/* manually build footer, buttons, etc. */)
    .render();
```

### 2. Single Modal Container

**✅ GOOD:**
```html
<!-- ONE modal container for ALL modals -->
<div id="edit-modal-container"></div>
```

**❌ BAD:**
```html
<!-- DON'T create multiple modal containers -->
<div id="add-module-modal"></div>
<div id="edit-module-modal"></div>
<div id="delete-modal"></div>
```

### 3. Use OOB Swaps for Multi-Section Updates

**✅ GOOD:**
```java
String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
String updatePage = renderPageContent().replace(
    "<div id=\"page-content\">",
    "<div hx-swap-oob=\"true\" id=\"page-content\">"
);
return clearModal + updatePage;
```

**❌ BAD:**
```java
// Don't mix main swap and OOB swaps
return pageContent;  // HTMX gets confused with multiple roots
```

### 4. Module Width is Layout Concern

**✅ GOOD:**
```java
// Width editing separate from module editing
Div combinedForm = new Div();
combinedForm.withChild(adapter.buildEditView());  // Module fields
combinedForm.withChild(widthSelector);  // Layout field
```

**❌ BAD:**
```java
// Don't add width to EditAdapter interface
// Modules should be layout-agnostic
```

### 5. Auto-Delete Empty Rows

**✅ GOOD:**
```java
@DeleteMapping("/delete/{moduleId}")
@ResponseBody
public String deleteModule(@PathVariable String moduleId) {
    // Remove module
    row.modules.removeIf(m -> m.id.equals(moduleId));

    // Auto-delete empty row
    if (row.modules.isEmpty()) {
        page.rows.remove(row);
        renumberRows();
    }

    return renderPageContent();
}
```

### 6. Use Button.create() for HTMX Buttons

**✅ GOOD:**
```java
Button saveBtn = Button.create("Save");  // type="button"
saveBtn.withAttribute("hx-post", "/save/123");
```

**❌ BAD:**
```java
Button saveBtn = Button.submit("Save");  // type="submit" blocks HTMX
```

### 7. Validate on Server

**✅ GOOD:**
```java
@PostMapping("/save/{id}")
@ResponseBody
public String save(@PathVariable String id, @RequestParam Map<String, String> formData) {
    // Validate
    EditAdapter<?> adapter = getModule(id);
    ValidationResult validation = adapter.validate(formData);
    if (!validation.isValid()) {
        return Modal.create()
            .withTitle("Validation Error")
            .withBody(Alert.danger(String.join(", ", validation.getErrors())))
            .render();
    }

    // Save...
}
```

### 8. Rebuild Module Content After Edits

**✅ GOOD:**
```java
@Override
public ContentModule applyEdits(Map<String, String> formData) {
    this.title = formData.get("title");
    this.content = formData.get("content");

    // Rebuild content (modules are build-once)
    children.clear();
    buildContent();

    return this;
}
```

---

## Troubleshooting

### Modal Doesn't Appear

**Problem:** Modal container missing or multiple containers
**Solution:** Ensure exactly ONE `<div id="edit-modal-container"></div>` in HTML

### Changes Don't Save

**Problem:** Button uses `type="submit"` instead of `type="button"`
**Solution:** Use `Button.create()` not `Button.submit()`

**Problem:** Form fields not included in POST
**Solution:** Add `hx-include` attribute:
```java
.withAttribute("hx-include", ".modal-body input, .modal-body textarea, .modal-body select")
```

### Modal Doesn't Close After Save

**Problem:** OOB swap not clearing modal container
**Solution:** Return OOB swap with empty div:
```java
String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
```

### HTMX Error: "can't access property matches"

**Problem:** Using `hx-include="closest .modal-body"`
**Solution:** Use direct CSS selector:
```java
.withAttribute("hx-include", ".modal-body input, .modal-body textarea, .modal-body select")
```

### Edit Buttons Not Visible

**Problem:** Z-index issue or buttons not rendering
**Solution:** Use framework CSS classes (automatic with `EditableModule`)

**Check:** Module has edit/delete URLs:
```java
.withEditUrl("/edit/123")  // Required for edit button
.withDeleteUrl("/delete/123")  // Required for delete button
```

### Page Doesn't Refresh After Edit

**Problem:** OOB swap selector doesn't match page container
**Solution:** Ensure IDs match:
```java
// Page rendering
return "<div id=\"page-content\">" + content.render() + "</div>";

// OOB swap
String updatePage = renderPageContent().replace(
    "<div id=\"page-content\">",
    "<div hx-swap-oob=\"true\" id=\"page-content\">"
);
```

### Authorization Not Working

**Problem:** Endpoints not protected
**Solution:** Wrap endpoints with `AuthWrapper`:
```java
return AuthWrapper.requireForEdit(
    () -> canUserEdit(id, username),
    () -> /* edit modal */
);
```

---

## Summary

The SimplyPages editing system provides:

1. **EditableModule** - Wrapper that adds edit/delete buttons with framework styling
2. **Modal** - Professional overlay modals with backdrop and animations
3. **EditModalBuilder** - Standardized edit modal construction with OOB support
4. **EditAdapter Interface** - Contract for modules to implement inline editing
5. **AuthWrapper** - Optional authorization wrapper for protected operations
6. **Permissions** - Fine-grained control over edit/delete/create actions
7. **HTMX Integration** - Seamless dynamic updates with OOB swap patterns

**Key Principles:**
- Server-first rendering (no complex frontend build)
- Auto-save (no manual save/load buttons)
- Framework-level styling (consistent UX)
- Defense in depth (permissions + authorization)
- OOB swaps for multi-section updates

**Reference Implementation:**
- **Phase6_5TestController** - Complete working example with all features
- **Phase8TestController** - Authorization integration with role-based permissions

**Next Steps:**
1. Copy the Step-by-Step Implementation section
2. Adapt data structures to your domain
3. Implement CRUD endpoints following the patterns
4. Add authorization with AuthWrapper
5. Test with `./mvnw spring-boot:run`

---

**Documentation Version:** 1.0
**Last Updated:** 2026-01-04
**Framework Version:** 0.1.1
