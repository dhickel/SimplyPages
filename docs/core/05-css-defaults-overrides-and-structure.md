[Previous](04-rendering-pipeline-high-and-low-level.md) | [Index](../INDEX.md)

# CSS: Defaults, Overrides, and Structure

This guide explains how to customize SimplyPages styling safely without forking framework CSS.

## Where Default CSS Comes From

The framework ships a default stylesheet at:

- `simplypages/src/main/resources/static/css/framework.css`

It is included by `ShellBuilder` in generated HTML as:

- `<link rel="stylesheet" href="/css/framework.css">`

## ShellBuilder CSS Controls

Use `ShellBuilder` to control the stylesheet chain:

- `withFrameworkCss(boolean)` toggles framework CSS on/off.
- `withFrameworkCssPath(String)` replaces the framework CSS href.
- `withCustomCss(String)` sets one custom stylesheet.
- `withCustomCss(List<String>)` sets an ordered list of custom stylesheets.
- `addCustomCss(String)` appends one custom stylesheet.

## CSS Load Order

`ShellBuilder.build()` loads styles in this order:

1. framework CSS (default `/css/framework.css`, or `withFrameworkCssPath(...)`)
2. custom stylesheets in configured order

If `withFrameworkCss(false)` is set, only custom stylesheets are included.

## Shared Framework Tokens

`framework.css` exposes shared `:root` variables with `--sp-*` names. Start customization by overriding these in your app CSS:

```css
:root {
  --sp-color-primary: #0f766e;
  --sp-color-primary-hover: #115e59;
  --sp-space-md: 24px;
  --sp-radius-lg: 12px;
}
```

This keeps framework component selectors intact while changing theme globally.

## Where You Put Your App CSS

For Spring Boot applications, place app CSS in:

- `src/main/resources/static/css/app.css` (global app CSS)
- `src/main/resources/static/css/pages/<page-name>.css` (page-specific CSS)
- `src/main/resources/static/css/modules/<module-name>.css` (module-specific CSS)

Reference one or many files through `ShellBuilder`:

```java
String html = ShellBuilder.create()
    .withPageTitle("Portal")
    .withCustomCss("/css/app.css")
    .addCustomCss("/css/pages/analytics.css")
    .addCustomCss("/css/modules/sales-summary.css")
    .build();
```

## Cross-App CSS vs Page CSS vs Module CSS

Recommended structure:

- Cross-app/global rules in `/css/app.css`
- Page-level rules in `/css/pages/...`
- Module-level rules in `/css/modules/...`

Load order strategy:

1. framework CSS
2. app global CSS
3. page CSS (if needed)
4. module CSS (if needed)

Later files win on equal specificity.

## How to Target Pages and Modules Safely

Use stable IDs/classes from your components and modules.

Examples:

```java
Page page = Page.builder()
    .addComponents(new Div().withId("analytics-page"))
    .build();

ContentModule module = ContentModule.create()
    .withModuleId("sales-summary")
    .withClass("sales-summary-module");
```

Then in CSS:

```css
/* page-level */
#analytics-page .module {
  margin-bottom: 24px;
}

/* module-level */
#sales-summary .module-title,
.sales-summary-module .module-title {
  letter-spacing: 0.02em;
}
```

## Override and Precedence Rules

CSS precedence in practical terms:

1. inline `style` attribute (highest)
2. stylesheet rules by specificity
3. stylesheet rules by source order (later wins if specificity ties)

In SimplyPages code:

- `.withClass(...)` / `.addClass(...)` adds classes for stylesheet-driven control.
- `.addStyle(...)` and size helpers (`withWidth`, `withMaxWidth`, `withMinWidth`) write inline style.

Use inline style sparingly for runtime-calculated values.

## Should You Edit `framework.css` Directly?

Default answer: no.

- Preferred: override `--sp-*` tokens and layer app selectors in your own CSS files.
- Edit framework `framework.css` directly only when contributing upstream framework defaults.
- If you fork framework CSS in an app, expect manual merge work on framework upgrades.

## Minimal Practical Setup

1. Keep framework defaults enabled.
2. Add `/css/app.css` using `withCustomCss(...)` or `withCustomCss(List<String>)`.
3. Namespace page styles under a page root ID.
4. Namespace module styles under module IDs/classes.
5. Keep overrides in CSS, not scattered inline styles.

## Advanced Mode: No Framework CSS

You can disable framework CSS:

```java
String html = ShellBuilder.create()
    .withFrameworkCss(false)
    .withCustomCss("/css/app.css")
    .build();
```

This is supported for advanced integrations. Without replacement styles, many built-in components may appear visually broken because they rely on framework class styling.

## Next Reading

For internals and behind-the-scenes load behavior, read:

- `docs/core/06-shell-project-structure-and-asset-load-chain.md`
