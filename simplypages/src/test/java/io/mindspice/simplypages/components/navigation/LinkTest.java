package io.mindspice.simplypages.components.navigation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.Template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkTest {

    @Test
    void rejectedSetterPreservesLiteralStateOrderAndPriorStaticSnapshot() {
        Link link = Link.create("/old?x=1&y=2", "<text>").withId("link").withTarget("_self");
        Template before = link.compile();
        String original = link.render();
        for (String prohibited : new String[] {"JaVa\tScRiPt:x", "da\nta:text/plain,x", "ftp://example.test"}) {
            assertThrows(IllegalArgumentException.class, () -> link.withHref(prohibited));
            assertEquals("/old?x=1&y=2", link.getHref());
            assertEquals("<text>", link.getText());
            assertEquals(original, link.render());
            assertEquals(original, before.render(RenderContext.empty()));
        }
        String raw = " \u0001HT\nTPS://example.test/?a=1&b=\"x\"\r ";
        assertSame(link, link.withHref(raw));
        assertEquals(raw, link.getHref());
        assertEquals(original, before.render(RenderContext.empty()));
        assertEquals(link.render(), link.compile().render(RenderContext.empty()));
        assertTrue(link.render().indexOf("target=") < link.render().indexOf("href="));
        assertTrue(link.render().contains("&amp;b=&#34;x&#34;"));
    }

    @Test
    void customContextRendererRemainsLiveAndTypedFluentReturnsStayIntact() {
        SlotKey<String> value = SlotKey.of("link-context");
        Link custom = new Link("/first", "text") {
            @Override public String render(RenderContext context) {
                return super.render(context) + context.get(value).orElseThrow();
            }
        };
        Template compiled = custom.compile();
        assertSame(custom, custom.withHref("ht\ttp:/next").withId("id").withClass("link")
            .withTarget("_self").withHxGet("custom:get").withHxPost("custom:post"));
        for (String text : new String[] {"one", "two"}) {
            RenderContext context = RenderContext.of(value, text);
            assertEquals(custom.render(context), compiled.render(context));
            assertTrue(compiled.render(context).endsWith(text));
        }
    }

    @Test
    @DisplayName("Link should render href and HTMX attributes")
    void testLinkRendering() {
        Link link = Link.create("/path", "Go")
            .withHxGet("/data")
            .withHxTarget("#target")
            .withHxSwap("outerHTML")
            .withHxScrollTargetTop();

        String html = link.render();

        assertTrue(html.contains("href=\"/path\""));
        assertTrue(html.contains("hx-get=\"/data\""));
        assertTrue(html.contains("hx-target=\"#target\""));
        assertTrue(html.contains("hx-swap=\"outerHTML\""));
        assertTrue(html.contains("data-sp-scroll-top=\"target\""));
    }

    @Test
    @DisplayName("Link should render target, id, and push-url attributes")
    void testLinkTargetAndId() {
        Link link = Link.create("/path", "Go")
            .withId("link-1")
            .openInNewTab()
            .withHxPost("/submit")
            .withHxPushUrl(true);

        String html = link.render();

        assertTrue(html.contains("id=\"link-1\""));
        assertTrue(html.contains("target=\"_blank\""));
        assertTrue(html.contains("hx-post=\"/submit\""));
        assertTrue(html.contains("hx-push-url=\"true\""));
    }

    @Test
    @DisplayName("Link should update href when set explicitly")
    void testLinkWithHref() {
        Link link = Link.create("/path", "Go")
            .withHref("https://example.com");

        String html = link.render();

        assertTrue(html.contains("href=\"https://example.com\""));
        assertFalse(html.contains("href=\"/path\""));
    }

    @Test
    @DisplayName("Link should reject unsafe href schemes")
    void testUnsafeHrefScheme() {
        assertThrows(IllegalArgumentException.class, () -> Link.create("javascript:alert(1)", "Bad"));
        assertThrows(IllegalArgumentException.class, () -> Link.create("/safe", "Go").withHref("data:text/html,<svg>"));
    }
}
