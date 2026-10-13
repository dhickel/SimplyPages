package io.mindspice.simplypages.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.Template;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class AccountWidgetTest {

    @Test
    @DisplayName("AccountWidget should render guest links")
    void testGuestWidget() {
        String html = AccountWidget.createGuest()
            .withLoginUrl("/login")
            .withSignupUrl("/signup")
            .render();

        assertTrue(html.contains("account-widget-guest"));
        assertTrue(html.contains("href=\"/login\""));
        assertTrue(html.contains("href=\"/signup\""));
    }

    @Test
    @DisplayName("AccountWidget should render authenticated links")
    void testAuthenticatedWidget() {
        String html = AccountWidget.createAuthenticated("alice")
            .withProfileUrl("/profile")
            .withLogoutUrl("/logout")
            .render();

        assertTrue(html.contains("Hello, alice"));
        assertTrue(html.contains("href=\"/profile\""));
        assertTrue(html.contains("href=\"/logout\""));
    }

    @Test
    void authenticatedDisclosureIsNamedStableAndEscaped() {
        var widget = AccountWidget.createAuthenticated("<alice>");
        var doc = org.jsoup.Jsoup.parse(widget.render());
        var button = doc.selectFirst("button.account-widget-greeting");
        var panel = doc.selectFirst(".account-widget-dropdown");
        assertEquals("Hello, <alice>", button.text());
        assertEquals("button", button.attr("type"));
        assertEquals("false", button.attr("aria-expanded"));
        assertEquals(panel.id(), button.attr("aria-controls"));
        assertTrue(panel.hasAttr("hidden"));
        assertEquals(widget.render(), widget.render());
        assertNotEquals(panel.id(), org.jsoup.Jsoup.parse(AccountWidget.createAuthenticated("bob").render())
            .selectFirst(".account-widget-dropdown").id());
    }

    @Test
    void freshCompilationRetainsInheritedContextAndCustomRenderer() {
        SlotKey<String> key = SlotKey.of("account-extra");
        var widget = AccountWidget.createGuest().withSignupUrl(null);
        widget.withChild(new HtmlTag("strong").withInnerText(key));
        widget.withChild(new HtmlTag("span") {
            @Override public String render(RenderContext context) {
                return "<em>" + context.get(key).orElseThrow().length() + "</em>";
            }
        });
        Template template = widget.compile(); // No direct-render warmup.
        String compiled = template.render(RenderContext.builder().with(key, "<extra>").build());
        assertEquals(compiled, widget.render(RenderContext.builder().with(key, "<extra>").build()));
        assertTrue(compiled.contains("<strong>&lt;extra&gt;</strong><em>7</em>"));
        assertFalse(compiled.contains("Sign Up"));
        assertEquals(compiled, widget.render(RenderContext.builder().with(key, "<extra>").build()));
    }

    @Test
    void generatedAccountContentDoesNotRewriteConfiguredChildren() throws Exception {
        SlotKey<String> key = SlotKey.of("account-preserved-child");
        AccountWidget widget = AccountWidget.createGuest().withSignupUrl(null);
        HtmlTag child = new HtmlTag("strong").withInnerText(key);
        widget.withChild(child);
        List<?> configured = storedChildren(widget);
        Template template = widget.compile();

        for (String value : List.of("first<&", "second")) {
            RenderContext directContext = RenderContext.of(key, value);
            String direct = widget.render(directContext);
            String compiled = template.render(RenderContext.of(key, value));
            assertEquals(direct, compiled);
            assertEquals(value, org.jsoup.Jsoup.parseBodyFragment(direct).selectFirst("strong").text());
            assertEquals(1, storedChildren(widget).size());
            assertSame(child, storedChildren(widget).get(0));
            assertSame(configured, storedChildren(widget));
        }
    }

    @SuppressWarnings("unchecked")
    private static List<?> storedChildren(AccountWidget widget) throws Exception {
        Field field = HtmlTag.class.getDeclaredField("children");
        field.setAccessible(true);
        return (List<?>) field.get(widget);
    }

    @Test
    void concurrentFirstDirectAndCompiledRenderingIsPure() throws Exception {
        for (boolean compiled : new boolean[]{false, true}) {
            for (var widget : java.util.List.of(AccountWidget.createGuest(), AccountWidget.createAuthenticated("alice"))) {
                SlotKey<String> key = SlotKey.of("account-request");
                widget.withChild(new HtmlTag("strong").withInnerText(key));
                widget.withChild(new HtmlTag("span") {
                    @Override public String render(RenderContext context) {
                        return "<em>" + org.owasp.encoder.Encode.forHtml(context.get(key).orElseThrow()) + "</em>";
                    }
                });
                Template template = compiled ? widget.compile() : null;
                var start = new CyclicBarrier(12);
                try (var pool = Executors.newFixedThreadPool(12)) {
                    var calls = new ArrayList<Callable<String>>();
                    for (int i = 0; i < 12; i++) {
                        final int request = i;
                        calls.add(() -> {
                            start.await(10, TimeUnit.SECONDS);
                            var context = RenderContext.builder().with(key, "request-" + request).build();
                            return compiled ? template.render(context) : widget.render(context);
                        });
                    }
                    var results = pool.invokeAll(calls);
                    for (int i = 0; i < results.size(); i++) {
                        var doc = org.jsoup.Jsoup.parse(results.get(i).get(10, TimeUnit.SECONDS));
                        assertEquals(1, doc.select(".account-widget-user, .account-widget-guest").size());
                        assertEquals(2, doc.select(".account-widget-link").size());
                        assertEquals("request-" + i, doc.selectFirst("strong").text());
                        assertEquals("request-" + i, doc.selectFirst("em").text());
                    }
                }
                assertTrue(widget.render(RenderContext.builder().with(key, "still-present").build())
                    .contains("<strong>still-present</strong><em>still-present</em>"));
            }
        }
    }
}
