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

## Native Page Sidebar

Call `withStickySidebar(component)` before adding main content. It enables independent scrolling
and emits one initially closed native `details`, its first `summary` (`Page navigation`), and sidebar
content. This disclosure is distinct from ShellBuilder's off-canvas navigation; no duplicate
mobile sidebar is generated.

With framework assets loaded once, widths of 769px and above keep the disclosure natively open,
not merely styled as visible. Fresh disclosures through 768px remain closed; each element restores its
remembered native/application open preference. Observable desktop closes are remembered for the next mobile presentation while
desktop reopens. Ordinary nested details keep their native state. New/replacement elements initialize
from their own open state; reparenting or reinserting the same element retains its preference.
Detached elements receive no responsive writes or focus transfer.

If a responsive transition hides focused content, focus moves to the usable summary; when desktop
removes a focused summary, focus moves to usable sidebar content or the generated aside fallback.
The aside has `tabindex="-1"` for programmatic focus, not a sequential stop. If the caller supplied a
`tabindex` on details, JS uses details as fallback instead. Caller attributes and application-selected
replacement focus are preserved. Covered/background Pages do not
steal modal focus or scroll the background. Retained in-dialog focus is revealed inside the existing
scroll/clipping boundary after responsive reflow, even when the disclosure stays open.

Without JS, the disclosure starts closed at every width, and its native summary remains keyboard-operable.
Native open state controls visibility; a focused open desktop summary stays visible until focus leaves,
allowing native keyboard recovery.
Custom layout/disclosure policies remain application-owned.

## Row Behavior

`Row.withChild(component)` wraps non-column children in a default `.col` container.

Use `addColumn(...)` when you need explicit column widths.

`Row.copy()` creates a shallow copy of row attributes, text payload, and current child component
references. Use it when a wrapper needs to preserve a configured row while adding render-time
structure without mutating the original row on repeated renders. EditableRow instead renders the
supplied row once and a separate registered-module row; it does not copy or merge the source.

Row modifier helpers preserve existing custom classes and replace only their own class family:

- `withGap("sm" | "medium" | "lg")`
- `withAlign("start" | "end" | "center" | "baseline" | "stretch")`, emitted as `items-*`
- `withJustify("start" | "end" | "center" | "between" | "around" | "evenly")`

## Column Widths

- `withWidth(1..12)` for fixed grid shares.
- `auto()` for content-sized columns.
- `fill()` for remaining space.

Columns establish the base `col` class during construction/configuration, not rendering.
`withWidth(int)`, `auto()`, `fill()`, and `withClass(...)` replace the class attribute;
`withClass("custom")` produces `col custom`, replacing any previous width class.
Use `addClass(...)` to append instead. Generic `withAttribute("class", ...)` replacement
also preserves the `col` token.

Finish mutable configuration before sharing a column. Standard rendering does not change
its attributes; a standard column is compiled as ordinary tag structure by `Template`.
Custom subclass render overrides remain live delegates and must be safe to share.

## Grid Behavior

`Grid.withColumns(1..6)` emits one `grid-cols-*` token. `withGap(...)` preserves the supplied
spelling, including `small` and `large`, rather than normalizing it. Updates replace only whole
owned layout tokens, preserve custom tokens and their case, and recognize logical class aliases.

## Sticky Sidebar

`Page.builder().withStickySidebar(...)` requires a non-null sidebar component and valid grid
widths. Invalid input fails immediately instead of silently producing missing content.

## Practical Guidance

1. Keep column math simple and explicit.
2. Put module sizing in layout (`Row`/`Column`), not inside `Module`.
3. Prefer readability over deeply nested row/column trees.
