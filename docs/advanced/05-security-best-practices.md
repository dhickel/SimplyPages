# Security Best Practices

SimplyPages includes several built-in security features, but developers must still follow best practices to ensure application security.

## Cross-Site Scripting (XSS) Prevention

XSS is the most common vulnerability in web applications. SimplyPages combats this by default:

### 1. Automatic Encoding
The `TextNode` component, which wraps all text content added via `withInnerText()` or similar methods, automatically HTML-encodes its content using `org.owasp.encoder.Encode`.

```java
// Safe: will be encoded as &lt;script&gt;...&lt;/script&gt;
new Div().withInnerText("<script>alert('xss')</script>");
```

### 2. HtmlTag Safety
When building custom components with `HtmlTag`:
- `withInnerText(String)`: **Safe** (Encoded)
- `withHtml(String)`: **Unsafe** (Raw HTML) - Use with extreme caution!

**Rule:** Avoid `withHtml()` unless you absolutely trust the source (e.g., static templates). Never pass user input to `withHtml()`.

## URL Validation

When accepting URLs from users (e.g., for profile links, image sources), always validate them.

- Ensure the protocol is safe (`http`, `https`, `mailto`).
- Avoid `javascript:` URLs.

SimplyPages components like `Link` and `Image` should ideally be used with validated inputs.

## CSRF Protection

If you are using Spring Security (recommended), ensure CSRF protection is enabled.
SimplyPages forms (HTMX or standard) need to include the CSRF token.

HTMX automatically includes CSRF tokens if configured in the meta tags.

```html
<meta name="_csrf" content="${_csrf.token}"/>
<meta name="_csrf_header" content="${_csrf.headerName}"/>
```

Ensure your base Shell/Layout includes these meta tags.

## Access Control

### Component-Level Authorization
Do not rely on hiding UI elements for security. Just because a "Delete" button is not rendered doesn't mean the endpoint is secure.

**Always enforce authorization checks at the Controller/Service level.**

```java
@PostMapping("/delete/{id}")
public String delete(@PathVariable String id, Principal principal) {
    if (!permissionService.canDelete(principal, id)) {
        throw new AccessDeniedException("Unauthorized");
    }
    // ... proceed
}
```

### AuthorizationChecker
The framework provides `AuthorizationChecker` interfaces for the Editing System. Implement these to control who can edit what.

```java
EditableModule.wrap(module)
    .withAuthChecker((user, action) -> user.hasRole("ADMIN"));
```

## SQL Injection

SimplyPages is a UI framework and does not handle database interaction. However, when binding UI inputs to DB queries:
- Always use Prepared Statements (via JPA/Hibernate or JDBC template).
- Never concatenate user input into SQL strings.

## Input Validation

Validate all inputs on the server side.
- Use `ValidationResult` in the Editing System.
- Use Bean Validation (`@Valid`) in Spring Controllers.

## Summary

1.  Trust defaults: Use `withInnerText` (safe) over `withHtml` (unsafe).
2.  Validate all user inputs (URLs, text, numbers).
3.  Enforce permissions at the backend, not just the frontend.
4.  Enable CSRF protection.
