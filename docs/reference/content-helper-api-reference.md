[Previous](chat-helper-api-reference.md) | [Index](../INDEX.md) | [Next](editing-api-reference.md)

# Static Content Helper API Reference

This page summarizes the opinionated markdown directory helper APIs.

## Core Entry Points

- `StaticContentSiteBuilder`
- `StaticContentSite`
- `ContentSectionConfig`
- `ContentSiteBundle`
- `ContentSectionSite`
- `ContentRouteIndex`
- `ContentEntryRecord`
- `ContentWarning`
- `ContentListItemComponent`
- `DefaultContentListItemComponent`

## `StaticContentSiteBuilder`

Factory:

- `StaticContentSiteBuilder.create()`

Fluent methods:

- `addSection(ContentSectionConfig)`
- `build()` -> `StaticContentSite`

## `ContentSectionConfig`

Factory:

- `ContentSectionConfig.create(String sectionKey, Path directoryPath, String basePath)`

Fluent methods:

- `withSectionTitle(String)`
- `withPageSize(int)`
- `withMaxDepth(int)`
- `withAllowUnsafeMarkdown(boolean)`
- `allowUnsafeMarkdown()`
- `withListItemComponentSupplier(Supplier<? extends ContentListItemComponent>)`

Defaults:

- `pageSize=10`
- `maxDepth=32`
- markdown safe mode enabled (`allowUnsafeMarkdown=false`)
- list item supplier uses `DefaultContentListItemComponent::create`

## `StaticContentSite`

Render method:

- `generate()` -> `ContentSiteBundle`

Behavior:

- Scans configured directories for `.md` files.
- Parses best-effort frontmatter (`title`, `summary`, `date`, `author`, `tags`, `slug`, `draft`).
- Excludes `draft: true` entries from generated pages.
- Sorts entries by date descending, then slug.
- Generates index pages with query pagination (`/section?page=n`).
- Generates detail pages with sticky TOC from `h2-h4` headings.

## `ContentSiteBundle`

Fields:

- `sectionsByKey()`
- `routeIndex()`
- `warnings()`

## `ContentRouteIndex`

Route helpers:

- `indexRoute(String sectionKey, int page)`
- `detailRoute(String sectionKey, String slug)`

Lookup helpers:

- `resolveIndex(String sectionKey, int page)`
- `resolveDetail(String sectionKey, String slug)`
- `resolveRequest(String requestPath, String pageParamValue)`
- `resolveRoute(String route)`

Exports:

- `indexPagesByRoute()`
- `detailPagesByRoute()`

## `ContentListItemComponent`

Renderer-populated fluent methods:

- `withSlug(String)`
- `withRoute(String)`
- `withTitle(String)`
- `withSummary(String)`
- `withAuthor(String)`
- `withPublishedAt(String)`
- `withTags(List<String>)`
