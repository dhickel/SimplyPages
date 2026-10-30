# Tutorials

Follow these lessons in order. Each produces observable HTML and includes complete Java source.
For clone/local-build instructions and runtime requirements, start at
[Getting started](../getting-started/README.md). Both lessons use SimplyPages **1.2**
from your local Maven repository and **JDK 25**; they do not assume public publication.

1. [Render your first page](first-page.md): install SimplyPages, create a tiny Maven application,
   compose a page, and inspect its output.
2. [Render request data with slots](dynamic-page.md): reuse one template with two independent
   contexts and observe safe escaping.

Afterward, [serve a page and HTMX fragment](../how-to/serve-pages-and-fragments.md)
in your Spring MVC host. Then choose a task:

- [Markdown blog](../how-to/build-a-markdown-blog.md)
- [Forum browser](../how-to/build-a-forum.md)
- [User-editable pages](../how-to/build-user-editable-pages.md)
- [Chat panel with polling](../how-to/add-chat.md)
- [Dashboard](../how-to/build-a-dashboard.md)
- [Server-validated forms](../how-to/handle-form-submissions.md)

These how-tos provide application controllers with explicit security and state
boundaries; they are not additional standalone tutorial apps. The existing
[editing walkthrough](../getting-started/03-editing-system-first-implementation.md)
remains available for more context.

[Documentation home](../README.md) · [Complete index](../INDEX.md)
