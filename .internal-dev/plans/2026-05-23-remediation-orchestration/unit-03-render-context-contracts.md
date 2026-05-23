# Context

Several components implement special behavior only in zero-arg `render()`. When nested, compiled, or rendered with `RenderContext`, those behaviors disappear.

# Goal

Make custom renderers behavior-preserving across `render()`, `render(RenderContext)`, nested rendering, and template compilation.

# In Scope

- `Code.block` title/code rendering.
- `Spinner.withMessage`.
- `Paragraph` alignment classes.
- `Modal` body/footer slot/context rendering.
- `Column` base-class normalization when nested/context/compiled.
- Any directly adjacent same-pattern component found while implementing.

# Out of Scope

- Visual redesign or CSS token changes beyond preserving existing intended markup.
- Broad template compiler rewrite unless required for `Column`.

# Implementation Steps

1. Establish failing tests first.
   - For each target component, create direct render, nested render, context render, and template render cases where relevant.
   - Use `HtmlAssert`; avoid `html.contains(...)`.

2. Implement `render(RenderContext)` for custom renderers.
   - `Code`: compose title/code via `HtmlTag` or escape title explicitly; ensure `withTitle` is safe.
   - `Spinner`: render wrapper/message through context-aware child rendering.
   - `Paragraph`: apply alignment before delegating to `super.render(context)`.
   - `Modal`: render body/footer with the supplied context.
   - `Column`: normalize `.col` in constructor or context render path so nested/template reads correct attributes.

3. Check template behavior.
   - If template compilation reads pre-normalized attributes, prefer constructor/build-time class initialization for base classes.
   - Do not make template compiler component-specific unless no simpler stable option exists.

4. Docs.
   - Update core rendering docs and component catalog with the rule: custom renderers must implement context-aware rendering.

# Validation

- Targeted tests:
  - `CodeTest`, `SpinnerTest`, `ParagraphTest`, `ModalTest`, `ColumnTest`, `TemplateTest`.
- `./mvnw -pl simplypages test`
- Before/after snapshots for direct valid usage should be byte-compatible except escaped code title if previously unsafe.

# Exit Criteria

- Custom behavior is identical across direct/nested/context render paths for valid inputs.
- Slot-backed content renders in modal/body/footer contexts.
- New tests fail on current code and pass after fixes.
