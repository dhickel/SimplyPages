package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.components.Image;
import io.mindspice.simplypages.components.forum.comments.DefaultForumCommentComponent;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** Current Java markup and independent input-unit/policy oracles; never included in the product. */
public class UrlValidationBrowserFixtureTest {
    private record Input(String value, boolean link, boolean image, String protocol) {}
    private record Fixture(String html, String json) {}

    @Test
    @ResourceLock("java.util.Locale.default")
    void writesCurrentDirectCompiledMarkupAndIndependentNumericOracles() throws Exception {
        var fixture = generate();
        var doc = Jsoup.parse(fixture.html());
        assertEquals(2, doc.select("#url-boundary-presentation a").size());
        assertEquals(2, doc.select("#url-boundary-presentation img").size());
        assertEquals(1, doc.select("#url-boundary-fallback .forum-comment-avatar-slot").size());
        assertEquals(0, doc.select("#url-boundary-fallback img").size());
        assertEquals(0, doc.select("script, [onload], [onerror]").size());
        assertTrue(doc.select(".url-boundary-case").size() > 500);
        Files.writeString(Path.of("target/url-validation-browser-fixture.html"), fixture.html());
        Files.writeString(Path.of("target/url-validation-browser-fixture.json"), fixture.json());
    }

    /** Regenerates both files against an extracted current packaged library in an isolated JVM. */
    public static void main(String[] args) throws Exception {
        var fixture = generate();
        Files.writeString(Path.of(args[0]), fixture.html(), StandardOpenOption.CREATE_NEW);
        Files.writeString(Path.of(args[1]), fixture.json(), StandardOpenOption.CREATE_NEW);
    }

    private static List<Input> inputs() {
        List<Input> cases = new ArrayList<>();
        for (String value : new String[] {null, "", " ", "\t\n\r", "/path?x=1&y=\"quote\"", "relative/path",
            "./path", "../path", "?query=1", "#fragment", "//example.test/path", ":literal", "data%3Atext/html,x"}) {
            cases.add(new Input(value, true, true, "http:"));
        }
        cases.add(new Input("http://example.test/path", true, true, "http:"));
        cases.add(new Input("HTTPS://example.test/path", true, true, "https:"));
        cases.add(new Input("MAILTO:reader@example.test", true, true, "mailto:"));
        cases.add(new Input("TEL:+15551234567", true, true, "tel:"));
        for (String controls : List.of("\t", "\n", "\r", "\t\t", "\r\n", "\t\n\r")) {
            cases.add(new Input("ht" + controls + "tp:/path", true, true, "http:"));
            cases.add(new Input("HT" + controls + "TPS://example.test/path", true, true, "https:"));
            cases.add(new Input("MAIL" + controls + "TO:reader@example.test", true, true, "mailto:"));
            cases.add(new Input("TE" + controls + "L:+15551234567", true, true, "tel:"));
            cases.add(new Input("data:im" + controls + "age/gif;base64,AA==", false, true, "data:"));
            cases.add(new Input("java" + controls + "script:void(0)", false, false, "javascript:"));
            cases.add(new Input("da" + controls + "ta:text/plain,harmless", false, false, "data:"));
        }
        cases.add(new Input("\u0001 HTTPS://example.test/path \u001f", true, true, "https:"));
        cases.add(new Input("\u0001 JAVASCRIPT:void(0) \u001f", false, false, "javascript:"));
        cases.add(new Input("JAVASCRIPT:void(0)", false, false, "javascript:"));
        cases.add(new Input("DATA:TEXT/PLAIN,harmless", false, false, "data:"));
        for (String value : List.of("data:image/", "DATA:IMAGE/GIF;base64,AA==", "data:image/svg+xml,<svg/>")) {
            cases.add(new Input(value, false, true, "data:"));
        }
        cases.add(new Input("custom-scheme:value", false, true, "custom-scheme:"));
        cases.add(new Input("ftp://example.test/image", false, true, "ftp:"));
        cases.add(new Input("blob:https://example.test/id", false, true, "blob:"));
        for (String value : List.of("java&#x09;script:x", "java&Tab;script:x", "java%09script:x", "java%0Ascript:x",
            "java script:x", "java\u000bscript:x", "java\u0000script:x", "javascr\u0130pt:x", "javascr\u0131pt:x",
            "java\u017fcript:x", "\u00a0javascript:x", "bad-\ud800:x")) {
            cases.add(new Input(value, false, true, "http:"));
        }
        cases.add(new Input("data:im age/gif,x", false, false, "data:"));
        return cases;
    }

    private static Fixture generate() {
        StringBuilder markup = new StringBuilder("<main id=\"url-validation-fixture\"><div id=\"url-boundary-matrix\" hidden>");
        StringBuilder json = new StringBuilder("{\"rows\":[");
        Locale original = Locale.getDefault();
        Locale display = Locale.getDefault(Locale.Category.DISPLAY);
        Locale format = Locale.getDefault(Locale.Category.FORMAT);
        int id = 0;
        try {
            for (Locale locale : List.of(Locale.ROOT, Locale.forLanguageTag("tr-TR"))) {
                Locale.setDefault(locale);
                for (Input input : inputs()) {
                    for (String type : List.of("link", "image")) {
                        for (String path : List.of("constructor", "factory", "setter")) {
                            int current = id++;
                            boolean expected = type.equals("link") ? input.link() : input.image();
                            Component component = null;
                            try {
                                component = switch (type + "/" + path) {
                                    case "link/constructor" -> new Link(input.value(), "URL boundary link");
                                    case "link/factory" -> Link.create(input.value(), "URL boundary link");
                                    case "link/setter" -> Link.create("/seed", "URL boundary link").withHref(input.value());
                                    case "image/constructor" -> new Image(input.value(), "URL boundary image");
                                    case "image/factory" -> Image.create(input.value(), "URL boundary image");
                                    default -> Image.create("/seed", "URL boundary image").withSrc(input.value());
                                };
                            } catch (IllegalArgumentException rejection) {
                                assertFalse(expected, "Unexpected rejection " + current);
                            }
                            assertEquals(expected, component != null, "Policy case " + current);
                            if (current > 0) json.append(',');
                            json.append("{\"id\":").append(current).append(",\"type\":\"").append(type)
                                .append("\",\"path\":\"").append(path).append("\",\"locale\":\"").append(locale.toLanguageTag())
                                .append("\",\"input\":").append(units(input.value())).append(",\"accepted\":").append(expected)
                                .append(",\"protocol\":\"").append(input.protocol()).append("\"}");
                            if (component != null) {
                                assertEquals(input.value(), component instanceof Link link ? link.getHref() : ((Image) component).getSrc());
                                String compiled = component.compile().render(RenderContext.empty());
                                String direct = component.render();
                                assertEquals(direct, compiled);
                                for (String mode : List.of("direct", "compiled")) {
                                    markup.append("<section class=\"url-boundary-case\" data-case=\"").append(current)
                                        .append("\" data-mode=\"").append(mode).append("\">")
                                        .append(mode.equals("direct") ? direct : compiled).append("</section>");
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            Locale.setDefault(original);
            Locale.setDefault(Locale.Category.DISPLAY, display);
            Locale.setDefault(Locale.Category.FORMAT, format);
        }
        assertEquals(original, Locale.getDefault());
        HtmlTag presentation = new HtmlTag("section").withId("url-boundary-presentation")
            .withChild(new HtmlTag("h1").withInnerText("URL boundaries"));
        for (String mode : List.of("direct", "compiled")) {
            HtmlTag group = new HtmlTag("div").withAttribute("data-mode", mode)
                .withChild(Link.create("https://example.test/guide", "Read the guide"))
                .withChild(Image.create("data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7", "Example image").withSize("40", "40"));
            presentation.withChild(new HtmlTag("div").withUnsafeHtml(mode.equals("direct") ? group.render()
                : group.compile().render(RenderContext.empty())));
        }
        var fallback = DefaultForumCommentComponent.create().withCommentId("url-boundary-avatar")
            .withAuthor("Default avatar fallback").withAvatarUrl("java\tscript:void(0)")
            .withBody(new HtmlTag("p").withInnerText("The existing invalid-avatar fallback keeps its reserved slot."));
        markup.append("</div>").append(presentation.render());
        markup.append(new HtmlTag("section").withId("url-boundary-fallback").withChild(fallback).render());
        markup.append("</main>");
        json.append("],\"localeRestored\":true,\"origins\":{");
        for (Class<?> type : List.of(Link.class, Image.class, HtmlTag.class)) {
            if (type != Link.class) json.append(',');
            String location = type.getProtectionDomain().getCodeSource().getLocation().toString();
            assertFalse(location.contains("\""));
            json.append('"').append(type.getSimpleName()).append("\":\"").append(location).append('"');
        }
        return new Fixture(markup.toString(), json.append("}}").toString());
    }

    private static String units(String value) {
        if (value == null) return "null";
        StringBuilder out = new StringBuilder("[");
        for (int index = 0; index < value.length(); index++) {
            if (index > 0) out.append(',');
            out.append((int) value.charAt(index));
        }
        return out.append(']').toString();
    }
}
