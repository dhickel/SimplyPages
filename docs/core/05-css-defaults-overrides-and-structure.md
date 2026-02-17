[Previous](04-rendering-pipeline-high-and-low-level.md) | [Index](../INDEX.md)

# CSS: Defaults, Overrides, and Structure

This guide answers how CSS is handled in SimplyPages and where your application CSS should live.

## Where Default CSS Comes From

The framework ships a default stylesheet at:

- `simplypages/src/main/resources/static/css/framework.css`

It is included by `ShellBuilder` in generated HTML as:

- `<link rel="stylesheet" href="/css/framework.css">`

## How CSS Is Loaded in ShellBuilder

`ShellBuilder.build()` loads styles in this order:

1. `/css/framework.css` (framework defaults)
2. your custom stylesheet from `withCustomCss(...)` (if set)

That order means your custom stylesheet can override framework rules when selectors have equal or higher specificity.

## Where You Put Your App CSS

For Spring Boot users, place app CSS in your app resources, typically:

- `src/main/resources/static/css/app.css` (global app CSS)
- `src/main/resources/static/css/pages/<page-name>.css` (page-specific CSS)
- `src/main/resources/static/css/modules/<module-name>.css` (module-specific CSS)

Then reference it through shell configuration:

```java
String html = ShellBuilder.create()
    .withPageTitle("Portal")
    .withCustomCss("/css/app.css")
    .build();
```

If you need multiple CSS files, use one aggregated app stylesheet that `@import`s others, or generate shell manually and add additional `<link>` tags.

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

## How to Target Pages and Modules

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

## How Overrides Work (Important)

CSS precedence in practical terms:

1. inline `style` attribute (highest)
2. stylesheet rules by specificity
3. stylesheet rules by source order (later wins if specificity ties)

In SimplyPages code:

- `.withClass(...)` / `.addClass(...)` adds classes for stylesheet-driven control.
- `.addStyle(...)` and size helpers (`withWidth`, `withMaxWidth`, `withMinWidth`) write inline style.

Use inline style sparingly. Prefer class-based styles in your CSS files for maintainability.

## Do Users Override in Code or CSS Files?

Use both, but default to CSS files.

- Use code-level style for one-off runtime values.
- Use CSS files for system-wide, page-wide, and module-wide styling.

## Minimal Practical Setup

1. Keep framework defaults enabled.
2. Add `/css/app.css` using `withCustomCss(...)`.
3. Namespace page styles under a page root ID.
4. Namespace module styles under module IDs/classes.
5. Keep overrides in CSS, not scattered inline styles.

## Request Mechanics

No additional HTTP behavior is needed beyond static resource serving.

- Browser requests HTML.
- Browser requests linked CSS files.
- CSS cascade resolves final visual output.
