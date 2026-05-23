[Previous](../patterns/04-editing-workflows-owner-user-approval.md) | [Index](../INDEX.md)

# Security Boundaries and Safe Rendering

## Framework Responsibilities

SimplyPages provides safe defaults in core rendering paths.

- Text output is escaped by default.
- Attribute names and tag names are validated before rendering.
- Attribute values are encoded before rendering.
- Slot text output is escaped.
- Inline styles added through `addStyle(...)` reject declaration-breakout characters and replace only exact property names.

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

`addTrustedStyle(...)` is also a trusted-input path. Use it only when a known-safe value needs CSS syntax that the hardened `addStyle(...)` path rejects, such as a trusted data URL.

## HTMX-Specific Security

1. Apply the same auth checks on HTMX endpoints as full-page endpoints.
2. Include CSRF token handling on state-changing requests.
3. Return controlled HTML fragments on auth failures.

## Quick Checklist

- No untrusted input to `withUnsafeHtml`.
- Validation before persistence.
- Authorization before edit/delete/update.
- No hidden admin operations behind client-only controls.
