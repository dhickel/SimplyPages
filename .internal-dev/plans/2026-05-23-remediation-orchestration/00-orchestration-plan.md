# Context

The 2026-05-23 project review identified high-priority SimplyPages contract drift across safe rendering, URL/style handling, render-context behavior, layout/module CSS contracts, editing/demo endpoints, and docs/process. Dwight's email baseline requires a large set of advanced refactor plans driven by an orchestration plan, with core correctness fixes first, minimal stylistic churn, heavy before/after verification, domain-based PRs, and Playwright validation for demo page edits.

# Goal

Execute remediation as a sequence of domain-based PRs that preserve existing valid-output behavior wherever possible while fixing concrete correctness, safety, and contract bugs. Each domain PR must contain one or more self-standing implementation units, each with its own advanced plan, exact targets, tests, docs updates, and validation loop.

# In Scope

- Core safe rendering and hardened value contracts.
- Public URL/style API consistency across components, modules, builders, and forms.
- Render-context normalization for components that currently only behave correctly through zero-arg `render()`.
- Layout/module CSS and interactive identity contract fixes.
- Editing framework and demo endpoint correctness.
- Docs/process updates required by behavior/API changes.
- Separate domain branches and draft PRs.

# Out of Scope

- Visual redesign or theme restyling.
- Broad API renames unless required for correctness or safety.
- Changing valid existing HTML output except where the bug fix explicitly requires it.
- Deploy workflow replacement before core/runtime correctness PRs are complete.
- Reverting unrelated user changes from the original checkout.

# Domain PR Strategy

1. `codex/core-safe-rendering-contracts`
   - Units: HTML tag/attribute name validation, style declaration model, `SlotKey` collision policy documentation/tests.
   - Base: `codex/remediation-orchestration-plan` unless the plan PR is merged first.

2. `codex/public-url-style-contracts`
   - Units: shared URL validation reuse, raw anchor replacement, raw style replacement, structured `hx-headers`.
   - Depends on: core safe rendering helper decisions.

3. `codex/render-context-contracts`
   - Units: `Code`, `Spinner`, `Paragraph`, `Modal`, `Column`, and similar render override fixes.
   - Depends on: core tests and helper patterns only.

4. `codex/layout-module-contracts`
   - Units: layout class composition, CSS token alignment, `TabsModule` IDs, module markup/CSS contract fixes.
   - Depends on: render-context PR if `Column` is fixed there; otherwise keep `Column` in this domain and adjust dependency.

5. `codex/editing-demo-contracts`
   - Units: `EditableRow.wrap`, immutable `ValidationResult`, id/path validation, demo endpoint authorization, nested child endpoints or URL removal.
   - Requires Playwright validation for demo surfaces.

6. `codex/docs-process-hardening`
   - Units: compiling docs examples, version/JDK guidance, reference-contract expansion, deploy process cleanup planning.
   - Depends on: code PR outcomes for exact API behavior.

# Execution Rules

- Before editing a domain, run `git status -sb` and confirm the worktree has no unrelated local changes.
- Every PR branch must be created from the previous accepted domain baseline or from the orchestration branch when independent.
- Stage only files owned by that domain PR.
- Do not mix unrelated domains in one commit.
- Each unit starts with a targeted failing test or snapshot proof when feasible.
- Preserve byte-for-byte output for valid existing examples unless the unit explicitly documents the expected delta.
- For output-affecting changes, capture before/after samples using tests or local rendering probes and summarize deltas in the PR.
- For demo page edits, use Playwright browser validation in addition to automated tests.
- Keep docs and package `AGENTS.md` aligned when API or contract behavior changes.

# Validation Matrix

Minimum per-domain validation:

- Core/framework domains:
  - `./mvnw -pl simplypages test`
  - targeted test classes for changed packages
  - structural assertions for rendered HTML
  - before/after rendering delta summary

- Demo domains:
  - `./mvnw -pl demo test`
  - local `./mvnw -pl demo spring-boot:run` on an available port
  - Playwright checks for edited demo flows
  - live deploy/browser validation before finalizing code fixes when required by root `AGENTS.md`

- Docs/process domains:
  - link/path spot checks for changed docs
  - compile or snippet verification for Java examples when feasible
  - `docs/INDEX.md`, README, and AGENTS consistency check

# Remediation Policy

- If validation fails, fix within the same domain branch before opening or updating the PR.
- If a finding proves larger than planned, stop and write a new unit plan instead of broadening the active unit silently.
- If a change causes unexpected valid-output drift, either revert that part or document the exact reason and obtain confirmation before proceeding.
- If a unit depends on a not-yet-merged PR, stack the branch intentionally and state the base branch in the PR.

# Handoff / PR Requirements

Every domain PR body must include:

- Units implemented.
- Files changed.
- Expected behavior deltas.
- Before/after compatibility evidence.
- Automated test commands and results.
- Browser/Playwright evidence when demo UI changed.
- Docs updates and `.internal-dev` changelog entry.
- Remaining risks and follow-up units intentionally deferred.

# Exit Criteria

- All high-priority findings from `.internal-dev/reviews/2026-05-23-project-quality-review.md` have either a merged/finalized domain PR or an explicitly deferred follow-up record.
- No active domain branch contains unrelated changes.
- Test and browser validation evidence exists per domain.
- Docs and internal process artifacts match the resulting behavior.
