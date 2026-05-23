# Context

This phase closes three release-readiness issues that directly impact runtime behavior and contract predictability:
- Sticky-sidebar responsive default-state mismatch (`desktop visible`, `mobile collapsed` intent)
- `RadioGroup.inline()` dropping base class due to `Div.withClass(...)` replacement behavior
- Duplicate `;;` in inline style serialization when width helpers are chained

These are foundation-level concerns in `layout`, `components/forms`, and `core` and must be fixed before broader polish.

# Goal

Ship deterministic, low-abstraction fixes that restore expected behavior with targeted regression coverage and no hidden side effects.

# In Scope

- Sticky-sidebar default-state markup fix in `Page.PageBuilder`
- `Div.withClass(String)` behavior correction to additive/dedupe semantics
- `HtmlTag.addStyle(...)` serialization normalization for chained style helpers
- Test updates for `PageTest`, `RadioGroupTest`, and `HtmlTagTest`

# Out of Scope

- New JS runtime behavior for sticky-sidebar
- Breakpoint redesign or CSS theme changes
- Null/blank class-token filtering (`withClass(String...)` style ergonomics are deferred)
- Any API redesign outside bug fix scope

# Implementation Steps

1. Sticky-sidebar responsive default-state
- Remove hardcoded `.withAttribute("open", "open")` from sticky-sidebar `<details>` construction in `Page`.
- Keep current CSS contract for responsive behavior:
  - Mobile (`max-width: 768px`): closed by default unless user opens `<details>`
  - Desktop (`min-width: 769px`): `.sticky-sidebar-mobile-collapse:not([open]) > .sticky-sidebar-content` remains visible
- Keep summary/content class hooks unchanged to preserve docs and consumer CSS compatibility.

2. Class contract repair for `Div` and `RadioGroup`
- Change `Div.withClass(String)` implementation to delegate to additive core class behavior (`super.withClass(...)`), preserving fluent return type.
- Keep `RadioGroup` constructor + `inline()` semantics unchanged; bug resolves through corrected `Div` behavior.
- Explicitly preserve duplicate-class dedupe behavior from `HtmlTag.addClass(...)`.

3. Style serialization cleanup
- Update `HtmlTag.addStyle(...)` merge path to normalize existing style string before append.
- Ensure final serialized style uses exactly one delimiter between declarations and one trailing semicolon per declaration.
- Maintain existing property replacement behavior (same property overwritten by newest value).

4. Regression tests
- `PageTest`: remove expectation that sticky details always have `open="open"`; assert mobile-collapse structure and no forced-open attribute.
- `RadioGroupTest`: assert group root includes both `form-radio-group` and `radio-inline` after `.inline()`.
- `HtmlTagTest`: add assertion that chained `withWidth/withMaxWidth/withMinWidth` output contains no `;;`.

5. Code-level guardrails
- Do not add extra validation behavior in this phase.
- Do not rename classes/ids used by framework CSS.

## Edge Cases

- Existing style strings ending with one or more semicolons must normalize cleanly.
- Repeated class additions must not duplicate tokens.
- Sticky-sidebar with absent `open` must remain visible on desktop purely via CSS.

## Gotchas

- Patching only `RadioGroup.inline()` masks the real class contract issue in `Div`.
- Regex for style-property replacement can accidentally over-match similar keys if not constrained.
- Sticky-sidebar tests cannot simulate viewport; assert markup contract and CSS hook alignment, not runtime breakpoint rendering.

## Assumptions

- Existing desktop CSS selector for closed sticky content remains intact.
- Additive `Div.withClass` behavior aligns with project conventions and consumer expectations.

## Best Practices

- Fix primitives first (`Div`, `HtmlTag`) instead of one-off downstream patches.
- Favor CSS/markup-native behavior over JS for responsive default state.
- Keep behavior deterministic and easy to reason about in tests.

## Style Fit

- No-bullshit: direct bug fixes over abstractions.
- Low abstraction: no new subsystems, no hidden runtime behavior.
- Predictable contracts: class and style serialization rules are explicit and tested.

# Validation

- `./mvnw -pl simplypages -Dtest=PageTest,RadioGroupTest,HtmlTagTest test`
- Then run package suite:
- `./mvnw -pl simplypages test`

Validation checks:
- Sticky-sidebar markup matches expected responsive strategy.
- `RadioGroup.inline()` preserves base class.
- Chained width helpers produce clean style serialization.

# Exit Criteria

- High sticky-sidebar finding resolved and covered by regression assertions.
- Medium RadioGroup class-loss finding resolved and tested.
- Low style `;;` issue resolved and tested.
- No regressions introduced in `simplypages` test suite.
