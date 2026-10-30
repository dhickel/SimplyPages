package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.core.RenderContext;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ButtonBrowserFixtureTest {
    @Test
    void generate() throws Exception {
        StringBuilder html = new StringBuilder();
        for (String mode : new String[]{"direct", "compiled"}) {
            for (String state : new String[]{"empty", "hidden", "invalid", "false", "until-found", "until-found-upper",
                    "visible", "override", "inline-override"}) {
                Button button = Button.create(mode + " " + state).withId(mode + "-" + state);
                if (!state.equals("visible")) {
                    button.withAttribute("hidden", switch (state) {
                        case "empty", "override", "inline-override" -> "";
                        case "hidden" -> "hidden";
                        case "invalid" -> "invalid";
                        case "false" -> "false";
                        case "until-found-upper" -> "UNTIL-FOUND";
                        default -> "until-found";
                    });
                }
                if (state.equals("override")) button.withClass("host-override");
                if (state.equals("inline-override")) button.withAttribute("style", "display:inline-block");
                String rendered = mode.equals("direct") ? button.render() : button.compile().render(RenderContext.empty());
                var element = Jsoup.parseBodyFragment(rendered).getElementById(mode + "-" + state);
                assertNotNull(element);
                assertEquals("button", element.tagName());
                assertEquals("button", element.attr("type"));
                assertEquals(state.equals("visible"), !element.hasAttr("hidden"));
                html.append(rendered);
            }
        }
        Files.writeString(Path.of("target/button-browser-fixture.html"), html.toString());
    }
}
