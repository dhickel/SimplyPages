# Demo Pages Agent Guide

## Purpose
Owns page composition classes used by demo controllers.

## Owns
- Demo page builders/compositions under `demo.pages`

## Invariants
- Pages are examples of framework usage, not framework internals.
- Page classes should stay readable and educational.
- Compositions should reflect current recommended APIs.

## Do
- Keep page classes focused on rendering/composition.
- Prefer reusable helper patterns when examples repeat.
- Update examples when APIs evolve.

## Do Not
- Embed business/domain persistence logic in page classes.
- Depend on unstable, deprecated framework APIs without explicit note.

## Common Pitfalls
- Example code lagging behind module/component API changes.
- Overly complex page classes that hide framework concepts.
- Mixing unrelated concerns in one demo page.

## Required Tests
- Add/adjust demo integration coverage when page behavior changes materially
- Ensure key routes still render after API migrations

## Dependencies
- Depends on framework APIs and demo controllers.
- Keep direction one-way: framework should not depend on demo pages.

## Maintenance Requirement
Keep this file updated whenever demo page composition patterns or API usage changes.

See root `AGENTS.md` for global standards.

Update this file in the same change whenever package-level behavior or conventions drift.
