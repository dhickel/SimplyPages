[Previous](06-shell-project-structure-and-asset-load-chain.md) | [Index](../INDEX.md)

# Mobile Rendering Model and Responsive Behavior

This guide explains how mobile behavior works in SimplyPages and what is automatic versus app-owned.

## Mental Model

SimplyPages mobile behavior is primarily CSS-driven, with small framework-generated HTML/JS hooks:

- `ShellBuilder` emits shell structure and mobile nav toggle markup when a sidebar exists.
- `framework.css` applies responsive breakpoints and stacking/wrapping behavior.
- `framework.js` handles interactive shell behavior like mobile sidebar open/close.
- Layout primitives (`Page`, `Row`, `Column`) provide stable class/markup contracts that CSS targets.

## What the Framework Handles Automatically

When you use `ShellBuilder` with a sidebar:

- Viewport meta is included: `width=device-width, initial-scale=1.0`.
- Mobile sidebar toggle button is rendered.
- Sidebar uses off-canvas mobile behavior via `.main-sidebar.mobile-open`.

When you use `Row`/`Column`:

- Columns collapse to full-width stacks on mobile breakpoints.
- Column/module text wrapping guards are applied to prevent overflow.

When you use `Page.withStickySidebar(...)`:

- Desktop sticky sidebar behavior remains.
- Mobile switches to stacked layout with a collapsible sidebar summary.

## Breakpoints

Framework defaults currently use:

- `@media (max-width: 768px)`:
  - shell becomes single-column
  - sidebar off-canvas mode
  - row/column stacking and sticky-sidebar mobile mode
- `@media (max-width: 480px)`:
  - tighter typography/spacing
  - InfoBox icon/value scaling

## Where Mobile Is Configured

- Shell/layout markup: `ShellBuilder` and `Page`
- Responsive behavior: `static/css/framework.css`
- Interactive shell behavior: `static/js/framework.js`

There is no separate "mobile mode" API flag to enable.

## Consumer Responsibilities

Framework defaults do not replace product-level mobile design decisions. App teams still own:

- page/module-specific responsive polish
- domain-specific navigation labels and density
- QA across real target devices

Use custom CSS layering via `withCustomCss(...)`/`addCustomCss(...)` for app-specific behavior.

## Practical Usage Pattern

1. Build shell with `ShellBuilder`.
2. Build content with `Page` + `Row` + `Column`.
3. Keep module sizing in layout (not module internals).
4. Add app CSS overrides for product-specific mobile tuning.

## Mobile Release Checklist

1. Verify no horizontal overflow at phone/tablet widths.
2. Verify sidebar toggle opens/closes correctly on mobile.
3. Verify multi-column rows stack to readable blocks.
4. Verify sticky sidebar content remains accessible and compact on mobile.
5. Verify long text/code/table content remains usable (wrap or horizontal scroll).
