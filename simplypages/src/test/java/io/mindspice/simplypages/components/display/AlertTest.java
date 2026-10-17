package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.core.Attribute;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.Template;
import io.mindspice.simplypages.testutil.HtmlAssert;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlertTest {

    @Test
    @DisplayName("Alert preserves exact non-dismissible markup for every severity")
    void nonDismissibleMarkupRemainsCompatible() {
        assertEquals("<div class=\"alert alert-info\">Info</div>", Alert.info("Info").render());
        assertEquals("<div class=\"alert alert-success\">Success</div>", Alert.success("Success").render());
        assertEquals("<div class=\"alert alert-warning\">Warning</div>", Alert.warning("Warning").render());
        assertEquals("<div class=\"alert alert-danger\">Danger</div>", Alert.danger("Danger").render());

        for (Alert alert : List.of(
                Alert.info("Info"), Alert.success("Success"),
                Alert.warning("Warning"), Alert.danger("Danger"))) {
            HtmlAssert.assertThat(alert.render())
                .doesNotHaveElement(".alert-dismissible, button.alert-close");
        }
    }

    @Test
    @DisplayName("dismissible configures exactly one native named close control idempotently")
    void dismissibleCloseIsNativeNamedAndIdempotent() {
        Alert alert = Alert.warning("Heads up").dismissible();
        alert.dismissible().dismissible();

        String initial = alert.render();
        HtmlAssert.assertThat(initial)
            .hasElement("div.alert.alert-warning.alert-dismissible")
            .hasElementCount("button.alert-close", 1)
            .attributeEquals("button.alert-close", "type", "button")
            .attributeEquals("button.alert-close", "class", "alert-close")
            .attributeEquals("button.alert-close", "aria-label", "Dismiss alert")
            .elementTextEquals("button.alert-close", "×");

        alert.withAttribute("class", "caller-replacement");
        alert.dismissible().dismissible();
        String replaced = alert.render();
        HtmlAssert.assertThat(replaced)
            .hasElement("div.caller-replacement.alert-dismissible")
            .hasElementCount("button.alert-close", 1)
            .doesNotHaveElement("button.alert-close + button.alert-close");
        HtmlAssert.assertThat(replaced).doesNotHaveElement("div.alert-warning");
    }

    @Test
    @DisplayName("dismissible alerts escape content and preserve caller semantics and attributes")
    void escapingAndCallerAttributesArePreserved() {
        Alert alert = Alert.danger("<script>alert('x')</script> & text");
        alert.withAttribute("role", "status")
            .withAttribute("aria-live", "polite")
            .withAttribute("data-owner", "<caller>")
            .withAttribute("data-custom", "one & two");
        alert.dismissible();

        String html = alert.render();
        HtmlAssert.assertThat(html)
            .hasElement("div.alert.alert-danger.alert-dismissible")
            .attributeEquals("div.alert", "role", "status")
            .attributeEquals("div.alert", "aria-live", "polite")
            .attributeEquals("div.alert", "data-owner", "<caller>")
            .attributeEquals("div.alert", "data-custom", "one & two")
            .elementTextEquals("div.alert", "<script>alert('x')</script> & text ×")
            .doesNotHaveElement("div.alert > script");
        assertEquals("<div role=\"status\" aria-live=\"polite\" data-owner=\"&lt;caller>\""
            + " data-custom=\"one &amp; two\" class=\"alert alert-danger alert-dismissible\">"
            + "&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; text"
            + "<button type=\"button\" class=\"alert-close\" aria-label=\"Dismiss alert\">×</button></div>", html);
    }

    @Test
    @DisplayName("fresh direct, contextual, and compiled renders retain one configured child")
    void rendersDoNotMutateStoredChildrenBeforeWarmupOrConcurrently() throws Exception {
        SlotKey<String> payload = SlotKey.of("alert-payload");
        Alert alert = Alert.info("Message").dismissible();
        alert.withChild(new HtmlTag("span").withClass("payload").withInnerText(payload));
        Template template = alert.compile();

        int workers = 16;
        CyclicBarrier start = new CyclicBarrier(workers);
        var executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int index = 0; index < workers; index++) {
                int request = index;
                futures.add(executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    RenderContext context = RenderContext.of(payload, "value-" + request + "<&");
                    return request % 2 == 0 ? alert.render(context) : template.render(context);
                }));
            }

            for (int index = 0; index < workers; index++) {
                String html = futures.get(index).get(20, TimeUnit.SECONDS);
                String expected = "<div class=\"alert alert-info alert-dismissible\">Message"
                    + "<button type=\"button\" class=\"alert-close\" aria-label=\"Dismiss alert\">×</button>"
                    + "<span class=\"payload\">value-" + index + "&lt;&amp;</span></div>";
                assertEquals(expected, html);
                HtmlAssert.assertThat(html).hasElementCount("button.alert-close", 1);
            }
        } finally {
            executor.shutdownNow();
        }

        String repeated = alert.render(RenderContext.of(payload, "after"));
        HtmlAssert.assertThat(repeated)
            .hasElementCount("button.alert-close", 1)
            .hasElementCount("span.payload", 1)
            .elementTextEquals("span.payload", "after");
    }

    @Test
    @DisplayName("standard Alert templates snapshot configured controls without a warmup render")
    void compiledStructureIsAConfigurationSnapshot() {
        Alert alert = Alert.info("Original").dismissible();
        Template template = alert.compile();
        alert.withInnerText("Changed").withAttribute("data-later", "new");
        alert.withChild(new HtmlTag("strong").withInnerText("Later child"));
        alert.dismissible();

        assertEquals("<div class=\"alert alert-info alert-dismissible\">Original"
            + "<button type=\"button\" class=\"alert-close\" aria-label=\"Dismiss alert\">×</button></div>",
            template.render(RenderContext.empty()));
        HtmlAssert.assertThat(alert.render()).elementTextEquals("strong", "Later child");

        Alert initiallyStatic = Alert.success("Before");
        Template nonDismissible = initiallyStatic.compile();
        initiallyStatic.dismissible();
        assertEquals("<div class=\"alert alert-success\">Before</div>", nonDismissible.render(RenderContext.empty()));
    }

    @Test
    @DisplayName("Alert message slots escape direct and compiled request values without changing configuration")
    void messageSlotsAndStoredStructureRemainStable() {
        SlotKey<String> message = SlotKey.of("alert-message");
        InspectableAlert alert = new InspectableAlert();
        alert.withInnerText(message);
        alert.dismissible();
        List<Attribute> attributes = alert.configuredAttributes();
        List<Component> children = alert.configuredChildren();
        Template template = alert.compile();
        for (String value : List.of("First <&", "Second & value")) {
            String direct = alert.render(RenderContext.of(message, value));
            String compiled = template.render(RenderContext.of(message, value));
            assertEquals(direct, compiled);
            HtmlAssert.assertThat(compiled)
                .hasElementCount("button.alert-close", 1)
                .elementTextEquals("div.alert", value + " ×");
        }
        assertEquals(attributes, alert.configuredAttributes());
        assertEquals(children, alert.configuredChildren());
    }

    @Test
    @DisplayName("custom Alert render overrides remain live contextual delegates")
    void customAlertRendererIsNotFlattened() {
        SlotKey<String> value = SlotKey.of("custom-alert");
        Alert alert = new Alert("Static", Alert.AlertType.INFO) {
            @Override
            public String render(RenderContext context) {
                return new HtmlTag("aside").withInnerText(context.get(value).orElse("missing")).render(context);
            }
        };
        alert.dismissible();
        Template template = alert.compile();
        assertEquals("<aside>Custom &lt;value&gt;</aside>", template.render(RenderContext.of(value, "Custom <value>")));
        assertEquals("<aside>Later</aside>", template.render(RenderContext.of(value, "Later")));
    }

    @Test
    @DisplayName("compiled alerts retain custom child delegates and request context")
    void compiledCustomChildDelegateRemainsContextAware() {
        SlotKey<String> value = SlotKey.of("delegate-value");
        AtomicInteger calls = new AtomicInteger();
        Component delegate = context -> {
            calls.incrementAndGet();
            return new HtmlTag("em")
                .withClass("delegate")
                .withInnerText(context.get(value).orElse("missing"))
                .render(context);
        };

        Alert alert = Alert.success("Base").dismissible();
        alert.withChild(delegate);
        Template template = alert.compile();

        String directFirst = alert.render(RenderContext.of(value, "first <value>"));
        String compiledFirst = template.render(RenderContext.of(value, "first <value>"));
        String directSecond = alert.render(RenderContext.of(value, "second & value"));
        String compiledSecond = template.render(RenderContext.of(value, "second & value"));

        assertEquals(directFirst, compiledFirst);
        assertEquals(directSecond, compiledSecond);
        assertEquals(4, calls.get());
        HtmlAssert.assertThat(compiledFirst)
            .hasElement("em.delegate")
            .elementTextEquals("em.delegate", "first <value>");
        HtmlAssert.assertThat(compiledSecond)
            .hasElement("em.delegate")
            .elementTextEquals("em.delegate", "second & value");
    }

    @Test
    @DisplayName("Alert keeps its public surface and does not replace rendering with an override")
    void publicSignaturesRemainUnchanged() throws Exception {
        Set<String> publicDeclaredMethods = Arrays.stream(Alert.class.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .map(AlertTest::signature)
            .collect(Collectors.toSet());
        assertEquals(Set.of(
            "info(java.lang.String)->io.mindspice.simplypages.components.display.Alert",
            "success(java.lang.String)->io.mindspice.simplypages.components.display.Alert",
            "warning(java.lang.String)->io.mindspice.simplypages.components.display.Alert",
            "danger(java.lang.String)->io.mindspice.simplypages.components.display.Alert",
            "dismissible()->io.mindspice.simplypages.components.display.Alert",
            "withClass(java.lang.String)->io.mindspice.simplypages.components.display.Alert",
            "withClass(java.lang.String)->io.mindspice.simplypages.core.HtmlTag"),
            publicDeclaredMethods);
        assertEquals(1, Alert.class.getConstructors().length);
        assertArrayEquals(new Class<?>[]{String.class, Alert.AlertType.class},
            Alert.class.getConstructors()[0].getParameterTypes());
        assertThrows(NoSuchMethodException.class,
            () -> Alert.class.getDeclaredMethod("render", RenderContext.class));
    }

    @Test
    @DisplayName("Alert rendering snapshots cover stable default and composed structures")
    void alertSnapshots() {
        SnapshotAssert.assertMatches("display/alert/default", Alert.info("Information").render());
        SnapshotAssert.assertMatches("display/alert/dismissible",
            Alert.success("Saved successfully").dismissible().render());

        Alert outer = Alert.warning("Outer").dismissible();
        Alert inner = Alert.danger("Inner").dismissible();
        outer.withChild(inner);
        SnapshotAssert.assertMatches("display/alert/nested", outer.render());

        Div severities = new Div().withClass("alert-severities");
        severities.withChild(Alert.info(longMessage("info")).dismissible())
            .withChild(Alert.success(longMessage("success")).dismissible())
            .withChild(Alert.warning(longMessage("warning")).dismissible())
            .withChild(Alert.danger(longMessage("danger")).dismissible());
        SnapshotAssert.assertMatches("display/alert/severities", severities.render());

        Alert modalAlert = Alert.info("Inside modal").dismissible();
        modalAlert.withId("modal-alert");
        Div body = new Div().withId("modal-parent")
            .withChild(modalAlert)
            .withChild(new HtmlTag("div").withId("modal-tall").withAttribute("style", "height: 1200px"))
            .withChild(Button.create("After").withId("modal-after"));
        Modal modal = Modal.create().withModalId("alert-containing-modal")
            .withTitle("Alert fixture")
            .withBody(body);
        SnapshotAssert.assertMatches("display/alert/modal-alert", modal.render());
    }

    private static class InspectableAlert extends Alert {
        private InspectableAlert() {
            super("Original", AlertType.INFO);
        }

        private List<Attribute> configuredAttributes() {
            return List.copyOf(attributes);
        }

        private List<Component> configuredChildren() {
            return List.copyOf(children);
        }
    }

    private static String signature(Method method) {
        String parameters = Arrays.stream(method.getParameterTypes())
            .map(Class::getName)
            .collect(Collectors.joining(","));
        return method.getName() + "(" + parameters + ")->" + method.getReturnType().getName();
    }

    private static String longMessage(String severity) {
        return ("This is a long repeated " + severity + " alert message used for layout coverage. ").repeat(6)
            + "x".repeat(240);
    }
}
