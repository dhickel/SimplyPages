[Previous](components-and-modules-catalog.md) | [Index](../INDEX.md)

# Builders Reference: Shell, Navigation, Banner, Account Bar

## ShellBuilder

`ShellBuilder` creates full app shell HTML.

Core options:

- `withTopBanner(...)`
- `withTopNav(...)`
- `withAccountBar(...)` (deprecated alias for `withTopNav(...)`)
- `withSideNav(...)`
- `withSideNav(..., boolean collapsible)`
- `withCollapsibleSideNav(boolean)`
- `withContentTarget(...)`
- `withContentTargetId(...)`
- `withContentTargetClass(...)`
- `withContentWrapper(Function<Component, Component>)`
- `withContent(...)`
- `buildTemplate()`
- `withPageTitle(...)`
- `withHtmx(boolean)`
- `withFrameworkCss(boolean)`
- `withFrameworkCssPath(String)`
- `withCustomCss(String)`
- `withCustomCss(List<String>)`
- `addCustomCss(String)`
- `withCustomJs(String)`
- `withCustomJs(List<String>)`
- `addCustomJs(String)`
- `buildBody()`

Returns full HTML document string from `build()`.

`buildTemplate()` returns a reusable `ShellTemplate` (compiled template wrapper) with a dedicated content slot for request-time content injection.

When a sidebar is configured, shell output includes a mobile navigation toggle hook compatible with framework CSS/JS defaults.

Stylesheet load order in `build()`:

1. framework CSS (if enabled)
2. custom CSS files in configured order

Script load order in `build()`:

1. HTMX script (if enabled)
2. framework JS (`/js/framework.js`)
3. custom JS files in configured order

Custom JS setup example:

```java
String shell = ShellBuilder.create()
    .withPageTitle("Admin Portal")
    .withCustomJs("/js/app.js")
    .addCustomJs("/js/pages/admin-dashboard.js")
    .withContentWrapper(content -> new HtmlTag("section")
        .withClass("content-shell")
        .withChild(content))
    .build();
```

## Template Shell Reuse

Use `buildTemplate()` when shell chrome is stable and page content changes per request.

```java
ShellTemplate shellTemplate = ShellBuilder.create()
    .withPageTitle("Admin Portal")
    .withTopNav(topNav)
    .buildTemplate();

String html = shellTemplate.renderWithContent(pageComponent);
```

Notes:

- `ShellTemplate.renderWithContent(...)` renders full document HTML (with doctype).
- If `withContent(...)` was configured before `buildTemplate()`, that content becomes default slot content.
- Template mode does not emit shell content auto-load attributes (`hx-get="/home"`, `hx-trigger="load"`).

## SideNavBuilder and TopNavBuilder

Use nav builders to produce stable navigation components.

Guidance:

1. Keep labels and URLs as data, not hardcoded in controllers.
2. Keep active-state logic deterministic.
3. Prefer `TopNavBuilder` for header-level nav composition (primary links, utility links/dropdowns, account widgets).

Generated navigation links validate targets through the shared `SafeUrl` contract. Use
relative paths, fragments, query-only URLs, protocol-relative URLs, or absolute `http`,
`https`, `mailto`, and `tel` URLs.

## BannerBuilder

Use for app or area-level brand/title/banner composition.

`withBackgroundImage(...)` accepts normal web image URLs and relative image paths. The value is
validated before being embedded in inline CSS. Color helpers use the hardened `addStyle(...)`
path and reject declaration-breakout characters.

## AccountBarBuilder

Legacy compatibility builder. Prefer `TopNavBuilder` for new work.

Link helpers use `SafeUrl` validation. `withBackgroundColor(...)` uses the hardened inline style
path.

## Minimal Shell Example

```java
String shell = ShellBuilder.create()
    .withPageTitle("Admin Portal")
    .withTopBanner(BannerBuilder.create()
        .withTitle("Admin Portal")
        .build())
    .withSideNav(SideNavBuilder.create()
        .addSection("Main")
        .addLink("Dashboard", "/dashboard", "")
        .build(), true)
    .withCustomCss("/css/app.css")
    .addCustomCss("/css/pages/dashboard.css")
    .build();
```
