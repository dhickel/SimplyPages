# Modal Overlay Usage & Z-Index Scale

## Z-Index Scale

To prevent stacking context conflicts, SimplyPages uses a strict Z-index scale defined in `framework.css`.

| Component | Z-Index | Notes |
|-----------|---------|-------|
| Base Content | 1 | Default stacking context |
| Dropdowns | 1000 | Standard dropdown menus |
| Sticky Sidebar | 1050 | Desktop sticky sidebar and mobile slide-out |
| Mobile Toggle | 1051 | Hamburger menu button (must be above sidebar) |
| Modal Backdrop | 1100 | Dark overlay behind modals |
| Modal Container | 1110 | The modal content itself |
| Toasts/Tooltips | 1200+ | (Future use) |

## Usage Guidelines

### Modals
Always use `Modal.create()` or `EditModalBuilder` to generate modals. These components automatically apply the correct classes (`modal-backdrop`, `modal-container`) to ensure they sit above navigation elements.

### Custom Components
If creating custom floating elements, ensure they fit into this scale.
- **Dropdown-like**: Use `1000`
- **Overlay-like**: Use `1100+`

### Troubleshooting
If a component is hidden behind another:
1. Check the `z-index` in DevTools.
2. Verify if a parent creates a new stacking context (e.g., `transform`, `opacity`, `filter`).
3. Ensure the component adheres to the scale above.
