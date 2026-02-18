[Previous](05-css-defaults-overrides-and-structure.md) | [Index](../INDEX.md)

# Shell Project Structure and Asset Load Chain

This page explains where app assets live, how `ShellBuilder` composes output, and what framework behavior is automatic.

## Project Structure for Assets

Framework assets:

- `simplypages/src/main/resources/static/css/framework.css`
- `simplypages/src/main/resources/static/js/framework.js`

Typical application assets (Spring Boot):

- `src/main/resources/static/css/app.css`
- `src/main/resources/static/css/pages/<page>.css`
- `src/main/resources/static/css/modules/<module>.css`
- `src/main/resources/static/js/app.js`

## ShellBuilder Load Chain

`ShellBuilder.build()` generates a full HTML document (`<!DOCTYPE html>`, `<html>`, `<head>`, `<body>`).

Head assets are added in this order:

1. framework CSS (if enabled)
2. custom CSS files in configured order
3. HTMX script (if enabled)
4. framework JS (`/js/framework.js`)

Body behavior:

- top banner and account bar (if configured)
- shell layout container and content target
- optional inline sidebar script (when collapsible sidebar is enabled)
- optional inline HTMX nav-active script (when HTMX is enabled)

## CSS Loading Modes

Default mode:

```java
ShellBuilder.create().build();
```

Replace framework CSS path:

```java
ShellBuilder.create()
    .withFrameworkCssPath("/css/base.css")
    .build();
```

Framework + multiple custom stylesheets:

```java
ShellBuilder.create()
    .withCustomCss("/css/app.css")
    .addCustomCss("/css/pages/dashboard.css")
    .addCustomCss("/css/modules/metrics.css")
    .build();
```

No framework CSS (advanced):

```java
ShellBuilder.create()
    .withFrameworkCss(false)
    .withCustomCss("/css/app.css")
    .build();
```

## Where to Put Your Own CSS

Use this layering pattern:

1. app-wide theme and shared selectors in `app.css`
2. page selectors in `css/pages/...`
3. module selectors in `css/modules/...`

Keep selectors anchored to stable IDs/classes you set in Java (`withId`, `withClass`).

## Behind-the-Scenes Behavior

ShellBuilder automatically handles:

- inclusion of framework CSS unless disabled
- inclusion of framework JS
- optional HTMX bootstrap script inclusion
- default content target ID (`content-area`) and optional HTMX load trigger (`hx-get="/home"` when HTMX is enabled)

Nothing else is auto-loaded for you. Any additional app CSS/JS must be linked explicitly by your app shell strategy.

## Integration Guidance

- Use framework defaults + custom CSS for fastest onboarding.
- Use framework replacement or no-framework mode only if your team owns full design system styling.
- If you disable framework CSS without replacement styles, built-in components will likely render unstyled.
