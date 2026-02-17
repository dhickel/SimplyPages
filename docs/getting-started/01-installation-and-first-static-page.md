[Previous](README.md) | [Index](../INDEX.md)

# Installation and First Static Page

## Dependency

Add SimplyPages to your `pom.xml`:

```xml
<dependency>
  <groupId>io.mindspice</groupId>
  <artifactId>simplypages</artifactId>
  <version>0.1.0</version>
</dependency>
```

## First Static Page

```java
@GetMapping("/about")
@ResponseBody
public String aboutPage() {
    Page page = Page.builder()
        .addComponents(Header.H1("About"))
        .addComponents(new Paragraph("This page is server rendered."))
        .build();

    return page.render();
}
```

## Serve a Page Built Once

For content that changes rarely, build and render once, then serve cached HTML.

```java
@Service
public class StaticPageService {
    private String docsHtml;

    @PostConstruct
    void init() {
        docsHtml = Page.builder()
            .addComponents(Header.H1("Documentation"))
            .addComponents(new Markdown("Static content generated at startup."))
            .build()
            .render();
    }

    public String docsHtml() {
        return docsHtml;
    }
}
```

```java
@GetMapping("/docs")
@ResponseBody
public String docs(StaticPageService service) {
    return service.docsHtml();
}
```

## When This Pattern Fits

- Marketing pages with infrequent updates
- Internal policy pages
- Versioned docs snapshots

For request-time dynamic data, use slots and render context (next guide).
