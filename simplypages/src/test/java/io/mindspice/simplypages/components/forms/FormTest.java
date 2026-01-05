package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.components.forms.Form.Method;
import io.mindspice.simplypages.components.forms.TextInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormTest {

    @Test
    @DisplayName("Form should render action and method")
    void testFormActionAndMethod() {
        Form form = Form.create()
            .withAction("/submit")
            .withMethod(Method.POST);

        String html = form.render();

        assertTrue(html.contains("action=\"/submit\""));
        assertTrue(html.contains("method=\"POST\""));
    }

    @Test
    @DisplayName("Form should add hidden _method for PUT/DELETE/PATCH")
    void testFormMethodOverride() {
        Form form = Form.create()
            .withMethod(Method.PUT);

        String html = form.render();

        assertTrue(html.contains("method=\"POST\""));
        assertTrue(html.contains("name=\"_method\""));
        assertTrue(html.contains("value=\"PUT\""));
    }

    @Test
    @DisplayName("Form GET should not add method override")
    void testFormGetMethod() {
        Form form = Form.create()
            .withMethod(Method.GET);

        String html = form.render();

        assertTrue(html.contains("method=\"GET\""));
        assertFalse(html.contains("name=\"_method\""));
    }

    @Test
    @DisplayName("Form should include CSRF token input")
    void testCsrfToken() {
        Form form = Form.create()
            .withCsrfToken("token123");

        String html = form.render();

        assertTrue(html.contains("name=\"_csrf\""));
        assertTrue(html.contains("value=\"token123\""));
    }

    @Test
    @DisplayName("Form should set HTMX CSRF header")
    void testHxPostCsrf() {
        Form form = Form.create()
            .withHxPostCsrf("/save", "token123");

        String html = form.render();

        assertTrue(html.contains("hx-post=\"/save\""));
        assertTrue(html.contains("hx-headers="));
        assertTrue(html.contains("X-CSRF-TOKEN"));
    }

    @Test
    @DisplayName("Form should render HTMX and form attributes")
    void testFormAttributes() {
        Form form = Form.create()
            .withId("contact-form")
            .withClass("compact")
            .withHxGet("/load")
            .withHxPut("/update")
            .withHxDelete("/remove")
            .withHxTarget("#target")
            .withHxSwap("outerHTML")
            .withHxTrigger("submit")
            .withEnctype("application/x-www-form-urlencoded")
            .noValidate()
            .multipart();

        String html = form.render();

        assertTrue(html.contains("id=\"contact-form\""));
        assertTrue(html.contains("class=\"form compact\""));
        assertTrue(html.contains("hx-get=\"/load\""));
        assertTrue(html.contains("hx-put=\"/update\""));
        assertTrue(html.contains("hx-delete=\"/remove\""));
        assertTrue(html.contains("hx-target=\"#target\""));
        assertTrue(html.contains("hx-swap=\"outerHTML\""));
        assertTrue(html.contains("hx-trigger=\"submit\""));
        assertTrue(html.contains("enctype=\"multipart/form-data\""));
        assertTrue(html.contains("novalidate"));
    }

    @Test
    @DisplayName("Form should handle labeled and unlabeled fields")
    void testFormFieldLabels() {
        Form labeled = Form.create()
            .addField("Name", TextInput.create("name"));
        Form unlabeled = Form.create()
            .addField("", TextInput.create("nickname"));

        String labeledHtml = labeled.render();
        String unlabeledHtml = unlabeled.render();

        assertTrue(labeledHtml.contains("form-label"));
        assertTrue(labeledHtml.contains(">Name<"));
        assertTrue(labeledHtml.contains("name=\"name\""));
        assertFalse(unlabeledHtml.contains("form-label"));
        assertTrue(unlabeledHtml.contains("name=\"nickname\""));
    }
}
