[Previous](01-installation-and-first-static-page.md) | [Index](../INDEX.md)

# Dynamic Pages with SlotKey and RenderContext

This is the default dynamic rendering pattern in SimplyPages.

## Define Slots and a Template Once

```java
public final class ArticleView {
    public static final SlotKey<String> ARTICLE_TITLE = SlotKey.of("article_title");
    public static final SlotKey<String> ARTICLE_BODY = SlotKey.of("article_body");

    public static final Template ARTICLE_TEMPLATE = Template.of(
        ContentModule.create()
            .withModuleId("article-view")
            .withTitle("Article")
            .withCustomContent(
                new Div()
                    .withChild(new HtmlTag("h2").withInnerText(ARTICLE_TITLE))
                    .withChild(new Paragraph().withChild(Slot.of(ARTICLE_BODY)))
            )
    );

    private ArticleView() {}
}
```

## Render Per Request Using Context

```java
@GetMapping("/articles/{id}")
@ResponseBody
public String article(@PathVariable String id) {
    Article article = articleService.load(id);

    RenderContext ctx = RenderContext.builder()
        .with(ArticleView.ARTICLE_TITLE, article.title())
        .with(ArticleView.ARTICLE_BODY, article.body())
        .build();

    return ArticleView.ARTICLE_TEMPLATE.render(ctx);
}
```

## Request Mechanics

- Browser sends `GET /articles/{id}`.
- Controller loads article data.
- Controller maps fields into `RenderContext`.
- Template resolves slots and returns HTML.

## Compile Policy for Reused Contexts

Use `COMPILE_ON_FIRST_HIT` only when you intentionally reuse a mutable context object.

```java
RenderContext ctx = RenderContext.builder()
    .withPolicy(RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT)
    .with(ArticleView.ARTICLE_TITLE, article.title())
    .with(ArticleView.ARTICLE_BODY, article.body())
    .build();
```

If you later call `ctx.put(key, newValue)`, compiled value for that key is replaced by live entry.

## Anti-Pattern

Do not rebuild entire page/module trees on every dynamic update if only data changed.

Use stable templates + slots for data churn.
