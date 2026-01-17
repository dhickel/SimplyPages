# SimplyPages Editing System - Overview

**Last Updated**: 2026-01-02
**Status**: Phase 3-4 Complete (Auto-Save & Constraints)

## What is the Editing System?

The SimplyPages editing system enables inline, user-friendly editing of page content without page reloads. It provides a framework-level solution for making modules editable through modal overlays, auto-saving changes, and dynamic page updates via HTMX.

## Key Features

- **Inline Editing**: Edit modules directly on the page using modal overlays
- **Auto-Save**: Changes save immediately - no manual save/load required
- **Type-Safe**: Full Java type safety with fluent builder APIs
- **Validation**: Built-in validation framework with clear error messages
- **Dynamic Updates**: HTMX-powered updates without full page reloads
- **Flexible Permissions**: Future support for field-level and action-level permissions
- **Module-Agnostic**: Works with any module type through EditAdapter interface

## Architecture

### Core Components

1. **EditAdapter Interface** (`io.mindspice.simplypages.editing.EditAdapter`)
   - Makes modules editable by implementing three methods
   - Contract: `buildEditView()`, `applyEdits()`, `validate()`
   - Implemented by editable modules (ContentModule, DataModule, etc.)

2. **Modal Component** (`io.mindspice.simplypages.components.Modal`)
   - Provides true overlay modals with backdrop
   - Used for all editing UI
   - Single container pattern prevents z-index conflicts

3. **EditModalBuilder** (`io.mindspice.simplypages.editing.EditModalBuilder`)
   - Standardized modal construction for edit operations
   - Handles save/delete buttons, OOB swaps, HTMX integration
   - Fluent API for customization

4. **ValidationResult** (`io.mindspice.simplypages.editing.ValidationResult`)
   - Standard validation response pattern
   - Collects multiple errors, provides clear feedback
   - Used by EditAdapter.validate()

5. **EditableModule** (`io.mindspice.simplypages.modules.EditableModule`)
   - Wrapper that adds edit/delete buttons to any module
   - Future: Framework-level button styling and positioning (Phase 5)
   - Integrates with EditAdapter for editing behavior

### Data Flow

```
User clicks "Edit" button
    ↓
HTMX GET request to edit endpoint
    ↓
Controller calls module.buildEditView()
    ↓
EditModalBuilder creates modal with form
    ↓
Modal renders as overlay (HTMX swaps into #edit-modal-container)
    ↓
User edits and clicks "Save"
    ↓
HTMX POST request with form data
    ↓
Controller calls module.validate() → module.applyEdits()
    ↓
Auto-save: Changes persist immediately
    ↓
OOB swap: Modal clears + page content updates (no page reload)
```

## Design Principles

### 1. Single Modal Container Pattern
**ONE** `<div id="edit-modal-container">` for **ALL** modals on the page.

**Why**: True overlay behavior requires backdrop, multiple containers cause z-index conflicts, simpler mental model.

**Implementation**:
```java
// In your page HTML
Div modalContainer = new Div().withAttribute("id", "edit-modal-container");
html.append(pageContent.render());
html.append(modalContainer.render());  // Always at end of page
```

### 2. Auto-Save by Default
All edits save immediately - no manual save/load required.

**Why**: Simpler UX, modern web app pattern (like Google Docs), reduces risk of lost work.

**Trade-off**: No preview before commit (some modules use USER_EDIT queue for staged changes).

### 3. Module Width as Layout Concern
Width is a **Column property**, not a **Module property**.

**Why**: Modules should be layout-agnostic (reusable in different contexts), same module might appear in different widths in different places.

**Implementation**: Width editing can be added to edit modals alongside module fields, but stored separately in layout data.

### 4. OOB Swaps for Updates
Use HTMX out-of-band swaps to update multiple parts of page simultaneously.

**Why**: Clear modal AND update content in one response, no JavaScript needed, declarative pattern.

**Implementation**:
```java
StringBuilder response = new StringBuilder();

// Clear modal (OOB swap)
response.append("<div id=\"edit-modal-container\" hx-swap-oob=\"true\"></div>");

// Update page content (OOB swap)
response.append("<div id=\"module-123\" hx-swap-oob=\"true\">")
    .append(updatedModule.render())
    .append("</div>");

return response.toString();
```

## Current Implementation Status

### ✅ Completed (Phases 1-4)
- Modal component with overlay and backdrop
- EditAdapter interface
- EditModalBuilder with standardized patterns
- ValidationResult framework
- ContentModule EditAdapter implementation (with markdown toggle)
- Auto-save architecture (no manual save/load)
- Row/module constraints (rows require at least one module)
- Width editing in edit modals
- Edit/delete button placement (with known bugs)

### 🐛 Known Issues
- Markdown toggle doesn't work (checkbox exists but has no effect)
- Edit/delete buttons only visible when left-aligned (right alignment broken)
- Z-index management not framework-level (users must add manually)
- Button styling not framework-level (users must style manually)

### ✅ Completed (Phases 5-6.5)
- **Phase 5**: Default button styling and positioning in framework
- **Phase 5**: Fix markdown toggle bug
- **Phase 5**: Fix button alignment bug
- **Phase 6**: Styling improvements for editing system
- **Phase 6.5**: Module locking and permission system

### 📋 Planned (Phases 7-9)
- **Phase 7**: Migrate all modules to EditAdapter
- **Phase 8**: Advanced validation and error handling
- **Phase 9**: Authentication and authorization integration

## Use Cases

### 1. Simple Content Editing
Edit markdown content with optional formatting toggle:
```java
ContentModule module = ContentModule.create()
    .withTitle("About Us")
    .withContent("# Welcome\n\nThis is our story...")
    .withModuleId("about-module");

// Make editable by implementing EditAdapter (already done for ContentModule)
// Edit button shows modal with title, content, and markdown toggle
```

### 2. Data Module Editing
Edit structured data tables:
```java
DataModule<User> module = DataModule.create(User.class)
    .withTitle("User Directory")
    .withModuleId("users");

// Future: Implement EditAdapter for DataModule
// Allow editing columns, filters, sort order
```

### 3. Locked Modules (Phase 6.5 ✅)
Restrict editing on certain modules:
```java
ContentModule module = ContentModule.create()
    .withTitle("Site Header")
    .withContent("Welcome to our site");

// Fully locked - no edit or delete buttons
EditableModule editable = EditableModule.wrap(module)
    .withCanEdit(false)
    .withCanDelete(false);

// Allow editing but not deleting
EditableModule editOnly = EditableModule.wrap(module)
    .withCanDelete(false);

// Future: Field-level restrictions
// .withEditableFields(Set.of("content"));  // Only allow editing content, not title
```

## When to Use the Editing System

### ✅ Use When:
- Building user-facing content management
- Creating admin panels for page configuration
- Allowing users to customize their own pages
- Building collaborative editing features
- Need inline editing without page reloads

### ❌ Don't Use When:
- Simple forms (use Form component directly)
- One-time configuration (use config files)
- Content doesn't change after deployment
- External CMS already handles editing

## Getting Started

1. **Read the Guide**: See [EDITING_SYSTEM_GUIDE.md](EDITING_SYSTEM_GUIDE.md) for step-by-step implementation
2. **Check the API**: See [EDITING_SYSTEM_API.md](EDITING_SYSTEM_API.md) for detailed API reference
3. **Review the Plan**: See [EDITING_SYSTEM_PLAN.md](EDITING_SYSTEM_PLAN.md) for implementation roadmap
4. **Check Known Issues**: See [DEVELOPMENT_NOTES.md](DEVELOPMENT_NOTES.md) for bugs and gotchas

## Related Documentation

- **[EDITING_SYSTEM_GUIDE.md](EDITING_SYSTEM_GUIDE.md)** - Step-by-step usage guide
- **[EDITING_SYSTEM_API.md](EDITING_SYSTEM_API.md)** - Complete API reference
- **[EDITING_SYSTEM_PLAN.md](EDITING_SYSTEM_PLAN.md)** - Implementation plan and roadmap
- **[MODAL_OVERLAY_USAGE.md](MODAL_OVERLAY_USAGE.md)** - Correct modal patterns
- **[DEVELOPMENT_NOTES.md](DEVELOPMENT_NOTES.md)** - Bugs, issues, and design decisions
- **[CLAUDE.md](CLAUDE.md)** - Main framework documentation

## Support and Contributing

**Issues**: Document bugs and issues in [DEVELOPMENT_NOTES.md](DEVELOPMENT_NOTES.md)
**Questions**: Check the guide and API documentation first
**Contributing**: Follow the implementation plan in EDITING_SYSTEM_PLAN.md
