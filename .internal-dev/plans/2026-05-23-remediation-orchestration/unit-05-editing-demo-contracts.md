# Context

Editing review found framework correctness bugs and demo endpoint gaps: `EditableRow.wrap` drops wrapped rows, `ValidationResult` is mutable despite immutability docs, ids are interpolated into DOM/URL paths, editing demo permissions are UI-only, nested child URLs point to missing endpoints, and demo state is weakly bounded.

# Goal

Make editing framework behavior correct and make demo editing routes teach production-safe endpoint enforcement.

# In Scope

- `EditableRow.wrap` preservation.
- `ValidationResult` defensive copying and unmodifiable errors.
- Identifier/path segment validation or encoding for editing IDs.
- Demo endpoint authorization matching UI permissions.
- Nested child endpoint implementation or removal of emitted child action URLs.
- Bounds checks for insert-row positions.
- Remove manual double escaping in demo where framework rendering owns escaping.
- Docs updates for editing save auth/CSRF and API names.

# Out of Scope

- Persistent storage backend.
- Full auth system for the demo.
- Large frontend redesign.

# Implementation Steps

1. Framework tests and fixes.
   - `EditableRowTest`: wrapped row content/configuration is preserved.
   - `ValidationResultTest`: caller mutation does not affect stored errors; `getErrors()` is unmodifiable.
   - Add id/path validation tests for page, row, module, and child ids.
   - Keep valid existing edit markup unchanged except corrected wrapped-row output.

2. Demo authorization.
   - Centralize permission lookup in `EditingDemoController` or extracted service.
   - Apply permission checks in edit/save/delete endpoints, not only `AuthWrapper`.
   - Return stable 403 or disabled fragment for unauthorized direct calls.

3. Nested child behavior.
   - Either implement child edit/delete routes for `SimpleListModule` or stop emitting child URLs in this demo.
   - Prefer implementation if it is small and testable; otherwise document unsupported nested edits.

4. Demo state robustness.
   - Bounds-check `insert-row/{position}`.
   - Cap user-submitted fields where appropriate.
   - Avoid manual HTML escaping before framework renderers.

5. Playwright validation.
   - Run local demo.
   - Verify editing page load, allowed edit/save, denied locked edit direct endpoint, nested child action behavior, insert row bounds, and no obvious visual regressions.

6. Docs.
   - Fix `vr.errors()` to `getErrors()` / `getErrorsAsString()`.
   - Add endpoint authorization and CSRF responsibilities to examples.

# Validation

- `./mvnw -pl simplypages -Dtest=EditableRowTest,ValidationResultTest,EditablePageTest,EditModalBuilderTest test`
- `./mvnw -pl demo -Dtest=EditingOobIntegrationTest test`
- `./mvnw -pl simplypages test`
- `./mvnw -pl demo test`
- Playwright browser validation for `/editing-demo`.
- Live deploy/browser validation before finalizing if code fixes affect live demo behavior.

# Exit Criteria

- Direct endpoints enforce permissions.
- Wrapped rows preserve content/config.
- Validation results are immutable.
- Demo editing docs/examples compile and state production responsibilities.
