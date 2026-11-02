# SimplyPages documentation

SimplyPages builds server-rendered HTML in Java. Start with the task you want to accomplish.
This documentation follows [Diátaxis](https://diataxis.fr/): learning through tutorials,
solving tasks with how-to guides, looking up reference facts, and understanding design through explanation.

| Your need | Start here |
| --- | --- |
| Set up a checkout and follow the learning path | [Getting started](getting-started/README.md) |
| Learn by running a small example | [Tutorials](tutorials/README.md) |
| Build a blog, forum, editable page, chat panel, dashboard, or form | [How-to guides](how-to/README.md) |
| Look up APIs, defaults, assets, and build requirements | [Reference](reference/README.md) |
| Understand rendering, reuse, and security boundaries | [Explanation](explanation/README.md) |
| Browse every existing guide by topic | [Complete index](INDEX.md) |

## Start here

1. [Getting started](getting-started/README.md): clone/local build and host requirements.
2. [First page](tutorials/first-page.md), then [dynamic page](tutorials/dynamic-page.md):
   two complete Java programs with a local `1.2.1` dependency.
3. [Serve full pages and HTMX fragments](how-to/serve-pages-and-fragments.md): add
   a real HTTP host, assets, and correct response targets.
4. Choose a task below. The guides extend your existing application; they are not
   six new tutorial applications or automatically installed sandbox endpoints.

## Build something useful

- [Markdown blog](how-to/build-a-markdown-blog.md): directory-backed articles and pagination.
- [Forum browser](how-to/build-a-forum.md): topics and paginated comments.
- [User-editable pages](how-to/build-user-editable-pages.md): owner-authorized modal editing.
- [Chat panel](how-to/add-chat.md): session-local messages with working HTMX polling.
- [Dashboard](how-to/build-a-dashboard.md): metrics, table, and responsive cards.
- [Server-validated forms](how-to/handle-form-submissions.md): errors, retained input, and confirmation.

SimplyPages supplies rendering and UI composition. Your application owns identity,
authorization, CSRF verification, persistence, and transport. Each task guide states
its prerequisites and whether its example state is local or nondurable.

## Requirements and version

The checkout builds `io.mindspice:simplypages:1.2.1`. Use **JDK 21 or later** to build and run it.
Install the library locally with `./mvnw -pl simplypages install`. GitHub release `v1.2.1`
provides the library artifacts; they are not published to Maven Central. The library and
Spring Boot 4.1.1 site both target Java 21 bytecode.

This is a controlled beta. Read the [known issues and support boundaries](reference/build-and-assets.md#beta-status-known-issues-and-support)
for open defects, the local Chromium baseline, and current site limitations before adoption.

## Read online or locally

These Markdown pages work in repository viewers and in the site's `/docs` browser.
Run `./build-site.sh` and then `java -jar site/target/simplypages-site.jar`; open
[local documentation](http://localhost:8080/docs), the [demo catalog](http://localhost:8080/demos),
or the [Java API entry](http://localhost:8080/api), which links to native generated Javadocs.
The local build skips tests and verifies the packaged Markdown and generated Javadoc inventories
and bytes in the site JAR, so those resources do not require a source checkout.

Existing topic-based pages remain available at their original paths. The four sections above
provide a focused reading path through those pages and the new runnable examples.
The historical [1.0.1 migration notes](operations/04-migrating-to-1.0.1.md) remain available;
they do not replace the current contracts.
