[Previous](../fundamentals/03-css-fundamentals.md) | [Index](../INDEX.md)

# Getting started

Start with a local build, learn rendering in two small programs, then serve that
HTML over HTTP. Afterward, choose the task guide that fits your application.

## 1. Clone and build locally

Clone the SimplyPages repository using its repository clone URL, or use your
existing checkout. Enter the checkout directory (usually `SimplyPages`), then run:

```bash
java -version
./mvnw -pl simplypages install
```

Use **JDK 21 or later** for both build and runtime. The checksum-pinned Maven wrapper is
the supported repository build entry point; it installs
`io.mindspice:simplypages:1.2.1` into your local Maven repository. GitHub release `v1.2.1`
provides library artifacts; this version is not published to Maven Central.

The library and repository site both target Java 21 bytecode, even when built with a later JDK.
The site uses Spring Boot **4.1.1**. A separate application may choose its own host; Spring is not a
core rendering dependency. See [build requirements and assets](../reference/build-and-assets.md)
for the Maven/JDK pins and resource setup.

This is a controlled beta. Read the [known issues and support boundaries](../reference/build-and-assets.md#beta-status-known-issues-and-support).
The current site examples default to loopback and are nondurable. Editing modes are not
identity/authorization, and its chat session ownership is not authentication.
Do not expose it as a production application or infer production security from
its UI controls.

## 2. Learn rendering in two runnable programs

1. [Render your first page](../tutorials/first-page.md): create a separate Maven
   project with the supplied complete POM and Java class, then inspect escaped HTML.
2. [Render request data with slots](../tutorials/dynamic-page.md): use the same
   project to render a reusable template with independent request contexts.

These lessons need no HTTP service or demo classes. They establish what a page,
component, template, and render context do before adding an application host.

## 3. Put the HTML behind HTTP

Continue with [Serve full pages and HTMX fragments](../how-to/serve-pages-and-fragments.md)
inside an existing Spring MVC application. It supplies the complete controller,
full-document and fragment routes, framework/WebJar asset setup, and target/swap
rules. Confirm direct navigation and fragment replacement before adding features.

To browse this checkout's site and documentation instead of creating a
host, run these commands from the repository root:

```bash
./build-site.sh
java -jar site/target/simplypages-site.jar
```

Open `http://localhost:8080/docs`. The site serves Markdown documentation there and
built API documentation at `http://localhost:8080/javadocs/index.html`.
The build script skips tests; it verifies packaged documentation and API inventory and bytes.
The site is not the security layer required by the application recipes below, which are not
installed sandbox routes.

## 4. Choose an application task

- [Build a Markdown blog](../how-to/build-a-markdown-blog.md): articles on disk,
  pagination, and ordinary/HTMX navigation.
- [Build a forum browser](../how-to/build-a-forum.md): topics and comments from
  ordered application data, without pretend writing actions.
- [Build user-editable pages](../how-to/build-user-editable-pages.md): an
  authorized edit modal, server validation, retained drafts, and saved view updates.
- [Add chat](../how-to/add-chat.md): an explicitly local session transcript with
  a real POST composer and HTMX polling, not an LLM or durable messaging backend.
- [Build a dashboard](../how-to/build-a-dashboard.md): metrics, table, and cards
  rendered from one immutable snapshot.
- [Handle form submissions](../how-to/handle-form-submissions.md): validated
  ordinary/HTMX posts with field errors and preserved values.

Each guide states what you must already supply. In particular, the editing and
form guides require real application identity and CSRF middleware; SimplyPages
does not install them. Do not disable existing security to make an example run.

## Existing walkthroughs

These topic URLs remain available for their longer explanations and examples:

1. [Installation and first static page](01-installation-and-first-static-page.md)
2. [Dynamic pages with SlotKey and RenderContext](02-dynamic-pages-with-slotkey-rendercontext.md)
3. [Editing system first implementation](03-editing-system-first-implementation.md)

For a bounded working edit controller with explicit security prerequisites, use
[Build user-editable pages](../how-to/build-user-editable-pages.md).
The [historical 1.0.1 migration notes](../operations/04-migrating-to-1.0.1.md) do not
change the current checkout's `1.2.1` version or JDK 21 minimum.

[Documentation home](../README.md) · [Tutorials](../tutorials/README.md) · [How-to guides](../how-to/README.md)
