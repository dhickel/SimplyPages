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
Use `Link`'s validated URL methods for user-supplied navigation destinations. Generic
`HtmlTag.withAttribute`, event-handler values, application URL schemes, and raw style strings
require application control. `addStyle` rejects declaration breakout; it is not a complete
CSS policy engine.

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
