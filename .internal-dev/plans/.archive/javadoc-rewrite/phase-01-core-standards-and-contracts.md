# Phase 01 - Core Standards and Contracts

## Context
`core` defines foundational rendering and lifecycle behavior used by all framework packages. Current Javadocs are inconsistent in depth/style, with some being overly tutorial and others under-specified.

## Goal
Establish final Javadoc standards and fully rewrite the `core` package to be the canonical contract baseline for the rest of the rewrite.

## In Scope
- Finalize and apply rewrite standard from `00-ALL-PHASE-CONSIDERATIONS.md`.
- Rewrite all Javadocs in `simplypages/src/main/java/io/mindspice/simplypages/core`.
- Explicitly document:
  - lifecycle semantics (`build`, render behavior, compile policy behavior).
  - mutability/thread-safety of core types.
  - escaping/safe rendering responsibilities.

## Out of Scope
- Non-Javadoc code changes.
- Other packages except for cross-reference verification.

## Implementation Steps
1. Inventory all `core` symbols and classify contract complexity.
2. Rewrite high-risk foundational types first:
- `Component`
- `HtmlTag`
- `Module`
- `RenderContext`
- `Template`
- `TemplateComponent`
- `Slot`, `SlotKey`, `SlotEntry`, `SlotKeyMap`
- `Attribute`, `Style`, `TypedValue`
3. Standardize class-level sections: purpose, lifecycle, mutability/thread-safety, invariants.
4. Standardize method/constructor docs: contract, side effects, pre/postconditions, errors.
5. Add targeted examples only where constraints are hard to express tersely (especially slot/template/render-policy flows).
6. Normalize `@see` and link targets; remove stale or misleading tutorial-style content.

## Validation
- Rubric audit for every `core` file.
- Javadoc generation for `simplypages` succeeds with no broken references from `core`.
- Spot-check that mutable core types explicitly document safe reuse boundaries.

## Exit Criteria
- 100% of `core` symbols rewritten and rubric-compliant.
- Lifecycle and mutability behavior is unambiguous for contributors and agents.
- `core` serves as style and contract reference for Phases 02-04.

## Assumptions and Defaults
- Scope includes private members/types.
- Style is terse, contract-first.
- No runtime behavior modifications are introduced.
