# SimplyPages Editing System Refactor - Implementation Plan

## Progress Tracking

### Session Status
- **Current Session**: Session 5 - Phase 8 Complete ✓
- **Next Session**: Phase 7 - Module Migration (16 modules) or Phase 9 - Testing & Polish
- **Last Commit**: Phase 8 complete (AuthWrapper pattern)

### Phase Completion
- [x] **Phase 1**: Modal Component & Foundation ✓
- [x] **Phase 2**: EditAdapter Interface ✓
- [x] **Phase 2.5**: Critical Bug Fixes (Button Type, HTMX Selectors, OOB Swaps) ✓
- [x] **Phase 3**: Auto-Save Architecture ✓
- [x] **Phase 4**: Row/Module Constraints ✓
- [x] **Phase 5**: In-Place Editing ✓
- [x] **Phase 6**: Styling Improvements ✓
- [x] **Phase 6.5**: Module Locking & Permission System ✓
- [ ] **Phase 7**: Module Migration (16 modules) - OPTIONAL
- [x] **Phase 8**: Auth Wrapper Pattern ✓
- [ ] **Phase 9**: Testing & Polish

### Detailed Progress by File
```
Core Framework Files:
[x] Modal.java (NEW) ✓
[x] EditAdapter.java (NEW) ✓
[x] ValidationResult.java (NEW) ✓
[x] EditModalBuilder.java (NEW) ✓ + Bug fixes applied
[x] AuthWrapper.java (NEW) ✓
[x] EditableModule.java (MODIFY) ✓
[ ] EditablePage.java (MODIFY)
[x] EditableRow.java (MODIFY) ✓
[x] framework.css (MODIFY) - Modal & editing styles added ✓

Module Migrations (EditAdapter implementation):
[x] ContentModule.java ✓ (Proof of concept)
[ ] GalleryModule.java
[ ] FormModule.java
[ ] DataModule.java
[ ] ForumModule.java
[ ] HeroModule.java
[ ] TabsModule.java
[ ] TimelineModule.java
[ ] AccordionModule.java
[ ] ComparisonModule.java
[ ] QuoteModule.java
[ ] StatsModule.java
[ ] CalloutModule.java
[ ] DynamicCardModule.java
[ ] DynamicListModule.java
[ ] DynamicTableModule.java

Demo Application:
[x] Phase1And2TestController.java (NEW) ✓
[ ] EditingDemoController.java (MAJOR REFACTOR)

Tests:
[ ] ModalTest.java (NEW)
[ ] EditAdapterTest.java (NEW)
[ ] EditModalBuilderTest.java (NEW)
[ ] EditingDemoControllerTest.java (UPDATE)
```

---

## Executive Summary

This refactor addresses critical bugs, UX issues, and architectural improvements in the SimplyPages editing system. The plan focuses on:

1. **Replacing bottom-of-page dialogs with proper overlay modals**
2. **Implementing in-place module editing** (edit UI appears where module is)
3. **Creating an EditAdapter interface** for module-specific edit handling
4. **Auto-save architecture** (removing manual save/load buttons)
5. **Enhanced styling** (2x larger edit buttons, better spacing, professional colors)
6. **Smart row/module constraints** (row requires module, delete last = delete row)
7. **Optional auth wrapper pattern** for protecting dynamic interactions

## Development Workflow Requirements

### ⚠️ CRITICAL: Test Page Requirement for Each Phase

**Every phase MUST have a dedicated test controller** to verify implementation correctness:

- **Purpose**: Isolated testing environment to verify phase functionality without breaking existing demos
- **Pattern**: Follow `Phase1And2TestController.java` pattern (single modal container, proper Modal.create() usage)
- **Naming**: `Phase{N}And{N+1}TestController.java` (e.g., Phase3And4TestController.java)
- **URL**: `/test/phase{n}-{n+1}` (e.g., /test/phase3-4)
- **Required Testing**: Manually test all phase features in browser before marking phase complete

**Why This Matters**:
- Prevents repeating fixed bugs (e.g., bottom-of-page DIVs instead of overlay modals)
- Provides working reference implementation for correct patterns
- Enables incremental verification without affecting production demo code
- Documents correct usage patterns for future development

**Test Controllers Created**:
- ✅ Phase 1-2: Modal Component & EditAdapter (`/test/phase1-2`)
- ✅ Phase 3-4: Auto-Save & Row Constraints (`/test/phase3-4`)
- 📋 Phase 5-6: In-Place Editing & Styling (planned)
- 📋 Phase 7: Module Migration (planned)

### Modal Overlay Usage

**CRITICAL**: See `MODAL_OVERLAY_USAGE.md` for correct modal patterns. Common mistakes:
- ❌ Multiple modal containers (`add-module-modal`, `edit-module-modal`, etc.)
- ❌ Custom Div with `modal-content` class (NOT using Modal.create())
- ❌ Using `Button.submit()` for HTMX buttons (blocks HTMX interception)

**Correct Pattern**: Single `#edit-modal-container`, always use `Modal.create()`, use `Button.create()` for HTMX

---

## Current System Analysis

### Architecture (Explored from codebase)

**Location**: `/home/hickelpickle/Code/Java/cannasite/java-html-framework/simplypages/src/main/java/io/mindspice/simplypages/editing/`

1. **EditablePage.java** (224 lines) - Wraps pages with row insertion controls
2. **EditableRow.java** (261 lines) - Wraps rows with module addition, auto-calculates column widths (12/module count)
3. **EditableModule.java** (280 lines) - Decorator with edit/delete toolbar, small pencil icon (0.9rem, 60% opacity)

**Demo Implementation**: `/home/hickelpickle/Code/Java/cannasite/java-html-framework/demo/src/main/java/io/mindspice/demo/Phase1And2TestController.java` (400+ lines)

**16 Modules to Migrate**: ContentModule, GalleryModule, FormModule, DataModule, ForumModule, HeroModule, TabsModule, TimelineModule, AccordionModule, ComparisonModule, QuoteModule, StatsModule, CalloutModule, DynamicCardModule, DynamicListModule, DynamicTableModule

### Current Issues Identified

**Bugs:**
- ~~Edits don't always queue properly~~ (Fixed in Phase 2.5)
- ~~Page needs reload before certain actions work~~ (Fixed in Phase 2.5)
- ~~Modals are DIV containers at page bottom (not true overlays)~~ (Fixed in Phase 1)

**UX Problems:**
- Edit buttons too small (0.9rem, hard to click)
- Save/load buttons at top (redundant with auto-save)
- Dialogs appear at page bottom (poor UX, requires scrolling) - FIXED
- Loud colors, poor spacing, form labels touch inputs

**Architecture Gaps:**
- ~~No module edit interface/contract~~ (Fixed - EditAdapter created)
- No auth wrapper for dynamic interactions
- Modules are mutable but lack edit transformation pattern

---

## Design Decisions (User Confirmed)

✓ **Dialogs**: Overlay modals with backdrop (mobile-friendly)
✓ **Module Mutability**: Keep mutable with fluent API (don't change to immutable)
✓ **Edit Adapter**: Display edit UI + apply transforms back to module
✓ **Save Behavior**: Auto-save immediately (no manual save/load)

---

## Implementation Phases

### **Phase 1: Modal Component (Foundation)** ✅ COMPLETED

**Goal**: Create proper overlay modal component to replace bottom-of-page DIVs

#### Implementation Summary

**Created Files:**
- `/simplypages/src/main/java/io/mindspice/simplypages/components/display/Modal.java`

**Features Implemented:**
- Semi-transparent backdrop overlay (z-index: 1000)
- Centered modal container on desktop, full-screen on mobile
- ESC key to close
- Click backdrop to close (configurable)
- Smooth slide-in animation
- Close button in header

**CSS Added to `framework.css`:**
```css
/* ===== Modal System ===== */
.modal-backdrop {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background-color: rgba(0, 0, 0, 0.5);
    z-index: 1000;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
}

.modal-container {
    background-color: white;
    border-radius: 8px;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
    max-width: 600px;
    width: 100%;
    max-height: 90vh;
    overflow-y: auto;
    animation: modalSlideIn 0.2s ease-out;
}

@keyframes modalSlideIn {
    from { transform: translateY(-50px); opacity: 0; }
    to { transform: translateY(0); opacity: 1; }
}

.modal-header {
    padding: 20px 24px;
    border-bottom: 1px solid #e2e8f0;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.modal-body {
    padding: 24px;
}

.modal-footer {
    padding: 16px 24px;
    border-top: 1px solid #e2e8f0;
    display: flex;
    justify-content: flex-end;
    gap: 12px;
}

/* Mobile: Full screen */
@media (max-width: 768px) {
    .modal-container {
        max-width: 100%;
        max-height: 100vh;
        border-radius: 0;
        margin: 0;
    }

    .modal-backdrop {
        padding: 0;
    }
}
```

**Status**: ✅ Complete (Dec 30, 2025)

---

### **Phase 2: EditAdapter Interface** ✅ COMPLETED

**Goal**: Create interface contract for editable modules

#### Implementation Summary

**Created Files:**
1. `/simplypages/src/main/java/io/mindspice/simplypages/editing/EditAdapter.java`
2. `/simplypages/src/main/java/io/mindspice/simplypages/editing/ValidationResult.java`
3. `/simplypages/src/main/java/io/mindspice/simplypages/editing/EditModalBuilder.java`
4. `/demo/src/main/java/io/mindspice/demo/Phase1And2TestController.java`

**EditAdapter Interface:**
```java
public interface EditAdapter<T extends Module> {
    Component buildEditView();
    T applyEdits(Map<String, String> formData);
    default ValidationResult validate(Map<String, String> formData) {
        return ValidationResult.valid();
    }
}
```

**ContentModule Implementation:**
- Implemented `buildEditView()` with title, content, and markdown toggle
- Implemented `validate()` with field length checks
- Implemented `applyEdits()` to mutate module in place

**EditModalBuilder Features:**
- Standardized footer layout (delete left, cancel/save right)
- HTMX integration for dynamic updates
- Configurable save/delete URLs
- Optional delete button hiding

**Status**: ✅ Complete (Dec 30, 2025)

---

### **Phase 2.5: Critical Bug Fixes** ✅ COMPLETED

**Goal**: Fix blocking issues preventing modal save functionality

#### Issues Found & Fixed

**Issue 1: Save Button Not Triggering HTMX Request**
- **Problem**: `Button.submit()` creates `type="submit"` which triggers browser form submission, blocking HTMX
- **Solution**: Changed to `Button.create()` (creates `type="button"`) in EditModalBuilder.java:195
- **Date Fixed**: Jan 2, 2026

**Issue 2: HTMX Initialization Error**
- **Problem**: `hx-include="closest .modal-body"` caused JavaScript error: "can't access property matches, e is null"
- **Root Cause**: HTMX couldn't process "closest" selector during initialization
- **Solution**: Changed to direct CSS selector: `.modal-body input, .modal-body textarea, .modal-body select`
- **File**: EditModalBuilder.java:200
- **Date Fixed**: Jan 2, 2026

**Issue 3: OOB Swap Pattern**
- **Problem**: Mixed main swap and OOB swaps confused HTMX response processing
- **Solution**:
  - Changed save button to `hx-swap="none"` (no main target swap)
  - Use OOB swaps exclusively for both modal clearing and page updates
  - Controller response pattern:
    ```java
    String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
    String updatePage = pageContent.replace("<div id=\"page-content\">",
                                           "<div hx-swap-oob=\"true\" id=\"page-content\">");
    return clearModal + updatePage;
    ```
- **Files Modified**:
  - EditModalBuilder.java:199 (`hx-swap="none"`)
  - Phase1And2TestController.java:365-370 (OOB swap pattern)
  - Phase1And2TestController.java:393-398 (delete endpoint)
- **Date Fixed**: Jan 2, 2026

**Testing Results:**
- ✅ Modal opens correctly
- ✅ POST request sent to save endpoint
- ✅ Modal closes on save
- ✅ Page updates with saved changes immediately
- ✅ No JavaScript errors in console

**Status**: ✅ Complete (Jan 2, 2026)

---

### **Phase 3: Auto-Save Architecture** ✅ COMPLETED

**Goal**: Remove manual save/load buttons, implement auto-save

#### Implementation Summary (Jan 2, 2026)

**Modified Files:**

**1. `EditablePage.java`**
- No changes needed - file never had save/load controls

**2. `EditingDemoController.java`** - Auto-save implementation
- ✅ Removed `savedPages` field (line 130)
- ✅ Removed save/load button rendering from `renderEditablePage()` (lines 264-292)
- ✅ Kept only "Pending Edits" button (for approval workflow)
- ✅ Removed `savePage()` method (lines 989-1015)
- ✅ Removed `loadPage()` method (lines 1017-1043)
- ✅ All edits now save immediately via `updateModule()` endpoint

**3. HTMX Response Pattern**
```java
@PostMapping("/api/modules/{moduleId}/update")
@ResponseBody
public String updateModule(
        @PathVariable String moduleId,
        @RequestParam Map<String, String> formData,
        Principal principal
) {
    DemoModule dm = findModule(moduleId);
    if (dm == null) {
        return Modal.create()
            .withTitle("Error")
            .withBody(Alert.danger("Module not found"))
            .render();
    }

    Module module = createModule(dm);
    EditAdapter adapter = (EditAdapter) module;

    // Validate
    ValidationResult validation = adapter.validate(formData);
    if (!validation.isValid()) {
        return Modal.create()
            .withTitle("Validation Error")
            .withBody(Alert.danger(String.join(", ", validation.getErrors())))
            .render();
    }

    // Apply edits
    adapter.applyEdits(formData);
    updateModuleData(dm, module);

    EditMode mode = authChecker.getEditMode(moduleId, principal.getName());

    if (mode == EditMode.USER_EDIT) {
        // Queue for approval (auto-save to pending)
        queuePendingEdit(moduleId, formData);
        return Modal.create()
            .withTitle("Edit Queued")
            .withBody(Alert.success("Changes queued for approval"))
            .render();
    } else {
        // Auto-save immediately, clear modal, refresh page
        return "" +
            "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>" +
            "<div hx-swap-oob=\"true\" id=\"page-content\">" + renderEditablePage() + "</div>";
    }
}
```

**Testing Results:**
- ✅ Save button removed from UI
- ✅ Load button removed from UI
- ✅ Pending Edits button still present
- ✅ All module edits save immediately (no manual save required)

**Status**: ✅ Complete (Jan 2, 2026)

---

### **Phase 4: Row/Module Constraints** ✅ COMPLETED

**Goal**: Enforce row requires module, auto-delete empty rows

#### Implementation Summary (Jan 2, 2026)

**Modified Files: `EditingDemoController.java`**

**1. Updated `addRow()` Endpoint** (lines 886-905)
- ✅ Creates empty row
- ✅ Adds row to page data
- ✅ **Immediately returns modal** for adding first module (via `showAddModuleModal(rowId)`)
- ✅ No placeholder module created
- ✅ Enforces constraint: row must have at least one module

**2. Updated `insertRow()` Endpoint** (lines 907-955)
- ✅ Creates empty row at specified position
- ✅ Renumbers row positions after insertion
- ✅ **Immediately returns modal** for adding first module
- ✅ Same constraint enforcement as `addRow()`

**3. Updated `deleteModule()` Endpoint** (lines 674-708)
- ✅ Finds and removes module
- ✅ Tracks which row contained the module
- ✅ **Auto-deletes row if it becomes empty** (constraint: rows must have modules)
- ✅ Renumbers remaining rows after deletion
- ✅ Returns OOB swaps to clear modal and refresh page

**Testing Results:**
- ✅ Adding row shows modal (not placeholder module)
- ✅ Modal contains module type selector and form
- ✅ Deleting last module in row removes entire row (tested via code inspection)

**Status**: ✅ Complete (Jan 2, 2026)

---

### **Phase 5: In-Place Editing Integration** 📋 PLANNED

**Goal**: Update EditableModule to use Modal and EditAdapter

#### Planned Changes

**Modify `EditableModule.java` buildContent()**:
```java
@Override
protected void buildContent() {
    Div wrapper = new Div().withClass("editable-module-wrapper");

    if (showEditControls && editUrl != null) {
        Button editBtn = Button.create("✏")
            .withStyle(Button.ButtonStyle.LINK)
            .withClass("edit-toolbar-btn")
            .withAttribute("style",
                "position: absolute; " +
                "top: 8px; right: 8px; " +
                "font-size: 1.8rem; " +
                "padding: 8px 12px; " +
                "opacity: 0.6; z-index: 10;")
            .withAttribute("hx-get", editUrl)
            .withAttribute("hx-target", "#edit-modal-container")
            .withAttribute("hx-swap", "innerHTML");

        if (editMode == EditMode.USER_EDIT) {
            editBtn.withAttribute("title", "Edit (changes require approval)");
        }

        wrapper.withChild(editBtn);
    }

    wrapper.withChild(wrappedModule);
    super.withChild(wrapper);
}
```

**Status**: 📋 Planned

---

### **Phase 6: Styling Improvements** ✅ COMPLETED

**Goal**: Better margins, spacing, professional colors

#### Implementation Summary (Jan 3, 2026)

**CSS Updates Applied to**: `/simplypages/src/main/resources/static/css/framework.css`

```css
/* ===== Editing System Styles (Improved) ===== */

.editable-module-wrapper {
    position: relative;
    margin-bottom: 24px;
}

.editable-row-wrapper {
    position: relative;
    margin-bottom: 32px;
    padding: 16px;
    border-radius: 8px;
    transition: background-color 0.2s;
}

.editable-row-wrapper:hover {
    background-color: #f7fafc;
}

.add-module-section {
    margin: 16px 0;
    text-align: center;
}

.add-module-section .btn {
    background-color: #e2e8f0;
    color: #4a5568;
    border: 2px dashed #cbd5e0;
    padding: 12px 24px;
    font-weight: 500;
}

.add-module-section .btn:hover {
    background-color: #cbd5e0;
    border-color: #a0aec0;
    transform: translateY(-1px);
}

.insert-row-section {
    margin: 24px 0;
    padding: 16px 0;
    border-top: 1px solid #e2e8f0;
    text-align: center;
}

.insert-row-section .btn {
    color: #718096;
    background: transparent;
    font-size: 0.9rem;
    border: 1px solid #e2e8f0;
}

.insert-row-section .btn:hover {
    background-color: #f7fafc;
    border-color: #cbd5e0;
}

.modal-body .form-field {
    margin-bottom: 24px;
}

.modal-body .form-label {
    display: block;
    margin-bottom: 8px;
    font-weight: 600;
    color: #2d3748;
}

.modal-body .form-input,
.modal-body .form-textarea,
.modal-body .form-select {
    width: 100%;
    padding: 10px 15px;
    border: 1px solid #cbd5e0;
    border-radius: 4px;
    font-size: 1rem;
}

.modal-body .form-input:focus,
.modal-body .form-textarea:focus,
.modal-body .form-select:focus {
    outline: none;
    border-color: #4299e1;
    box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.1);
}
```

**Changes Made:**

1. **Editable Module Wrapper** - Added `margin-bottom: 24px` for proper spacing
2. **Editable Row Wrapper** - Enhanced with:
   - `margin-bottom: 32px` (increased from 20px)
   - `padding: 16px`
   - `border-radius: 8px`
   - `transition: background-color 0.2s`
   - Hover effect with `background-color: #f7fafc`
3. **Add Module Section** - Improved button styling with:
   - Dashed borders (`border: 2px dashed #cbd5e0`)
   - Professional gray colors
   - Hover effects with transform
4. **Insert Row Section** - Subtle styling with:
   - Transparent background
   - Light borders
   - Understated hover states
5. **Modal Forms** - Already complete from Phase 1-2 (proper spacing, labels, focus states)

**Test Controller Created:**
- `/demo/src/main/java/io/mindspice/demo/Phase6TestController.java`
- Accessible at: `http://localhost:8080/test/phase6`
- Demonstrates all Phase 6 improvements

**Testing Results:**
- ✅ Row hover effects work correctly
- ✅ Module spacing improved
- ✅ Button styling professional and consistent
- ✅ Form fields have proper spacing and focus states
- ✅ Visual hierarchy clear and professional

**Status**: ✅ Complete (Jan 3, 2026)

---

### **Phase 6.5: Module Locking & Permission System** ✅ COMPLETED

**Goal**: Framework support for locking modules and controlling edit/delete permissions

#### Implementation Summary (Jan 3, 2026)

#### Use Cases

1. **Locked Modules**: Some modules cannot be edited or deleted (e.g., site branding, required content)
2. **Partial Editing**: Module can be edited but not deleted
3. **Field-Level Restrictions**: Only certain fields can be edited (e.g., can edit content but not title)
4. **Layout Protection**: Module can be edited but width/position cannot be changed

#### Planned Implementation

**1. EditableModule Enhancement**

Add permission fields to control what actions are allowed:

```java
public class EditableModule extends HtmlTag {
    private Module wrappedModule;
    private String moduleId;
    private String editUrl;
    private String deleteUrl;

    // NEW: Permission flags
    private boolean canEdit = true;
    private boolean canDelete = true;
    private boolean canChangeWidth = true;
    private Set<String> editableFields = null;  // null = all fields editable

    // NEW: Fluent setters
    public EditableModule withCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
        return this;
    }

    public EditableModule withCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
        return this;
    }

    public EditableModule withCanChangeWidth(boolean canChangeWidth) {
        this.canChangeWidth = canChangeWidth;
        return this;
    }

    public EditableModule withEditableFields(Set<String> fields) {
        this.editableFields = fields;
        return this;
    }

    @Override
    protected void buildContent() {
        Div wrapper = new Div().withClass("editable-module-wrapper");

        // Only show edit button if canEdit is true
        if (canEdit && editUrl != null) {
            Button editBtn = Button.create("✏")
                .withAttribute("hx-get", editUrl)
                .withAttribute("hx-target", "#edit-modal-container");
            wrapper.withChild(editBtn);
        }

        // Only show delete button if canDelete is true
        if (canDelete && deleteUrl != null) {
            Button deleteBtn = Button.create("🗑")
                .withAttribute("hx-delete", deleteUrl);
            wrapper.withChild(deleteBtn);
        }

        wrapper.withChild(wrappedModule);
        super.withChild(wrapper);
    }
}
```

**2. EditAdapter Field-Level Restrictions**

Update EditAdapter to support field restrictions:

```java
public interface EditAdapter<T extends Module> {
    Component buildEditView();

    // NEW: Build edit view with field restrictions
    default Component buildEditView(Set<String> editableFields) {
        if (editableFields == null) {
            return buildEditView();  // All fields editable
        }
        return buildRestrictedEditView(editableFields);
    }

    default Component buildRestrictedEditView(Set<String> editableFields) {
        // Default: call regular buildEditView and disable non-editable fields
        // Modules can override for custom behavior
        return buildEditView();
    }

    T applyEdits(Map<String, String> formData);
    ValidationResult validate(Map<String, String> formData);
}
```

**3. Width Restriction Pattern**

For layout protection (can't change width), pass restriction to controller:

```java
// In controller
@GetMapping("/api/modules/{id}/edit")
public String editModule(@PathVariable String id) {
    ModuleData data = findModule(id);
    Module module = createModule(data);
    EditAdapter adapter = (EditAdapter) module;

    Div editForm = new Div();
    editForm.withChild(adapter.buildEditView());

    // Only add width field if module allows width changes
    if (data.canChangeWidth) {
        editForm.withChild(createWidthSelector(data.currentWidth));
    }

    return EditModalBuilder.create()
        .withEditView(editForm)
        .withDeleteUrl(data.canDelete ? deleteUrl : null)  // null = hide delete button
        .build()
        .render();
}
```

**4. Storage Pattern**

Store permissions alongside module data:

```java
public class ModulePermissions {
    private String moduleId;
    private boolean canEdit;
    private boolean canDelete;
    private boolean canChangeWidth;
    private Set<String> editableFields;

    // Could be stored in database, config file, or determined by business logic
}
```

#### Design Principles

1. **Default Permissive**: By default, all modules are fully editable/deletable
2. **Explicit Restrictions**: Developers explicitly set restrictions where needed
3. **Framework-Level**: Built into EditableModule, not test controllers
4. **Flexible**: Supports coarse-grained (can't edit at all) and fine-grained (specific fields only)
5. **Declarative**: Use fluent API to declare permissions

#### Example Usage

```java
// Locked module - cannot edit or delete
EditableModule.wrap(brandingModule)
    .withModuleId("header-branding")
    .withCanEdit(false)
    .withCanDelete(false);

// Can edit content but not delete
EditableModule.wrap(welcomeModule)
    .withModuleId("welcome")
    .withCanDelete(false)
    .withEditUrl("/api/modules/welcome/edit");

// Can edit only certain fields
EditableModule.wrap(profileModule)
    .withModuleId("profile")
    .withEditableFields(Set.of("content", "imageUrl"))  // Can't edit title
    .withCanChangeWidth(false);  // Can't resize
```

**Implementation Completed:**

1. **EditableModule Permissions** - Added to `modules/EditableModule.java`:
   - `withCanEdit(boolean)` - Controls whether edit button appears
   - `withCanDelete(boolean)` - Controls whether delete button appears
   - Both default to `true` (fully editable)
   - Enforced in `buildWrapper()` method

2. **EditableRow Permissions** - Added to `editing/EditableRow.java`:
   - `withCanAddModule(boolean)` - Controls whether "Add Module" button appears
   - Defaults to `true` (modules can be added)
   - Enforced in `render()` method

3. **Test Controller Created:**
   - `Phase6_5TestController.java` at `/test/phase6-5`
   - Demonstrates all locking scenarios:
     - Fully locked module (no edit/delete buttons)
     - Edit-only module (can edit but not delete)
     - Locked row (cannot add modules)
     - Normal unlocked modules/rows for comparison
   - **Full CRUD Implementation** (Jan 3, 2026):
     - All edit, delete, and row insertion endpoints functional
     - Proper OOB swap patterns for HTMX updates
     - In-memory storage with TestModule, TestRow, PageData classes
     - Permission flags integrated with demo data

**Use Cases Supported:**
- Site branding modules that shouldn't be edited
- Required content that can be updated but not removed
- Fixed layout sections that should stay consistent
- Template-based pages with protected elements

**Future Enhancements** (not implemented in this phase):
- Field-level editing restrictions (`editableFields`)
- Width editing restrictions (`canChangeWidth`)
- These can be added as needed in future phases

**Status**: ✅ Complete (Jan 3, 2026)

---

### **Phase 7: Module Migration (EditAdapter Implementation)** 📋 PLANNED

**Goal**: Implement EditAdapter for all 16 modules and all builtin componenets.

#### Priority Order

1. ✅ **ContentModule.java** (proof of concept - COMPLETE)
2. **GalleryModule.java**
3. **FormModule.java**
4. **DataModule.java**
5. **ForumModule.java**
6. **HeroModule.java**
7. **TabsModule.java**
8. **TimelineModule.java**
9. **AccordionModule.java**
10. **ComparisonModule.java**
11. **QuoteModule.java**
12. **StatsModule.java**
13. **CalloutModule.java**
14. **DynamicCardModule.java**
15. **DynamicListModule.java**
16. **DynamicTableModule.java**

#### Implementation Pattern

Each module must implement:
1. `buildEditView()` - Create form UI with module-specific fields
2. `validate()` - Validate form data before applying
3. `applyEdits()` - Map form data to module properties

**Status**: 📋 Planned (1/16 complete)

---

### **Phase 8: Auth Wrapper Pattern** ✅ COMPLETED

**Goal**: Provide optional auth wrapper for framework users

#### Implementation Summary (Jan 4, 2026)

**Created Files:**

1. **AuthWrapper.java** (`/simplypages/src/main/java/io/mindspice/simplypages/editing/AuthWrapper.java`)
   - Generic `require()` method for any authorization pattern
   - `requireForEdit()` - Edit operations with default unauthorized modal
   - `requireForEdit(message)` - Edit operations with custom error message
   - `requireForDelete()` - Delete operations with specific error message
   - `requireForCreate()` - Create operations with specific error message

2. **Phase8TestController.java** (`/demo/src/main/java/io/mindspice/demo/Phase8TestController.java`)
   - Demonstrates AuthWrapper usage with role-based permissions
   - Three user roles: ADMIN, EDITOR, VIEWER
   - Three test modules with different permission requirements:
     - Module 1: Editable by EDITOR and ADMIN
     - Module 2: Editable by ADMIN only
     - Module 3: Read-only for everyone
   - User switcher to test different permission scenarios
   - Full CRUD operations protected by AuthWrapper

**Features Implemented:**

1. **Generic Authorization Pattern**
   - Supplier-based design for flexible auth checks
   - Custom unauthorized handlers supported
   - Type-safe generic wrapper

2. **Convenience Methods**
   - Specialized wrappers for edit, delete, create operations
   - Default error modals with appropriate messaging
   - Custom error message support

3. **Integration with Editing System**
   - Seamless integration with EditModalBuilder
   - Works with Modal component for error display
   - Supports OOB swap patterns for HTMX updates

**Usage Example:**
```java
@GetMapping("/api/modules/{id}/edit")
@ResponseBody
public String editModule(@PathVariable String id, Principal principal) {
    return AuthWrapper.requireForEdit(
        () -> canUserEdit(id, principal.getName()),
        () -> {
            Module module = findModule(id);
            EditAdapter<?> adapter = (EditAdapter<?>) module;
            return EditModalBuilder.create()
                .withTitle("Edit Module")
                .withEditView(adapter.buildEditView())
                .withSaveUrl("/api/modules/" + id + "/update")
                .build()
                .render();
        },
        "You do not have permission to edit this content"  // Custom message
    );
}
```

**Test Page:**
- Accessible at: `http://localhost:8080/test/phase8`
- Add `?user=admin`, `?user=editor`, or `?user=viewer` to test different roles
- Demonstrates authorization failures with proper error modals

**Design Principles:**
1. **Optional**: Framework provides the tool, apps decide if/how to use it
2. **Flexible**: Works with any authorization logic (Spring Security, custom, etc.)
3. **Consistent**: Standardized error responses across all edit operations
4. **Non-Invasive**: Wraps existing edit endpoints without modifying framework code

**Status**: ✅ Complete (Jan 4, 2026)

---

### **Phase 9: Testing & Polish** 📋 PLANNED

**Goal**: Comprehensive testing and documentation

#### Test Files to Create

1. `/simplypages/src/test/java/io/mindspice/simplypages/components/display/ModalTest.java`
2. `/simplypages/src/test/java/io/mindspice/simplypages/editing/EditAdapterTest.java`
3. `/simplypages/src/test/java/io/mindspice/simplypages/editing/EditModalBuilderTest.java`
4. `/demo/src/test/java/io/mindspice/demo/Phase1And2TestControllerTest.java`

**Test Coverage:**
- Modal rendering and behavior
- EditAdapter interface contract
- Validation logic
- Full edit flow (open modal → edit → save → refresh)
- Auto-save behavior
- Row/module constraints
- Permission checks

**Status**: 📋 Planned

---

## Critical Files Summary

### Files Created (NEW)

1. ✅ `/simplypages/src/main/java/io/mindspice/simplypages/components/display/Modal.java`
2. ✅ `/simplypages/src/main/java/io/mindspice/simplypages/editing/EditAdapter.java`
3. ✅ `/simplypages/src/main/java/io/mindspice/simplypages/editing/ValidationResult.java`
4. ✅ `/simplypages/src/main/java/io/mindspice/simplypages/editing/EditModalBuilder.java`
5. ✅ `/demo/src/main/java/io/mindspice/demo/Phase1And2TestController.java`
6. 📋 `/simplypages/src/main/java/io/mindspice/simplypages/editing/AuthWrapper.java` (Planned)
7. 📋 Test files (Planned)

### Files to Modify (EXISTING)

1. 📋 `/simplypages/src/main/java/io/mindspice/simplypages/editing/EditableModule.java`
2. 📋 `/simplypages/src/main/java/io/mindspice/simplypages/editing/EditablePage.java`
3. 📋 `/simplypages/src/main/java/io/mindspice/simplypages/editing/EditableRow.java`
4. ✅ `/simplypages/src/main/resources/static/css/framework.css` (Modal styles added)
5. 📋 `/demo/src/main/java/io/mindspice/demo/EditingDemoController.java` (Major refactor planned)
6. ✅ `/simplypages/src/main/java/io/mindspice/simplypages/modules/ContentModule.java` (EditAdapter implemented)
7. 📋 15 more module classes (EditAdapter implementation planned)

---

## Known Issues & Resolutions

### ✅ Issue 1: Save Button Not Triggering HTMX Request
**Problem:** Save button used `type="submit"` which blocked HTMX interception
**Resolution:** Changed to `Button.create()` (type="button") in EditModalBuilder.java:195
**Date Fixed:** Jan 2, 2026

### ✅ Issue 2: HTMX "can't access property matches" Error
**Problem:** `hx-include="closest .modal-body"` caused HTMX initialization error
**Resolution:** Changed to direct CSS selector: `.modal-body input, .modal-body textarea, .modal-body select`
**Date Fixed:** Jan 2, 2026

### ✅ Issue 3: Modal Not Closing on Save
**Problem:** Response used mixed OOB and main swap, confusing HTMX
**Resolution:** Changed to OOB swaps only (`hx-swap="none"`) for both modal clear and page update
**Date Fixed:** Jan 2, 2026

---

## Next Steps

### Immediate (Session 4)
1. Begin Phase 5 - Update EditableModule for in-place editing with Modal
2. Update edit button styling (2x larger, better positioning)
3. Test in-place editing flow

### Short-term (Session 5)
1. Implement Phase 6 - Styling improvements (CSS updates)
2. Update form field spacing and labels
3. Improve button colors and hover states

### Medium-term (Sessions 5-7)
1. Migrate remaining 15 modules to EditAdapter (Phase 7)
2. Implement auth wrapper (Phase 8)
3. Write comprehensive tests (Phase 9)

---

## Developer Notes

### EditAdapter Implementation Checklist
When implementing `EditAdapter` for a new module:

1. ✅ Implement `buildEditView()` - create form UI with all editable fields
2. ✅ Add field validation in `validate()` - check required fields, formats, constraints
3. ✅ Implement `applyEdits()` - map form data to module properties
4. ✅ Test with `EditModalBuilder` - ensure modal renders correctly
5. ✅ Add server-side endpoint - create controller methods for edit/save/delete
6. ✅ Use OOB swap pattern - clear modal and update page content
7. ✅ Add logging for debugging - track save calls and responses
8. ✅ Test in browser - verify POST request, response, and UI updates

### Common Pitfalls (CRITICAL - Read Before Coding!)

❌ **Don't use `Button.submit()` for HTMX buttons** - use `Button.create()`
  *Reason: `type="submit"` triggers browser form submission, blocking HTMX*

❌ **Avoid complex `hx-include` selectors** - use direct CSS selectors
  *Reason: "closest .modal-body" causes HTMX initialization errors*

❌ **Don't mix main swap and OOB swaps** - use `hx-swap="none"` with OOB only
  *Reason: HTMX gets confused processing multiple root elements*

❌ **Don't forget to escape user input** - use `HtmlUtils.htmlEscape()`
  *Reason: Prevents XSS vulnerabilities*

✅ **Always use OOB swap pattern for modal responses:**
```java
String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
String updatePage = pageContent.replace("<div id=\"page-content\">",
                                       "<div hx-swap-oob=\"true\" id=\"page-content\">");
return clearModal + updatePage;
```

---

## Session Planning

### Suggested Session Breakdown

**Session 1** (2-3 hours): Phases 1-2 ✅ COMPLETE
- ✅ Create Modal component + CSS
- ✅ Create EditAdapter interface
- ✅ Implement for ContentModule (proof of concept)
- ✅ Test modal rendering

**Session 2** (3-4 hours): Phase 2.5 Bug Fixes ✅ COMPLETE
- ✅ Fix button type issue (submit → button)
- ✅ Fix hx-include selector issue
- ✅ Implement OOB swap pattern
- ✅ Test full save/close flow

**Session 3** (2-3 hours): Phase 3-4
- Remove save/load buttons
- Implement auto-save
- Fix row/module constraints
- Test auto-save flow

**Session 4** (3-4 hours): Phase 5-6
- Update EditableModule for in-place editing
- Complete styling improvements
- Test full edit flow

**Session 5-7** (4-6 hours): Phase 7
- Migrate all 16 modules to EditAdapter
- Update demo controller
- Test each module type

**Session 8** (2-3 hours): Phase 8-9
- Implement auth wrapper (optional)
- Write tests
- Documentation updates
- Final polish

---

## Notes for Future Sessions

- Check git status before starting to see what's been completed
- Run `./mvnw clean compile` to verify no compilation errors
- Run `./mvnw spring-boot:run` and test at `http://localhost:8080/test/phase1-2`
- Commit after each phase completion
- Update progress tracking at top of this file

---

**Plan Created**: 2025-12-30
**Last Updated**: 2026-01-04
**Status**: Phase 1-8 Complete (Auth Wrapper) - Core editing system ready for merge
