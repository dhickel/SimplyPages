package io.mindspice.simplypages.security;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.components.display.Modal;
import io.mindspice.simplypages.components.forms.Select;
import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.editing.EditModalBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * XSS protection tests for components that render user content.
 */
class XssProtectionTest {

    @Test
    @DisplayName("Select.Option should escape HTML in value attribute")
    void testSelectOptionValueEscaping() {
        Select select = Select.create("test")
            .addOption("<script>alert('xss')</script>", "Label");

        String html = select.render();

        assertFalse(html.contains("<script>"), "Raw script tag found in value");
        assertTrue(html.contains("&lt;script&gt;") || html.contains("&#"),
            "Script tag should be HTML-encoded");
    }

    @Test
    @DisplayName("Select.Option should escape HTML in label")
    void testSelectOptionLabelEscaping() {
        Select select = Select.create("test")
            .addOption("safe", "<img src=x onerror=alert('xss')>");

        String html = select.render();

        assertFalse(html.contains("<img"), "Raw img tag found in label");
        assertTrue(html.contains("&lt;img") || html.contains("&#"),
            "Img tag should be HTML-encoded");
    }

    @Test
    @DisplayName("Select.Option should escape quotes in value attribute")
    void testSelectOptionQuoteEscaping() {
        String payload = "value\" onclick=\"alert('xss')";
        Select select = Select.create("test")
            .addOption(payload, "Label");

        String html = select.render();

        assertFalse(html.contains(payload), "Attribute value should be encoded");
    }

    @Test
    @DisplayName("Modal should reject invalid IDs that could enable XSS")
    void testModalIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            Modal.create().withModalId("x').remove();alert('xss');//");
        });
    }

    @Test
    @DisplayName("Modal should accept valid alphanumeric IDs")
    void testModalValidId() {
        Modal modal = Modal.create().withModalId("valid-modal-123");
        String html = modal.render();
        assertTrue(html.contains("id=\"valid-modal-123\""));
    }

    @Test
    @DisplayName("Modal should escape title content")
    void testModalTitleEscaping() {
        Modal modal = Modal.create()
            .withTitle("<script>alert('xss')</script>");

        String html = modal.render();

        assertFalse(html.contains("<script>alert"), "Raw script in title");
    }

    @Test
    @DisplayName("EditModalBuilder should reject invalid modal container IDs")
    void testEditModalBuilderContainerIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            EditModalBuilder.create()
                .withModalContainerId("x').innerHTML='<script>alert(1)</script>')//");
        });
    }

    @Test
    @DisplayName("EditModalBuilder should reject invalid page container IDs")
    void testEditModalBuilderPageContainerIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            EditModalBuilder.create()
                .withPageContainerId("invalid id with spaces");
        });
    }

    @Test
    @DisplayName("TextInput should escape value attribute")
    void testTextInputValueEscaping() {
        TextInput input = TextInput.create("test")
            .withValue("\"><script>alert('xss')</script>");

        String html = input.render();

        assertFalse(html.contains("<script>"), "Raw script in value");
    }

    @Test
    @DisplayName("TextInput should escape placeholder attribute")
    void testTextInputPlaceholderEscaping() {
        TextInput input = TextInput.create("test")
            .withPlaceholder("\"><script>alert('xss')</script>");

        String html = input.render();

        assertFalse(html.contains("<script>"), "Raw script in placeholder");
    }

    @Test
    @DisplayName("Paragraph should escape inner text by default")
    void testParagraphInnerTextEscaping() {
        Paragraph p = new Paragraph("<script>alert('xss')</script>");

        String html = p.render();

        assertFalse(html.contains("<script>alert"), "Raw script in paragraph");
        assertTrue(html.contains("&lt;script&gt;") || html.contains("&#"),
            "Script should be encoded");
    }

    @Test
    @DisplayName("Div should escape inner text by default")
    void testDivInnerTextEscaping() {
        String html = new Div()
            .withInnerText("<script>alert('xss')</script>")
            .render();

        assertFalse(html.contains("<script>alert"), "Raw script in div");
    }

    @Test
    @DisplayName("Header should escape text content")
    void testHeaderTextEscaping() {
        Header header = Header.H1("<script>alert('xss')</script>");

        String html = header.render();

        assertFalse(html.contains("<script>alert"), "Raw script in header");
    }

    @Test
    @DisplayName("Alert should escape message content")
    void testAlertMessageEscaping() {
        Alert alert = Alert.danger("<script>alert('xss')</script>");

        String html = alert.render();

        assertFalse(html.contains("<script>alert"), "Raw script in alert");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<script>alert('xss')</script>",
        "<img src=x onerror=alert('xss')>",
        "<svg onload=alert('xss')>",
        "javascript:alert('xss')",
        "<body onload=alert('xss')>",
        "<iframe src=\"javascript:alert('xss')\">",
        "'-alert('xss')-'",
        "\"><script>alert('xss')</script>",
        "' onclick='alert(1)'",
        "<ScRiPt>alert('xss')</ScRiPt>"
    })
    @DisplayName("Common XSS payloads should be neutralized in text content")
    void testCommonXssPayloads(String payload) {
        Paragraph p = new Paragraph(payload);
        String html = p.render();

        assertFalse(html.contains("<script"), "Raw script tag found");
        assertFalse(html.contains("<Script"), "Raw Script tag found");
        assertFalse(html.contains("<ScRiPt"), "Raw ScRiPt tag found");
        assertFalse(html.contains("<img"), "Raw img tag found");
        assertFalse(html.contains("<svg"), "Raw svg tag found");
        assertFalse(html.contains("<iframe"), "Raw iframe tag found");
        assertFalse(html.contains("<body"), "Raw body tag found");
    }
}
