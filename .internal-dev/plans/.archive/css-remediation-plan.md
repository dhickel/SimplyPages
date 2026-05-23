# SimplyPages CSS Remediation Plan

## Overview
The current `framework.css` is a large, monolithic file (3058 lines) that lacks modern CSS features like variables and has significant duplication between base styles and module-specific overrides. This plan outlines a strategy to refactor the CSS for better maintainability, usability, and flexibility.

## Phase 1: Refactor `framework.css`
The goal is to eliminate duplication, introduce CSS variables for consistency, and provide educational comments for users.

### 1.1 Introduce CSS Variables (`:root`)
Define a centralized set of variables at the top of the file to manage the framework's visual theme. This allows users to override colors, spacing, and typography globally by just changing a few variables.
- **Colors**: Primary, secondary, success, danger, warning, info, background, surface, text-primary, text-secondary, border.
- **Spacing**: Base padding, base margin, grid gaps.
- **Typography**: Base font-family, base font-size, line-heights.
- **Transitions**: Default duration and timing function.

### 1.2 Eliminate Duplication
- **Markdown Tables & Headings**: Instead of redefining `table`, `h1-h6` for `.content-module`, use a mixin-like approach or ensure the base styles are generic enough to handle markdown-rendered HTML without full redefinition.
- **Banner Systems**: Consolidate `top-banner` (legacy) and `banner` (new) into a single, unified banner system with modifiers.
- **Component Overlap**: Audit `account-widget` and `dropdown` to share common utility patterns for hover states and positioning.

### 1.3 Categorization & Documentation
Reorganize the file into clearly marked sections with a "Table of Contents" at the top.
- **Reset & Base**: Global resets and base element styling.
- **Theme Variables**: The `:root` section.
- **Layout System**: Containers, Grid, Rows/Cols.
- **Core Components**: Buttons, Cards, Alerts, Tables.
- **Modules**: Module-specific styling (Navigation, Gallery, Forum).
- **Utility Classes**: Margin, Padding, Alignment, Display.
- **Editing System**: Internal styles for the framework's editing features.

### 1.4 Educational Commenting
Enhance comments to explain CSS concepts and how they relate to Java components.
- **Example**: Before the Grid section, explain how `display: grid` works and how it correlates with the `Grid` and `Column` Java classes.
- **Customization Tips**: Add comments like `/* Edit --primary-color in :root to change the color of all buttons */`.

## Phase 2: Refactor CSS Documentation
The documentation needs to be updated to reflect the new features and the ability to disable default styling.

### 2.1 Update `docs/core/05-css-defaults-overrides-and-structure.md`
- **New Feature**: Document the `.withDefaultCss(boolean)` method in `ShellBuilder`.
- **Theme Customization**: Add a guide on overriding the new `:root` variables in a custom CSS file.
- **Load Order**: Re-verify and document the load order (Default CSS -> Custom CSS -> Inline Styles).

### 2.2 Future Considerations (Refactor Proposal)
Based on `future_considerations`, propose a move toward a modular CSS approach:
- Instead of one giant `framework.css`, explore a system where `ShellBuilder` (or components themselves) can selectively load only the CSS they need (e.g., `components.css`, `layout.css`, `modules.css`).

## Phase 3: Enhance `ShellBuilder` for Customization
Add the ability to disable the default framework CSS to allow for complete custom themes.

### 3.1 Proposed Changes to `ShellBuilder.java`
- Add `private boolean includeDefaultCss = true;`
- Add `public ShellBuilder withDefaultCss(boolean include) { ... }`
- Modify `build()` to conditionally include `/css/framework.css`.

## Summary of Expected Outcomes
- **Reduced File Size**: By eliminating redundant rules.
- **Easier Customization**: Through CSS variables.
- **Better Developer Experience**: Clearer comments and educational content for CSS beginners.
- **Greater Flexibility**: Ability to opt-out of framework styles entirely.
