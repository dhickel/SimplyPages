# Forms Components Agent Guide

## Purpose
Owns form input and form container components.

## Owns
- `Form`, `TextInput`, `TextArea`, `Select`, `Checkbox`, `RadioGroup`, `Button`

## Invariants
- Form element APIs remain fluent and typed.
- Name/value/attribute rendering stays HTML-encoded, not URL-policy validated. Form.withAction, URL-taking HTMX methods and withHxPostCsrf require application-controlled or separately endpoint-context-validated destinations. CSRF helpers attach supplied tokens/headers only; issuance, verification, authorization and destination policy remain application-owned. Do not introduce Link/Image validation by analogy.
- Form.withHxPostCsrf raw JSON remains exactly one string pair with existing getters/null-string/order/static-template/UTF-16 semantics. Do not change Form.java or introduce a generated Form marker for client compatibility.
- With HTMX 2.0.11 and framework JS, one delegated configRequest hook repairs only missing Object.prototype-name collisions from an actual source FORM's own literal lexically-one-string-pair hx-headers. No CSS-class requirement; identically shaped hand-authored forms also qualify. Exclude inherited-only/evaluated/multi/duplicate/non-string/non-form shapes. Safely define an own data entry without prototype mutation, preserving existing own case-insensitive data/null/accessor/non-enumerable entries, nonextensible maps, prior cancellation, and later overrides/cancellation. Ordinary deleted headers are not restored; own null/cancel explicitly suppresses collisions.
- Ownership begins at available configRequest entries. Upstream-discarded ajax options.__proto__ overrides are not recovered as those earlier values; stopped propagation/frozen maps remain host controlled. No generalized options/parser/XHR/transport/retry/header policy or token verification. Exact JSON representability does not guarantee arbitrary HTTP header transport; applications own destinations/CORS/CSRF/auth.
- Width helper methods on form components remain valid and validated by core style rules.
- Input type helpers map to correct HTML input types.

- `Form.addField` associates nonempty labels with known single controls (`TextInput`, `TextArea`, `Select`, `Checkbox`, `Button`). Preserve existing control IDs, including raw ID attributes on single-tag controls; assign absent/empty IDs once during field assembly, never during rendering.
- Configure control IDs before `addField`; changing IDs afterward does not update assembled field labels. Applications own explicit ID uniqueness and configure components before sharing or compiling.
- Checkbox IDs and labels stay associated regardless of setter order. `Checkbox.getId/withId` target the internal input; inherited `withAttribute("id", ...)` targets the wrapper. A Form label must target the input while preserving an independently configured wrapper ID and the checkbox's own label.
- RadioGroup has `role="radiogroup"`. A nonempty Form field caption uses `span.form-label` with a stable unique ID referenced by the group's `aria-labelledby`; do not point a native label at the group div or replace individual option labels. Standalone groups use application-supplied `aria-label`/`aria-labelledby` through existing attribute APIs; the submission name is not an accessible name.
- Radio option IDs are unique and stable per instance; consumers must not derive IDs from field values.
- RadioGroup.required() applies to subsequently added options as well as existing ones.
- Arbitrary/custom and multi-control components passed to `addField` receive only a visible `span.form-label` caption. Applications compose their own native labels or accessible names inside/alongside those components. Do not infer a control from rendered HTML, a generic HtmlTag, or a wrapper ID. Null/empty field labels do not generate IDs or change application names.

## Do
- Preserve ergonomics for common form tasks.
- Keep generated markup semantically correct for HTML forms.
- Add options with predictable defaults.

## Do Not
- Implement server-side business validation here.
- Bypass safe attribute rendering.

## Common Pitfalls
- Breaking attribute names required by browsers (`required`, `maxlength`, etc.).
- Inconsistent button/form class naming that breaks framework CSS.
- Forgetting compatibility with HTMX form usage patterns.

## Required Tests
- `FormTest`, `TextInputTest`, `TextAreaTest`, `SelectTest`, `CheckboxTest`, `RadioGroupTest`, `ButtonTest`
- Regression tests for new fluent methods and attribute rendering
- Semantic field/control linkage for default, explicit and raw IDs, including Checkbox wrapper/input separation and preservation of option labels
- Unique IDs across repeated names/independent forms, assignment before the first render, repeated-render stability, fresh compiled/direct parity and custom-component labeling boundaries
- Deliberately review forms snapshot changes and rerun tests without snapshot update mode

## Dependencies
- Depends on `core` and shared components.
- Do not depend on `modules` or demo controller logic.

## Maintenance Requirement
Keep this file updated whenever form component APIs, attributes, or default behaviors change.

See root `AGENTS.md` for global standards.

## Documentation TOC (Terse)
- Full index: `docs/INDEX.md`
- Fundamentals: `docs/fundamentals/01-web-and-htmx-primer.md`, `docs/fundamentals/02-simplypages-mental-model.md`, `docs/fundamentals/03-css-fundamentals.md`
- Getting started: `docs/getting-started/README.md`, `docs/getting-started/01-installation-and-first-static-page.md`, `docs/getting-started/02-dynamic-pages-with-slotkey-rendercontext.md`, `docs/getting-started/03-editing-system-first-implementation.md`
- Core: `docs/core/01-components-htmltag-and-module-lifecycle.md`, `docs/core/02-layout-page-row-column-grid.md`, `docs/core/03-template-rendercontext-slotkey-reference.md`, `docs/core/04-rendering-pipeline-high-and-low-level.md`, `docs/core/05-css-defaults-overrides-and-structure.md`
- Patterns: `docs/patterns/01-static-page-serving-patterns.md`, `docs/patterns/02-dynamic-fragment-caching-patterns.md`, `docs/patterns/03-htmx-endpoint-and-swap-patterns.md`, `docs/patterns/04-editing-workflows-owner-user-approval.md`
- Security: `docs/security/01-security-boundaries-and-safe-rendering.md`, `docs/security/02-authwrapper-authorizationchecker-integration.md`
- Operations: `docs/operations/01-performance-threading-and-cache-lifecycles.md`, `docs/operations/02-testing-and-troubleshooting-playbook.md`
- Reference: `docs/reference/components-and-modules-catalog.md`, `docs/reference/builders-shell-nav-banner-accountbar.md`, `docs/reference/editing-api-reference.md`

## Documentation Sync Requirement
- Any API-surface change or major internal behavior change must trigger a docs review.
- Update affected docs in the same workstream when applicable (`README.md`, `docs/INDEX.md`, and related pages).
- If no docs update is needed, explicitly note why in the PR/commit/task summary.
