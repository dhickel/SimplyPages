# Editing System Guide

> Deprecated: This document is legacy and may conflict with current modal/OOB patterns.
> Use `EDITING_SYSTEM_GUIDE.md` and `MODAL_OVERLAY_USAGE.md` instead.

The Java HTML Framework includes a powerful editing system that enables in-place content editing, page building, and dynamic module management. Built on HTMX for smooth user experiences, the editing system uses a wrapper/decorator pattern to add editing capabilities to any module without modifying the original module code.

## Table of Contents

1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Quick Start](#quick-start)
4. [EditableModule](#editablemodule)
5. [EditableRow & EditablePage](#editablerow--editablepage)
6. [HTMX Integration](#htmx-integration)
7. [Backend Implementation](#backend-implementation)
8. [Authorization & Security](#authorization--security)
9. [Complete Examples](#complete-examples)
10. [Best Practices](#best-practices)

---

## Overview

### What is the Editing System?

The editing system provides:
- **In-place editing**: Users can edit content directly on the page without navigation
- **HTMX-powered updates**: Smooth partial page updates without full page reloads
- **Page builder**: Add, remove, and arrange modules dynamically
- **Approval workflows**: Support for USER_EDIT (approval required) and OWNER_EDIT (immediate) modes
- **Type-safe integration**: Full Java type safety with Spring Security

### Architecture

The editing system follows these design principles:

1. **Wrapper/Decorator Pattern**: Editing capabilities are added by wrapping modules, not by inheritance
2. **Separation of Concerns**: Editing UI is separate from module logic
3. **Server-Side Rendering**: All HTML generation happens on the server
4. **Backend Flexibility**: Interfaces define contracts, you implement the business logic

```
┌─────────────────────────────────────────────────┐
│ Frontend (Browser)                              │
│  ├─ Module with Edit Button                    │
│  ├─ HTMX triggers requests                     │
│  └─ Partial HTML updates                       │
└─────────────────────────────────────────────────┘
                      ↕ HTMX
┌─────────────────────────────────────────────────┐
│ Backend (Spring Controller)                     │
│  ├─ Edit endpoint returns form                 │
│  ├─ Update endpoint processes changes          │
│  └─ Returns updated module HTML                │
└─────────────────────────────────────────────────┘
                      ↕
┌─────────────────────────────────────────────────┐
│ Framework Components                            │
│  ├─ EditableModule (wrapper)                   │
│  ├─ EditableRow (page builder)                 │
│  ├─ EditablePage (full page editing)           │
│  └─ Interfaces (your implementation)           │
└─────────────────────────────────────────────────┘
```

---

## Core Concepts

### EditMode

Defines how edits are processed:

```java
public enum EditMode {
    USER_EDIT,    // Changes require approval
    OWNER_EDIT    // Changes go live immediately
}
```

**USER_EDIT**: Used for community contributions, wiki-style editing, or when users edit content they don't own. Changes are submitted for review.

**OWNER_EDIT**: Used when the user owns the content or has admin/moderator privileges. Changes go live immediately.

### EditableModule

A wrapper that adds edit/delete controls to any module:

```java
EditableModule.wrap(module)
    .withModuleId("module-123")
    .withEditUrl("/api/modules/123/edit")
    .withDeleteUrl("/api/modules/123/delete")
    .withEditMode(EditMode.OWNER_EDIT);
```

**Key Features**:
- Adds edit toolbar above the module
- HTMX-powered edit/delete buttons
- Visual indicators for edit mode (USER_EDIT shows "Changes require approval" tooltip)
- Can be hidden conditionally

### EditableRow

A row wrapper for page builder functionality:

```java
EditableRow editableRow = EditableRow.wrap(row, "row-1", "page-123")
    .addEditableModule(contentModule, "module-1")
    .addEditableModule(galleryModule, "module-2");
```

**Key Features**:
- Manages modules within a row
- Automatic column width calculation
- Maximum modules per row (default 3, configurable)
- "Add Module" button when space available

### EditablePage

A page wrapper for complete page building:

```java
EditablePage editablePage = EditablePage.create("page-123")
    .addEditableRow(editableRow1)
    .addEditableRow(editableRow2);
```

**Key Features**:
- Manages multiple EditableRows
- "Insert Row Below" buttons between rows
- "Add Row at Bottom" button at page end
- Clean visual separators

---

## Quick Start

### 1. Add Edit Capability to a Module

```java
@GetMapping("/page/{pageId}")
public String viewPage(@PathVariable String pageId, Principal principal) {
    // Create your module as normal
    ContentModule module = ContentModule.create()
        .withTitle("My Article")
        .withContent("# Article Content\n\nEdit me!");

    // Wrap for editing if user has permission
    if (canUserEdit(pageId, principal)) {
        EditMode mode = getEditMode(pageId, principal);

        module = EditableModule.wrap(module)
            .withModuleId("content-1")
            .withEditUrl("/api/modules/content-1/edit")
            .withDeleteUrl("/api/modules/content-1/delete")
            .withEditMode(mode);
    }

    Page page = Page.builder()
        .addRow(row -> row.withChild(module));

    return renderWithShell(page);
}
```

### 2. Implement Edit Endpoint

```java
@GetMapping("/api/modules/{moduleId}/edit")
@ResponseBody
public String editModule(@PathVariable String moduleId) {
    // Load module data
    ModuleData data = loadModuleData(moduleId);

    // Build edit form
    Div form = new Div();
    form.withAttribute("id", moduleId);
    form.withClass("module content-module p-3");

    form.withChild(Header.H3("Edit Content"));

    // Title field
    Div titleGroup = new Div().withClass("mb-3");
    titleGroup.withChild(new Paragraph("Title:"));
    TextInput titleInput = TextInput.create("title")
        .withValue(data.getTitle());
    titleInput.withAttribute("style", "width: 100%; max-width: 600px;");
    titleGroup.withChild(titleInput);
    form.withChild(titleGroup);

    // Content field
    Div contentGroup = new Div().withClass("mb-3");
    contentGroup.withChild(new Paragraph("Content (Markdown):"));
    TextArea contentArea = TextArea.create("content")
        .withValue(data.getContent())
        .withRows(15);
    contentArea.withAttribute("style", "width: 100%; max-width: 800px;");
    contentGroup.withChild(contentArea);
    form.withChild(contentGroup);

    // Buttons
    Div buttons = new Div().withClass("d-flex gap-2");

    Button saveBtn = Button.submit("Save");
    saveBtn.withAttribute("hx-post", "/api/modules/" + moduleId + "/update");
    saveBtn.withAttribute("hx-target", "#" + moduleId);
    saveBtn.withAttribute("hx-swap", "outerHTML");
    buttons.withChild(saveBtn);

    Button cancelBtn = Button.create("Cancel")
        .withStyle(Button.ButtonStyle.SECONDARY);
    cancelBtn.withAttribute("hx-get", "/api/modules/" + moduleId + "/view");
    cancelBtn.withAttribute("hx-target", "#" + moduleId);
    cancelBtn.withAttribute("hx-swap", "outerHTML");
    buttons.withChild(cancelBtn);

    form.withChild(buttons);

    return form.render();
}
```

### 3. Implement Update Endpoint

```java
@PostMapping("/api/modules/{moduleId}/update")
@ResponseBody
public String updateModule(
    @PathVariable String moduleId,
    @RequestParam Map<String, String> formData,
    Principal principal
) {
    EditMode mode = getEditMode(moduleId, principal);

    if (mode == EditMode.OWNER_EDIT) {
        // Save directly
        saveModuleData(moduleId, formData);

        // Return updated module
        ContentModule module = ContentModule.create()
            .withTitle(formData.get("title"))
            .withContent(formData.get("content"));

        return EditableModule.wrap(module)
            .withModuleId(moduleId)
            .withEditUrl("/api/modules/" + moduleId + "/edit")
            .withDeleteUrl("/api/modules/" + moduleId + "/delete")
            .withEditMode(EditMode.OWNER_EDIT)
            .render();
    } else {
        // Submit for approval
        submitForApproval(moduleId, formData, principal);

        return Alert.warning("Your changes have been submitted for review.")
            .render();
    }
}
```

That's it! You now have in-place editing with HTMX.

---

## EditableModule

### Basic Usage

```java
// Wrap any module
Module anyModule = ContentModule.create()
    .withTitle("Title")
    .withContent("Content");

EditableModule editable = EditableModule.wrap(anyModule)
    .withModuleId("module-123")
    .withEditUrl("/api/modules/123/edit")
    .withDeleteUrl("/api/modules/123/delete")
    .withEditMode(EditMode.USER_EDIT);
```

### Configuration Options

#### Edit URL (Required)
```java
.withEditUrl("/api/modules/123/edit")
```
When user clicks "Edit", HTMX will GET this URL. Server should return edit form HTML.

#### Delete URL (Optional)
```java
.withDeleteUrl("/api/modules/123/delete")
```
If provided, shows delete button. HTMX will DELETE to this URL with confirmation dialog.

#### Module ID (Required)
```java
.withModuleId("module-123")
```
Used as HTML `id` attribute and HTMX target for in-place updates.

#### Edit Mode
```java
.withEditMode(EditMode.OWNER_EDIT)  // or EditMode.USER_EDIT
```
Determines workflow and visual indicators.

#### Hide Edit Controls
```java
.hideEditControls()
```
Hides the edit toolbar. Useful for conditional display.

### Edit Toolbar Structure

The EditableModule renders this structure:

```html
<div id="module-123" class="editable-module-wrapper">
    <div class="edit-toolbar">
        <button class="btn btn-primary btn-sm"
                hx-get="/api/modules/123/edit"
                hx-target="#module-123"
                hx-swap="outerHTML"
                title="Changes require approval">
            Edit
        </button>
        <button class="btn btn-danger btn-sm"
                hx-delete="/api/modules/123/delete"
                hx-confirm="Are you sure you want to delete this module?"
                hx-target="#module-123"
                hx-swap="outerHTML swap:1s">
            Delete
        </button>
    </div>
    <!-- Wrapped module content here -->
</div>
```

### Conditional Wrapping

Only wrap modules for users with edit permission:

```java
public Module buildModule(String moduleId, Principal principal) {
    Module module = moduleFactory.create(moduleId);

    // Only make editable if user can edit
    if (authChecker.canEdit(moduleId, principal.getName())) {
        EditMode mode = authChecker.getEditMode(moduleId, principal.getName());

        module = EditableModule.wrap(module)
            .withModuleId(moduleId)
            .withEditUrl("/api/modules/" + moduleId + "/edit")
            .withDeleteUrl("/api/modules/" + moduleId + "/delete")
            .withEditMode(mode);
    }

    return module;
}
```

---

## EditableRow & EditablePage

### EditableRow

For page builder functionality with module management.

#### Basic Usage

```java
Row row = new Row();
EditableRow editableRow = EditableRow.wrap(row, "row-1", "page-123");

// Add modules
editableRow.addEditableModule(contentModule, "module-1");
editableRow.addEditableModule(galleryModule, "module-2");
```

#### Configuration

**Max Modules Per Row**:
```java
editableRow.withMaxModules(4);  // Default is 3
```

**Edit Mode**:
```java
editableRow.withEditMode(EditMode.OWNER_EDIT);
```
Applied to all modules in the row.

#### Auto Column Width Calculation

EditableRow automatically calculates equal column widths using the 12-column grid:

- 1 module: 12/1 = width 12 (100%)
- 2 modules: 12/2 = width 6 (50% each)
- 3 modules: 12/3 = width 4 (33% each)

#### Add Module Button

When `modules.size() < maxModules`, EditableRow renders:

```html
<div class="add-module-section">
    <button class="btn btn-secondary"
            hx-get="/api/pages/page-123/rows/row-1/add-module-form"
            hx-target="#add-module-modal"
            hx-swap="innerHTML">
        + Add Module
    </button>
</div>
```

### EditablePage

For full page editing with row management.

#### Basic Usage

```java
EditablePage editablePage = EditablePage.create("page-123");

// Add rows
EditableRow row1 = EditableRow.wrap(new Row(), "row-1", "page-123");
row1.addEditableModule(module1, "mod-1");
editablePage.addEditableRow(row1);

EditableRow row2 = EditableRow.wrap(new Row(), "row-2", "page-123");
row2.addEditableModule(module2, "mod-2");
editablePage.addEditableRow(row2);

String html = editablePage.render();
```

#### UI Structure

EditablePage generates:

```
[EditableRow 1]
  [Module 1] [Edit] [Delete]
  [+ Add Module]
[+ Insert Row Below]

[EditableRow 2]
  [Module 2] [Edit] [Delete]
  [+ Add Module]
[+ Insert Row Below]

[+ Add Row at Bottom]
```

#### Insert Row Flow

1. User clicks "+ Insert Row Below"
2. HTMX POST to `/api/pages/{pageId}/rows/insert`
3. Server creates new row, saves to database
4. Returns new EditableRow HTML
5. HTMX inserts before the button (`hx-swap="beforebegin"`)

---

## HTMX Integration

### Edit Flow

**Step 1: User Clicks Edit**
```
User clicks "Edit" button
↓
HTMX GET /api/modules/123/edit
hx-target="#module-123"
hx-swap="outerHTML"
```

**Step 2: Server Returns Form**
```java
@GetMapping("/api/modules/{id}/edit")
public String editForm(@PathVariable String id) {
    return buildEditForm(id);  // Returns form HTML
}
```

**Step 3: Form Replaces Module**
```
HTMX swaps module with form
Module content → Edit form with fields
```

**Step 4: User Submits**
```
User clicks "Save"
↓
HTMX POST /api/modules/123/update
hx-target="#module-123"
hx-swap="outerHTML"
```

**Step 5: Server Returns Updated Module**
```java
@PostMapping("/api/modules/{id}/update")
public String update(@PathVariable String id, @RequestParam Map<String, String> data) {
    saveData(id, data);
    return buildUpdatedModule(id);  // Returns updated module HTML
}
```

**Step 6: Form Replaced with Updated Module**
```
HTMX swaps form with updated module
Edit form → Updated module content
```

### Delete Flow

```
User clicks "Delete"
↓
Browser shows confirmation dialog (hx-confirm)
↓
User confirms
↓
HTMX DELETE /api/modules/123/delete
hx-target="#module-123"
hx-swap="outerHTML swap:1s"  // 1 second fade
↓
Server returns empty string
↓
HTMX removes element with fade animation
```

### Cancel Flow

```
User clicks "Cancel"
↓
HTMX GET /api/modules/123/view
hx-target="#module-123"
hx-swap="outerHTML"
↓
Server returns original module (wrapped in EditableModule)
↓
HTMX swaps form back to module
```

### HTMX Attributes Reference

| Attribute | Purpose | Example |
|-----------|---------|---------|
| `hx-get` | GET request | `/api/modules/123/edit` |
| `hx-post` | POST request | `/api/modules/123/update` |
| `hx-delete` | DELETE request | `/api/modules/123/delete` |
| `hx-target` | Element to update | `#module-123` |
| `hx-swap` | How to swap content | `outerHTML`, `innerHTML` |
| `hx-confirm` | Confirmation dialog | `Are you sure?` |

### Advanced HTMX Patterns

**Out-of-Band Updates** (update multiple elements):
```java
// Return updated module + notification
String moduleHtml = editableModule.render();
String notification = Alert.success("Saved!").render();

return moduleHtml +
       "<div hx-swap-oob=\"true\" id=\"notifications\">" + notification + "</div>";
```

**Loading States**:
```java
Button saveBtn = Button.submit("Save");
saveBtn.withAttribute("hx-post", "/api/modules/" + id + "/update");
saveBtn.withAttribute("hx-indicator", "#spinner");
```

**Optimistic UI** (show update immediately, rollback on error):
```java
saveBtn.withAttribute("hx-swap", "outerHTML");
saveBtn.withAttribute("hx-swap-oob", "true");
```

---

## Backend Implementation

### Controller Structure

```java
@RestController
@RequestMapping("/api/modules")
public class ModuleEditController {

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private AuthorizationChecker authChecker;

    @GetMapping("/{id}/edit")
    @PreAuthorize("@authChecker.canEdit(#id, authentication.name)")
    public String editForm(@PathVariable String id) {
        return moduleService.renderEditForm(id);
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("@authChecker.canEdit(#id, authentication.name)")
    public String update(
        @PathVariable String id,
        @RequestParam Map<String, String> data,
        Principal principal
    ) {
        EditMode mode = authChecker.getEditMode(id, principal.getName());
        return moduleService.handleUpdate(id, data, mode);
    }

    @DeleteMapping("/{id}/delete")
    @PreAuthorize("@authChecker.canDelete(#id, authentication.name)")
    public String delete(@PathVariable String id, Principal principal) {
        EditMode mode = authChecker.getEditMode(id, principal.getName());
        return moduleService.handleDelete(id, mode);
    }

    @GetMapping("/{id}/view")
    public String view(@PathVariable String id, Principal principal) {
        return moduleService.renderView(id, principal);
    }
}
```

### Service Layer

```java
@Service
public class ModuleService {

    @Autowired
    private ModuleRepository moduleRepo;

    @Autowired
    private ApprovalQueueService approvalService;

    public String renderEditForm(String moduleId) {
        ModuleEntity entity = moduleRepo.findById(moduleId)
            .orElseThrow(() -> new NotFoundException("Module not found"));

        // Build form based on module type
        return switch (entity.getType()) {
            case "content" -> buildContentEditForm(entity);
            case "gallery" -> buildGalleryEditForm(entity);
            default -> Alert.warning("Edit not supported").render();
        };
    }

    public String handleUpdate(String moduleId, Map<String, String> data, EditMode mode) {
        if (mode == EditMode.OWNER_EDIT) {
            // Direct update
            ModuleEntity entity = moduleRepo.findById(moduleId).orElseThrow();
            updateEntity(entity, data);
            moduleRepo.save(entity);

            // Return updated editable module
            Module module = buildModule(entity);
            return EditableModule.wrap(module)
                .withModuleId(moduleId)
                .withEditUrl("/api/modules/" + moduleId + "/edit")
                .withDeleteUrl("/api/modules/" + moduleId + "/delete")
                .withEditMode(EditMode.OWNER_EDIT)
                .render();
        } else {
            // Submit for approval
            approvalService.submitEdit(moduleId, data);

            return Alert.warning("Your changes have been submitted for review.")
                .render();
        }
    }

    public String handleDelete(String moduleId, EditMode mode) {
        if (mode == EditMode.OWNER_EDIT) {
            moduleRepo.deleteById(moduleId);
            return "";  // Empty - HTMX removes element
        } else {
            approvalService.submitDeletion(moduleId);
            return Alert.info("Deletion request submitted for approval.").render();
        }
    }

    public String renderView(String moduleId, Principal principal) {
        ModuleEntity entity = moduleRepo.findById(moduleId).orElseThrow();
        Module module = buildModule(entity);

        boolean canEdit = /* check permission */;
        if (canEdit) {
            EditMode mode = /* determine mode */;
            return EditableModule.wrap(module)
                .withModuleId(moduleId)
                .withEditUrl("/api/modules/" + moduleId + "/edit")
                .withDeleteUrl("/api/modules/" + moduleId + "/delete")
                .withEditMode(mode)
                .render();
        }

        return module.render();
    }
}
```

### Approval Queue Service

```java
@Service
public class ApprovalQueueService {

    @Autowired
    private PendingEditRepository editRepo;

    @Autowired
    private NotificationService notificationService;

    public void submitEdit(String moduleId, Map<String, String> editData) {
        PendingEdit edit = new PendingEdit();
        edit.setModuleId(moduleId);
        edit.setEditData(editData);
        edit.setStatus("PENDING");
        edit.setSubmittedAt(LocalDateTime.now());
        editRepo.save(edit);

        // Notify moderators
        notificationService.notifyModerators(
            "New edit pending review for module " + moduleId
        );
    }

    public void approveEdit(Long editId, String reviewerId) {
        PendingEdit edit = editRepo.findById(editId).orElseThrow();

        // Apply changes
        moduleService.applyEdit(edit.getModuleId(), edit.getEditData());

        // Update status
        edit.setStatus("APPROVED");
        edit.setReviewedAt(LocalDateTime.now());
        edit.setReviewerId(reviewerId);
        editRepo.save(edit);

        // Notify submitter
        notificationService.notifyUser(
            edit.getSubmitterId(),
            "Your edit has been approved!"
        );
    }

    public void rejectEdit(Long editId, String reviewerId, String reason) {
        PendingEdit edit = editRepo.findById(editId).orElseThrow();

        edit.setStatus("REJECTED");
        edit.setReviewedAt(LocalDateTime.now());
        edit.setReviewerId(reviewerId);
        edit.setRejectionReason(reason);
        editRepo.save(edit);

        notificationService.notifyUser(
            edit.getSubmitterId(),
            "Your edit was rejected: " + reason
        );
    }
}
```

---

## Authorization & Security

### Authorization Checker Implementation

```java
@Service
public class ModuleAuthChecker implements AuthorizationChecker {

    @Autowired
    private ModuleRepository moduleRepo;

    @Autowired
    private UserRepository userRepo;

    @Override
    public boolean canEdit(String moduleId, String userId) {
        ModuleEntity module = moduleRepo.findById(moduleId).orElse(null);
        if (module == null) return false;

        User user = userRepo.findByUsername(userId).orElse(null);
        if (user == null) return false;

        // Check ownership or admin role
        return module.getOwnerId().equals(user.getId())
            || user.hasRole("ADMIN")
            || user.hasRole("MODERATOR")
            || module.isPubliclyEditable();
    }

    @Override
    public boolean canDelete(String moduleId, String userId) {
        ModuleEntity module = moduleRepo.findById(moduleId).orElse(null);
        if (module == null) return false;

        User user = userRepo.findByUsername(userId).orElse(null);
        if (user == null) return false;

        // Delete typically more restrictive
        return module.getOwnerId().equals(user.getId())
            || user.hasRole("ADMIN");
    }

    @Override
    public EditMode getEditMode(String moduleId, String userId) {
        ModuleEntity module = moduleRepo.findById(moduleId).orElse(null);
        User user = userRepo.findByUsername(userId).orElse(null);

        // Owners and admins get immediate updates
        if (module.getOwnerId().equals(user.getId()) || user.hasRole("ADMIN")) {
            return EditMode.OWNER_EDIT;
        }

        // Regular users need approval
        return EditMode.USER_EDIT;
    }
}
```

### Spring Security Integration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/modules/*/edit").authenticated()
                .requestMatchers("/api/modules/*/update").authenticated()
                .requestMatchers("/api/modules/*/delete").authenticated()
                .anyRequest().permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")  // Or configure CSRF for AJAX
            );

        return http.build();
    }
}
```

### CSRF Protection for HTMX

**Method 1: Include CSRF token in all requests**
```java
@ControllerAdvice
public class CsrfHeaderAdvice {

    @ModelAttribute("_csrf")
    public CsrfToken csrf(HttpServletRequest request) {
        return (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    }
}
```

```html
<meta name="csrf-token" content="${_csrf.token}"/>
<meta name="csrf-header" content="${_csrf.headerName}"/>

<script>
document.body.addEventListener('htmx:configRequest', (event) => {
    const token = document.querySelector('meta[name="csrf-token"]').content;
    const header = document.querySelector('meta[name="csrf-header"]').content;
    event.detail.headers[header] = token;
});
</script>
```

**Method 2: Include in form as hidden field**
```java
Button saveBtn = Button.submit("Save");
// Add hidden CSRF field
form.withChild(new HtmlTag("input")
    .withAttribute("type", "hidden")
    .withAttribute("name", "_csrf")
    .withAttribute("value", csrfToken));
```

### Input Validation

**Always validate server-side**:

```java
@PostMapping("/api/modules/{id}/update")
public String update(
    @PathVariable String id,
    @Valid @RequestBody ModuleUpdateRequest request
) {
    // Spring Boot validates automatically

    // Additional business logic validation
    if (request.getTitle().length() > 200) {
        return Alert.danger("Title too long").render();
    }

    if (containsProfanity(request.getContent())) {
        return Alert.danger("Content contains inappropriate language").render();
    }

    // Process update
    return handleUpdate(id, request);
}
```

### XSS Prevention

The framework handles HTML escaping automatically:

```java
// Safe - automatically escaped
new Paragraph().withInnerText(userInput);

// Safe - Markdown parser handles escaping
ContentModule.create().withContent(userMarkdown);

// UNSAFE - only use with trusted content
new Div().withUnsafeHtml(trustedHtml);
```

---

## Complete Examples

### Example 1: Simple Blog Post Editing

```java
// Entity
@Entity
public class BlogPost {
    @Id
    private String id;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    private String authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// Controller
@RestController
@RequestMapping("/api/posts")
public class PostEditController {

    @Autowired
    private BlogPostRepository postRepo;

    @GetMapping("/{id}/edit")
    @PreAuthorize("@postAuthChecker.canEdit(#id, authentication.name)")
    public String editPost(@PathVariable String id) {
        BlogPost post = postRepo.findById(id).orElseThrow();

        Div form = new Div();
        form.withAttribute("id", "post-" + id);
        form.withClass("module content-module p-4");

        form.withChild(Header.H3("Edit Post"));

        // Title
        Div titleGroup = new Div().withClass("mb-3");
        titleGroup.withChild(new Paragraph("Title:"));
        TextInput titleInput = TextInput.create("title")
            .withValue(post.getTitle());
        titleInput.withAttribute("style", "width: 100%; max-width: 600px;");
        titleGroup.withChild(titleInput);
        form.withChild(titleGroup);

        // Content
        Div contentGroup = new Div().withClass("mb-3");
        contentGroup.withChild(new Paragraph("Content (Markdown):"));
        TextArea contentArea = TextArea.create("content")
            .withValue(post.getContent())
            .withRows(20);
        contentArea.withAttribute("style", "width: 100%;");
        contentGroup.withChild(contentArea);
        form.withChild(contentGroup);

        // Buttons
        Div buttons = new Div().withClass("d-flex gap-2");

        Button saveBtn = Button.submit("Save");
        saveBtn.withAttribute("hx-post", "/api/posts/" + id + "/update");
        saveBtn.withAttribute("hx-target", "#post-" + id);
        saveBtn.withAttribute("hx-swap", "outerHTML");
        buttons.withChild(saveBtn);

        Button cancelBtn = Button.create("Cancel")
            .withStyle(Button.ButtonStyle.SECONDARY);
        cancelBtn.withAttribute("hx-get", "/api/posts/" + id + "/view");
        cancelBtn.withAttribute("hx-target", "#post-" + id);
        cancelBtn.withAttribute("hx-swap", "outerHTML");
        buttons.withChild(cancelBtn);

        form.withChild(buttons);

        return form.render();
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("@postAuthChecker.canEdit(#id, authentication.name)")
    public String updatePost(
        @PathVariable String id,
        @RequestParam String title,
        @RequestParam String content
    ) {
        BlogPost post = postRepo.findById(id).orElseThrow();
        post.setTitle(title);
        post.setContent(content);
        post.setUpdatedAt(LocalDateTime.now());
        postRepo.save(post);

        // Return updated module
        ContentModule module = ContentModule.create()
            .withTitle(post.getTitle())
            .withContent(post.getContent());

        return EditableModule.wrap(module)
            .withModuleId("post-" + id)
            .withEditUrl("/api/posts/" + id + "/edit")
            .withDeleteUrl("/api/posts/" + id + "/delete")
            .withEditMode(EditMode.OWNER_EDIT)
            .render();
    }

    @GetMapping("/{id}/view")
    public String viewPost(@PathVariable String id, Principal principal) {
        BlogPost post = postRepo.findById(id).orElseThrow();

        ContentModule module = ContentModule.create()
            .withTitle(post.getTitle())
            .withContent(post.getContent());

        // Make editable if user can edit
        if (principal != null && canUserEdit(id, principal.getName())) {
            return EditableModule.wrap(module)
                .withModuleId("post-" + id)
                .withEditUrl("/api/posts/" + id + "/edit")
                .withDeleteUrl("/api/posts/" + id + "/delete")
                .withEditMode(EditMode.OWNER_EDIT)
                .render();
        }

        return module.render();
    }

    @DeleteMapping("/{id}/delete")
    @PreAuthorize("@postAuthChecker.canDelete(#id, authentication.name)")
    public String deletePost(@PathVariable String id) {
        postRepo.deleteById(id);
        return "";
    }
}
```

### Example 2: Page Builder for User Pages

```java
@Controller
@RequestMapping("/user-page")
public class UserPageController {

    @GetMapping("/{userId}/edit")
    @PreAuthorize("#userId == authentication.name or hasRole('ADMIN')")
    public String editUserPage(@PathVariable String userId) {
        UserPageData pageData = userPageService.load(userId);

        EditablePage editablePage = EditablePage.create(userId);

        for (PageRow rowData : pageData.getRows()) {
            Row row = new Row();
            EditableRow editableRow = EditableRow.wrap(row, rowData.getId(), userId);

            for (PageModule moduleData : rowData.getModules()) {
                Module module = buildModule(moduleData);
                editableRow.addEditableModule(module, moduleData.getId());
            }

            editablePage.addEditableRow(editableRow);
        }

        String content = editablePage.render();

        // Add modal container for "Add Module" workflow
        content += "<div id=\"add-module-modal\" class=\"mt-4\"></div>";

        return renderWithShell(content, "Edit Your Page");
    }

    @GetMapping("/api/pages/{userId}/rows/{rowId}/add-module-form")
    public String addModuleForm(@PathVariable String userId, @PathVariable String rowId) {
        Div modal = new Div().withClass("modal-content p-4");
        modal.withChild(Header.H3("Add New Module"));

        Row typeRow = new Row();

        Button contentBtn = Button.create("📝 Content")
            .withStyle(Button.ButtonStyle.PRIMARY);
        contentBtn.withAttribute("hx-get", "/user-page/api/pages/" + userId + "/rows/" + rowId + "/add-content");
        contentBtn.withAttribute("hx-target", "#add-module-modal");
        typeRow.withChild(Column.create().withWidth(4).withChild(contentBtn.fullWidth()));

        Button galleryBtn = Button.create("🖼️ Gallery")
            .withStyle(Button.ButtonStyle.PRIMARY);
        galleryBtn.withAttribute("hx-get", "/user-page/api/pages/" + userId + "/rows/" + rowId + "/add-gallery");
        galleryBtn.withAttribute("hx-target", "#add-module-modal");
        typeRow.withChild(Column.create().withWidth(4).withChild(galleryBtn.fullWidth()));

        modal.withChild(typeRow);

        return modal.render();
    }

    // Additional endpoints for add-content, add-gallery, add-module, etc.
}
```

---

## Best Practices

### 1. Always Check Permissions

```java
// GOOD
if (authChecker.canEdit(moduleId, userId)) {
    return EditableModule.wrap(module)...;
}
return module;  // Not editable

// BAD
return EditableModule.wrap(module)...;  // Always editable!
```

### 2. Use Proper Edit Modes

```java
// GOOD
EditMode mode = module.getOwnerId().equals(userId)
    ? EditMode.OWNER_EDIT
    : EditMode.USER_EDIT;

// BAD
EditMode mode = EditMode.OWNER_EDIT;  // Everyone gets immediate updates!
```

### 3. Validate on Server

```java
// GOOD
@PostMapping("/api/modules/{id}/update")
public String update(@PathVariable String id, @Valid @RequestBody Request req) {
    validateTitle(req.getTitle());
    validateContent(req.getContent());
    // ... process
}

// BAD
@PostMapping("/api/modules/{id}/update")
public String update(@PathVariable String id, @RequestParam Map<String, String> data) {
    // Trusting client data without validation!
    saveDirectly(data);
}
```

### 4. Return Proper HTMX Responses

```java
// GOOD - Return updated module
return EditableModule.wrap(updatedModule).render();

// GOOD - Return alert for approval workflow
return Alert.warning("Submitted for review").render();

// BAD - Return JSON (not HTML)
return "{\"success\": true}";  // HTMX expects HTML!
```

### 5. Include Cancel Button

```java
// GOOD
form.withChild(saveButton);
form.withChild(cancelButton);  // Let users back out!

// BAD
form.withChild(saveButton);  // No way to cancel
```

### 6. Use Confirmation for Destructive Actions

```java
// GOOD
deleteBtn.withAttribute("hx-confirm", "Are you sure?");

// BAD
deleteBtn.withAttribute("hx-delete", url);  // Deletes immediately!
```

### 7. Maintain Module IDs

```java
// GOOD - Same ID throughout lifecycle
editForm.withAttribute("id", moduleId);
updatedModule.withModuleId(moduleId);

// BAD - Changing IDs
editForm.withAttribute("id", "edit-" + moduleId);  // HTMX target won't match!
```

### 8. Handle Errors Gracefully

```java
// GOOD
try {
    return processUpdate(id, data);
} catch (ValidationException e) {
    return Alert.danger(e.getMessage()).render();
} catch (Exception e) {
    log.error("Update failed", e);
    return Alert.danger("An error occurred. Please try again.").render();
}

// BAD
return processUpdate(id, data);  // Uncaught exception = HTTP 500
```

### 9. Sanitize User Input

```java
// GOOD
String safeTitle = HtmlUtils.htmlEscape(userTitle);
String safeContent = markdownParser.parse(userMarkdown);  // Parser handles escaping

// BAD
String content = "<div>" + userInput + "</div>";  // XSS vulnerability!
```

### 10. Log Edit History

```java
// GOOD
@PostMapping("/api/modules/{id}/update")
public String update(@PathVariable String id, @RequestParam Map<String, String> data, Principal principal) {
    logEdit(id, principal.getName(), data);  // Audit trail
    return processUpdate(id, data);
}
```

---

## Troubleshooting

### Edit button doesn't do anything

**Check:**
1. Is HTMX included in your page? (`<script src="https://unpkg.com/htmx.org"></script>`)
2. Is the `hx-get` URL correct?
3. Check browser console for errors
4. Verify endpoint returns HTML (not JSON)

### Form submits but nothing happens

**Check:**
1. `hx-target` matches module ID
2. Endpoint returns HTML wrapped in element with same ID
3. CSRF token included if using Spring Security
4. Check browser network tab for response

### Delete button asks for confirmation but doesn't delete

**Check:**
1. `hx-delete` URL is correct
2. Endpoint returns empty string or alert
3. `@PreAuthorize` isn't blocking the request
4. Check browser console for errors

### "Permission denied" errors

**Check:**
1. `@PreAuthorize` annotation is correct
2. `AuthorizationChecker` implementation returns true
3. User is authenticated (`Principal` is not null)
4. Method security is enabled (`@EnableMethodSecurity`)

---

## Summary

The editing system provides:
- **EditableModule**: Add edit/delete controls to any module
- **EditableRow**: Page builder row management
- **EditablePage**: Full page editing with row insertion
- **HTMX Integration**: Smooth partial page updates
- **Backend Interfaces**: Flexible implementation contracts
- **Two Edit Modes**: USER_EDIT (approval) and OWNER_EDIT (immediate)

**Key Principles:**
- Wrapper pattern keeps editing separate from modules
- Server-side rendering with HTMX for smooth UX
- Type-safe Java with Spring Security integration
- You implement business logic via interfaces

**Next Steps:**
1. Check out the `/editing-demo` page for a live example
2. Implement `AuthorizationChecker` for your app
3. Create edit endpoints for your modules
4. Build approval queue UI if using USER_EDIT mode

Happy editing! 🎉
