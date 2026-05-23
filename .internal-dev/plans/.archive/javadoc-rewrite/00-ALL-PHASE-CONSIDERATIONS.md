# 00 - All-Phase Considerations: SimplyPages Javadoc Rewrite

## Purpose
This document defines cross-phase standards and acceptance gates for the full Javadoc rewrite in `simplypages/src/main/java/io/mindspice/simplypages`.

## Global Scope
- In scope: all Java symbols in `simplypages` (public, protected, package-private, private).
- Out of scope: `demo` module and runtime behavior changes.
- Work type: documentation-only rewrite unless explicitly split into separate implementation tasks.

## Documentation Standard (Applies to All Phases)
1. Contract-first and terse by default.
2. Class-level Javadoc must state:
- Purpose and role.
- Lifecycle semantics (build/render/init where relevant).
- Mutability and thread-safety.
- Key invariants and extension boundaries.
3. Method/constructor Javadoc must state:
- Input/behavior/output contract.
- Side effects and state mutation.
- Preconditions/postconditions when relevant.
- Failure behavior (`@throws` when applicable).
4. Field/private helper docs must explain maintenance intent when behavior is not obvious.
5. Examples are optional and used only when constraints are hard to communicate tersely in prose.

## Mutability and Thread-Safety Policy (Mandatory)
For mutable APIs/types, documentation must explicitly cover:
- Why mutability exists.
- Safe lifecycle scope (typically request-scoped).
- Thread-safety status.
- Correct reuse vs incorrect reuse patterns.

## Security and Contract Boundaries
Javadocs must clearly indicate:
- Escaped-by-default rendering paths.
- Unsafe/trusted HTML boundaries.
- Framework vs application responsibilities in editing/auth/HTMX integrations.

## Source Alignment Rules
Use framework docs as intent references without mirroring them:
- `docs/core/*`
- `docs/security/*`
- `docs/operations/*`
- `docs/reference/*`
- package-level `AGENTS.md` files under `simplypages/src/main/java/io/mindspice/simplypages/**/AGENTS.md`

## Quality Rubric
A symbol is considered complete when:
1. Behavior is unambiguous without reading implementation.
2. Mutability/threading guidance is explicit when mutable.
3. Input/output and side effects are clear.
4. Any non-obvious lifecycle or ordering contract is documented.
5. Wording is terse and technical; no tutorial bloat.

## Validation Gates (Per Phase + Final)
1. Rubric-driven package review completed.
2. Javadoc generation succeeds for `simplypages` module.
3. Broken links/invalid tags resolved.
4. Spot checks confirm consistency of terminology (`SimplyPages`, lifecycle wording, mutability phrasing).

## Non-Goals
- No API redesign.
- No behavior refactor.
- No demo documentation work.

## Final Deliverable Criteria
- All symbols in `simplypages` have rewritten, release-grade Javadocs.
- Cross-package contracts are consistent.
- Contributors and agents can infer usage and constraints directly from Javadocs.
