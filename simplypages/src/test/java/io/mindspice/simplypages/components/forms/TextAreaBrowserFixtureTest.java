package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.Template;
import org.junit.jupiter.api.Test;
import org.owasp.encoder.Encode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextAreaBrowserFixtureTest {
    private static final Path ROOT = Path.of("target", "textarea-browser-fixtures");

    @Test
    void writesFreshNativeTextAreaFixtures() throws Exception {
        String[] values = {"\nFirst\nLine", "\n\nFirst", "", "plain", "\n", "\n\n", "First\nLine",
            "\rFirst", "\r\nFirst", "\r", "\r\n", "First\r\nLine\rLast", "\n\r\nFirst", "\t\nFirst",
            "\n</textarea><script>window.injected=true</script><b>雪 & \"quotes\" ' 😀</b>&amp;&#10;\t\n"};
        List<String> rows = new ArrayList<>();
        StringBuilder markup = new StringBuilder();
        for (int index = 0; index < values.length; index++) {
            String value = values[index];
            for (String mode : List.of("static", "slot", "cached", "default", "precompiled", "generic")) {
                for (boolean compiled : new boolean[] {false, true}) {
                    String id = "value-" + index + "-" + mode + (compiled ? "-compiled" : "-direct");
                    SlotKey<String> key = SlotKey.of(id, context -> value);
                    HtmlTag area = mode.equals("generic") ? new HtmlTag("TEXTAREA").withAttribute("name", id)
                        : TextArea.create(id).withRows(3).withWrap("soft");
                    area.withId(id).withAttribute("aria-label", id);
                    RenderContext context = RenderContext.empty();
                    if (mode.equals("static")) {
                        ((TextArea) area).withValue(value);
                    } else if (mode.equals("generic")) {
                        area.withInnerText(value);
                    } else {
                        area.withInnerText(key);
                        if (mode.equals("slot") || mode.equals("cached")) context.put(key, value);
                        if (mode.equals("cached")) context.withPolicy(RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT);
                        if (mode.equals("precompiled")) context.putCompiled(key, Encode.forHtml(value));
                    }
                    Template template = area.compile();
                    String output = compiled ? template.render(context) : area.render(context);
                    assertEquals(output, template.render(context));
                    assertEquals(output, area.render(context));
                    markup.append(output);
                    String expected = value.replace("\r\n", "\n").replace('\r', '\n');
                    rows.add("{\"id\":\"" + id + "\",\"expected\":\""
                        + Base64.getEncoder().encodeToString(expected.getBytes(StandardCharsets.UTF_16LE)) + "\"}");
                }
            }
        }
        TextArea empty = TextArea.create("missing");
        empty.withId("missing").withInnerText(SlotKey.<String>of("missing"));
        markup.append(empty.compile().render(RenderContext.empty()));
        rows.add("{\"id\":\"missing\",\"expected\":\"\"}");
        Form form = Form.create().withId("values").withMethod(Form.Method.POST).withAction("/submit");
        form.withUnsafeHtml(markup.toString());
        form.withChild(Button.submit("Native submit").withId("native-submit"));
        form.withChild(Button.create("HTMX submit").withId("htmx-submit").withAttribute("type", "button")
            .hxPost("/submit").hxInclude("closest form").hxSwap("none"));
        form.withChild(Button.create("Reset").withId("reset").withAttribute("type", "reset"));
        String html = "<!doctype html><html><head><meta charset=\"UTF-8\">"
            + "<link rel=\"stylesheet\" href=\"/css/framework.css\">"
            + "<script defer src=\"/webjars/htmx.org/dist/htmx.min.js\"></script>"
            + "</head><body><main>" + form.render() + "</main></body></html>";
        List<String> hashes = new ArrayList<>();
        for (String relative : List.of(
                "src/main/java/io/mindspice/simplypages/components/forms/TextArea.java",
                "src/main/java/io/mindspice/simplypages/core/HtmlTag.java",
                "src/main/java/io/mindspice/simplypages/core/Template.java",
                "src/main/java/io/mindspice/simplypages/core/Slot.java",
                "src/test/java/io/mindspice/simplypages/components/forms/TextAreaBrowserFixtureTest.java",
                "target/classes/io/mindspice/simplypages/components/forms/TextArea.class",
                "target/classes/io/mindspice/simplypages/core/HtmlTag.class",
                "target/classes/io/mindspice/simplypages/core/Template.class",
                "target/classes/io/mindspice/simplypages/core/Template$TextSlotSegment.class",
                "target/test-classes/io/mindspice/simplypages/components/forms/TextAreaBrowserFixtureTest.class")) {
            hashes.add("\"" + relative + "\":\"" + sha256(Files.readAllBytes(Path.of(relative))) + "\"");
        }
        Files.createDirectories(ROOT);
        Files.writeString(ROOT.resolve("textarea.html"), html, StandardCharsets.UTF_8);
        Files.writeString(ROOT.resolve("oracle.json"), "{\"htmlSha256\":\""
            + sha256(html.getBytes(StandardCharsets.UTF_8)) + "\",\"hashes\":{" + String.join(",", hashes)
            + "},\"cases\":[" + String.join(",", rows) + "]}", StandardCharsets.UTF_8);
        assertEquals(181, rows.size());
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
