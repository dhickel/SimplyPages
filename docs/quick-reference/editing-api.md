# Editing System API Reference

**Package**: `io.mindspice.simplypages.editing`

## EditAdapter Interface

The `EditAdapter` interface allows modules to be editable by defining the contract for building edit forms, applying changes, and validating input.

### Methods

#### `Component buildEditView()`

Build the edit form UI for this module.

**Returns**: `Component` - Usually a `Div` containing form fields

**Example**:
```java
@Override
public Component buildEditView() {
    return new Div()
        .withChild(TextInput.create("title").withValue(title));
}
```

#### `T applyEdits(Map<String, String> formData)`

Apply form data to this module (mutates in place).

**Parameters**: `formData` - Form field name → value map
**Returns**: `this` (for chaining)

**Example**:
```java
@Override
public ContentModule applyEdits(Map<String, String> formData) {
    this.title = formData.get("title");
    return this;
}
```

#### `ValidationResult validate(Map<String, String> formData)`

Validate form data before applying.

**Returns**: `ValidationResult.valid()` or `ValidationResult.invalid(errors)`

**Example**:
```java
@Override
public ValidationResult validate(Map<String, String> formData) {
    if (formData.get("title").isEmpty()) {
        return ValidationResult.invalid("Title is required");
    }
    return ValidationResult.valid();
}
```

---

## EditModalBuilder

Helper for building standardized edit modals.

### Usage

```java
EditModalBuilder.create()
    .withTitle("Edit Module")
    .withModuleId(moduleId)
    .withEditView(formComponent)
    .withSaveUrl("/api/save")
    .withDeleteUrl("/api/delete") // Optional
    .build(); // Returns Modal
```

---

## ValidationResult

Immutable result object for validation.

```java
// Success
return ValidationResult.valid();

// Failure
return ValidationResult.invalid("Error 1", "Error 2");
```

---

## Form Components

Standard components for building edit forms.

*   `TextInput.create(name)`
*   `TextArea.create(name)`
*   `Select.create(name)`
*   `Checkbox.create(name, label)`
*   `Button.create(text)`

---

## HTMX Integration

The editing system relies on HTMX for:
1.  **GET** requests to fetch the modal (`hx-target="#edit-modal-container"`).
2.  **POST** requests to save data (`hx-swap="none"` with OOB updates).
3.  **DELETE** requests to remove modules (`hx-swap="outerHTML"`).

### OOB Swaps Pattern

When saving, return multiple OOB swaps to update the page and close the modal.

```java
StringBuilder response = new StringBuilder();

// 1. Close Modal
response.append("<div id=\"edit-modal-container\" hx-swap-oob=\"true\"></div>");

// 2. Update Content
response.append("<div id=\"module-123\" hx-swap-oob=\"true\">")
    .append(updatedModule.render())
    .append("</div>");

return response.toString();
```
