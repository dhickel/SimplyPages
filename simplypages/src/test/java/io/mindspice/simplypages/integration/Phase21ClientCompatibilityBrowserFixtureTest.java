package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.components.forms.Form;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Actual Java markup for the native client suite; never packaged as an application endpoint. */
public class Phase21ClientCompatibilityBrowserFixtureTest {
    private static final List<String> NAMES = List.of("X-CSRF-TOKEN", "hasOwnProperty", "constructor", "toString",
        "__proto__", "HASOWNPROPERTY", "__defineGetter__", "__defineSetter__", "__lookupGetter__",
        "__lookupSetter__", "isPrototypeOf", "propertyIsEnumerable", "valueOf", "toLocaleString");

    @Test
    void produceActualDirectCompiledAndShellFixturesWithProvenance() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        if (!Files.isDirectory(root.resolve("simplypages/src"))) root = root.getParent();
        produce(root, root.resolve("simplypages/target/phase21-client-browser-fixtures"));
    }

    /** A consumer can regenerate the same markup with only the fixture and the current packaged library. */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Expected repository root and owned output directory");
        produce(Path.of(args[0]), Path.of(args[1]));
    }

    private static Form form(String id, String name) {
        Form form = Form.create().withId(id).withCsrfHeaderName(name)
            .withHxPostCsrf("/receive?id=" + id, "synthetic-token").withHxTarget("#out").withHxSwap("none");
        form.withAttribute("class", "caller-replaced");
        form.withChild(new HtmlTag("input", true).withAttribute("name", "field").withAttribute("value", "native-value"));
        form.withChild(new HtmlTag("button").withAttribute("type", "submit").withId(id + "-submit").withInnerText("Send"));
        return form;
    }

    private static void produce(Path root, Path output) throws Exception {
        Files.createDirectories(output);
        HtmlTag forms = new HtmlTag("main");
        for (int index = 0; index < NAMES.size(); index++) {
            for (boolean compiled : List.of(false, true)) {
                Form form = form("pair-" + index + "-" + compiled, NAMES.get(index));
                String direct = form.render();
                Component rendered = compiled ? form.compile()::render : form;
                assertEquals(direct, rendered.render(RenderContext.empty()));
                forms.withChild(rendered);
            }
        }
        for (var entry : List.of(Map.entry("evaluated", "js:{toString:'evaluated-token'}"),
                Map.entry("multi", "{\"toString\":\"synthetic-token\",\"X-Other\":\"other\"}"),
                Map.entry("duplicate", "{\"toString\":\"first\",\"toString\":\"last\"}"),
                Map.entry("nonstring", "{\"toString\":17}"))) {
            Form excluded = form(entry.getKey(), "toString");
            excluded.withAttribute("hx-headers", entry.getValue());
            forms.withChild(excluded);
        }
        Form inherited = Form.create().withId("inherited").withHxPost("/receive?id=inherited").withHxSwap("none")
            .withChild(new HtmlTag("button").withAttribute("type", "submit").withId("inherited-submit").withInnerText("Inherited"));
        forms.withChild(new HtmlTag("section").withAttribute("hx-headers", "{\"toString\":\"synthetic-token\"}").withChild(inherited));
        forms.withChild(new HtmlTag("button").withAttribute("type", "button").withId("nonform")
            .withAttribute("hx-post", "/receive?id=nonform").withAttribute("hx-headers", "{\"toString\":\"synthetic-token\"}")
            .withAttribute("hx-swap", "none").withInnerText("Nonform"));
        forms.withChild(new HtmlTag("div").withId("late")).withChild(new HtmlTag("div").withId("out"));
        Map<String, String> documents = new LinkedHashMap<>();
        documents.put("forms.html", ShellBuilder.create().withContent(forms).build());
        var parsed = Jsoup.parse(documents.get("forms.html"));
        assertEquals(33, parsed.select("form").size());
        assertEquals(28, parsed.select("form.caller-replaced[id^=pair-]").size());
        assertEquals(2, parsed.select("head script[src]").size());
        for (boolean included : List.of(true, false)) {
            ShellBuilder builder = ShellBuilder.create().withHtmx(included).withCustomJs("/custom.js")
                .withContent(new HtmlTag("div").withId("shell-content")
                    .withChild(new HtmlTag("div").withId("scroll-target").withAttribute("style", "margin-top:1800px;height:500px")));
            documents.put("shell-" + included + ".html", builder.build());
            documents.put("template-" + included + ".html", builder.buildTemplate().render(RenderContext.empty()));
            documents.put("body-" + included + ".html", builder.buildBody().render());
            assertTrue(Jsoup.parseBodyFragment(documents.get("body-" + included + ".html")).select("script,meta,link").isEmpty());
        }
        StringBuilder oracle = new StringBuilder("{\n  \"files\": {");
        boolean first = true;
        for (var entry : documents.entrySet()) {
            byte[] bytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
            Files.write(output.resolve(entry.getKey()), bytes);
            if (!first) oracle.append(',');
            first = false;
            oracle.append("\n    \"").append(entry.getKey()).append("\": \"").append(sha(bytes)).append('"');
        }
        oracle.append("\n  },\n  \"inputs\": {");
        Map<String, String> inputs = new LinkedHashMap<>();
        for (String path : List.of("simplypages/src/main/java/io/mindspice/simplypages/components/forms/Form.java",
                "simplypages/src/main/java/io/mindspice/simplypages/builders/ShellBuilder.java",
                "simplypages/src/main/resources/static/js/framework.js",
                "simplypages/src/test/java/io/mindspice/simplypages/integration/Phase21ClientCompatibilityBrowserFixtureTest.java")) {
            inputs.put(path, sha(Files.readAllBytes(root.resolve(path))));
        }
        for (Class<?> type : List.of(Form.class, ShellBuilder.class, Phase21ClientCompatibilityBrowserFixtureTest.class)) {
            String resource = type.getName().replace('.', '/') + ".class";
            try (var stream = type.getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(stream);
                inputs.put(resource, sha(stream.readAllBytes()));
            }
        }
        first = true;
        for (var entry : inputs.entrySet()) {
            if (!first) oracle.append(',');
            first = false;
            oracle.append("\n    \"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append('"');
        }
        oracle.append("\n  },\n  \"formOrigin\": \"").append(Form.class.getProtectionDomain().getCodeSource().getLocation())
            .append("\",\n  \"shellOrigin\": \"").append(ShellBuilder.class.getProtectionDomain().getCodeSource().getLocation())
            .append("\"\n}\n");
        Files.writeString(output.resolve("oracle.json"), oracle);
    }

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
