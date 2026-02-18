[Previous](02-simplypages-mental-model.md) | [Index](../INDEX.md)

# CSS Fundamentals

This is a practical CSS starter for users newer to frontend styling.

## What CSS Does

HTML defines structure.
CSS defines visual presentation.

In SimplyPages, Java builds HTML and CSS controls how that HTML looks.

## Three Ways Styles Apply

1. Class-based rules (recommended)
- Example class in code: `.withClass("card")`
- Rule in stylesheet: `.card { ... }`

2. ID-based rules
- Example ID in code: `.withId("sales-summary")`
- Rule in stylesheet: `#sales-summary { ... }`

3. Inline styles (highest priority)
- Example in code: `.addStyle("margin-top", "16px")`

## Specificity and Override Order

When multiple rules target the same element:

1. Inline style usually wins.
2. More specific selectors win over less specific ones.
3. If specificity is equal, later-loaded stylesheet rule wins.

## Recommended Workflow

1. Add stable classes/IDs in components/modules.
2. Put most styling in CSS files.
3. Use inline styles only for one-off runtime values.

## Minimal Example

Java:

```java
Div card = new Div()
    .withClass("metric-card")
    .withId("active-users-card")
    .withChild(Header.H3("Active Users"));
```

CSS:

```css
.metric-card {
  border: 1px solid #d0d7de;
  border-radius: 8px;
  padding: 16px;
}

#active-users-card {
  background: #f8fafc;
}
```

## Common CSS Units

- `px`: fixed size (`16px`)
- `%`: relative to parent (`100%`)
- `rem`: relative to root font size (`1rem`)
- `vh`/`vw`: viewport-relative units

## Layout Basics You Will Use Most

- Flexbox for row/column alignment.
- Grid for dashboard-like multi-column layouts.
- Spacing consistency with shared classes.

## Token-Based Styling (Recommended)

Prefer CSS variables for app-wide theme values:

```css
:root {
  --brand-primary: #0f766e;
  --page-gutter: 20px;
}
```

Then consume the variables in your selectors:

```css
.metric-card {
  border-color: var(--brand-primary);
  padding: var(--page-gutter);
}
```

## SimplyPages-Specific Next Steps

This page is only the CSS primer. For framework behavior and load order:

1. Read `docs/core/05-css-defaults-overrides-and-structure.md`.
2. Read `docs/core/06-shell-project-structure-and-asset-load-chain.md`.
