# Modal & Overlay Usage Guide

> **Core Principle**: Z-index management is centralized in `framework.css`. Do not hardcode z-index values in components.

## Z-Index Scale

The framework enforces a strict z-index scale to prevent stacking context conflicts, especially between fixed navigation, dropdowns, and modal overlays.

| Component | Z-Index | Defined In | Notes |
|-----------|---------|------------|-------|
| Base Content | 1 | `body`, default | Standard page content |
| Edit Controls | 100 | `.edit-toolbar` | Toolbar buttons above module content |
| Dropdowns | 1000 | `.dropdown-menu` | Menus, tooltips |
| Mobile Sidebar | 1050 | `.main-sidebar` | Off-canvas navigation on mobile |
| Mobile Toggle | 1051 | `.mobile-sidebar-toggle` | Hamburger button (always visible over sidebar) |
| Modal Backdrop | 1100 | `.modal-backdrop` | Dark overlay covering EVERYTHING else |
| Modal Container | 1110 | `.modal-container` | The active modal dialog |

## Modal Patterns

### 1. Standard Modal (HTMX)
Used for editing, confirmations, and complex interactions.

**Structure**:
```html
<div id="modal-container" class="modal-backdrop">
    <div class="modal-container">
        <!-- Content -->
    </div>
</div>
```

**Java Usage**:
```java
// Create and configure
Modal modal = Modal.create()
    .withTitle("Edit Item")
    .withBody(formContent)
    .withFooter(buttons);

// Render (usually returned by HTMX endpoint)
return modal.render();
```

### 2. Edit System Modals
The editing system uses a dedicated container `#edit-modal-container` in the shell.

**Flow**:
1. User clicks "Edit"
2. HTMX GET requests form
3. Server returns `Modal` component
4. Response targets `#edit-modal-container`
5. `hx-swap="innerHTML"` injects modal

### 3. Z-Index Conflict Resolution

**Problem**: Modal appears *behind* sidebar or top navigation.
**Cause**: Sidebar z-index (1050) > Modal z-index (old default 1000).
**Solution**:
- Ensure Modals use the `.modal-backdrop` class (z-index: 1100).
- Ensure Sidebar uses `.main-sidebar` (z-index: 1050).
- **Never** manually set z-index on inline styles for these structural elements.

## Best Practices

1. **One Modal at a Time**: The framework is designed for single-layer modals. Nested modals are not supported.
2. **Backdrop Click**: Clicking the backdrop should close the modal (handled via JS or HTMX `hx-trigger="click from:body"` patterns).
3. **Focus Management**: Modals should trap focus (future enhancement).
4. **Mobile Responsiveness**: Modals automatically become full-screen on mobile (<768px).

## Troubleshooting

- **Dropdowns appearing over modal**: Check if dropdown z-index > 1110. (Should be 1000).
- **Modal under sidebar**: Verify `framework.css` has `modal-backdrop` at 1100+ and `main-sidebar` at 1050.
- **Input fields not clickable**: Check for invisible overlays with higher z-index.
