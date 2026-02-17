[Previous](01-static-page-serving-patterns.md) | [Index](../INDEX.md)

# Dynamic Fragment Caching Patterns

Use template-level caching for dynamic sections with stable structure and changing data.

## Pattern A: Stable Template + New Context Per Request

This is the safest default.

```java
public static final Template USER_CARD = Template.of(
    new Div().withClass("user-card")
        .withChild(new Paragraph().withChild(Slot.of(USER_NAME)))
        .withChild(new Paragraph().withChild(Slot.of(USER_ROLE)))
);
```

```java
RenderContext ctx = RenderContext.builder()
    .with(USER_NAME, user.name())
    .with(USER_ROLE, user.role())
    .build();

return USER_CARD.render(ctx);
```

## Pattern B: Reused Context + Compile-On-First-Hit

Use when you intentionally keep a context object around for repeated rendering of same fragment identity.

```java
RenderContext reused = RenderContext.builder()
    .withPolicy(RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT)
    .with(USER_NAME, "Alice")
    .with(USER_ROLE, "Admin")
    .build();

String first = USER_CARD.render(reused);
String second = USER_CARD.render(reused);
```

To refresh a key:

```java
reused.put(USER_ROLE, "Owner");
String refreshed = USER_CARD.render(reused);
```

## Pseudo Caching Scaffolding

```text
cache key = tenantId + moduleId + locale
value = preselected Template + rendering strategy metadata
request -> load domain data -> build/refresh RenderContext -> render
```

## Request Mechanics (HTMX Fragment)

- Client: `hx-get` hits fragment endpoint.
- Server: renders one module/fragment template.
- Response: `outerHTML` swap for target module ID.
