# Context

Docs/process review found non-compiling examples, stale version/JDK guidance, stale AGENTS links, shallow reference docs, and deploy workflow risks.

# Goal

Bring public docs and process docs in line with the remediated code contracts and make follow-up process work explicit.

# In Scope

- README/getting-started example fixes.
- Version/JDK/toolchain guidance.
- Docs/AGENTS TOC alignment.
- Reference docs expansion for changed contracts.
- Deploy workflow risk documentation and bug consolidation plan.
- `.internal-dev` closeout/changelogs for each domain PR.

# Out of Scope

- Full deploy script replacement before domain correctness PRs finish.
- Generated docs system unless chosen as a later dedicated project.

# Implementation Steps

1. After each code domain PR, update exact affected docs in the same branch.
   - Security docs for safety contracts.
   - Component/module/builder/layout/editing references for parameter contracts.
   - Getting-started examples for API changes.

2. Final docs/process branch.
   - Normalize README and getting-started dependency versions to current release policy.
   - Document Java/toolchain requirements explicitly.
   - Align `AGENTS.md` docs list with `docs/INDEX.md`.
   - Fix `.internal-dev/AGENTS.md` links to existing docs or create the referenced index files.
   - Expand catalog from example-only to production API map for current public surfaces.

3. Deploy workflow plan.
   - Do not silently rewrite deploy script in this docs branch.
   - Create a future implementation plan for key-based auth, known-hosts, split smoke phases, and remote-curl-free validation.
   - Consolidate duplicate deploy-curl bugs only after the plan names the replacement behavior.

4. Validation.
   - Verify snippets compile where feasible.
   - Run docs link/path spot checks.
   - Run relevant package tests if docs changes accompany code.

# Validation

- `rg -n "vr\\.errors\\(|1\\.0\\.0|docs/api/00-index|docs/internal/00-index" README.md docs AGENTS.md .internal-dev/AGENTS.md`
- Targeted Java snippet compile checks where examples are extracted.
- `./mvnw -pl simplypages test` and `./mvnw -pl demo test` if docs branch includes code or build docs changes that can affect resource copy.

# Exit Criteria

- Public examples compile or are explicitly pseudo-code.
- Version/JDK guidance is coherent.
- Docs and AGENTS navigation do not conflict.
- Deploy risks are tracked in a dedicated plan/bug state instead of stale contradictory records.
