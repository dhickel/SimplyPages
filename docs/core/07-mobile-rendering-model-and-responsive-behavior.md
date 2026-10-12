[Previous](06-shell-project-structure-and-asset-load-chain.md) | [Index](../INDEX.md)

# Mobile Rendering Model and Responsive Behavior

This guide explains how mobile behavior works in SimplyPages and what is automatic versus app-owned.

## Mental Model

SimplyPages mobile behavior is primarily CSS-driven, with small framework-generated HTML/JS hooks:

- `ShellBuilder` emits shell structure and mobile nav toggle markup when a sidebar exists.
- Shell layout defaults to full-width content when no sidebar is configured.
- `framework.css` applies responsive breakpoints and stacking/wrapping behavior.
- `framework.js` handles shared shell collapse/drawer behavior and the native Page disclosure lifecycle.
- Layout primitives (`Page`, `Row`, `Column`) provide stable class/markup contracts that CSS targets.

## What the Framework Handles Automatically

When you use `ShellBuilder` with a sidebar:

- Full/template documents include viewport meta: `width=device-width, initial-scale=1.0`.
  Body-only fragments omit document/head/chrome/assets/scripts; their host loads framework assets once.
- Shared framework JS initializes existing and later direct/HTMX shell fragments idempotently.
  Collapsible desktop buttons are named native non-submit controls; desktop preference persistence
  through `sidebarCollapsed` is best effort, with document-local operation when storage is denied.
- Mobile sidebar toggle button is rendered.
- Sidebar uses off-canvas mobile behavior via `.main-sidebar.mobile-open` at widths through 768px.
- Closed shell navigation is inert and has `aria-hidden="true"`; the named toggle tracks expanded state.
- Opening focuses a navigation control; Escape returns focus to the toggle, and outside clicks dismiss.
- Resize restores desktop usability without discarding the desktop collapsed preference. That preference
  does not narrow or prevent opening the mobile drawer.
- The floating toggle cannot overlay or activate through an active modal. CSS owns modal base layering
  (`--sp-modal-layer`, default 1100); JS adds only relative stack offsets.

When you use `Row`/`Column`:

- Columns collapse to full-width stacks on mobile breakpoints.
- Column/module text wrapping guards are applied to prevent overflow.

When you use `Page.withStickySidebar(...)`:

- One native `details`/first `summary` disclosure starts open; mobile uses stacked layout.
- Framework JS keeps desktop (769px and above) natively open and restores each element's remembered
  observable native/application open preference through 768px. Desktop closes update that preference
  before reopening; ordinary nested details are untouched.
- New/replacement elements initialize from their own native state. Reinserted/reparented elements
  retain preference; detached elements receive no responsive writes or focus transfer.
- A closing disclosure with contained focus transfers to its usable summary. Desktop transfers a
  focused summary to usable content or the generated `tabindex="-1"` details fallback, without
  overwriting later caller tabindex or application focus. Covered/background Pages do not take focus
  or scroll away from the top modal.
- Without JS, a closed desktop summary remains available, and an open summary remains displayed while
  itself focused for native keyboard recovery. Visibility follows native open state, not forced boxes.
- This Page lifecycle is separate from ShellBuilder's off-canvas drawer. Check both presentations in
  the application; custom policies remain application-owned.

When you use `TabsModule`:

- Tab navigation stacks vertically through 768px; desktop navigation stays horizontal.
- Framework JS matches `aria-orientation` and keyboard arrows to the computed flex axis:
  Up/Down vertically, Left/Right horizontally, with wrapping/automatic activation and Home/End.
- Initialization, resize and dynamic insertion synchronize orientation without resetting focus,
  selection or nested tabs. Perpendicular arrows and modified keys retain native behavior.
- SSR markup uses a horizontal fallback until the framework assets apply the viewport layout.

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
- Interactive shell and native Page lifecycle: `static/js/framework.js`

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
