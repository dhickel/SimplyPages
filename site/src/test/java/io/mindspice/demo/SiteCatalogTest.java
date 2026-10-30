package io.mindspice.demo;

import io.mindspice.demo.pages.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class SiteCatalogTest {
    @Test void explicitManifestMatchesEveryPublicBuiltinAndSupportingContract() throws Exception {
        Path root = Path.of("../simplypages/src/main/java/io/mindspice/simplypages");
        if (!Files.isDirectory(root)) root = Path.of("simplypages/src/main/java/io/mindspice/simplypages");
        Set<String> actual = new TreeSet<>();
        var declaration = Pattern.compile("(?m)^public (?:abstract |final |sealed )?(?:class|interface|enum|record) ");
        for (String pkg : List.of("components", "layout", "modules", "builders", "editing", "core")) {
            try (var files = Files.walk(root.resolve(pkg))) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    if (declaration.matcher(Files.readString(file)).find()) actual.add("io.mindspice.simplypages." + root.relativize(file).toString().replace('/', '.').replace(".java", ""));
                }
            }
        }
        Set<String> declared = new TreeSet<>();
        SiteCatalog.examples().forEach(e -> assertTrue(declared.add(e.symbol().getName()), "duplicate " + e.symbol()));
        FeatureCatalog.contracts().forEach(e -> assertTrue(declared.add(e.symbol()), "duplicate " + e.symbol()));
        assertEquals(actual, declared, "New public built-ins need explicit working catalog coverage, not an automatic class listing");
        for (String name : declared) Class.forName(name);
        assertEquals(19, SiteCatalog.examples().stream().filter(e -> e.category().equals("Modules")).count());
        assertEquals(6, SiteCatalog.examples().stream().filter(e -> e.category().equals("Builders")).count());
    }

    @Test void everyOrdinaryExampleHasRealPreviewEscapedSourceAndExactReferences() {
        for (String route : SiteCatalog.routes()) {
            if (route.startsWith("/demos/features/")) continue;
            var doc = Jsoup.parse(SiteCatalog.render(route));
            assertEquals(1, doc.select("h1").size(), route);
            for (var entry : SiteCatalog.examples().stream().filter(e -> e.route().equals(route)).toList()) {
                var section = doc.getElementById(entry.id());
                assertNotNull(section, entry.destination());
                assertFalse(section.select("[data-example-preview] > *").isEmpty(), entry.destination());
                assertFalse(section.select("code").text().isBlank(), entry.destination());
                assertTrue(section.select("a[href]").stream().anyMatch(a -> a.attr("href").equals(entry.api())));
                assertTrue(section.select("a[href]").stream().anyMatch(a -> a.attr("href").equals(entry.docs())));
                assertTrue(section.select("script").isEmpty(), "Source must be escaped");
            }
        }
    }

    @Test void presentationPreservesNativeDraftBytesAndUsesOneLiteralTokenPerPostForm() {
        String draft = "<textarea name=\"text\">\n\nA &lt; B\n</textarea>";
        String decorated = SiteTokenPresentation.decorate("<form hx-post=\"/editing-demo/save/module9\">" + draft + "</form><button hx-delete=\"/editing-demo/delete/module9\">Delete</button>", "token-123");
        assertTrue(decorated.contains(draft));
        var doc = Jsoup.parse(decorated);
        assertEquals(1, doc.select("form input[name=_csrf]").size());
        assertEquals("token-123", doc.selectFirst("input[name=_csrf]").val());
        assertEquals("{\"X-Sandbox-CSRF\":\"token-123\"}", doc.selectFirst("button").attr("hx-headers"));
        assertEquals("post", doc.selectFirst("form").attr("method"));
        assertEquals("/editing-demo/save/module9", doc.selectFirst("form").attr("action"));
        assertFalse(SiteTokenPresentation.decorate("<form method=\"GET\" action=\"/forum\"></form>", "token-123").contains("name=\"_csrf\""));
    }
}
