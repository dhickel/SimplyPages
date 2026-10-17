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

Keep the modal host stable and load the framework CSS/JS once in the shell. `Modal` markup
inserted here gets focus entry, Tab containment, top-dialog close handling and restoration
to a connected launcher on close/removal. Replacing the dialog retains its original
launcher rather than a disappearing child control. See the
[Modal contract](../reference/components-and-modules-catalog.md#modal).

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
    .withHxSwap("outerHTML")
    .withHxScrollTargetTop()
    .withHxPushUrl(true);
```

The docs endpoint returns the `#docs-content` wrapper as well as its contents. Replace it
with `outerHTML` to retain exactly one target, rather than nesting a duplicate ID.

Notes:
1. `withHxScrollTargetTop()` tags the request so the swapped HTMX target fragment scrolls to top.
2. Keep in-document anchors (`#section-id`) as normal links for same-page jumps.
3. If no scroll tag is present, SimplyPages keeps push-url window scroll fallback behavior for history-style navigation.

## Endpoint Contracts

1. Each endpoint owns one primary target contract.
2. IDs in HTML must stay stable across renders.
3. Avoid hidden coupling between unrelated HTMX endpoints.

## Error Handling

Return user-visible HTML fragments for validation and authorization errors.
Do not return raw stack traces or transport-only messages.
