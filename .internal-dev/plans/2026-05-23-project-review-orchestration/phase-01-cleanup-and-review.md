# Context
The repository has accumulated active `.internal-dev` plan artifacts, bug reports, reviews, notes, knowledge entries, and changelogs across the SimplyPages development cycle. The requested work is to clean up finalized `.internal-dev` material where evidence supports archiving, then run a package-focused project review for quality, robustness, issue risk, contract quality, documentation quality, and high-benefit refactor targets.

# Goal
Produce a single consolidated project review in `.internal-dev/reviews/` after package-level parallel review. The review must cover code and documentation readiness and identify practical remediation targets without mutating production code during the review pass.

# In Scope
- Archive stale/completed `.internal-dev/plans` artifacts when matching changelogs and current code/docs indicate completion.
- Keep active bug reports in place unless they are proven fixed.
- Run read-only package/domain review agents for `core`, `layout`, `components`, `modules`, `editing`, `builders`, `demo`, `docs`, and repository/build process.
- Require every package review to assess code quality, robustness, issue risk, API/contract quality, test coverage, documentation depth, and high-benefit refactor targets.
- Compile all package reviews into one final `.internal-dev/reviews/2026-05-23-project-quality-review.md`.

# Out of Scope
- Production code fixes from review findings.
- Broad documentation rewrites during the review pass.
- Closing open bugs without targeted verification.
- Reverting unrelated current worktree changes.

# Implementation Steps
1. Read root `AGENTS.md`, `.internal-dev/AGENTS.md`, and package `AGENTS.md` files that apply to each review domain.
2. Inventory `.internal-dev/plans` and `.internal-dev/bugs` using targeted reads.
3. Move completed/stale plan artifacts into `.internal-dev/plans/.archive/`, preserving relative grouping where useful.
4. Create shared orchestration notes at `demo/.codex-orchestration/project-review/notes.md`.
5. Spawn read-only package review agents using `gpt-5.5` and `xhigh` reasoning:
   - `core`
   - `layout`
   - `components` including subpackages
   - `modules`
   - `editing`
   - `builders`
   - `demo`
   - `docs and developer process`
6. While agents run, the orchestrator performs build/test inventory and repo-level review to avoid duplicating agent work.
7. Collect agent outputs, deduplicate findings, classify severity, and separate immediate bugs from refactor opportunities and documentation gaps.
8. Write the consolidated review file with the required `.internal-dev` headings: `Scope`, `Findings`, `Risk Assessment`, `Recommendations`, and `Follow-ups`.
9. Write a changelog entry for the finalized cleanup/review artifact work.

# Validation
- Confirm archived paths no longer appear as active plans.
- Confirm the review file exists and includes all required headings.
- Confirm the changelog file exists and includes all required headings.
- Run targeted compile/test commands when feasible for a review baseline:
  - `../mvnw -pl simplypages test`
  - `../mvnw -pl demo test`
- Record any validation failures or skipped checks in the review and changelog.

# Exit Criteria
- `.internal-dev` active plan area is materially cleaner without hiding unresolved bugs.
- Package review agent outputs have been synthesized into a single review file.
- Documentation quality is assessed alongside code quality.
- Required `.internal-dev` changelog closeout exists.
