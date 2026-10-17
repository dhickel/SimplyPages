package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.core.RenderContext;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Fresh Java markup and an independent code-unit oracle for native browser JSON regression tests. */
class CsrfJsonBrowserFixtureTest {
    private static final Path FIXTURE_ROOT = Path.of("target", "csrf-json-browser-fixtures");
    private static final int CHUNK_SIZE = 1024;

    @Test
    void writesFreshDirectAndCompiledCsrfJsonBrowserFixtures() throws Exception {
        StringBuilder markup = new StringBuilder();
        List<String> rows = new ArrayList<>();
        addBoth(markup, rows, "request-default", "X-CSRF-TOKEN", "plain-token", true, -1);
        addBoth(markup, rows, "request-custom", "X-Phase14-CSRF",
            "value\", \"X-Injected\": \"bad\\tail &amp; % <tag> ' end", true, -1);
        int combination = 0;
        for (String name : new String[] {null, "", "custom"}) {
            for (String token : new String[] {null, "", "token"}) {
                addBoth(markup, rows, "null-empty-" + combination++, name, token, false, -1);
            }
        }
        String injection = "\"} onload=\"bad\"><script>bad</script>\\, \"X-Injected\": \"yes";
        addBoth(markup, rows, "injection", injection, injection, false, -1);
        addBoth(markup, rows, "entities-percent", "&quot;&#34;&amp;%22%5C'<>/ :",
            "~ &quot; &#x0; &amp; %00 %22 %5C <tag> / '", false, -1);
        addBoth(markup, rows, "printable-ascii", units(0x20, 0x7f), units(0x20, 0x7f), false, -1);
        addBoth(markup, rows, "c0-c1", units(0, 0x20), units(0x7f, 0xa0), false, -1);
        addBoth(markup, rows, "unicode", "\u00e9e\u0301\u212b\u2028\u2029\uD83D\uDE00\uD800a\uDC00",
            "\u0000\u20ac\uFFFF\uDBFF\uDFFF\uDC00z\uD800", false, -1);
        for (String reserved : List.of("hasOwnProperty", "constructor", "toString", "__proto__")) {
            // Representation only. These are not claims about bundled HTMX/HTTP support (F22).
            addBoth(markup, rows, "reserved-" + reserved, reserved, "token", false, -1);
        }
        for (int start = 0; start <= Character.MAX_VALUE; start += CHUNK_SIZE) {
            StringBuilder chunk = new StringBuilder(CHUNK_SIZE * 3);
            for (int unit = start; unit < start + CHUNK_SIZE; unit++) {
                // ASCII separators exercise each surrogate independently, without accidental pairs.
                chunk.append('a').append((char) unit).append('z');
            }
            addBoth(markup, rows, "matrix-key-" + start, chunk.toString(), "fixed-value", false, start);
            addBoth(markup, rows, "matrix-value-" + start, "fixed-name", chunk.toString(), false, start);
        }
        String html = "<!doctype html><html><head><meta charset=\"UTF-8\">"
            + "<link rel=\"stylesheet\" href=\"/css/framework.css\">"
            + "<script defer src=\"/webjars/htmx.org/dist/htmx.min.js\"></script>"
            + "</head><body><main id=\"csrf-json-fixture\">" + markup
            + "<div id=\"csrf-result\">not submitted</div></main></body></html>";
        var document = Jsoup.parse(html);
        assertEquals(rows.size(), document.select("form").size());
        assertEquals(rows.size(), document.select("form[hx-headers]").size());
        assertEquals(4, document.select("button[type=submit]").size());
        assertEquals(1, document.select("#csrf-result").size());
        assertEquals(1, document.select("script[src]").size());
        assertEquals(0, document.select("script:not([src]), [onload], tag").size());
        assertEquals(rows.size(), java.util.regex.Pattern.compile(" hx-headers=\"").matcher(html).results().count());
        assertTrue(html.chars().allMatch(unit -> unit < 0x80), "No UTF-8 transport of malformed surrogates");

        String manifest = "{\"formSourceSha256\":\"" + sha256(Path.of("src/main/java/io/mindspice/simplypages/components/forms/Form.java"))
            + "\",\"fixtureSourceSha256\":\"" + sha256(Path.of("src/test/java/io/mindspice/simplypages/components/forms/CsrfJsonBrowserFixtureTest.java"))
            + "\",\"formClassSha256\":\"" + sha256(Path.of("target/classes/io/mindspice/simplypages/components/forms/Form.class"))
            + "\",\"fixtureClassSha256\":\"" + sha256(Path.of("target/test-classes/io/mindspice/simplypages/components/forms/CsrfJsonBrowserFixtureTest.class"))
            + "\",\"htmlSha256\":\"" + sha256(html.getBytes(StandardCharsets.US_ASCII))
            + "\",\"chunkSize\":" + CHUNK_SIZE + ",\"cases\":[" + String.join(",", rows) + "]}";
        Files.createDirectories(FIXTURE_ROOT);
        Files.writeString(FIXTURE_ROOT.resolve("csrf-json.html"), html, StandardCharsets.US_ASCII);
        Files.writeString(FIXTURE_ROOT.resolve("oracle.json"), manifest, StandardCharsets.US_ASCII);
        assertEquals(html, Files.readString(FIXTURE_ROOT.resolve("csrf-json.html"), StandardCharsets.US_ASCII));
    }

    private static void addBoth(StringBuilder markup, List<String> rows, String prefix,
                                String name, String token, boolean request, int start) {
        for (boolean compiled : new boolean[] {false, true}) {
            String id = prefix + (compiled ? "-compiled" : "-direct");
            Form form = Form.create().withId(id);
            if (!prefix.equals("request-default")) {
                form.withCsrfHeaderName(name);
            }
            if (request) {
                form.withHxTarget("#csrf-result").withChild(Button.submit("Submit " + id).withId(id + "-submit"));
            }
            form.withHxPostCsrf("/csrf-json-receiver?id=" + id, token);
            // Fresh compiled forms are compiled before any direct render, with no markup rewriting.
            String output = compiled ? form.compile().render(RenderContext.empty()) : form.render();
            assertEquals(output, form.render());
            assertEquals(output, form.compile().render(RenderContext.empty()));
            markup.append(output);
            rows.add("{\"id\":\"" + id + "\",\"nameUnits\":\"" + unitBytes(name)
                + "\",\"valueUnits\":\"" + unitBytes(token) + "\",\"compiled\":" + compiled
                + ",\"request\":" + request + ",\"start\":" + start + "}");
        }
    }

    private static String units(int start, int end) {
        char[] values = new char[end - start];
        for (int unit = start; unit < end; unit++) {
            values[unit - start] = (char) unit;
        }
        return new String(values);
    }

    private static String unitBytes(String value) {
        String text = String.valueOf(value);
        byte[] bytes = new byte[text.length() * 2];
        for (int index = 0; index < text.length(); index++) {
            int unit = text.charAt(index);
            bytes[index * 2] = (byte) (unit & 0xff);
            bytes[index * 2 + 1] = (byte) (unit >>> 8);
        }
        // Manual byte packing preserves lone surrogates; a charset encoder would replace them.
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String sha256(Path path) throws Exception {
        return sha256(Files.readAllBytes(path));
    }

    private static String sha256(byte[] bytes) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
