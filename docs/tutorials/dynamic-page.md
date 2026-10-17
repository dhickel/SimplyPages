# Render request data with slots

Use the Maven project from [your first page](first-page.md). You will render two names with
one compiled template, without changing its structure.

Add `src/main/java/example/DynamicPage.java`:

```java
package example;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.Template;

public final class DynamicPage {
    private static final SlotKey<String> NAME = SlotKey.of("greeting.name", "Guest");
    private static final Template GREETING = new Div().withId("greeting")
        .withChild(new HtmlTag("h2").withInnerText(NAME))
        .compile();

    public static void main(String[] args) {
        System.out.println(GREETING.render(RenderContext.of(NAME, "Ada")));
        System.out.println(GREETING.render(RenderContext.of(NAME, "<Lin> & team")));
        System.out.println(GREETING.render(RenderContext.empty()));
    }
}
```

Run it from your example directory:

```bash
mvn compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
java -cp "target/classes:$(cat target/classpath.txt)" example.DynamicPage
```

You will see three greeting containers. Their headings contain `Ada`, escaped `<Lin> & team`,
and the default `Guest`. Each render uses a separate context; the template stays reusable.

Remove the default argument from `SlotKey.of` and rerun. The third heading is now empty.
Restore the default before continuing.

The [serving guide](../how-to/serve-pages-and-fragments.md) turns this pattern into an HTTP
endpoint. The [rendering reference](../reference/rendering.md) describes component-valued
slots, explicit compiled entries, and caching policy.

[All tutorials](README.md) · [Why reuse works](../explanation/rendering-and-lifecycle.md)
