# Forms Components Agent Guide

## Purpose
Owns form input and form container components.

## Owns
- `Form`, `TextInput`, `TextArea`, `Select`, `Checkbox`, `RadioGroup`, `Button`

## Invariants
- Form element APIs remain fluent and typed.
- Name/value/attribute rendering stays HTML-safe.
- Width helper methods on form components remain valid and validated by core style rules.
- Input type helpers map to correct HTML input types.

## Do
- Preserve ergonomics for common form tasks.
- Keep generated markup semantically correct for HTML forms.
- Add options with predictable defaults.

## Do Not
- Implement server-side business validation here.
- Bypass safe attribute rendering.

## Common Pitfalls
- Breaking attribute names required by browsers (`required`, `maxlength`, etc.).
- Inconsistent button/form class naming that breaks framework CSS.
- Forgetting compatibility with HTMX form usage patterns.

## Required Tests
- `FormTest`, `TextInputTest`, `TextAreaTest`, `SelectTest`, `CheckboxTest`, `RadioGroupTest`, `ButtonTest`
- Regression tests for new fluent methods and attribute rendering

## Dependencies
- Depends on `core` and shared components.
- Do not depend on `modules` or demo controller logic.

## Maintenance Requirement
Keep this file updated whenever form component APIs, attributes, or default behaviors change.

See root `AGENTS.md` for global standards.
