# Publish a Markdown directory

Use a directory you control. Add `.md` files, optionally starting with frontmatter:

```markdown
---
title: Release notes
slug: release-notes
date: 2026-09-08
author: Documentation team
tags: release, java
draft: false
---
# Release notes

## Changes

The release is ready for review.
```

Generate a bundle at startup or after a content change:

```java
var site = io.mindspice.simplypages.components.content.StaticContentSiteBuilder.create()
    .addSection(io.mindspice.simplypages.components.content.ContentSectionConfig
        .create("news", java.nio.file.Path.of("content/news"), "/news")
        .withPageSize(10))
    .build();
var bundle = site.generate();
```

Route `/news?page=2` through `bundle.routeIndex().resolveRequest("/news", "2")`.
Route `/news/release-notes` through `resolveRequest("/news/release-notes", null)`.
Render a present component inside your shell; return HTTP 404 when resolution is empty.

Inspect `bundle.warnings()` during generation. Parsing is best-effort, not a complete YAML
implementation. Drafts are excluded; duplicate slugs are made unique. Missing directories
produce warnings and an empty section. Markdown escapes raw HTML by default.

The built site snapshots section settings. To change settings, build another site. Generate
a replacement bundle and publish it atomically in your application; the helper does not
watch files, schedule refreshes, or maintain an HTTP cache. Cache rendered HTML when you
want immutable output shared across requests.

See [frontmatter and route reference](../reference/content-helper-api-reference.md) and
[the detailed content pipeline](../patterns/08-static-content-helper-markdown-directory-pipeline.md).

[All how-to guides](README.md)
