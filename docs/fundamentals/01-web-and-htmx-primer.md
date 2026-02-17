[Previous](../INDEX.md) | [Index](../INDEX.md)

# Web and HTMX Primer

This chapter explains the minimum web model you need to use SimplyPages effectively.

## The Request/Response Loop

A browser asks for a URL.
Your server code runs.
Your server returns HTML.
The browser renders that HTML.

With server rendering, your backend owns UI output.

## Where SimplyPages Fits

- You build HTML structures in Java (`Component`, `HtmlTag`, `Module`, `Page`).
- You return rendered HTML from controllers.
- You can return full pages or fragments.

## What HTMX Adds

HTMX lets HTML trigger background HTTP requests.
The server still returns HTML.
The browser swaps a target DOM area with returned HTML.

Common attributes:

- `hx-get="/endpoint"`
- `hx-post="/endpoint"`
- `hx-target="#target-id"`
- `hx-swap="outerHTML"` or `innerHTML`

## Typical HTMX Cycle

1. User clicks a button.
2. Browser sends request to your endpoint.
3. Endpoint returns HTML fragment.
4. HTMX swaps target element.

## Minimal Example

```java
@GetMapping("/status")
@ResponseBody
public String status() {
    return new Div()
        .withId("status")
        .withInnerText("Healthy")
        .render();
}
```

```java
Button.create("Refresh")
    .withAttribute("hx-get", "/status")
    .withAttribute("hx-target", "#status")
    .withAttribute("hx-swap", "outerHTML");
```

## Security Boundaries

SimplyPages handles HTML escaping in core render paths.
Your application must still handle:

- Authentication
- Authorization
- CSRF
- Input validation
- Business rule enforcement

## Practical Rules

1. Return HTML from endpoints, not JSON, when HTMX is the consumer.
2. Keep endpoint ownership clear: each endpoint updates one stable DOM target.
3. Treat HTMX requests as normal server requests: same auth and validation rules.
