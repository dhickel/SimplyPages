# Editing Package Agent Guide

## Purpose
Owns editing contracts and reusable helpers for inline/module editing flows.

## Owns
- Editing interfaces (`Editable`, deprecated `Editable`)
- Edit helpers/builders (`EditFormBuilder`, `EditModalBuilder`)
- Validation/auth wrappers (`ValidationResult`, `AuthWrapper`, `AuthorizationChecker`)
- Edit mode and editable-child primitives

## Invariants
- `Editable` is the primary contract; `Editable` remains deprecated compatibility.
- Validation must be explicit and non-throwing for normal user errors.
- Helper outputs must be HTMX-friendly for modal/OOB workflows.

## Do
- Keep editing abstractions framework-generic (not app-domain specific).
- Preserve compatibility where deprecated interfaces still exist.
- Keep auth wrapper behavior explicit and testable.

## Do Not
- Hardcode app-level authorization policy here.
- Mix HTTP controller concerns into low-level editing contracts.

## Common Pitfalls
- Breaking expected field names between form builders and apply/validate paths.
- Treating edit flows as trusted input.
- Forgetting compatibility with existing edit interfaces.

## Required Tests
- `EditableTest`, `EditFormBuilderTest`, `EditModalBuilderTest`
- `ValidationResultTest`, `AuthWrapperTest`
- Validation success/failure coverage for new edit flow behavior

## Dependencies
- Depends on `core`, `components`, and module wrappers where needed.
- Keep no dependency on demo-specific services/controllers.

## Maintenance Requirement
Keep this file updated whenever editing contracts, modal flow, or validation behavior changes.

See root `AGENTS.md` for global standards.
