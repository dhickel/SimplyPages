[Previous](01-components-htmltag-and-module-lifecycle.md) | [Index](../INDEX.md)

# Layout: Page, Row, Column, Grid

SimplyPages layout is server-defined and CSS-driven.

## Page Builder

`Page.builder()` composes top-level page content.

```java
Page page = Page.builder()
    .addComponents(Header.H1("Dashboard"))
    .addRow(row -> row
        .addColumn(Column.create().withWidth(8).withChild(new Paragraph("Main")))
        .addColumn(Column.create().withWidth(4).withChild(new Paragraph("Side"))))
    .build();
```

`build()` is idempotent for unchanged builder state and does not duplicate sticky-sidebar wrappers when called repeatedly.

## Row Behavior

`Row.withChild(component)` wraps non-column children in a default `.col` container.

Use `addColumn(...)` when you need explicit column widths.

`Row.copy()` creates a shallow copy of row attributes, text payload, and current child component
references. Use it when a wrapper needs to preserve a configured row while adding render-time
structure without mutating the original row on repeated renders.

Row modifier helpers preserve existing custom classes and replace only their own class family:

- `withGap("sm" | "medium" | "lg")`
- `withAlign("start" | "end" | "center" | "baseline" | "stretch")`, emitted as `items-*`
- `withJustify("start" | "end" | "center" | "between" | "around" | "evenly")`

## Column Widths

- `withWidth(1..12)` for fixed grid shares.
- `auto()` for content-sized columns.
- `fill()` for remaining space.

`Column` always preserves the exact `.col` base class, including direct, nested, context-aware,
and template render paths.

## Grid Behavior

`Grid` emits one `grid-cols-*` token and one `gap-*` token.

- `withColumns(1..6)`
- `withGap("sm" | "medium" | "lg")`

Changing columns or gap replaces the previous token from that family without dropping custom
classes.

## Sticky Sidebar

`Page.builder().withStickySidebar(...)` requires a non-null sidebar component and valid grid
widths. Invalid input fails immediately instead of silently producing missing content.

## Practical Guidance

1. Keep column math simple and explicit.
2. Put module sizing in layout (`Row`/`Column`), not inside `Module`.
3. Prefer readability over deeply nested row/column trees.
