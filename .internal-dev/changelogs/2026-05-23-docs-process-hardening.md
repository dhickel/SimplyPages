# 2026-05-23 Docs and Process Hardening

## Summary

- Fixed stale editing examples that referenced the removed `ValidationResult.errors()` API.
- Added explicit current release and Java toolchain guidance to README/getting-started docs.
- Expanded the components/modules catalog into a production-oriented API map.
- Aligned package `AGENTS.md` documentation TOCs with `docs/INDEX.md`.
- Added a deploy workflow hardening plan that captures preflight, phased deploy, and smoke-test behavior.

## Validation

- `rg -n "vr\\.errors\\(|docs/api/00-index|docs/internal/00-index|Java 17|0\\.1\\.0|SNAPSHOT" README.md docs AGENTS.md .internal-dev/AGENTS.md`
- `git diff --check`
- Spot-checked newly listed docs paths from AGENTS TOCs.
- `./mvnw -pl demo -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test`

## Follow-Up

- Threading and race-condition tests for request-shared objects remain deferred to the final standalone PR.
