[Previous](02-dynamic-fragment-caching-patterns.md) | [Index](../INDEX.md)

# HTMX Endpoint and Swap Patterns

This guide defines stable endpoint patterns for predictable updates.

## Framework Helpers

### Polling fragments

Use `PollingPanel` for app-owned fragments that should refresh on load and interval:

```java
PollingPanel.create("job-status", "/jobs/123/status")
    .everySeconds(3)
    .withLoadingText("Loading status");
```

The endpoint should return the replacement panel or compatible fragment selected by the configured
`hx-target`/`hx-swap` values.

### Tab and master-detail fragment loading

Use `HtmxTabNav` or `MasterDetailBrowserModule` when server routes own the detail fragment:

```java
HtmxTabNav.create("project-tabs", "#project-panel")
    .addTab("overview", "Overview", "/projects/123/overview")
    .addTab("runs", "Runs", "/projects/123/runs");
```

### Out-of-band responses

Use `OobFragments` when one request should update a primary fragment and one or more secondary
targets:

```java
OobFragments.response(
    projectPanel,
    OobFragments.swap("project-status", StatusBadge.success("Saved"))
);
```

Applications still own the endpoint contract, authorization, validation, and returned domain data.

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

## Endpoint Contracts

1. Each endpoint owns one primary target contract.
2. IDs in HTML must stay stable across renders.
3. Avoid hidden coupling between unrelated HTMX endpoints.

## Error Handling

Return user-visible HTML fragments for validation and authorization errors.
Do not return raw stack traces or transport-only messages.
