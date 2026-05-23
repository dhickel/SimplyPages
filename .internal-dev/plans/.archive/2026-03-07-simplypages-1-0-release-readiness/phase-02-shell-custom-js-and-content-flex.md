# Context

Release readiness flagged missing custom JavaScript integration guidance and API support in `ShellBuilder`. QOL #2 also calls out shell content wrapper rigidity, with existing demo code using string replacement to inject content into shell output.

This phase addresses both with explicit, minimal builder APIs and test-backed output contracts.

# Goal

Provide a predictable, low-abstraction `ShellBuilder` extension surface for custom JS and content-area wrapping while preserving existing defaults and load behavior.

# In Scope

- New `ShellBuilder` custom JS methods
- New content-area flexibility methods from QOL #2
- Deterministic script load order update in shell output
- Demo refactor to remove shell HTML string replacement hack
- `ShellBuilderTest` expansions for new behavior contracts

# Out of Scope

- JS bundling, fingerprinting, or asset pipeline concerns
- New runtime script orchestration beyond ordered `<script defer>` emission
- Any behavior changes to framework JS itself (`/js/framework.js`)

# Implementation Steps

1. Add custom JS API to `ShellBuilder`
- Add builder field: `LinkedHashSet<String> customJsPaths`.
- Add public methods:
  - `withCustomJs(String jsPath)`
  - `withCustomJs(List<String> jsPaths)`
  - `addCustomJs(String jsPath)`
- Path validation: non-null, non-blank only (same strictness as custom CSS path handling).
- Dedupe by insertion order (`LinkedHashSet`) to keep output stable.

2. Update shell head load chain
- Keep existing order for CSS and HTMX.
- Emit scripts in this exact order:
  1. HTMX (if enabled)
  2. Framework JS (`/js/framework.js`)
  3. Custom JS scripts (new, in configured order)
- All emitted scripts include `defer`.
- Do not modify inline script append behavior in body (collapsible sidebar + nav-active snippets).

3. Implement QOL #2 content flexibility
- Keep existing `withContentTarget(String)`.
- Add alias method `withContentTargetId(String)` delegating to existing target setter.
- Add `withContentTargetClass(String className)` to apply class on content target div.
- Add `withContentWrapper(Function<Component, Component> wrapper)`:
  - wrapper receives content-target component
  - wrapper result is inserted inside `<main class="content-wrapper">`
  - if wrapper is null or returns null, throw `IllegalArgumentException`
  - default behavior is identity wrapper (no output change)

4. Demo migration
- Replace `EditingDemoController.renderWithShell(...)` string replacement with native builder composition:
  - pass content via `.withContent(new RawHtml(content))`
  - rely on builder API instead of string mutation
- Preserve current page semantics and remove brittle `replaceAll/replace` logic.

5. Add tests for new contract
- Extend `ShellBuilderTest` with cases for:
  - custom JS inclusion and order
  - dedupe behavior
  - `withCustomJs(String)` reset semantics
  - list replacement semantics
  - js path validation failures
  - `withContentTargetId(...)` alias behavior
  - content target class emission
  - wrapper application output
  - wrapper null guard behavior

## Edge Cases

- Duplicate JS paths should render once in first-seen order.
- Wrapper should still apply when content is absent and HTMX auto-load attrs are present on content target.
- `buildBody()` and `build()` should share content wrapper/target behavior consistently.

## Gotchas

- Do not emit custom JS before framework JS.
- Avoid applying wrapper twice through layered helper calls.
- Keep `withContent(...)` behavior intact: explicit content disables default HTMX `hx-get="/home"` autoload.

## Assumptions

- Consumers own trust/safety of script URLs.
- Non-blank path validation is sufficient for 1.0.

## Best Practices

- Mirror CSS API shape for JS to maximize discoverability.
- Keep extension points explicit and composable.
- Favor builder-native composition over post-render string surgery.

## Style Fit

- Low abstraction: thin APIs around explicit HTML output.
- Pragmatic DX: solves real integration pain without adding hidden magic.
- Contract stability: preserves existing shell structure and default behavior.

# Validation

- `./mvnw -pl simplypages -Dtest=ShellBuilderTest test`
- `./mvnw -pl demo test`
- `./mvnw -pl simplypages test`

Validation checks:
- Script load order deterministic and tested.
- Wrapper and content-target hooks render as specified.
- Demo no longer relies on string replacement for shell content injection.

# Exit Criteria

- Medium custom JS readiness finding addressed with API + test coverage.
- QOL #2 implemented and demo usage aligned.
- Existing shell defaults remain backward compatible unless new methods are used.
