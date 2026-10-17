package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.core.HtmlTag;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Writes fresh framework-generated alert and modal fragments for the native browser suite. */
class AlertBrowserFixtureTest {

    private static final Path FIXTURE_ROOT = Path.of("target", "alert-browser-fixtures");

    @Test
    @DisplayName("write fresh Java-generated Alert dismissal browser fixtures")
    void writesFreshAlertBrowserFixtures() throws IOException {
        Files.createDirectories(FIXTURE_ROOT);

        Alert alert = Alert.info("Message").dismissible();
        alert.withId("alert");
        write("alert.html", alert.render());

        Alert alert2 = Alert.success("Second message").dismissible();
        alert2.withId("alert2");
        write("alert2.html", alert2.render());

        Alert nonDismissible = Alert.warning("Persistent message");
        nonDismissible.withId("non-dismissible");
        write("non-dismissible.html", nonDismissible.render());

        Alert outer = Alert.info("Outer message").dismissible();
        outer.withId("outer-alert");
        Alert inner = Alert.danger("Inner message").dismissible();
        inner.withId("inner-alert");
        outer.withChild(inner);
        write("nested.html", outer.render());

        Div severities = new Div().withId("alert-severities");
        severities
            .withChild(severity(Alert.AlertType.INFO, "severity-info"))
            .withChild(severity(Alert.AlertType.SUCCESS, "severity-success"))
            .withChild(severity(Alert.AlertType.WARNING, "severity-warning"))
            .withChild(severity(Alert.AlertType.DANGER, "severity-danger"));
        write("severities.html", severities.render());

        Modal modal = Modal.create()
            .withModalId("alert-fixture-modal")
            .withTitle("Alert fixture modal")
            .withBody(new Div().withId("modal-persistent-content")
                .withInnerText("Persistent modal content"))
            .withFooter(Button.create("After").withId("modal-after"));
        write("modal.html", modal.render());

        Div modalParent = new Div().withId("modal-parent");
        Alert modalAlert = Alert.warning("Modal alert").dismissible();
        modalAlert.withId("modal-alert");
        modalParent
            .withChild(modalAlert)
            .withChild(new HtmlTag("div")
                .withId("modal-tall")
                .withAttribute("style", "height: 1200px"))
            .withChild(Button.create("After").withId("modal-after"));
        Modal alertContainingModal = Modal.create()
            .withModalId("alert-containing-modal")
            .withTitle("Alert containing modal")
            .withBody(modalParent);
        write("modal-alert.html", alertContainingModal.render());

        Div nestedBody = new Div()
            .withChild(new HtmlTag("button")
                .withAttribute("type", "button")
                .withId("nested-launcher")
                .withInnerText("Open nested"))
            .withChild(new Div().withId("nested-host"));
        Modal nestedModal = Modal.create()
            .withModalId("alert-outer-modal")
            .withTitle("Outer alert modal")
            .withBody(nestedBody);
        write("nested-modal.html", nestedModal.render());
    }

    private static Alert severity(Alert.AlertType type, String id) {
        Alert alert = new Alert(longMessage(type.name().toLowerCase()), type).dismissible();
        alert.withId(id);
        return alert;
    }

    private static String longMessage(String severity) {
        return ("This is a long repeated " + severity + " alert message used for responsive layout coverage. ").repeat(8)
            + "x".repeat(240);
    }

    private static void write(String fileName, String html) throws IOException {
        var document = Jsoup.parseBodyFragment(html);
        assertFalse(document.select(".alert, .modal-container").isEmpty(), fileName);
        for (var alert : document.select(".alert")) {
            var closes = alert.children().stream().filter(child -> child.hasClass("alert-close")).toList();
            assertEquals(alert.hasClass("alert-dismissible") ? 1 : 0, closes.size(), fileName);
            for (var close : closes) {
                assertEquals("button", close.tagName());
                assertEquals("button", close.attr("type"));
                assertEquals("Dismiss alert", close.attr("aria-label"));
                assertEquals("×", close.text());
            }
            assertFalse(alert.hasAttr("role"));
            assertFalse(alert.hasAttr("aria-live"));
        }
        Path output = FIXTURE_ROOT.resolve(fileName);
        Files.writeString(output, html, StandardCharsets.UTF_8);
        assertTrue(Files.isRegularFile(output), "Fixture was not written: " + output);
        assertEquals(html, Files.readString(output, StandardCharsets.UTF_8));
    }
}
