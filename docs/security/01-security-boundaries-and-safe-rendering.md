[Previous](../patterns/04-editing-workflows-owner-user-approval.md) | [Index](../INDEX.md)

# Security Boundaries and Safe Rendering

## Framework Responsibilities

SimplyPages provides safe defaults in core rendering paths.

- Text output is escaped by default.
- Attribute names and tag names are validated before rendering.
- Attribute values are encoded before rendering.
- Link-like component URLs are validated through the shared `SafeUrl` helper.
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

## URL Safety

Navigation and builder helpers that create anchors validate href values before rendering.

Allowed hyperlink values:

- Empty or null values, where the component supports them
- Fragments, query-only URLs, root-relative URLs, and `./` or `../` relative URLs
- Protocol-relative URLs
- Absolute `http`, `https`, `mailto`, and `tel` URLs

Disallowed values include executable or document-embedding schemes such as `javascript:`,
`data:`, and unsupported absolute schemes such as `ftp:`.

Background-image helpers validate the URL before embedding it in CSS and reject CSS breakout
characters such as quotes, parentheses, semicolons, braces, backslashes, and control characters.
Use normal web image URLs or relative image paths for `HeroModule.withBackgroundImage(...)` and
`BannerBuilder.withBackgroundImage(...)`.

## HTMX-Specific Security

1. Apply the same auth checks on HTMX endpoints as full-page endpoints.
2. Include CSRF token handling on state-changing requests.
3. Return controlled HTML fragments on auth failures.

## Quick Checklist

- No untrusted input to `withUnsafeHtml`.
- No unvalidated raw URLs when building anchors manually.
- Validation before persistence.
- Authorization before edit/delete/update.
- No hidden admin operations behind client-only controls.
