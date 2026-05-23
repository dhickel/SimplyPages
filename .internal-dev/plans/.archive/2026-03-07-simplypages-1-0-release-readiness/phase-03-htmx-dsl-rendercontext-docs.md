# Context

QOL #1 and #4 target ergonomics without changing the rendering model:
- HTMX attribute setting is verbose on generic tags
- `RenderContext.builder()` is heavy for common single-slot scenarios

In parallel, docs need stronger front-loaded thread-safety guidance and clear custom JS integration examples for production users.

# Goal

Add ergonomic aliases/factories that are thin wrappers over existing behavior, then document these paths in the core/builder docs with explicit usage patterns.

# In Scope

- HTMX DSL helper methods on `HtmlTag` (QOL #1)
- Single-slot factory overload for `RenderContext` (QOL #4)
- Documentation updates for thread-safety and custom JS/shell usage
- Test coverage for new helper/factory methods

# Out of Scope

- Typed HTMX enums or compile-time HTMX value validation
- RenderContext immutability/freeze behavior
- Broad docs rewrite beyond impacted pages

# Implementation Steps

1. Add HTMX helper methods to `HtmlTag`
- Add these public fluent methods, all delegating to `withAttribute(...)`:
  - `hxGet(String url)` -> `hx-get`
  - `hxPost(String url)` -> `hx-post`
  - `hxPut(String url)` -> `hx-put`
  - `hxPatch(String url)` -> `hx-patch`
  - `hxDelete(String url)` -> `hx-delete`
  - `hxTarget(String selector)` -> `hx-target`
  - `hxSwap(String mode)` -> `hx-swap`
  - `hxTrigger(String trigger)` -> `hx-trigger`
  - `hxInclude(String selector)` -> `hx-include`
  - `hxPushUrl(boolean enabled)` -> `hx-push-url`
- Preserve return type `HtmlTag`.
- Keep existing component-specific HTMX helpers (for example in `Link`) unchanged for compatibility.

2. Add RenderContext convenience overload
- Add `public static <T> RenderContext of(SlotKey<T> key, T value)`.
- Behavior:
  - uses default policy (`NEVER_COMPILE`)
  - if value is null, context contains no live entry for key
- Leave existing map overload unchanged.

3. Tests
- Extend `HtmlTagTest` with one focused test that chains multiple HTMX helpers and verifies exact emitted attributes.
- Extend `RenderContextTest` for:
  - single-slot factory with non-null value
  - single-slot factory with null value behavior

4. Documentation updates
- Add a top-of-page `Thread Safety First` section in core docs at start of the component/lifecycle page.
- Clarify mutable request-scoped types vs reusable template constructs.
- Update shell docs to include concrete custom JS usage with `withCustomJs(...)` APIs and load-order statement.
- Update builders reference to list new shell methods and wrapper hooks.
- Add one concise example showing new HTMX DSL (`hxGet/hxTarget/hxSwap`) and one example using `RenderContext.of(key, value)`.

## Edge Cases

- HTMX helper calls should overwrite previous same attribute names exactly like `withAttribute` does.
- `hxPushUrl(false)` must render literal `"false"`.
- `RenderContext.of(key, null)` should not throw and should behave as missing key.

## Gotchas

- Do not introduce hidden validation or behavior in helper methods; they are aliases only.
- Avoid deleting older API methods; this phase is additive and backward compatible.
- Keep docs examples synchronized with actual method names/signatures.

## Assumptions

- Alias-based ergonomics match project goals better than additional abstraction layers.
- One single-slot factory covers most QOL #4 demand; map factory already exists.

## Best Practices

- Keep helpers explicit and transparent.
- Prefer examples that mirror real SSR + HTMX usage patterns already in repo.
- Ensure every API addition lands with tests and docs in same workstream.

## Style Fit

- Java-first pragmatic ergonomics with no hidden runtime model changes.
- Minimal JS, explicit HTMX attributes, predictable output.
- Readability over abstraction.

# Validation

- `./mvnw -pl simplypages -Dtest=HtmlTagTest,RenderContextTest test`
- `./mvnw -pl simplypages test`

Validation checks:
- New helpers emit expected `hx-*` attributes.
- New context factory behaves as documented.
- Docs accurately reflect added APIs and thread-safety boundaries.

# Exit Criteria

- QOL #1 and #4 delivered as additive, tested APIs.
- Core docs begin with clear thread-safety guidance.
- Shell/custom JS docs and reference pages updated for 1.0 readiness.
