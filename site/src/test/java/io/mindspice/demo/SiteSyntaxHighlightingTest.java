package io.mindspice.demo;

import io.mindspice.simplypages.components.Code;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class SiteSyntaxHighlightingTest {
    private static final String VENDOR = "/vendor/highlightjs/11.12.0/";
    private static final String SOURCE = "\n\t// <script>window.snippetExecuted=true</script> &amp; <img src=x onerror=alert(1)>\n"
        + "class Example {\n\tString value = \"<>&\\\"' — café\";  \n\tint count = 42;\n}\n\n";

    @Test void sharedShellLoadsOneLocalVendorBeforeSiteInitialization() {
        var doc = Jsoup.parse(SiteShell.render("Syntax", "/", Code.block(SOURCE).withLanguage("java").render()));
        var scripts = doc.select("script[src]");
        var sources = scripts.eachAttr("src");
        assertEquals(1, sources.stream().filter((VENDOR + "highlight.min.js")::equals).count());
        assertTrue(sources.indexOf(VENDOR + "highlight.min.js") < sources.indexOf("/js/site.js"));
        assertTrue(scripts.stream().allMatch(script -> script.attr("src").startsWith("/")));
        assertTrue(doc.selectFirst("script[src='" + VENDOR + "highlight.min.js']").hasAttr("defer"));
        assertEquals(SOURCE, doc.selectFirst("pre code.language-java").wholeText());
        assertTrue(doc.select("pre code span, pre code script, pre code img").isEmpty());
    }

    @Test void packagedVendorAndLicenseMatchVerifiedUpstreamBytes() throws Exception {
        for (var entry : new String[][] {
            {"highlight.min.js", "8ab71eb09c51f501e5e25157d9cff100e46cc29bcbfc744d0b746d451fca7f53"},
            {"LICENSE", "6c081431591d9df696c82dc598fe1423765b8a299b200ed00b281afd0f64c490"}
        }) {
            try (var resource = getClass().getResourceAsStream("/static" + VENDOR + entry[0])) {
                assertNotNull(resource, entry[0]);
                assertEquals(entry[1], HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(resource.readAllBytes())));
            }
        }
    }

    @Test void escapedCodeAndMarkdownFencesPreserveLanguagesAndGenerateFreshBrowserFixture() throws Exception {
        var docs = new DocumentationService();
        StringBuilder body = new StringBuilder("<div class=sp-site-docs-article>");
        for (String language : new String[] {"java", "xml", "html", "bash", "sh", "css", "markdown", "md", "unknown", "text", "mermaid"}) {
            String rendered = docs.renderMarkdown("syntax.md", "```" + language + "\n" + SOURCE + "```\n");
            var code = Jsoup.parse(rendered).selectFirst("pre code");
            assertNotNull(code);
            assertEquals("language-" + language, code.className());
            assertEquals(SOURCE, code.wholeText());
            assertTrue(code.children().isEmpty());
            body.append(rendered);
        }
        body.append(docs.renderMarkdown("syntax.md", "```\n" + SOURCE + "```\n"));
        body.append(Code.block(SOURCE).withLanguage("java").withClass("nohighlight").render());
        body.append("<div class=sp-site-preview><p id=rendered-preview>Actual preview</p>")
            .append(Code.block(SOURCE).withLanguage("java").withTitle("Literal <script> title").render()).append("</div>");
        body.append(Code.inline("class Inline {}").withLanguage("java").render()).append("</div>");
        String html = SiteShell.render("Syntax fixture", "/syntax-fixture", body.toString());
        assertTrue(Jsoup.parse(html).select("pre script, pre img").isEmpty());
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/site-syntax-highlighting-fixture.html"), html);
    }
}
