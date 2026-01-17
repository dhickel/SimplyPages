# Modal Overlay Usage Guide

## Critical Pattern: One Container, Many Modals

### ✅ CORRECT Pattern

**Key Principle**: Use a **single modal container** for all modals, and return `Modal.create()` components that render as true overlays.

#### 1. HTML Structure (One Container)

```java
// In your main page rendering method
Div modalContainer = new Div().withAttribute("id", "edit-modal-container");

// Include this ONCE at the end of your page body
html.append(modalContainer.render());
```

**Important**: You need exactly **ONE** modal container with id `edit-modal-container`. Do NOT create multiple containers like:
- ❌ `<div id="add-module-modal"></div>`
- ❌ `<div id="edit-module-modal"></div>`
- ❌ `<div id="pending-edits-modal"></div>`

#### 2. HTMX Button Configuration

```java
Button editBtn = Button.create("Edit")
    .withAttribute("hx-get", "/api/modules/123/edit")
    .withAttribute("hx-target", "#edit-modal-container")  // Always target the same container
    .withAttribute("hx-swap", "innerHTML");
```

#### 3. Modal Endpoint Response

```java
@GetMapping("/api/modules/{id}/edit")
@ResponseBody
public String editModule(@PathVariable String id) {
    // Build your form content
    Div body = new Div();
    body.withChild(TextInput.create("title").withValue("Title"));

    // Use Modal.create() - this creates the overlay automatically
    return Modal.create()
        .withTitle("Edit Module")
        .withBody(body)
        .withFooter(createFooter())
        .render();
}
```

#### 4. Closing Modals (OOB Swap)

When saving/deleting, use OOB swap to clear the modal:

```java
@PostMapping("/api/modules/{id}/save")
@ResponseBody
public String saveModule(@PathVariable String id, @RequestParam Map<String, String> formData) {
    // Process the save...

    // Clear modal and update page using OOB swaps
    String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
    String updatePage = "<div hx-swap-oob=\"true\" id=\"page-content\">" + renderPage() + "</div>";

    return clearModal + updatePage;
}
```

### ❌ WRONG Pattern (Bottom-of-Page DIVs)

**Do NOT do this:**

```java
// ❌ WRONG: Multiple modal containers
html.append("<div id=\"add-module-modal\"></div>");
html.append("<div id=\"edit-module-modal\"></div>");
html.append("<div id=\"pending-edits-modal\"></div>");

// ❌ WRONG: Custom div with modal styling (not an overlay!)
Div modal = new Div()
    .withClass("modal-content p-4")
    .withAttribute("style", "background-color: white; border-radius: 8px;");
modal.withChild(Header.H3("Edit Module"));
// ... this creates a div at the bottom of the page, NOT an overlay

return modal.render();
```

**Why this is wrong:**
- Creates DIVs at the bottom of the page, NOT overlays
- No backdrop
- No centering
- Requires scrolling to see
- Poor mobile experience

### Using EditModalBuilder

`EditModalBuilder` is a helper that wraps `Modal.create()` with standard patterns for edit forms:

```java
@GetMapping("/api/modules/{id}/edit")
@ResponseBody
public String editModule(@PathVariable String id) {
    Module module = findModule(id);
    EditAdapter adapter = (EditAdapter) module;

    return EditModalBuilder.create()
        .withTitle("Edit Module")
        .withModuleId(id)
        .withEditView(adapter.buildEditView())
        .withSaveUrl("/api/modules/" + id + "/save")
        .withDeleteUrl("/api/modules/" + id + "/delete")
        .withPageContainerId("page-content")
        .withModalContainerId("edit-modal-container")
        .build()
        .render();
}
```

**Important**: `EditModalBuilder.build()` returns a `Modal` component, which renders as a proper overlay.

### Save Button Configuration

```java
// ✅ CORRECT: Use Button.create() with hx-swap="none" and OOB swaps
Button saveBtn = Button.create("Save")  // NOT Button.submit()
    .withStyle(Button.ButtonStyle.PRIMARY);
saveBtn.withAttribute("hx-post", "/api/modules/123/save");
saveBtn.withAttribute("hx-swap", "none");  // No main swap, use OOB only
saveBtn.withAttribute("hx-include", ".modal-body input, .modal-body textarea, .modal-body select");
```

**Why `Button.create()` not `Button.submit()`:**
- `Button.submit()` creates `type="submit"` which triggers browser form submission
- This blocks HTMX from intercepting the request
- Always use `Button.create()` (creates `type="button"`) for HTMX buttons

### Complete Example

See `Phase1And2TestController.java` for a complete working example:
- Single modal container: line 180
- Modal.create() usage: line 219
- EditModalBuilder usage: line 310
- OOB swap pattern: lines 365-370

### Common Mistakes Checklist

- [ ] ❌ Multiple modal container DIVs with different IDs
- [ ] ❌ Using custom Div with `modal-content` class instead of `Modal.create()`
- [ ] ❌ Using `Button.submit()` for HTMX save buttons
- [ ] ❌ Using `hx-include="closest .modal-body"` (causes HTMX errors)
- [ ] ❌ Mixing main swap and OOB swaps (causes confusion)

### Correct Pattern Summary

✅ **ONE** modal container: `<div id="edit-modal-container"></div>`
✅ **Always** use `Modal.create()` for overlays
✅ **Always** use `Button.create()` for HTMX buttons (not `submit()`)
✅ **Always** use `hx-swap="none"` with OOB swaps for save/delete
✅ **Always** target `#edit-modal-container` from all modal buttons
