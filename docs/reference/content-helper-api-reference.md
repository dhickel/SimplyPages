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

Duplicate section keys and normalized base paths are rejected by `addSection`.

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
- Reports published detail/index route collisions as nonfatal warnings without removing or reassigning pages.

## `ContentSiteBundle`

Fields:

- `sectionsByKey()`
- `routeIndex()`
- `warnings()`

`warnings()` is an immutable list of nonfatal `ContentWarning` diagnostics. Inspect it after
initial generation and every refresh. A detail/index collision warning uses the detail section
key and source file as its warning context; its message names the generated route, both section
keys, detail file and index directory. It explains detail priority and masked index pagination,
and suggests choosing non-overlapping section base paths or published slugs. Paths are diagnostic
context, not text to display automatically to site visitors.

Collision diagnostics use final published routes after slug uniquification. Drafts do not
participate. Each conflicting detail/index owner pair produces one warning, not one per index
page; ordering is stable for unchanged configuration and content. Existing parse and slug warnings
are retained.

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

`resolveRequest` and `resolveRoute` give detail pages priority over index pages, including when
a page query is supplied. For example, a root section `/` with `foo.md` publishes detail `/foo`.
A second section mounted at `/foo` has an index at the same path: `/foo`, `/foo/`, and
`/foo?page=2` resolve to the detail. Generation succeeds with a warning. Both route maps still
retain their pages, and `resolveIndex(secondSectionKey, 2)` retrieves that section's second
index page directly if it exists. The same collision can occur at nested paths.

No new routing normalization or collision policy is introduced. Avoid the overlap by changing
the conflicting base path or published slug in application configuration/content.

## `ContentListItemComponent`

Renderer-populated fluent methods:

- `withSlug(String)`
- `withRoute(String)`
- `withTitle(String)`
- `withSummary(String)`
- `withAuthor(String)`
- `withPublishedAt(String)`
- `withTags(List<String>)`

## Edge-case contracts

A built `StaticContentSite` snapshots its section settings. Duplicate slugs and heading IDs
remain unique even when source values already contain numeric suffixes. A section mounted at
`/` produces local detail routes such as `/entry`. Malformed frontmatter is handled best-effort;
it is not a complete YAML parser.

Draft entries do not reserve slugs or change the URLs assigned to published entries.
