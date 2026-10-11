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

When a sidebar is configured, full/template/body output includes a named `Toggle navigation`
button with `aria-controls="main-sidebar"` and synchronized expanded state. At widths through
768px, closed off-canvas navigation is inert and hidden from assistive technology. Opening focuses
a usable navigation control; Escape closes and returns to the toggle, and outside clicks dismiss.
Resize restores desktop usability while preserving desktop collapsed state; mobile opening ignores
that collapse styling. Framework assets must be loaded once by the host for body-only fragments.
This does not change the separate body-only desktop-collapse lifecycle contract.

The mobile toggle is unavailable behind a modal. Modal stack offsets are managed by JS, but CSS
owns the base layer through `--sp-modal-layer` (default 1100); theme overrides should keep dialogs
above navigation. See [mobile behavior](../core/07-mobile-rendering-model-and-responsive-behavior.md).

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

## BannerBuilder

Use for app or area-level brand/title/banner composition.

## AccountBarBuilder

Legacy compatibility builder. Prefer `TopNavBuilder` for new work.

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
