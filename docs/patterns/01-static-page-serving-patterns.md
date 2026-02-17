[Previous](../core/05-css-defaults-overrides-and-structure.md) | [Index](../INDEX.md)

# Static Page Serving Patterns

Use static serving for pages that rarely change.

## Pattern A: Pre-Render Full HTML at Startup

```java
@Service
public class StaticPages {
    private String termsHtml;

    @PostConstruct
    void init() {
        termsHtml = Page.builder()
            .addComponents(Header.H1("Terms"))
            .addComponents(new Markdown("Long static legal content..."))
            .build()
            .render();
    }

    public String termsHtml() { return termsHtml; }
}
```

## Pattern B: Cache Static Template and Render On Demand

```java
public final class LegalTemplate {
    public static final Template TERMS = Template.of(
        Page.builder()
            .addComponents(Header.H1("Terms"))
            .addComponents(new Paragraph("Static copy."))
            .build()
    );
}
```

```java
@GetMapping("/terms")
@ResponseBody
public String terms() {
    return LegalTemplate.TERMS.render(RenderContext.empty());
}
```

## Invalidation Strategy (Pseudo)

```text
if configVersion changed or contentVersion changed:
    rebuild static html/template cache
```

## Do Not

- Recompose full static pages per request without need.
- Mix mutable request state into static page singletons.
