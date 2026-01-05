# SimplyPages Development Notes

**Living Document** - Updated as development progresses

**Purpose**: Track bugs, issues, design decisions, and future concerns during active development.

---

## 📋 Table of Contents

1. [Known Bugs](#known-bugs)
2. [Active Issues](#active-issues)
3. [Future Concerns](#future-concerns)
4. [Design Decisions](#design-decisions)
5. [Framework vs Implementation](#framework-vs-implementation)
6. [Refactoring Needed](#refactoring-needed)

---

## 🐛 Known Bugs

### 1. **✅ FIXED: Markdown Toggle Not Working**
- **Location**: ContentModule.applyEdits()
- **Status**: **FIXED** (Jan 2, 2026 - Phase 5)
- **Root Cause**: Module lifecycle is build-once. When `applyEdits()` changed `useMarkdown`, the module had already been built and wouldn't rebuild.
- **Fix**: Added `children.clear()` and `buildContent()` call in `applyEdits()` to force content rebuild after edits
- **File**: `ContentModule.java:143-144`

### 2. **✅ FIXED: Edit/Delete Buttons Not Rendering**
- **Location**: EditableModule.render()
- **Status**: **FIXED** (Jan 2, 2026 - Phase 5)
- **Root Cause 1**: Initially `buildWrapper()` called `children.clear()` every time, causing children to be cleared on multiple render() calls
- **Fix 1**: Added idempotent `built` flag - `buildWrapper()` only runs once
- **Root Cause 2**: Overrode `render()` instead of `render(RenderContext context)`, so EditableModule.render() was never called when component was rendered as a child
- **Fix 2**: Changed override to `render(RenderContext context)` (the actual Component interface method)
- **File**: `EditableModule.java:287-290,338-342`
- **Lesson Learned**: Always override `render(RenderContext context)`, not `render()`, when extending HtmlTag/Div

---

## ⚠️ Active Issues

### 1. **✅ RESOLVED: Z-Index Management** (Phase 5)
- **Status**: **RESOLVED** (Jan 2, 2026)
- **Fix**: Framework CSS classes (`.module-edit-btn`, `.module-delete-btn`) now include `z-index: 10`
- **File**: `framework.css:1666,1684`

### 2. **✅ RESOLVED: Button Styling Standardized** (Phase 5)
- **Status**: **RESOLVED** (Jan 2, 2026)
- **Fix**: Created EditableModule wrapper class and framework CSS classes for consistent styling
- **Files**:
  - `EditableModule.java` - Wrapper that adds buttons
  - `framework.css:1658-1692` - `.module-edit-btn` and `.module-delete-btn` CSS classes
- **Default Styling**:
  - Edit button: Gray (#6c757d), positioned right: 30px, top: 2px
  - Delete button: Red (#dc3545), positioned right: 2px, top: 2px
  - Both: 1.2rem font size, z-index: 10, hover states
- **Usage**: Simply wrap any module with `EditableModule.wrap(module)`

### 3. **Width Editing Not Framework-Level**
- **Status**: Design Gap (Jan 2, 2026)
- **Description**: Module width editing is implemented in test controllers, not framework
- **Current State**:
  - Phase3And4TestController manually adds width dropdown to edit modals
  - Width is stored in test controller's TestModule class
  - No framework support for width editing
- **Problem**: Every implementation must manually add width editing
- **Should Be**:
  - EditableModule or EditModalBuilder supports width editing as optional feature
  - Width stored with module layout data (not in module itself - layout concern)
- **Design Question**: Should width be:
  - A. Part of EditAdapter interface? (NO - layout concern, not module concern)
  - B. Part of EditableModule/EditableRow? (MAYBE - they manage layout)
  - C. Part of EditModalBuilder as optional extra field? (YES - modal can include layout fields)
- **Fix**: Add optional width editing to EditModalBuilder or create EditableModuleBuilder
- **Priority**: Phase 6 or later - Enhancement

---

## 🔮 Future Concerns

### 1. **Modal Container Pattern Fragility**
- **Concern**: Single modal container requirement is easy to get wrong
- **Risk**: Developers might create multiple containers, breaking overlay pattern
- **Mitigation Needed**: Better error messages or framework enforcement
- **Potential Solutions**:
  - Framework checks for modal container existence
  - Auto-inject modal container if missing
  - Error message if multiple containers detected
- **Priority**: Phase 7 or later

### 2. **EditAdapter Scaling to 16 Modules**
- **Concern**: Implementing EditAdapter for 16 different module types
- **Risk**: Repetitive code, inconsistent patterns across modules
- **Mitigation**: Create helper methods or base classes for common patterns
- **Potential Solutions**:
  - Abstract base class with common field rendering
  - Builder pattern for edit forms
  - Standardized validation patterns
- **Priority**: Phase 7 (Module Migration)

### 3. **Permission System Complexity**
- **Concern**: Phase 6.5 permission system (canEdit, canDelete, editableFields) adds complexity
- **Risk**: Confusing API, hard to debug permission issues
- **Mitigation**: Clear documentation, good defaults, helpful error messages
- **Potential Solutions**:
  - Fluent API with clear naming
  - Default to permissive (everything allowed)
  - Log warnings when permissions block actions
- **Priority**: Phase 6.5

### 4. **Auto-Save vs Manual Save**
- **Concern**: Phase 3 removed manual save - what if users want staged editing?
- **Current Design**: Everything auto-saves immediately
- **Potential Problem**: No way to preview changes before committing
- **Alternative Approach**: Optional preview mode before save
- **Priority**: Post-Phase 9 (if users request it)

### 5. **Row Grid Width Enforcement**
- **Concern**: Rows currently don't enforce 12-column limit
- **Current Behavior**: Modules exceeding 12 grid units automatically wrap to next line
- **Risk**: Developers might accidentally create layouts that don't align as expected
- **Potential Solution**: Optional strict mode that validates total column width
- **Considerations**:
  - Current behavior provides flexibility (wrapping can be intentional)
  - Strict validation could be opt-in via Row.withStrictWidth(true)
  - Warning messages when total width exceeds 12
- **Priority**: Enhancement - May want to make this configurable in future
- **Status**: Current auto-wrap behavior is acceptable, no immediate action needed

---

## 🎯 Design Decisions

### Decision 1: Module Width is Layout Concern, Not Module Concern
- **Date**: Jan 2, 2026
- **Context**: Adding width editing to module edit modals
- **Decision**: Width is a layout property (Column.withWidth()), not a module property
- **Reasoning**:
  - Modules should be layout-agnostic (reusable in different contexts)
  - Same module might appear in different widths in different places
  - Width is determined by Column wrapper, not module itself
- **Implementation**: Width editing added to edit modal alongside module fields, but stored separately
- **Impact**: EditAdapter doesn't need width methods, keeps modules clean

### Decision 2: Single Modal Container for All Modals
- **Date**: Dec 30, 2025 (Phase 1)
- **Context**: Fixing bottom-of-page div modals
- **Decision**: ONE `<div id="edit-modal-container">` for ALL modals
- **Reasoning**:
  - True overlay behavior requires backdrop
  - Multiple containers cause z-index conflicts
  - Simpler mental model (one place for all modals)
- **Implementation**: Modal.create() returns overlay HTML, all HTMX targets #edit-modal-container
- **Impact**: Must document pattern clearly to prevent mistakes

### Decision 3: Auto-Save by Default
- **Date**: Jan 2, 2026 (Phase 3)
- **Context**: Removing manual save/load buttons
- **Decision**: All edits save immediately, no manual save required
- **Reasoning**:
  - Simpler UX (fewer buttons, less confusion)
  - Modern web app pattern (like Google Docs)
  - Reduces risk of lost work
- **Trade-off**: No preview before commit (addressed by USER_EDIT queue for some modules)
- **Impact**: Removed savePage() and loadPage() methods

### Decision 4: Markdown Checkbox in ContentModule
- **Date**: Phase 2 (Dec 30, 2025)
- **Context**: Some content should be plain text, some markdown
- **Decision**: Include toggle in edit form to control rendering
- **Reasoning**: Different use cases need different formatting
- **Current Status**: ⚠️ BUG - Toggle exists but doesn't work
- **Impact**: Needs fixing in Phase 5

---

## 🏗️ Framework vs Implementation

### Framework-Level (Built-In) ✅

These are properly encapsulated in the framework:

1. **Modal Component** ✅
   - Location: `Modal.java`
   - Provides: Overlay modal with backdrop, proper z-index, close behavior
   - Users just call: `Modal.create().withTitle().withBody().render()`

2. **EditAdapter Interface** ✅
   - Location: `EditAdapter.java`
   - Provides: Contract for editable modules (buildEditView, applyEdits, validate)
   - Users implement: Interface methods for their module types

3. **EditModalBuilder** ✅
   - Location: `EditModalBuilder.java`
   - Provides: Standardized edit modal with save/delete buttons, OOB swaps
   - Users just call: `EditModalBuilder.create().withTitle().withEditView()...`

4. **ValidationResult** ✅
   - Location: `ValidationResult.java`
   - Provides: Standard validation response pattern
   - Users use: `ValidationResult.valid()` or `.addError()`

### Implementation-Level (End User Must Code) ⚠️

These require end users to implement - may need framework support:

1. **Edit Button Styling** ⚠️ NEEDS FRAMEWORK SUPPORT
   - Current: Test controllers manually style buttons
   - Should: EditableModule provides default styling
   - Gap: No standard appearance, users must know CSS details
   - Fix: Add default styling to EditableModule.buildContent()

2. **Z-Index Management** ⚠️ NEEDS FRAMEWORK SUPPORT
   - Current: Test controllers manually add z-index to buttons
   - Should: EditableModule handles z-index automatically
   - Gap: Users must remember to add z-index or buttons disappear
   - Fix: EditableModule adds z-index by default

3. **Width Editing** ⚠️ OPTIONAL FRAMEWORK SUPPORT
   - Current: Test controllers manually add width dropdowns
   - Should: Optional feature in EditModalBuilder or similar
   - Gap: Every implementation reinvents width editing
   - Fix: Add `.withWidthEditing()` to EditModalBuilder (opt-in)

4. **Button Positioning** ⚠️ NEEDS FRAMEWORK SUPPORT
   - Current: Test controllers manually position edit/delete buttons
   - Should: EditableModule positions buttons correctly by default
   - Gap: ❌ BUG - Right alignment doesn't work
   - Fix: Fix EditableModule to properly position and display buttons

5. **Auto-Save Logic** ✅ CORRECTLY APPLICATION-LEVEL
   - Current: Application controllers implement save endpoints
   - Should: Remain application-level (business logic varies)
   - Status: ✅ Correct - framework provides tools, apps implement logic

6. **Row/Module Constraints** ✅ CORRECTLY APPLICATION-LEVEL
   - Current: Application controllers enforce constraints (row requires module)
   - Should: Remain application-level (requirements vary)
   - Status: ✅ Correct - framework provides tools, apps implement rules

### Action Items: Move to Framework

**High Priority (Phase 5)**:
1. ✅ Move edit/delete button default styling to EditableModule
2. ✅ Move z-index handling to EditableModule (always apply)
3. ✅ Fix button positioning bug (right alignment not working)

**Medium Priority (Phase 6)**:
4. Add optional width editing to EditModalBuilder (`.withLayoutEditing()` or similar)
5. Add default hover states for edit/delete buttons

**Low Priority (Phase 7+)**:
6. Consider EditableModuleBuilder for more complex configurations
7. Add framework-level button position options (top-right, top-left, etc.)

---

## 🔧 Refactoring Needed

### 1. EditableModule Enhancements (Phase 5)

**Current Issues**:
- Buttons styled in test controllers, not framework
- No z-index handling
- Button positioning broken for right alignment

**Needed Changes**:
```java
// In EditableModule.buildContent()
protected void buildContent() {
    Div wrapper = new Div().withClass("editable-module-wrapper");

    if (showEditControls && editUrl != null) {
        Button editBtn = Button.create("✏")
            .withStyle(Button.ButtonStyle.LINK)
            .withClass("module-edit-btn");  // Framework CSS class
        // Framework handles: position, z-index, size, color, hover
        editBtn.withAttribute("hx-get", editUrl);
        editBtn.withAttribute("hx-target", "#edit-modal-container");
        wrapper.withChild(editBtn);
    }

    if (deleteUrl != null) {
        Button deleteBtn = Button.create("🗑")
            .withStyle(Button.ButtonStyle.LINK)
            .withClass("module-delete-btn");  // Framework CSS class
        // Framework handles: position, z-index, size, color, hover
        deleteBtn.withAttribute("hx-delete", deleteUrl);
        wrapper.withChild(deleteBtn);
    }

    wrapper.withChild(wrappedModule);
    super.withChild(wrapper);
}
```

**Required CSS in framework.css**:
```css
.module-edit-btn {
    position: absolute;
    top: 2px;
    right: 30px;
    font-size: 1.2rem;
    text-decoration: none;
    color: #6c757d;
    padding: 2px 4px;
    z-index: 10;
}

.module-delete-btn {
    position: absolute;
    top: 2px;
    right: 2px;
    font-size: 1.2rem;
    text-decoration: none;
    color: #dc3545;
    padding: 2px 4px;
    z-index: 10;
}

.module-edit-btn:hover {
    color: #495057;
}

.module-delete-btn:hover {
    color: #bd2130;
}
```

### 2. EditModalBuilder Width Editing (Phase 6)

**Proposed API**:
```java
EditModalBuilder.create()
    .withTitle("Edit Module")
    .withEditView(adapter.buildEditView())
    .withLayoutEditing(currentWidth)  // NEW: Adds width dropdown
    .withSaveUrl(saveUrl)
    .build()
    .render();
```

**Implementation**: EditModalBuilder adds width dropdown if `withLayoutEditing()` called

### 3. Documentation Consolidation (Phase 5)

**Current State**: Documentation scattered across multiple files
**Needed**: Top-down comprehensive editing system documentation

**Proposed Structure**:
- EDITING_SYSTEM_OVERVIEW.md (high-level concepts)
- EDITING_SYSTEM_GUIDE.md (step-by-step usage)
- EDITING_SYSTEM_API.md (API reference)
- EDITING_SYSTEM_PLAN.md (implementation plan - already exists)

---

## 📝 Notes

**Last Updated**: 2026-01-04 (Phase 8 Complete)
**Next Review**: Before Phase 9 (Testing & Polish) or when starting Phase 7 (Module Migration)
**Maintainer**: Development team

### Phase 8 Summary (Jan 4, 2026)

**✅ Completed:**
1. Created AuthWrapper utility class with authorization pattern
2. Implemented convenience methods (requireForEdit, requireForDelete, requireForCreate)
3. Created Phase8TestController demonstrating role-based permissions
4. Full integration with editing system (Modal, EditModalBuilder, HTMX)

**🔐 Authorization Features:**
- **Generic Pattern**: `AuthWrapper.require()` for any authorization scenario
- **Edit Protection**: `requireForEdit()` with default/custom error messages
- **Delete Protection**: `requireForDelete()` with specific error messaging
- **Create Protection**: `requireForCreate()` for new content authorization
- **Flexible**: Works with any auth system (Spring Security, custom, etc.)

**📁 Files Created:**
- `simplypages/src/main/java/io/mindspice/simplypages/editing/AuthWrapper.java` (Authorization utility)
- `demo/src/main/java/io/mindspice/demo/Phase8TestController.java` (Demo with 3 roles, 3 modules)

**🧪 Test Controller Features:**
- Three user roles: ADMIN, EDITOR, VIEWER
- Three modules with different permission levels
- User switcher to test different scenarios
- Full CRUD operations with authorization checks
- Accessible at `/test/phase8?user=[admin|editor|viewer]`

**Design Decisions:**
- **Optional Tool**: Framework provides wrapper, apps choose if/when to use it
- **Non-Invasive**: No modifications to core framework classes
- **Supplier-Based**: Clean functional approach for auth checks
- **Standardized Errors**: Consistent error modals across all operations

**Why Phase 8 Before Phase 7:**
- Phase 7 (Module Migration) is optional - ContentModule already implements EditAdapter
- Phase 8 (AuthWrapper) completes the core editing system architecture
- Branch can be merged without migrating all 16 modules
- Module migration can happen incrementally as needed

### Phase 6.5 Summary (Jan 3, 2026)

**✅ Completed:**
1. Added permission flags to EditableModule (canEdit, canDelete)
2. Added row locking support to EditableRow (canAddModule)
3. Created Phase6_5TestController at `/test/phase6-5` with full CRUD implementation
4. Framework-level permission enforcement

**🔒 Permission System:**
- **Module Locking**: `.withCanEdit(false)` and `.withCanDelete(false)`
- **Row Locking**: `.withCanAddModule(false)`
- All permissions default to `true` (fully editable)
- Enforced at render time (buttons conditionally added)

**📁 Files Modified:**
- `simplypages/src/main/java/io/mindspice/simplypages/modules/EditableModule.java` (permission flags)
- `simplypages/src/main/java/io/mindspice/simplypages/editing/EditableRow.java` (row locking)
- `demo/src/main/java/io/mindspice/demo/Phase6_5TestController.java` (NEW test page, full CRUD endpoints)

**🧪 Test Controller Features:**
- Complete CRUD operations (edit, save, delete, insert-row, add-module)
- Proper OOB swap patterns for HTMX updates
- In-memory storage structure for testing
- Permission flags integrated with demo data
- All endpoints functional at `/test/phase6-5`

**Use Cases Supported:**
- Site branding modules that shouldn't be edited
- Required content that can be updated but not removed
- Fixed layout sections
- Template-based pages with protected elements

**Future Enhancements:**
- Field-level editing restrictions (editableFields)
- Width editing restrictions (canChangeWidth)
- These can be added incrementally as needed

### Phase 6 Summary (Jan 3, 2026)

**✅ Completed:**
1. Enhanced editable-module-wrapper styling (margin-bottom: 24px)
2. Improved editable-row-wrapper (padding, border-radius, hover effects)
3. Professional add-module-section button styling (dashed borders, gray colors)
4. Subtle insert-row-section styling (transparent background)
5. Modal form styling already complete from Phase 1-2
6. Created Phase6TestController at `/test/phase6`

**🎨 CSS Improvements:**
- Better margins and spacing throughout (24px modules, 32px rows)
- Professional color scheme (grays: #e2e8f0, #cbd5e0, #4a5568)
- Smooth transitions (background-color 0.2s)
- Enhanced visual hierarchy with hover effects

**📁 Files Modified:**
- `simplypages/src/main/resources/static/css/framework.css` (CSS enhancements)
- `demo/src/main/java/io/mindspice/demo/Phase6TestController.java` (NEW test page)

**Testing:**
- ✅ Row hover effects work correctly
- ✅ Button styling consistent and professional
- ✅ Form spacing and focus states excellent
- ✅ Visual hierarchy clear

### Phase 5 Summary (Jan 2, 2026)

**✅ Completed:**
1. Created EditableModule wrapper class (`modules/EditableModule.java`)
2. Added framework CSS classes (`.module-edit-btn`, `.module-delete-btn`)
3. Fixed markdown toggle bug in ContentModule
4. Fixed button rendering bug (idempotent buildWrapper)
5. Updated EditingDemoController and EditableRow to use new API
6. Refactored Phase3And4TestController to use EditableModule
7. Deleted old EditableModule from editing package

**🐛 Bugs Fixed:**
- Markdown toggle now works (rebuild content after edits)
- Edit/delete buttons now render correctly (idempotent buildWrapper)
- Z-index managed by framework CSS
- Button styling consistent across all implementations

**📁 Files Modified:**
- `simplypages/src/main/java/io/mindspice/simplypages/modules/EditableModule.java` (NEW)
- `simplypages/src/main/resources/static/css/framework.css` (CSS classes added)
- `simplypages/src/main/java/io/mindspice/simplypages/modules/ContentModule.java` (markdown fix)
- `simplypages/src/main/java/io/mindspice/simplypages/editing/EditableRow.java` (API update)
- `demo/src/main/java/io/mindspice/demo/EditingDemoController.java` (API update)
- `demo/src/main/java/io/mindspice/demo/Phase3And4TestController.java` (refactored)

**Files Deleted:**
- `simplypages/src/main/java/io/mindspice/simplypages/editing/EditableModule.java` (old version)

---

## Quick Reference

**Related Documents**:
- [EDITING_SYSTEM_PLAN.md](EDITING_SYSTEM_PLAN.md) - Implementation plan
- [MODAL_OVERLAY_USAGE.md](MODAL_OVERLAY_USAGE.md) - Correct modal patterns
- [CLAUDE.md](CLAUDE.md) - Project overview

**Key Decisions**:
- Single modal container pattern
- Auto-save by default
- Width is layout concern, not module concern

**Priority Bugs**:
1. Markdown toggle doesn't work
2. Edit/delete buttons not visible when right-aligned

**Priority Framework Gaps**:
1. Button styling not framework-level
2. Z-index not framework-level
3. Button positioning broken
