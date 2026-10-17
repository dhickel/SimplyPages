[Previous](../patterns/04-editing-workflows-owner-user-approval.md) | [Index](../INDEX.md)

# Security Boundaries and Safe Rendering

## Framework Responsibilities

SimplyPages provides safe defaults in core rendering paths.

- Text output is escaped by default.
- Attribute values are encoded; malformed tag/attribute names are rejected.
- Encoding does not validate URL schemes, event-handler code, or raw style attributes.
  Generic attribute values remain application-controlled.
- Slot text output is escaped.
- Inline styles added through `addStyle(...)` reject declaration-breakout characters and replace only exact property names.

## URL classification is method-specific

`Link` constructors, `create` and `withHref` allow `http`, `https`, `mailto`, `tel`
and the existing relative/protocol-relative forms. `Image` constructors, factories and
`withSrc` reject `javascript:` and `data:` without an image prefix, but retain other
schemes, including custom schemes, FTP and blob URLs. A `data:image/` prefix is not
MIME or payload inspection, including for SVG.

These methods classify a view with boundary C0 controls/ASCII space trimmed, TAB/LF/CR
removed, and ASCII letters case-folded independently of locale. Thus an actual TAB in
`ht<TAB>tp:` or `data:im<TAB>age/` can produce an accepted spelling. Getters and emitted
attributes retain the original string through ordinary HTML encoding. There is no
entity/percent decoding or Unicode normalization. This is scheme classification,
not destination authorization, same-origin enforcement, SSRF protection or a promise
that a URL loads safely.

Other URL conveniences require **application-controlled or separately context-validated
values**. This includes Dropdown links; AccountWidget destinations/dynamic endpoints;
NavBar, SideNav and Breadcrumb entries (including public nested entry constructors and
utility links); TopNavBuilder/SideNavBuilder links and aliases; Hero CTA links; Card/Gallery
image sources; Audio/Video sources and video posters; Form actions/HTMX/CSRF URLs and
FormModule submit URLs. Generic `withAttribute`, inherited HTMX methods and Link's HTMX
helpers do not apply the href/src validators. Generic replacement can change a Link/Image
attribute without changing its getter. Template compilation does not add validation.

Two existing delegated paths differ: AccountBarBuilder link conveniences construct `Link`
immediately; BannerBuilder's ordinary horizontal image constructs `Image` when generated
content is built. Dynamic endpoints and CSS backgrounds do not share those checks. Default
forum avatar handling uses its existing Image/fallback path, not a new global URL policy.

## CSS values need their own trust boundary

`HtmlTag.addStyle` checks property names and rejects declaration-breakout characters in
values. It is not a semantic CSS or URL sanitizer. `addTrustedStyle` bypasses that value
check; `Style.addInlineStyle` and raw `style` attributes retain declaration separators.
Supply application-controlled values or validate for the actual CSS context. Width helpers
have a separate, narrower size-token grammar.

Banner/Hero background-image methods interpolate into a single-quoted CSS `url()` without
CSS escaping. Image validation alone does not control quotes, backslashes or declaration
syntax there. Banner background/text colors, Hero/AccountBar background colors,
`Spacer.custom` and `ProgressBar.withHeight` also assemble raw styles. HTML attribute
encoding prevents markup breakout, not CSS declaration breakout or unsafe CSS semantics.

## Application Responsibilities

Your application must enforce:

- Authentication
- Authorization
- CSRF protections
- Input validation
- Business authorization checks

For editing flows, UI-level affordances are not an authorization boundary. Controllers or service
handlers must re-check edit/delete/create permissions on every HTMX endpoint, including nested child
edit endpoints.

## Unsafe HTML

`withUnsafeHtml(...)` is intentionally dangerous.
Use only with trusted, sanitized sources.
Never pass raw user input.

`addTrustedStyle(...)` is also a trusted-input path. Use it only when a known-safe value needs CSS syntax that the hardened `addStyle(...)` path rejects, such as a trusted data URL.

## URL Safety

Apply the [method-specific classification rules](#url-classification-is-method-specific) rather
than assuming every helper uses Link's allowlist. In particular, Hero/Banner CSS backgrounds
require application-owned CSS syntax and destination validation as described
[above](#css-values-need-their-own-trust-boundary).

## HTMX-Specific Security

1. Apply the same auth checks on HTMX endpoints as full-page endpoints.
2. Include CSRF token handling on state-changing requests.
3. Return controlled HTML fragments on auth failures.
4. Validate path variables and reject invalid row positions or child identifiers before mutation.
5. Do not pre-escape persisted form text; rely on SimplyPages render paths to escape plain text
   once at output time.

## Quick Checklist

- No untrusted input to `withUnsafeHtml`.
- No unvalidated raw URLs when building anchors manually.
- Validation before persistence.
- Authorization before edit/delete/update.
- No hidden admin operations behind client-only controls.
