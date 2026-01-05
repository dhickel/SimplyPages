# SimplyPages Editing System - API Reference

**Last Updated**: 2026-01-02
**Package**: `io.mindspice.simplypages.editing`

## Table of Contents

1. [EditAdapter Interface](#editadapter-interface)
2. [EditModalBuilder](#editmodalbuilder)
3. [ValidationResult](#validationresult)
4. [Modal Component](#modal-component)
5. [Form Components](#form-components)
6. [HTMX Integration](#htmx-integration)

---

## EditAdapter Interface

**Package**: `io.mindspice.simplypages.editing.EditAdapter`
**Type**: Interface
**Generic Parameter**: `T extends Module` (self-referential for fluent API)

### Purpose

Makes modules editable by defining the contract for building edit forms, applying changes, and validating input.

### Methods

#### `Component buildEditView()`

Build the edit form UI for this module.

**Returns**: `Component` - Usually a `Div` containing form fields

**Description**: Creates the form UI that allows editing of the module's properties. Use form components like `TextInput`, `TextArea`, `Select`, `Checkbox`, etc. Field names should match the parameter names expected by `applyEdits()`.

**Example**:
```java
@Override
public Component buildEditView() {
    Div form = new Div();

    form.withChild(new Div().withClass("form-field")
        .withChild(new Paragraph("Title:").withClass("form-label"))
        .withChild(TextInput.create("title").withValue(title)));

    form.withChild(new Div().withClass("form-field")
        .withChild(new Paragraph("Content:").withClass("form-label"))
        .withChild(TextArea.create("content")
            .withValue(content)
            .withRows(10)));

    return form;
}
```

#### `T applyEdits(Map<String, String> formData)`

Apply form data to this module (mutates in place).

**Parameters**:
- `formData` - `Map<String, String>` - Form field name → value map

**Returns**: `T` - `this` (for method chaining)

**Description**: Receives form data and updates the module's internal state. The module is mutated in place following the fluent builder pattern. This method is called after `validate()` returns valid.

**Example**:
```java
@Override
public ContentModule applyEdits(Map<String, String> formData) {
    if (formData.containsKey("title")) {
        this.title = formData.get("title");
    }
    if (formData.containsKey("content")) {
        this.content = formData.get("content");
    }
    // Checkboxes only send data when checked
    if (formData.containsKey("useMarkdown")) {
        this.useMarkdown = "on".equals(formData.get("useMarkdown"));
    } else {
        this.useMarkdown = false;
    }
    return this;
}
```

#### `ValidationResult validate(Map<String, String> formData)` (default)

Validate form data before applying.

**Parameters**:
- `formData` - `Map<String, String>` - Form field name → value map

**Returns**: `ValidationResult` - Validation result indicating success or failure with errors

**Description**: Override this method to provide custom validation logic. Return `ValidationResult.valid()` if the data is valid, or `ValidationResult.invalid()` with error messages if validation fails. The default implementation accepts all data as valid.

**Example**:
```java
@Override
public ValidationResult validate(Map<String, String> formData) {
    List<String> errors = new ArrayList<>();

    String title = formData.get("title");
    if (title == null || title.trim().isEmpty()) {
        errors.add("Title is required");
    }
    if (title != null && title.length() > 200) {
        errors.add("Title must be less than 200 characters");
    }

    String content = formData.get("content");
    if (content == null || content.trim().isEmpty()) {
        errors.add("Content cannot be empty");
    }

    return errors.isEmpty() ?
        ValidationResult.valid() :
        ValidationResult.invalid(errors);
}
```

---

## EditModalBuilder

**Package**: `io.mindspice.simplypages.editing.EditModalBuilder`
**Type**: Builder Class

### Purpose

Helper for building standardized edit modals with consistent layout and behavior. Handles footer layout (delete on left, cancel/save on right) and HTMX attributes for dynamic updates.

### Factory Method

#### `static EditModalBuilder create()`

Create a new EditModalBuilder instance.

**Returns**: `EditModalBuilder` - New builder instance

**Example**:
```java
EditModalBuilder builder = EditModalBuilder.create();
```

### Configuration Methods

#### `EditModalBuilder withTitle(String title)`

Set the modal title.

**Parameters**:
- `title` - `String` - The modal title

**Returns**: `EditModalBuilder` - this for chaining

**Default**: `"Edit Module"`

**Example**:
```java
builder.withTitle("Edit Content Module");
```

#### `EditModalBuilder withModuleId(String moduleId)`

Set the module ID being edited.

**Parameters**:
- `moduleId` - `String` - The module identifier

**Returns**: `EditModalBuilder` - this for chaining

**Example**:
```java
builder.withModuleId("module-123");
```

#### `EditModalBuilder withEditView(Component editView)`

Set the edit view component (form fields).

**Parameters**:
- `editView` - `Component` - The edit form component (from `EditAdapter.buildEditView()`)

**Returns**: `EditModalBuilder` - this for chaining

**Required**: Yes

**Example**:
```java
builder.withEditView(adapter.buildEditView());
```

#### `EditModalBuilder withSaveUrl(String saveUrl)`

Set the save endpoint URL.

**Parameters**:
- `saveUrl` - `String` - The save endpoint URL

**Returns**: `EditModalBuilder` - this for chaining

**Required**: Yes

**Description**: The save button will POST to this URL with form data.

**Example**:
```java
builder.withSaveUrl("/api/modules/123/update");
```

#### `EditModalBuilder withDeleteUrl(String deleteUrl)`

Set the delete endpoint URL.

**Parameters**:
- `deleteUrl` - `String` - The delete endpoint URL

**Returns**: `EditModalBuilder` - this for chaining

**Required**: No (but required if delete button should be shown)

**Description**: The delete button will DELETE to this URL.

**Example**:
```java
builder.withDeleteUrl("/api/modules/123/delete");
```

#### `EditModalBuilder hideDelete()`

Hide the delete button.

**Returns**: `EditModalBuilder` - this for chaining

**Description**: Use this when adding new modules or when deletion is not allowed.

**Example**:
```java
builder.hideDelete();
```

#### `EditModalBuilder withPageContainerId(String pageContainerId)`

Set the page container ID for HTMX target.

**Parameters**:
- `pageContainerId` - `String` - The page container element ID

**Returns**: `EditModalBuilder` - this for chaining

**Default**: `"page-content"`

**Example**:
```java
builder.withPageContainerId("content-area");
```

#### `EditModalBuilder withModalContainerId(String modalContainerId)`

Set the modal container ID for HTMX target.

**Parameters**:
- `modalContainerId` - `String` - The modal container element ID

**Returns**: `EditModalBuilder` - this for chaining

**Default**: `"edit-modal-container"`

**Example**:
```java
builder.withModalContainerId("my-modal-container");
```

### Build Method

#### `Modal build()`

Build the Modal component.

**Returns**: `Modal` - Modal component ready to render

**Throws**: `IllegalStateException` - if required fields are missing (editView or saveUrl)

**Example**:
```java
Modal modal = builder
    .withTitle("Edit Content")
    .withEditView(form)
    .withSaveUrl("/save")
    .withDeleteUrl("/delete")
    .build();

return modal.render();
```

### Complete Example

```java
@GetMapping("/api/modules/{id}/edit")
@ResponseBody
public String editModule(@PathVariable String id) {
    MyModule module = modules.get(id);
    EditAdapter<MyModule> adapter = module;

    return EditModalBuilder.create()
        .withTitle("Edit Content Module")
        .withModuleId(id)
        .withEditView(adapter.buildEditView())
        .withSaveUrl("/api/modules/" + id + "/update")
        .withDeleteUrl("/api/modules/" + id + "/delete")
        .build()
        .render();
}
```

---

## ValidationResult

**Package**: `io.mindspice.simplypages.editing.ValidationResult`
**Type**: Immutable Class

### Purpose

Encapsulates the outcome of validating form data, including whether validation passed and any error messages if it failed.

### Factory Methods

#### `static ValidationResult valid()`

Create a successful validation result.

**Returns**: `ValidationResult` - ValidationResult indicating validation passed

**Example**:
```java
return ValidationResult.valid();
```

#### `static ValidationResult invalid(String... errors)`

Create a failed validation result with error messages (varargs).

**Parameters**:
- `errors` - `String...` - One or more error messages

**Returns**: `ValidationResult` - ValidationResult indicating validation failed

**Throws**: `IllegalArgumentException` - if no error messages provided

**Example**:
```java
return ValidationResult.invalid(
    "Title cannot be empty",
    "Content must be at least 10 characters"
);
```

#### `static ValidationResult invalid(List<String> errors)`

Create a failed validation result with error messages (list).

**Parameters**:
- `errors` - `List<String>` - List of error messages

**Returns**: `ValidationResult` - ValidationResult indicating validation failed

**Throws**: `IllegalArgumentException` - if errors list is null or empty

**Example**:
```java
List<String> errors = new ArrayList<>();
errors.add("Field is required");
errors.add("Value is too long");
return ValidationResult.invalid(errors);
```

### Methods

#### `boolean isValid()`

Check if validation passed.

**Returns**: `boolean` - true if validation passed, false otherwise

**Example**:
```java
ValidationResult result = module.validate(formData);
if (result.isValid()) {
    module.applyEdits(formData);
}
```

#### `List<String> getErrors()`

Get validation error messages.

**Returns**: `List<String>` - List of error messages (empty if validation passed)

**Example**:
```java
if (!result.isValid()) {
    for (String error : result.getErrors()) {
        System.out.println("Error: " + error);
    }
}
```

#### `String getErrorsAsString(String separator)`

Get all error messages as a single string with custom separator.

**Parameters**:
- `separator` - `String` - Separator between messages (e.g., `", "` or `"; "`)

**Returns**: `String` - Concatenated error messages

**Example**:
```java
String errorMessage = result.getErrorsAsString("; ");
// "Title cannot be empty; Content is required"
```

#### `String getErrorsAsString()`

Get all error messages as a single string (comma-separated).

**Returns**: `String` - Concatenated error messages with comma separator

**Example**:
```java
String errorMessage = result.getErrorsAsString();
// "Title cannot be empty, Content is required"
```

### Complete Example

```java
@Override
public ValidationResult validate(Map<String, String> formData) {
    List<String> errors = new ArrayList<>();

    // Required field check
    if (formData.get("title") == null || formData.get("title").trim().isEmpty()) {
        errors.add("Title is required");
    }

    // Length check
    if (formData.get("title") != null && formData.get("title").length() > 200) {
        errors.add("Title must be less than 200 characters");
    }

    // Content check
    if (formData.get("content") == null || formData.get("content").trim().isEmpty()) {
        errors.add("Content cannot be empty");
    }

    // Return result
    return errors.isEmpty() ?
        ValidationResult.valid() :
        ValidationResult.invalid(errors);
}
```

---

## Modal Component

**Package**: `io.mindspice.simplypages.components.display.Modal`
**Type**: Component Class
**Extends**: `HtmlTag`

### Purpose

Overlay modal component with backdrop, ESC key support, and customizable behavior.

### Features

- Semi-transparent backdrop overlay
- Centered on desktop, full-screen on mobile
- ESC key to close
- Optional click backdrop to close
- Z-index layering (backdrop: 1000, modal: 1001)
- Auto-escape HTML in title to prevent XSS

### Factory Method

#### `static Modal create()`

Create a new Modal instance.

**Returns**: `Modal` - New modal instance

**Example**:
```java
Modal modal = Modal.create();
```

### Configuration Methods

#### `Modal withModalId(String modalId)`

Set the modal ID.

**Parameters**:
- `modalId` - `String` - The modal identifier

**Returns**: `Modal` - this for chaining

**Default**: Auto-generated timestamp-based ID

**Example**:
```java
modal.withModalId("edit-modal-123");
```

#### `Modal withTitle(String title)`

Set the modal title.

**Parameters**:
- `title` - `String` - The modal title (will be HTML-escaped)

**Returns**: `Modal` - this for chaining

**Example**:
```java
modal.withTitle("Edit Content Module");
```

#### `Modal withBody(Component body)`

Set the modal body content.

**Parameters**:
- `body` - `Component` - The body component

**Returns**: `Modal` - this for chaining

**Example**:
```java
modal.withBody(editForm);
```

#### `Modal withFooter(Component footer)`

Set the modal footer content.

**Parameters**:
- `footer` - `Component` - The footer component

**Returns**: `Modal` - this for chaining

**Example**:
```java
Div footer = new Div()
    .withChild(Button.create("Cancel"))
    .withChild(Button.create("Save"));
modal.withFooter(footer);
```

#### `Modal closeOnBackdrop(boolean enabled)`

Enable or disable closing modal when clicking backdrop.

**Parameters**:
- `enabled` - `boolean` - true to enable backdrop click to close

**Returns**: `Modal` - this for chaining

**Default**: `true`

**Example**:
```java
modal.closeOnBackdrop(false);  // Prevent accidental closes
```

#### `Modal showCloseButton(boolean show)`

Show or hide the close button (×) in header.

**Parameters**:
- `show` - `boolean` - true to show close button

**Returns**: `Modal` - this for chaining

**Default**: `true`

**Example**:
```java
modal.showCloseButton(false);  // Hide × button
```

### Complete Example

```java
Modal modal = Modal.create()
    .withTitle("Edit Module")
    .withBody(formComponent)
    .withFooter(buttonsComponent)
    .closeOnBackdrop(false)
    .showCloseButton(true);

return modal.render();
```

### CSS Classes

The modal uses these CSS classes (defined in framework.css):

- `.modal-backdrop` - Semi-transparent overlay (z-index: 1000)
- `.modal-container` - Modal box (z-index: 1001)
- `.modal-header` - Modal header with title and close button
- `.modal-title` - Modal title text
- `.modal-close` - Close button (×)
- `.modal-body` - Modal content area
- `.modal-footer` - Modal footer with buttons

---

## Form Components

These components are used within `buildEditView()` to create edit forms.

### TextInput

**Package**: `io.mindspice.simplypages.components.forms.TextInput`

**Factory**: `TextInput.create(String name)`

**Methods**:
- `.withValue(String value)` - Set current value
- `.withPlaceholder(String placeholder)` - Set placeholder text
- `.withType(String type)` - Set input type (text, email, number, etc.)
- `.withMaxWidth(String width)` - Constrain width (e.g., "300px", "50%")
- `.withRequired()` - Mark as required

**Example**:
```java
TextInput.create("title")
    .withValue(currentTitle)
    .withPlaceholder("Enter title...")
    .withMaxWidth("100%")
```

### TextArea

**Package**: `io.mindspice.simplypages.components.forms.TextArea`

**Factory**: `TextArea.create(String name)`

**Methods**:
- `.withValue(String value)` - Set current value
- `.withPlaceholder(String placeholder)` - Set placeholder text
- `.withRows(int rows)` - Set number of rows
- `.withMaxWidth(String width)` - Constrain width

**Example**:
```java
TextArea.create("content")
    .withValue(currentContent)
    .withRows(15)
    .withMaxWidth("100%")
```

### Select

**Package**: `io.mindspice.simplypages.components.forms.Select`

**Factory**: `Select.create(String name)`

**Methods**:
- `.addOption(String value, String label, boolean selected)` - Add option
- `.addOption(String value, String label)` - Add option (not selected)
- `.withMaxWidth(String width)` - Constrain width

**Example**:
```java
Select.create("width")
    .addOption("3", "1/4 (3/12)", currentWidth == 3)
    .addOption("4", "1/3 (4/12)", currentWidth == 4)
    .addOption("6", "1/2 (6/12)", currentWidth == 6)
    .addOption("12", "Full (12/12)", currentWidth == 12)
```

### Checkbox

**Package**: `io.mindspice.simplypages.components.forms.Checkbox`

**Factory**: `Checkbox.create(String name, String label)`

**Methods**:
- `.checked()` - Mark as checked
- `.unchecked()` - Mark as unchecked (default)

**Important**: Checkboxes only send data when checked. In `applyEdits()`, you must handle the unchecked case explicitly:

```java
if (formData.containsKey("useMarkdown")) {
    this.useMarkdown = "on".equals(formData.get("useMarkdown"));
} else {
    this.useMarkdown = false;  // Unchecked
}
```

**Example**:
```java
Checkbox checkbox = Checkbox.create("published", "Publish immediately");
if (currentlyPublished) {
    checkbox.checked();
}
```

### Button

**Package**: `io.mindspice.simplypages.components.forms.Button`

**Factory**: `Button.create(String text)`

**Methods**:
- `.withStyle(ButtonStyle style)` - Set button style (PRIMARY, SECONDARY, DANGER, LINK)
- `.withType(String type)` - Set button type (button, submit, reset)
- `.withAttribute(String name, String value)` - Add custom attribute (e.g., HTMX)

**Example**:
```java
Button saveBtn = Button.create("Save Changes")
    .withStyle(Button.ButtonStyle.PRIMARY);
saveBtn.withAttribute("hx-post", "/save");
saveBtn.withAttribute("hx-swap", "none");
```

---

## HTMX Integration

The editing system uses HTMX for dynamic updates without page reloads.

### HTMX Attributes

#### Edit Button (GET)

```java
Button editBtn = Button.create("✏")
    .withStyle(Button.ButtonStyle.LINK);
editBtn.withAttribute("hx-get", "/edit-module/" + id);
editBtn.withAttribute("hx-target", "#edit-modal-container");
```

**Attributes**:
- `hx-get` - GET request to show edit modal
- `hx-target` - Swap response into this element (modal container)

#### Save Button (POST)

```java
Button saveBtn = Button.create("Save")
    .withStyle(Button.ButtonStyle.PRIMARY);
saveBtn.withAttribute("hx-post", "/update-module/" + id);
saveBtn.withAttribute("hx-swap", "none"); // Use OOB swaps only
saveBtn.withAttribute("hx-include", ".modal-body input, .modal-body textarea, .modal-body select");
```

**Attributes**:
- `hx-post` - POST request with form data
- `hx-swap` - "none" because we use OOB swaps
- `hx-include` - Include all form fields in POST

#### Delete Button (DELETE)

```java
Button deleteBtn = Button.create("🗑")
    .withStyle(Button.ButtonStyle.LINK);
deleteBtn.withAttribute("hx-delete", "/delete-module/" + id);
deleteBtn.withAttribute("hx-confirm", "Are you sure?");
deleteBtn.withAttribute("hx-target", "#module-" + id);
deleteBtn.withAttribute("hx-swap", "outerHTML");
```

**Attributes**:
- `hx-delete` - DELETE request
- `hx-confirm` - Confirmation dialog
- `hx-target` - Target element to remove
- `hx-swap` - "outerHTML" to remove entire element

### Out-of-Band (OOB) Swaps

OOB swaps allow updating multiple parts of the page in one response.

**Pattern**:
```java
StringBuilder response = new StringBuilder();

// Clear modal (OOB swap)
response.append("<div id=\"edit-modal-container\" hx-swap-oob=\"true\"></div>");

// Update module content (OOB swap)
response.append("<div id=\"module-").append(id).append("\" hx-swap-oob=\"true\">");
response.append(updatedModule.render());
response.append("</div>");

return response.toString();
```

**How it works**:
1. Server returns HTML with `hx-swap-oob="true"` attributes
2. HTMX finds elements by ID and swaps their content
3. Multiple OOB swaps can occur in one response
4. Main response target (if any) is also swapped

**Example - Save Endpoint with OOB**:
```java
@PostMapping("/update-module/{id}")
@ResponseBody
public String updateModule(@PathVariable String id, @RequestParam Map<String, String> formData) {
    MyModule module = modules.get(id);

    // Validate and apply edits
    ValidationResult validation = module.validate(formData);
    if (!validation.isValid()) {
        // Show modal with errors (EditModalBuilder doesn't render errors for you)
        Div body = new Div()
            .withChild(Alert.warning(validation.getErrorsAsString()))
            .withChild(module.buildEditView());

        return EditModalBuilder.create()
            .withEditView(body)
            .withSaveUrl("/update-module/" + id)
            .build()
            .render();
    }

    module.applyEdits(formData);

    // Return OOB swaps
    StringBuilder response = new StringBuilder();

    // Clear modal
    response.append("<div id=\"edit-modal-container\" hx-swap-oob=\"true\"></div>");

    // Update module
    response.append("<div id=\"module-").append(id).append("\" hx-swap-oob=\"true\">");
    response.append(renderModule(module));
    response.append("</div>");

    return response.toString();
}
```

---

## Common Patterns

### Pattern: Modal Container Setup

**CRITICAL**: ONE modal container for ALL modals on the page.

```java
@GetMapping("/my-page")
@ResponseBody
public String myPage() {
    StringBuilder html = new StringBuilder();

    // Build page content
    html.append(buildPageContent());

    // Add single modal container at end
    Div modalContainer = new Div().withAttribute("id", "edit-modal-container");
    html.append(modalContainer.render());

    return html.toString();
}
```

### Pattern: Edit Endpoint

```java
@GetMapping("/edit-module/{id}")
@ResponseBody
public String showEditModal(@PathVariable String id) {
    MyModule module = modules.get(id);

    return EditModalBuilder.create()
        .withTitle("Edit Module")
        .withEditView(module.buildEditView())
        .withSaveUrl("/update-module/" + id)
        .withDeleteUrl("/delete-module/" + id)
        .build()
        .render();
}
```

### Pattern: Save Endpoint with Validation

```java
@PostMapping("/update-module/{id}")
@ResponseBody
public String updateModule(@PathVariable String id, @RequestParam Map<String, String> formData) {
    MyModule module = modules.get(id);

    // Validate
    ValidationResult validation = module.validate(formData);
    if (!validation.isValid()) {
        Div body = new Div()
            .withChild(Alert.warning(validation.getErrorsAsString()))
            .withChild(module.buildEditView());

        return EditModalBuilder.create()
            .withTitle("Edit Module")
            .withEditView(body)
            .withSaveUrl("/update-module/" + id)
            .withDeleteUrl("/delete-module/" + id)
            .build()
            .render();
    }

    // Apply edits
    module.applyEdits(formData);

    // Return OOB swaps
    StringBuilder response = new StringBuilder();
    response.append("<div id=\"edit-modal-container\" hx-swap-oob=\"true\"></div>");
    response.append("<div id=\"module-" + id + "\" hx-swap-oob=\"true\">");
    response.append(renderModule(module));
    response.append("</div>");

    return response.toString();
}
```

### Pattern: Delete Endpoint

```java
@DeleteMapping("/delete-module/{id}")
@ResponseBody
public String deleteModule(@PathVariable String id) {
    modules.remove(id);

    // Return OOB swap to remove from DOM (empty div)
    return "<div id=\"module-" + id + "\" hx-swap-oob=\"true\"></div>";
}
```

---

## Related Documentation

- **[EDITING_SYSTEM_OVERVIEW.md](EDITING_SYSTEM_OVERVIEW.md)** - High-level concepts and architecture
- **[EDITING_SYSTEM_GUIDE.md](EDITING_SYSTEM_GUIDE.md)** - Step-by-step implementation guide
- **[EDITING_SYSTEM_PLAN.md](EDITING_SYSTEM_PLAN.md)** - Implementation roadmap
- **[MODAL_OVERLAY_USAGE.md](MODAL_OVERLAY_USAGE.md)** - Correct modal patterns
- **[DEVELOPMENT_NOTES.md](DEVELOPMENT_NOTES.md)** - Known bugs and issues

---

## Version History

- **2026-01-03**: Phase 6.5 complete - Permission system APIs added
- **2026-01-02**: Phase 5 complete - EditableModule wrapper added
- **2026-01-02**: Initial API reference (Phase 3-4 complete)
- Future: Add error display to EditModalBuilder (Phase 7)
