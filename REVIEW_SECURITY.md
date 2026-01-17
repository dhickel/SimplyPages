# Security and Bug Review

## Critical Bugs

### 1. Broken `withClass` Implementation
**Severity: Critical**
*   **The Issue**: Multiple components (`TextInput`, `Badge`, `Button`, `Alert`, `RadioGroup`) implement `withClass` using logic that attempts to parse existing attributes:
    ```java
    attr.toString().split("=")[1].replaceAll("\"", "").trim()
    ```
*   **Why it fails**: `Attribute` does not override `Object.toString()`. `attr.toString()` returns something like `io.mindspice.jhf.core.Attribute@1a2b3c`. The split logic fails or behaves unexpectedly.
*   **Consequence**: `withClass` fails to detect existing classes. Chaining `withClass("a").withClass("b")` will result in only "b" (or just the default class + "b"), losing previous classes.
*   **Recommendation**: Use `Attribute.getValue()` to access the attribute value. Better yet, move `withClass` logic to `HtmlTag` or a utility to avoid code duplication and this exact error.

### 2. Composite Component Re-rendering
**Severity: High**
*   **The Issue**: As noted in the General Review, components like `Card`, `Grid`, `Select`, `RadioGroup`, `DataTable` add children to the parent `HtmlTag` list inside `render()`.
*   **Consequence**: Calling `render()` multiple times duplicates the content.
*   **Recommendation**: Clear `children` at the start of `render()` or use a local list.

## Security Vulnerabilities

### 1. Stored XSS via Markdown Component
**Severity: High**
*   **The Issue**: The `Markdown` component uses `commonmark`'s `HtmlRenderer` with default settings.
    ```java
    private static final HtmlRenderer renderer = HtmlRenderer.builder().build();
    ```
    By default, CommonMark *passes through* raw HTML tags found in the markdown text.
*   **Exploit**: If a user submits markdown containing `<script>alert('xss')</script>`, it will be rendered as executable JS.
*   **Recommendation**: Configure the renderer to escape HTML:
    ```java
    HtmlRenderer.builder().escapeHtml(true).build();
    ```
    Or use a sanitizer (like OWASP Java HTML Sanitizer) if you want to allow safe HTML tags.

### 2. Insecure CSS Class Parsing (DoS/Error Prone)
**Severity: Low**
*   **The Issue**: The regex usage in `withClass` (even if fixed to use `getValue()`) is fragile.
*   **Recommendation**: Avoid parsing/regexing HTML attributes. Store classes in a `Set<String>` or `List<String>` in `HtmlTag` or a helper class, and render them only when needed.

### 3. Missing CSRF Protection for Non-HTMX Forms
**Severity: Medium**
*   **The Issue**: `Form.withCsrfToken` exists, but it's manual.
*   **Recommendation**: Ensure developers are aware they *must* call this.

### 4. Form Method Spoofing
**Severity: Medium**
*   **The Issue**: `Form.withMethod(Method.PUT)` renders `<form method="PUT">`. Browsers treat this as GET (or maybe POST depending on quirks), but definitely not PUT.
*   **Recommendation**: If method is not GET/POST, render `method="POST"` and add `<input type="hidden" name="_method" value="PUT">` for Spring compatibility.

## Functional Gaps

### 1. `DataTable` HTML Content
**Severity: Low**
*   `DataTable` cells are always escaped (using `withInnerText`). There is no way to render links or buttons inside a table cell using the current `Function<T, String>` API.
*   **Recommendation**: Change `Function<T, String>` to `Function<T, Component>` or `Function<T, Object>` (handling String vs Component) to allow rich content in tables.

### 2. `HtmlTag.withWidth` Restrictions
**Severity: Low**
*   Regex validation prevents modern CSS values like `calc()`, `min-content`, `fit-content`.
