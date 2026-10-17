[Previous](07-chat-conversation-scoping-and-authorization-patterns.md) | [Index](../INDEX.md)

# Static Content Helper Markdown Directory Pipeline

This guide defines the integration model for generating static markdown section pages from directory content.

## What This Solves

- Opinionated section index rendering from `.md` files in configured directories
- Query-parameter pagination for section roots (`/blogs?page=2`)
- Detail page rendering with sticky heading TOC (`h2-h4`)
- Route lookup helper so applications can map request path/query to pre-rendered `Component`s

## Design Rules

- Helper generation is stateless per call.
- Application owns cache/refresh lifecycle and HTTP routing.
- Frontmatter parsing is best-effort; invalid values become warnings.
- Markdown is escaped by default; unsafe mode is explicit per section.

## Quick Start

```java
StaticContentSite site = StaticContentSiteBuilder.create()
    .addSection(ContentSectionConfig.create("blogs", Path.of("content/blogs"), "/blogs")
        .withSectionTitle("Blog")
        .withPageSize(10)
        .withMaxDepth(4)
        .withListItemComponentSupplier(DefaultContentListItemComponent::create))
    .addSection(ContentSectionConfig.create("projects", Path.of("content/projects"), "/projects")
        .withPageSize(12))
    .build();

ContentSiteBundle bundle = site.generate();
```

## Frontmatter DSL

```md
---
title: Building the Parser
summary: Tradeoffs and internals
date: 2026-03-11T14:30
author: SimplyPages Team
tags: java,simplypages,rendering
slug: parser-internals
draft: false
---
```

Date formats accepted:

- `yyyy-MM-dd`
- `yyyy-MM-dd'T'HH:mm`

## Routing Pattern

- Section root page 1: `/blogs`
- Section root page n: `/blogs?page=n`
- Detail route: `/blogs/{slug}`

Controller-side generic lookup pattern:

```java
Optional<Component> resolved = bundle.routeIndex().resolveRequest(path, request.getParameter("page"));
```

### Inspect generation warnings

Warnings are nonfatal: generation retains every generated page and its direct section lookup.
Inspect `bundle.warnings()` at startup and after each refresh, rather than treating successful
generation as evidence that request paths do not overlap. For example:

```java
for (ContentWarning warning : bundle.warnings()) {
    System.err.printf("Content section %s, source %s: %s%n",
        warning.sectionKey(), warning.sourcePath(), warning.message());
}
```

Keep filesystem source paths in application diagnostics rather than automatically displaying
them to visitors. See the [API reference](../reference/content-helper-api-reference.md#contentsitebundle)
for warning fields and ordering.

A root section `/` containing `foo.md` publishes `/foo`. If another section uses `/foo` as its
base path, the detail shadows that section's index. The same overlap can occur at a nested
path, such as `/blogs/foo`. Detail-first resolution is unchanged: `/foo`, `/foo/`,
`/foo?page=1` and `/foo?page=2` all select the detail, masking index pagination. A nonfatal
collision warning names the route, both section keys, detail source file and index source
directory. Choose a non-overlapping section base path or published slug to make both reachable
through request lookup. `resolveIndex(sectionKey, page)` still retrieves the retained index
directly; neither route map loses its generated pages.

Diagnostics use final published routes after slug uniquification and exclude drafts. They emit
one warning per conflicting detail/index owner pair, regardless of page count. Duplicate
normalized section bases remain configuration errors; this diagnostic does not add a routing
mode or new URL normalization.

## Caching Pattern

- Build once at startup: `bundle = site.generate()`.
- Serve from `ContentRouteIndex` lookups per request.
- Rebuild bundle on deploy or app-defined refresh schedule.

## Testing Checklist

- Frontmatter best-effort warnings for invalid values
- Draft exclusion and date-desc sort order
- Query pagination route generation and lookup
- Nonfatal detail/index collision warnings with both source owners, retained direct lookups and detail-first request resolution
- Draft exclusion, final uniquified routes and repeatable collision warning counts
- Sticky TOC heading extraction (`h2-h4`) and heading anchor IDs
- Markdown raw-HTML escaping in safe mode
