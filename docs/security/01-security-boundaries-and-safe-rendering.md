[Previous](../patterns/04-editing-workflows-owner-user-approval.md) | [Index](../INDEX.md)

# Security Boundaries and Safe Rendering

## Framework Responsibilities

SimplyPages provides safe defaults in core rendering paths.

- Text output is escaped by default.
- Attribute rendering is encoded.
- Slot text output is escaped.

## Application Responsibilities

Your application must enforce:

- Authentication
- Authorization
- CSRF protections
- Input validation
- Business authorization checks

## Unsafe HTML

`withUnsafeHtml(...)` is intentionally dangerous.
Use only with trusted, sanitized sources.
Never pass raw user input.

## HTMX-Specific Security

1. Apply the same auth checks on HTMX endpoints as full-page endpoints.
2. Include CSRF token handling on state-changing requests.
3. Return controlled HTML fragments on auth failures.

## Quick Checklist

- No untrusted input to `withUnsafeHtml`.
- Validation before persistence.
- Authorization before edit/delete/update.
- No hidden admin operations behind client-only controls.
