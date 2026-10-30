# How-to guides

Choose a concrete task after you can compose and render a SimplyPages component.
New to the library? Follow [Getting started](../getting-started/README.md): the two
[runnable tutorials](../tutorials/README.md), then the
[HTTP serving guide](serve-pages-and-fragments.md).

## Build an application feature

Each guide supplies a complete controller, concrete data/validation, response
contracts, and checks you can perform in your application. These are task recipes,
not six separate tutorial applications or bundled demo routes.

| Task | What you will build | Application boundary |
| --- | --- | --- |
| [Build a Markdown blog](build-a-markdown-blog.md) | Directory-backed articles, paginated index, full/HX navigation | Filesystem publishing and refresh policy |
| [Build a forum browser](build-a-forum.md) | Read-only topics and paginated comments | Ordered application data; no write endpoints or accounts |
| [Build user-editable pages](build-user-editable-pages.md) | Owner-only briefing edit modal with validated saves | Existing identity/CSRF integration; nondurable example storage |
| [Add chat](add-chat.md) | Session-local composer, transcript, and real HTMX polling | Local, unauthenticated, nondurable; no assistant or shared-room service |
| [Build a dashboard](build-a-dashboard.md) | Summary metrics, queue table, and responsive cards | One immutable example snapshot; application owns live queries |
| [Handle form submissions](handle-form-submissions.md) | Server-validated address preview with retained drafts | Existing identity/CSRF integration; nothing sent or persisted |

Use SimplyPages **1.2** with **JDK 25**, installed from this checkout. The guides
assume an existing Spring MVC host and working framework/WebJar resources. Helpers
and modules do not supply authentication, authorization, CSRF verification,
persistence, or transport servers. Keep the security prerequisites in each guide.

## Serving, publishing, and maintenance

- [Serve full pages and HTMX fragments](serve-pages-and-fragments.md)
- [Implement an authorized editing flow](edit-content.md)
- [Publish a Markdown directory](publish-markdown.md)
- [Build, test, and browse the documentation](build-test-and-document.md)
- [Compose a shell with navigation and assets](../core/06-shell-project-structure-and-asset-load-chain.md)
- [Customize forum rendering](../patterns/05-forum-helper-implementation-and-customization.md)
- [Connect chat to SSE or WebSocket hooks](../patterns/06-chat-helper-sse-and-ws-hooks.md)
- [Scope and authorize chat conversations](../patterns/07-chat-conversation-scoping-and-authorization-patterns.md)
- [Cache dynamic fragments](../patterns/02-dynamic-fragment-caching-patterns.md)
- [Write component and module tests](../operations/03-writing-tests-for-components-and-modules.md)

For API facts and defaults, use [Reference](../reference/README.md). For the reasons
behind rendering and security boundaries, use [Explanation](../explanation/README.md).

[Documentation home](../README.md) · [Complete index](../INDEX.md)
