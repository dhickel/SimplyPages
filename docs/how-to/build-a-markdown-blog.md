[How-to index](README.md) | [Learning path](../getting-started/README.md)

# Build a Markdown blog

Serve a directory of Markdown articles at `/guide/blog`, with a paginated index,
article pages, and HTMX navigation that also works as ordinary links.

## Prerequisites

- A working Spring MVC application running on JDK 25 with SimplyPages **1.1.1**
  installed locally; follow [Getting started](../getting-started/README.md) and
  [serve pages and fragments](serve-pages-and-fragments.md) first.
- Framework CSS, framework JavaScript, and the HTMX WebJar available through Spring's
  static-resource handling. `ShellBuilder` links these assets; it does not install them.
- The application mounted at `/` for the root-relative links used here.

## 1. Create two articles on disk

From the directory where you will start the application, run:

```sh
mkdir -p content/blog
```

Save this as `content/blog/first-post.md`:

```markdown
---
title: First post
summary: Start with a directory-backed blog.
date: 2026-03-10T09:00
author: Alex
tags: java, markdown
slug: first-post
draft: false
---
# First post

This blog is rendered by the server.

## Publishing

Edit the Markdown files, then rebuild the generated content.

### Links

Read the [earlier note](/guide/blog/earlier-note).
```

Save this as `content/blog/earlier-note.md`:

```markdown
---
title: Earlier note
summary: A second article makes pagination visible.
date: 2026-03-09
author: Sam
tags: notes, markdown
slug: earlier-note
draft: false
---
# Earlier note

Keep content separate from controller code.

## Storage

These files live in a filesystem directory, not in a database.
```

Frontmatter is a best-effort key/value format, not a general YAML processor.
Dates accept `yyyy-MM-dd` or `yyyy-MM-dd'T'HH:mm`; tags are comma-separated.
`draft: true` excludes an article. Use explicit, unique slugs for stable links.
Include a body heading: the frontmatter title labels the index item, while the
article's visible title comes from its Markdown.

## 2. Add the controller

Save the following as `MarkdownBlogController.java` under your application's
component-scan root. The example package is `com.example.app.guides`; change it
consistently if your application has a different root package.

The default constructor configures
`ContentSectionConfig.create("blog", Path.of("content/blog"), "/guide/blog")`.
The `Path` constructor permits the same code to read another real directory.

```java
package com.example.app.guides;

import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.components.content.ContentSectionConfig;
import io.mindspice.simplypages.components.content.ContentSiteBundle;
import io.mindspice.simplypages.components.content.StaticContentSiteBuilder;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MarkdownBlogController {
    private final ContentSiteBundle bundle;

    @Autowired
    public MarkdownBlogController() {
        this(Path.of("content/blog"));
    }

    public MarkdownBlogController(Path directory) {
        bundle = StaticContentSiteBuilder.create()
            .addSection(ContentSectionConfig.create("blog", directory, "/guide/blog")
                .withSectionTitle("Engineering blog")
                .withPageSize(1))
            .build()
            .generate();
        var logger = System.getLogger(MarkdownBlogController.class.getName());
        for (var warning : bundle.warnings()) {
            logger.log(System.Logger.Level.WARNING,
                "Content warning: " + warning.sectionKey() + " / "
                    + warning.sourcePath() + " / " + warning.message());
        }
    }

    @GetMapping(value = {"/guide/blog", "/guide/blog/**"},
        produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> blog(
        HttpServletRequest request,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        String path = request.getRequestURI()
            .substring(request.getContextPath().length());
        return render(path, request.getParameter("page"), hxRequest);
    }

    public ResponseEntity<String> render(String path, String page, String hxRequest) {
        Component content = bundle.routeIndex().resolveRequest(path, page).orElse(null);
        int status = content == null ? 404 : 200;
        if (content == null) {
            content = new HtmlTag("p").withInnerText("Blog content not found.");
        }
        HtmlTag view = new HtmlTag("div").withId("blog-view")
            .withAttribute("hx-boost", "true")
            .withAttribute("hx-target", "#blog-view")
            .withAttribute("hx-swap", "outerHTML")
            .withAttribute("hx-push-url", "true")
            .withChild(content);
        String body = "true".equals(hxRequest) ? view.render()
            : ShellBuilder.create()
                .withPageTitle("Engineering blog")
                .withContent(view)
                .build();
        return ResponseEntity.status(status)
            .header("Vary", "HX-Request")
            .contentType(MediaType.TEXT_HTML)
            .body(body);
    }
}
```

The generated index links, pagination links, and article back link are normal
anchors. The application-owned `blog-view` wrapper boosts them with HTMX.
Every successful fragment response retains that wrapper and its ID, so
`outerHTML` replacement does not destroy the next navigation target. Same-page
heading anchors remain ordinary in-document navigation.

Only the exact header value `HX-Request: true` selects a fragment. Missing,
`false`, or other values receive a full shell. `Vary: HX-Request` distinguishes
these representations for caches. Both representations use the same generated
content; fragments do not repeat document assets. Supplying initial content to
`ShellBuilder` also avoids its empty-shell `/home` auto-load behavior.

## 3. Verify routes and navigation

Start your existing application normally, then check:

1. Open `/guide/blog`. It shows **First post**, page 1 of 2, and a Next control.
2. Follow Next. The URL becomes `/guide/blog?page=2`, showing **Earlier note**.
3. Follow an article title. `/guide/blog/first-post` includes its article body
   and a heading table of contents. Test a TOC anchor and the back link.
4. Reload an article URL directly. It must still render a full HTML document,
   not an unstyled fragment.
5. Inspect HTMX requests: responses contain one `id="blog-view"`, no document
   shell, and `Vary: HX-Request`. The page must not accumulate nested wrappers.
6. Request `/guide/blog/not-a-post` or `/guide/blog?page=3`: expect HTTP 404.
   HTMX does not swap error responses by default; this example leaves the
   current view in place on a failed boosted navigation.

Absent, malformed, or nonpositive page values resolve to page 1; a positive
page beyond the generated index has no match. Article lookup takes precedence
and does not use the page query value.

For a direct Java smoke check, construct `new MarkdownBlogController(directory)`
with a `Path` containing the two files above. Call
`render("/guide/blog", null, null)`,
`render("/guide/blog", "2", "true")`, and
`render("/guide/blog/first-post", null, "true")`; each should return 200.
`render("/guide/blog/missing", null, null)` should return 404. The HTTP handler
`blog(request, hxRequest)` delegates to exactly this rendering path.

## 4. Choose the refresh and deployment policy

The controller generates one bundle at startup and performs only route lookup
and rendering per request. Editing a file does **not** update that bundle.
Restart after publishing, or implement an application-owned refresh that
builds a complete replacement bundle before publishing it to requests. Do not
mutate the generated component trees per visitor.

`Path.of("content/blog")` is resolved against the process working directory.
Mount/copy that directory into your deployment and set its permissions explicitly.
It is not a classpath lookup. If you package articles inside a JAR, your application
must materialize them into a real directory before calling the generator; see
`demo/src/main/java/io/mindspice/demo/BlogContentService.java` for an example.
That demo class and `BlogController` are source references, not library dependencies.

Review warnings in trusted application logs after each generation. Missing
content directories can produce an empty index with a warning; malformed dates,
draft values, or duplicate slugs can change publication/order/routes. Do not
expose filesystem diagnostics to visitors. Keep unsafe Markdown disabled and
review published content. A draft flag is not an access-control mechanism for
files exposed separately by your web server.

For a larger blog, increase `withPageSize(1)` and keep explicit slug ownership.
If you change `/guide/blog`, update controller mappings and all authored links
along with the section configuration.

## Related material

- [How-to index](README.md)
- [Content helper reference](../reference/content-helper-api-reference.md)
- [Directory pipeline pattern](../patterns/08-static-content-helper-markdown-directory-pipeline.md)
- [HTMX endpoint and swap patterns](../patterns/03-htmx-endpoint-and-swap-patterns.md)
- [Security boundaries](../security/01-security-boundaries-and-safe-rendering.md)
