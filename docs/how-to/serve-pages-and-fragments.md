# Serve full pages and HTMX fragments

Use this pattern inside an existing Spring MVC application running on JDK 25 with the
SimplyPages dependency installed. Spring is an application integration choice; the core
library does not require it.

## Return a complete document

Create a controller that compiles a shell once. Populate the shell content and its nested
slots with a fresh context on each request:

```java
package example;

import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.builders.ShellTemplate;
import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.SlotKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class GreetingController {
    private static final SlotKey<String> NAME = SlotKey.of("greeting.name");
    private static final HtmlTag CARD = new Div().withId("greeting")
        .withChild(new HtmlTag("h2").withInnerText(NAME));
    private static final ShellTemplate SHELL = ShellBuilder.create()
        .withPageTitle("Greeting").buildTemplate();

    @GetMapping(value = "/greeting", produces = "text/html")
    public String page(@RequestParam(defaultValue = "Guest") String name) {
        return SHELL.render(RenderContext.builder()
            .with(SHELL.contentSlot(), CARD).with(NAME, name).build());
    }

    @GetMapping(value = "/greeting/fragment", produces = "text/html")
    public String fragment(@RequestParam(defaultValue = "Guest") String name) {
        return CARD.render(RenderContext.of(NAME, name));
    }
}
```

Request `/greeting?name=Ada`; it returns a document with a head, body, and greeting.
Request `/greeting/fragment?name=Ada`; it returns only the `div#greeting` fragment.

## Serve the assets

Expose `/css/framework.css` and `/js/framework.js` from the library's `static/` classpath
resources. Spring Boot's default resource handling does this. The default HTMX URL is
`/webjars/htmx.org/dist/htmx.min.js`; the demo includes `webjars-locator-core` for this
versionless path. Configure an equivalent mapping in another host.

Keep custom CSS after framework CSS. Use `ShellBuilder.withCustomCss("/css/app.css")`.
The shell loads HTMX, framework JavaScript, then custom JavaScript, all deferred.

## Update one target

A trigger can request the fragment with `hx-get="/greeting/fragment?name=Ada"`,
`hx-target="#greeting"`, and `hx-swap="outerHTML"`. The response contains the same target id.
For `innerHTML`, return only the target's children. Repeating the target wrapper inside an
`innerHTML` response creates duplicate IDs.

If one URL serves both full documents and fragments, set `Vary: HX-Request` and ensure your
cache varies on that header. Authenticate and authorize both response paths.

See [HTMX endpoint patterns](../patterns/03-htmx-endpoint-and-swap-patterns.md) and the
[asset reference](../reference/build-and-assets.md).

[All how-to guides](README.md)
