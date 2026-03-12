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

## Caching Pattern

- Build once at startup: `bundle = site.generate()`.
- Serve from `ContentRouteIndex` lookups per request.
- Rebuild bundle on deploy or app-defined refresh schedule.

## Testing Checklist

- Frontmatter best-effort warnings for invalid values
- Draft exclusion and date-desc sort order
- Query pagination route generation and lookup
- Sticky TOC heading extraction (`h2-h4`) and heading anchor IDs
- Markdown raw-HTML escaping in safe mode
