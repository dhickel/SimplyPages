[Previous](01-performance-threading-and-cache-lifecycles.md) | [Index](../INDEX.md)

# Testing and Troubleshooting Playbook

## What to Test for Every UI Flow

1. Render correctness
- expected IDs/classes for HTMX targets
- expected content values

2. Security behavior
- escaping of untrusted text
- authorization checks on edit/delete endpoints

3. Lifecycle behavior
- module build-once expectations
- template slot resolution behavior

4. Dynamic updates
- correct swap target
- OOB responses update intended DOM nodes

## Framework Test Suites to Mirror

Key suites in this repo:

- `TemplateTest`
- `RenderContextTest`
- `ModuleTest`
- `Editable*` tests
- `AuthWrapperTest`

When in doubt, match your app behavior tests to these contracts.

## Common Failure Modes

1. Slot renders empty
- check key identity/name consistency
- verify context population

2. Stale dynamic value
- check context reuse and compile policy
- verify updates use `put(...)` on reused contexts

3. Edit save appears to do nothing
- verify HTMX target ID exists
- verify save response contains expected OOB nodes

4. Content not rebuilding after edit
- for editable modules, ensure `applyEdits` logic triggers necessary rebuild path

## Minimal Regression Harness Example

```java
@Test
void saveReturnsOobModalCloseAndModuleUpdate() {
    String html = controller.saveModule("m1", Map.of("title", "Updated", "content", "Body"));
    assertTrue(html.contains("id=\"edit-modal-container\""));
    assertTrue(html.contains("hx-swap-oob=\"true\""));
    assertTrue(html.contains("id=\"m1\""));
}
```
