# Building Editable Pages

The SimplyPages Editing System allows for inline, user-friendly content management without complex CMS integration.

## Concept

Any standard `Module` can be wrapped in an `EditableModule` to provide:
1.  **Edit Button**: Triggers a modal.
2.  **Delete Button**: Removes the module.
3.  **HTMX Integration**: Seamless updates without page reload.

## Quick Start

### 1. Define the Page Container

Your page shell must include the modal container.

```java
// In your Shell or Page layout
new Div().withAttribute("id", "edit-modal-container");
```

### 2. Wrap a Module

```java
ContentModule content = ContentModule.create()
    .withTitle("Welcome")
    .withContent("Editable content")
    .withModuleId("welcome-msg"); // ID is required for targeting

// Wrap it
EditableModule editable = EditableModule.wrap(content);
```

### 3. Permissions

You can control who can edit or delete.

```java
editable
    .withCanEdit(user.isAdmin())
    .withCanDelete(false);
```

## The Editable

Modules must implement `Editable` to define how they are edited. `ContentModule` implements this out of the box.

To make a custom module editable, implement `Editable<T>`.

```java
public class MyModule extends Module implements Editable<MyModule> {

    @Override
    public Component buildEditView() {
        // Return a form (e.g., EditModalBuilder)
    }

    @Override
    public MyModule applyEdits(Map<String, String> formData) {
        // Apply changes from form data
        return this;
    }
}
```

## EditModalBuilder

Use `EditModalBuilder` to easily construct standard edit forms.

```java
return new EditModalBuilder()
    .withTitle("Edit My Module")
    .addInput("Title", "title", this.title)
    .addTextArea("Content", "content", this.content)
    .build();
```

## Controller Handling

You need endpoints to handle the "Edit" click (GET) and the "Save" (POST).

```java
@GetMapping("/edit/{moduleId}")
public String edit(@PathVariable String moduleId) {
    MyModule module = repository.findById(moduleId);
    return module.buildEditView().render();
}

@PostMapping("/edit/{moduleId}")
public String save(@PathVariable String moduleId, @RequestParam Map<String, String> formData) {
    MyModule module = repository.findById(moduleId);
    module.applyEdits(formData);
    repository.save(module);

    // Return updated module OOB
    return module.render();
}
```

## Advanced: Validation

The `Editable` also supports validation.

```java
@Override
public ValidationResult validate(Map<String, String> formData) {
    ValidationResult result = new ValidationResult();
    if (formData.get("title").isEmpty()) {
        result.addError("title", "Title is required");
    }
    return result;
}
```
