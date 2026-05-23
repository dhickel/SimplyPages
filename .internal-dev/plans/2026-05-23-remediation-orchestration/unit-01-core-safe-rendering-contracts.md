# Context

Core review found that `HtmlTag` and `Attribute` encode values but emit tag and attribute names raw; `HtmlTag.addOrReplaceStyle` uses regex replacement that can corrupt substring properties; null/empty attributes and slot-key typing have ambiguous contracts.

# Goal

Harden core rendering contracts without changing valid existing rendered output except for invalid names/values now rejected earlier and style replacement bugs now producing correct declarations.

# In Scope

- Validate HTML tag names at `HtmlTag` construction.
- Validate attribute names in `withAttribute` / `Attribute`.
- Preserve `data-*`, `aria-*`, `hx-*`, boolean attributes, and normal custom attributes that are valid HTML names.
- Replace regex style merging with property-boundary-safe declaration handling.
- Add tests for tag/attribute-name injection, style collisions, boolean/empty semantics, and slot-key collision behavior.
- Update security/core docs to state exact safety boundary.

# Out of Scope

- Replacing the public `Attribute` record with a large new attribute subsystem unless tests prove it is required.
- Changing valid `style` attribute ordering beyond the changed property's deterministic replacement.
- Runtime type enforcement for `SlotKey<T>` unless scoped as a small compatibility-safe change; otherwise document and test current collision behavior.

# Implementation Steps

1. Add core validators.
   - Target: `simplypages/src/main/java/io/mindspice/simplypages/core/HtmlTag.java` and `Attribute.java`.
   - Tag regex: `^[A-Za-z][A-Za-z0-9:-]*$`.
   - Attribute regex: `^[A-Za-z_:][A-Za-z0-9_:.\\-]*$`.
   - Reject null/blank names with `IllegalArgumentException`.
   - Keep values escaped by existing OWASP encoder path.

2. Replace style declaration replacement.
   - Parse existing style by `;`, trim, split on first `:`.
   - Preserve declaration order for untouched properties.
   - Replace only case-insensitive exact property name matches.
   - Append new property at the end when absent.
   - Continue rejecting declaration breakout in `addStyle`.
   - Continue allowing `addTrustedStyle` for trusted advanced values.

3. Add tests.
   - `HtmlTagTest` / `XssProtectionTest`: invalid tag name, invalid attribute name, valid `data-*`, `aria-*`, `hx-*`.
   - `AddStyleInjectionTest`: `max-width` then `width`, `background-color` then `color`, `min-width` then `width`.
   - `ModuleTest`: `withModuleId(null)` behavior must either reject or intentionally remove id; choose fail-fast if compatible with tests.
   - `SlotKeyTest` / `RenderContextTest`: same-name different generic keys collision is documented behavior or becomes fail-fast if a compatible key type marker is added.

4. Update docs.
   - `docs/security/01-security-boundaries-and-safe-rendering.md`: values, names, safe/unsafe APIs.
   - `docs/core/04-rendering-pipeline-high-and-low-level.md`: style merge and name validation expectations.

# Validation

- `./mvnw -pl simplypages -Dtest=HtmlTagTest,AddStyleInjectionTest,XssProtectionTest,ModuleTest,SlotKeyTest,RenderContextTest test`
- `./mvnw -pl simplypages test`
- Before/after render snapshots for common tags, `hx-*`, `data-*`, ARIA attributes, and style helpers.

# Exit Criteria

- Invalid tag/attribute names fail fast.
- Existing valid generated pages render byte-compatible except expected style replacement corrections.
- Security docs no longer overstate name safety ambiguously.
