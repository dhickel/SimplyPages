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
    .withHxSwap("innerHTML show:window:top")
    .withHxPushUrl(true);
```

Notes:
1. `show:window:top` ensures the viewport returns to the top on each navigation.
2. Keep in-document anchors (`#section-id`) as normal links for same-page jumps.
3. SimplyPages also resets scroll for HTMX requests that push browser history (`hx-push-url`), so this pattern works even when navigation is initiated from reusable side-nav components.

## Endpoint Contracts

1. Each endpoint owns one primary target contract.
2. IDs in HTML must stay stable across renders.
3. Avoid hidden coupling between unrelated HTMX endpoints.

## Error Handling

Return user-visible HTML fragments for validation and authorization errors.
Do not return raw stack traces or transport-only messages.
