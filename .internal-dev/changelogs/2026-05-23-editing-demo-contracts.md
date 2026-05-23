# 2026-05-23 Editing and Demo Contract Hardening

## Summary

- Hardened editing path identifiers for editable pages, rows, modules, and modal module IDs.
- Preserved wrapped `Row` configuration/content in `EditableRow` by rendering from a shallow row copy.
- Made `ValidationResult` error collections defensive and immutable.
- URL-encoded child edit/delete identifiers when expanding `EditModalBuilder` child URL templates.
- Enforced editing-demo permissions at HTMX endpoints, added nested child edit/save/delete endpoints, bounded row insertion, and removed pre-escaping from saved demo text.

## Tests

- `./mvnw -pl simplypages -Dtest=EditableRowTest,EditablePageTest,ValidationResultTest,EditModalBuilderTest test`
- `./mvnw -pl demo -am -Dtest=EditingOobIntegrationTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test`
- `./mvnw -pl simplypages test`
- `./mvnw -pl demo -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test`
- Local browser validation against `http://127.0.0.1:8082/editing-demo` with Playwright.
- `./deploy.sh host 192.168.1.113 host` built successfully but remote deployment did not complete because SSH to `192.168.1.113:22` was refused.

## Follow-Up

- Threading and race-condition tests for request-shared objects are intentionally deferred to the final standalone PR requested by the user.
