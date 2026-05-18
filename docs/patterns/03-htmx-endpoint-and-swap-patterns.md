[Previous](02-dynamic-fragment-caching-patterns.md) | [Index](../INDEX.md)

# HTMX Endpoint and Swap Patterns

This guide defines stable endpoint patterns for predictable updates.

## Pattern 1: Replace One Module (`outerHTML`)

```java
@GetMapping("/widgets/{id}")
@ResponseBody
public String widget(@PathVariable String id) {
    return widgetRenderer.render(id);
}
```

```java
Button.create("Refresh")
    .withAttribute("hx-get", "/widgets/active-users")
    .withAttribute("hx-target", "#active-users")
    .withAttribute("hx-swap", "outerHTML");
```

## Pattern 2: Update Modal Container (`innerHTML`)

```java
Button.create("Edit")
    .withAttribute("hx-get", "/modules/42/edit")
    .withAttribute("hx-target", "#edit-modal-container")
    .withAttribute("hx-swap", "innerHTML");
```

## Pattern 3: OOB Multi-Target Update

Use for save flows that must update more than one area.

```text
Response body contains:
- <div id="edit-modal-container" hx-swap-oob="true"></div>
- <div id="module-42" hx-swap-oob="true">...</div>
```

## Pattern 4: Sticky Sidebar Nav + URL History

Use this when a sticky table-of-contents or side menu navigates between documents using HTMX.

```java
Link.create("/docs/core/01-components-htmltag-and-module-lifecycle", "Components")
    .withHxGet("/docs/core/01-components-htmltag-and-module-lifecycle")
    .withHxTarget("#docs-content")
    .withHxSwap("innerHTML")
    .withHxScrollTargetTop()
    .withHxPushUrl(true);
```

Notes:
1. `withHxScrollTargetTop()` tags the request so the swapped HTMX target fragment scrolls to top.
2. Keep in-document anchors (`#section-id`) as normal links for same-page jumps.
3. If no scroll tag is present, SimplyPages keeps push-url window scroll fallback behavior for history-style navigation.

## Pattern 5: Search/Autocomplete Selector

Use this for HTMX-driven typed search where selecting an option should replace the full selector region.

```java
Autocomplete selector = Autocomplete.create("projectId")
    .withLabel("Project")
    .withPlaceholder("Search projects")
    .withOptionsEndpoint("/selectors/projects/options?name=projectId")
    .withValidationEndpoint("/selectors/projects/validate?name=projectId")
    .withContextParam("workspaceId", workspaceId)
    .required();
```

Options endpoint response:

```java
return Autocomplete.options(
    new Autocomplete.OptionsConfig("#sp-autocomplete-projectId", "outerHTML", "No matches"),
    rows
).render();
```

Validation endpoint response:

```java
return Autocomplete.status(
    new Autocomplete.StatusMessage("Selected: " + label, Autocomplete.State.SELECTED)
).render();
```

## Endpoint Contracts

1. Each endpoint owns one primary target contract.
2. IDs in HTML must stay stable across renders.
3. Avoid hidden coupling between unrelated HTMX endpoints.

## Error Handling

Return user-visible HTML fragments for validation and authorization errors.
Do not return raw stack traces or transport-only messages.
