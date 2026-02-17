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

## Row Behavior

`Row.withChild(component)` wraps non-column children in a default `.col` container.

Use `addColumn(...)` when you need explicit column widths.

## Column Widths

- `withWidth(1..12)` for fixed grid shares.
- `auto()` for content-sized columns.
- `fill()` for remaining space.

## Practical Guidance

1. Keep column math simple and explicit.
2. Put module sizing in layout (`Row`/`Column`), not inside `Module`.
3. Prefer readability over deeply nested row/column trees.
