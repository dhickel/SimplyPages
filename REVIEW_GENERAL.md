# General Code Review

## Architecture & Consistency

### 1. Re-rendering Bug in Composite Components
**Severity: High**
Many components that build their children dynamically in the `render()` method (e.g., `Card`, `DataTable`, `Select`, `RadioGroup`, `Grid`) suffer from a re-rendering bug.
*   **The Issue**: `HtmlTag` (the base class) maintains a persistent list of `children`. When `render()` calls `super.withChild(...)`, it adds to this list.
*   **Consequence**: If `render()` is called multiple times on the same component instance (e.g., during debugging, logging, or complex caching scenarios), the children are duplicated (doubled, tripled, etc.).
*   **Recommendation**:
    *   **Option A**: Clear children at the start of `render()` in these subclasses, similar to how `Module` does it.
    *   **Option B (Preferred)**: Build the structure in the constructor or builder methods instead of `render()`, so `render()` is idempotent and side-effect free (except for maybe verifying state).
    *   **Option C**: Use a temporary local list of children in `render()` and pass it to a protected helper in `HtmlTag` that renders a provided list instead of `this.children`.

### 2. `HtmlTag.withAttribute` vs. `withClass`
**Severity: Medium**
There is a subtle inconsistency in how attributes are handled.
*   **The Issue**: `HtmlTag.withAttribute("class", "foo")` *overwrites* any existing class attribute. Many subclasses (like `Column`, `Form`) implement `withClass("bar")` which *appends* to the existing class.
*   **Consequence**: If a user calls `card.withAttribute("class", "my-class")`, they might accidentally wipe out the default `card` class, breaking the component's styling.
*   **Recommendation**:
    *   Change `HtmlTag.withAttribute` to handle "class" (and maybe "style") specially, or providing a `addClass` method on `HtmlTag` that appends.
    *   Alternatively, provide `replaceAttribute` and make `withAttribute` append for multi-value attributes (though standard HTML attributes are usually single-value, `class` is the exception).
    *   At minimum, document this behavior clearly or add `withClass` to `HtmlTag` directly so users don't fall back to `withAttribute("class", ...)`.

### 3. `Grid.render()` Overwrites Classes
**Severity: Medium**
*   **The Issue**: `Grid.render()` calls `this.withAttribute("class", "grid grid-cols-" + columns + " gap-" + gap)`.
*   **Consequence**: This overwrites any custom classes added via `withClass(...)` prior to rendering.
*   **Recommendation**: Retrieve existing classes first, append the grid classes, and then set the attribute.

### 4. `Module` inheriting `HtmlTag.withWidth`
**Severity: Low (DX)**
*   **The Issue**: `CLAUDE.md` states `Module` should not use `withWidth`. However, since `Module` extends `HtmlTag`, these methods are present. Calling them breaks the fluent chain (returns `HtmlTag` instead of `Module` subclass), which effectively prevents usage but produces a confusing type error rather than a clear "method not available" message.
*   **Recommendation**: Override `withWidth`, `withMaxWidth`, `withMinWidth` in `Module` and mark them `@Deprecated` or throw an `UnsupportedOperationException` to make it explicit.

### 5. `Row` Auto-wrapping Magic
**Severity: Low (Opinion)**
*   **The Issue**: `Row.withChild()` automatically wraps any non-`Column` component in a `div.col`.
*   **Consequence**: While convenient, this "magic" can be problematic if a user intends to add a non-visual element (like a hidden input, script, or style tag) or a component that handles its own layout properties.
*   **Recommendation**: Consider removing this magic or checking if the component is "displayable" before wrapping. Or, providing an `addRawChild` method for bypassing the wrapper.

## Logic & Functionality

### 1. `HtmlTag.addStyle` Implementation
**Severity: Low**
*   **The Issue**: The regex logic `replaceAll(propertyPattern, "")` in `addStyle` handles simple cases but might be brittle with complex style strings (e.g. inside functional notation like `calc()`).
*   **Recommendation**: For a server-side rendering framework, parsing CSS strings is risky. It might be better to store styles in a `Map<String, String>` and render them all at once in `render()`.

### 2. Thread Safety
**Severity: Info**
*   The framework is explicitly **not thread-safe**. This is well-documented but worth reiterating. Components are request-scoped.

## Missing Functionality

1.  **Style Builder Integration**: `Style` class exists but isn't integrated into `HtmlTag`. Adding `withStyle(Style style)` to `HtmlTag` would be a nice enhancement.
2.  **Event Handling**: No easy way to add raw JS event handlers safely (though `onclick` attribute works). This aligns with the "Minimal JS" philosophy, so maybe acceptable.

## Code Style

*   **Consistency**: The code is generally very consistent and readable.
*   **Naming**: Class and method names are clear and descriptive.
*   **Documentation**: Javadoc is excellent.
