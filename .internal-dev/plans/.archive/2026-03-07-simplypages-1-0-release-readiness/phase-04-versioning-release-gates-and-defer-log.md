# Context

The release is targeted for first production cut and requires:
- explicit version alignment
- hard test/build gate confirmation
- clear documentation of intentionally deferred items

This phase ensures release execution is auditable and does not rely on implied decisions.

# Goal

Finalize the release branch as a `1.0.0`-aligned, fully validated candidate with explicit defer rationale for out-of-scope recommendations.

# In Scope

- Maven version alignment to `1.0.0`
- Full release gate command execution and result capture
- Deferred item record for excluded QOL/recommendations
- Docs/API consistency verification after all prior phases

# Out of Scope

- Implementing trace logging
- Implementing deferred QOL:
  - #3 `copy()/freeze()`
  - #5 null-safe varargs class joining
  - #6 broad Java 18+ javadoc snippet rollout

# Implementation Steps

1. Align versions
- Update root parent POM version to `1.0.0`.
- Update `simplypages` module version to `1.0.0`.
- Update `demo` artifact version and internal `simplypages` dependency reference to `1.0.0`.
- Update docs/readme dependency snippets where version literals are shown.

2. Run release gates
- Execute and record outcomes for:
  - `./mvnw -pl simplypages test`
  - `./mvnw -pl demo test`
  - `./mvnw clean install`
- Fail-fast rule: any failing gate blocks release until corrected.

3. Consistency and drift checks
- Verify docs changed in prior phases match actual APIs and behavior.
- Verify no package-level `AGENTS.md` drift due changed contracts.
- If AGENTS updates are not required, explicitly note why.

4. Deferred-item record
- Create explicit defer record in internal-dev outputs for:
  - trace logging recommendation
  - QOL #3/#5/#6
- Include reason: defer to protect 1.0 stability and avoid high-risk abstraction expansion.

5. Release summary artifact
- Produce final implementation summary with:
  - completed phases
  - gate results
  - deferred list
  - known residual risk (if any)

## Edge Cases

- Partial version alignment can pass local compile but break downstream consumer expectation.
- Demo dependency mismatch can accidentally resolve wrong artifact in non-reactor environments.
- Doc snippets can lag API names if not validated post-change.

## Gotchas

- Do not skip `clean install`; reactor-only module tests are insufficient as final gate.
- Do not rely on memory for deferred items; record them explicitly.
- Do not silently defer without rationale tied to 1.0 goals.

## Assumptions

- `1.0.0` is final release target for this cycle.
- Prior phases are merged or available in branch before gate run.

## Best Practices

- Keep release decision binary and evidence-based.
- Treat docs as part of release contract, not post-release cleanup.
- Preserve no-bullshit scope discipline: ship stable core, defer high-risk extras.

## Style Fit

- Predictable contracts over rushed feature expansion.
- Strong test/documentation alignment.
- Explicit risk ownership for deferred work.

# Validation

- Full gate command success required:
  - `./mvnw -pl simplypages test`
  - `./mvnw -pl demo test`
  - `./mvnw clean install`

Additional checks:
- Confirm docs render and links remain valid in demo doc-serving path.
- Confirm release notes/defer entries are present in internal-dev artifacts.

# Exit Criteria

- All relevant POM versions aligned to `1.0.0`.
- All gate commands pass.
- Deferred items are explicitly documented with rationale.
- Release candidate can be marked external `Go` based on resolved High/Medium findings and completed scoped work.
