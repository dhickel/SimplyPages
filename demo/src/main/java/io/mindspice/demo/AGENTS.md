# Demo Package Agent Guide

## Purpose
Owns Spring Boot demo app wiring and integration examples for SimplyPages.

## Owns
- Demo application bootstrap and controllers in this package
- Example integration routes showcasing framework features

## Invariants
- Demo remains illustrative and runnable.
- Demo code should reflect real framework usage patterns.
- Demo must not redefine framework internals.

## Do
- Keep demo endpoints focused on examples and docs support.
- Update demo flows when framework APIs change.
- Maintain clarity over completeness.

## Do Not
- Move framework logic into demo package as a workaround.
- Treat demo patterns as hard framework requirements unless documented.

## Common Pitfalls
- Demo drift after framework API changes.
- Hardcoding outdated versions/paths.
- Coupling demo controllers too tightly to transient test data.

## Required Tests
- Demo integration tests covering major editing/HTMX flows
- Sanity checks for any new major demo route

## Dependencies
- Depends on `simplypages` published module APIs.
- No dependency from framework modules back to demo.

## Maintenance Requirement
Keep this file updated whenever demo architecture or integration patterns change.

See root `AGENTS.md` for global standards.
