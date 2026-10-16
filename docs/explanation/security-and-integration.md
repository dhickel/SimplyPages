# Security and integration boundaries

SimplyPages handles HTML construction, while the host application handles who may perform
an operation and what input is allowed for that operation. Keeping those responsibilities
explicit helps prevent a rendering convenience from being mistaken for access control.

## Escaping has a specific context

Ordinary text is HTML-escaped. Attribute values are attribute-encoded, and malformed names
are rejected. Markdown escapes raw HTML and sanitizes link destinations by default.
These protections keep text from becoming unintended markup.

Encoding an attribute does not validate the behavior of that attribute. An encoded
`javascript:` URL is still a JavaScript URL, and an encoded event handler is still code.
`Link` constructors/factory/`withHref` classify http/https/mailto/tel and relative forms;
`Image` constructors/factories/`withSrc` instead reject javascript and non-image data URLs
while retaining other schemes. Their classification view trims boundary C0/ASCII space,
removes TAB/LF/CR and folds ASCII case independently of locale. Supported obfuscated
spellings are accepted, but getters and encoded attributes preserve the original input.
There is no entity/percent decoding, Unicode normalization, destination authorization or
payload/MIME inspection. An accepted image source is not a safety or loadability guarantee.

The policy belongs to those methods, not every anchor or image helper. Dropdown/account/nav
entries and public nested nav constructors, top/side navigation builder links and aliases,
Hero CTAs, Card/Gallery images, media sources/posters and form/HTMX endpoints require
application-controlled or separately context-validated destinations. Generic attribute
replacement can bypass a typed validator without updating its getter. AccountBar link
conveniences already delegate to Link at configuration; Banner's generated ordinary horizontal
image delegates to Image at build. Their dynamic endpoints and CSS backgrounds remain separate.
Existing editing complete URLs remain caller-owned; endpoint-template placement and ID encoding
do not establish a destination allowlist.

`HtmlTag.withAttribute`, event-handler values and raw style strings require application
control or separate validation for the actual context. `addStyle` checks property names and
rejects declaration breakout, not semantic CSS/URL values. `addTrustedStyle`, `Style` inline
fragments and raw style attributes retain trusted separators; width helpers impose a separate
restricted size grammar. Banner/Hero background URLs interpolate single-quoted CSS `url()`;
Image validation alone cannot secure that CSS syntax. Banner/Hero/AccountBar colors,
Spacer custom spacing and ProgressBar height also use raw CSS assembly. Encoding those
attributes is not CSS sanitization.

`RawHtml`, `withUnsafeHtml`, `putCompiled`, and unsafe Markdown opt into trust. Only pass
trusted generated or sanitized HTML. Template compilation does not sanitize these paths.

## Authorization belongs at the operation

Hiding an edit button changes presentation. It does not protect the save or delete endpoint.
`AuthWrapper` can conditionally execute a callback, but the application supplies the
identity and permission check. Check authorization again for each state-changing request,
validate CSRF through the HTTP layer, and validate domain data before persistence.

Forum and chat helpers render application-supplied data. Their transport hooks do not create
a secure session, store history, authorize conversations, or implement moderation. Applications
must filter data according to the authenticated user's access before rendering or streaming.

## Deployment and assets

The core produces strings and static resources. The HTTP host chooses response headers,
content types, security policy, resource mappings, and caching. A demo controller illustrates
an integration; it is not a production security configuration.

See [safe-rendering guidance](../security/01-security-boundaries-and-safe-rendering.md),
[authorization integration](../security/02-authwrapper-authorizationchecker-integration.md),
and [the editing guide](../how-to/edit-content.md).

[All explanation](README.md)
