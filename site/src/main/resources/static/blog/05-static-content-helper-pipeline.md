---
title: Static Content Helper Pipeline Walkthrough
summary: Directory-backed markdown, frontmatter metadata, paginated index pages, and detail routes.
date: 2026-03-07T10:45
author: Docs Team
tags: content,markdown,pagination
slug: static-content-helper-pipeline
---
# Static Content Helper Pipeline Walkthrough

The static content helper turns markdown directories into route-indexed list and detail pages.

## Frontmatter DSL

Use metadata keys like `title`, `summary`, `date`, `tags`, `slug`, and `draft`.

### Query Pagination

Section roots render page one at `/blog` and later pages via `/blog?page=n`.

#### Route Index Wiring

`ContentRouteIndex.resolveRequest(path, pageParam)` maps request paths to generated components.

## Safe by Default

Markdown HTML is escaped unless unsafe mode is explicitly enabled per section.
