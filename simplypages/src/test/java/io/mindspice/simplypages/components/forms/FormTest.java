package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.components.forms.Form.Method;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FormTest {

    @Test
    @DisplayName("Form should render action and method")
    void testFormActionAndMethod() {
        Form form = Form.create()
            .withAction("/submit")
            .withMethod(Method.POST);

        String html = form.render();

        HtmlAssert.assertThat(html)
            .hasElement("form.form")
            .attributeEquals("form.form", "action", "/submit")
            .attributeEquals("form.form", "method", "POST");
    }

    @Test
    @DisplayName("Form should add hidden _method for PUT/DELETE/PATCH")
    void testFormMethodOverride() {
        Form form = Form.create()
            .withMethod(Method.PUT);

        String html = form.render();

        HtmlAssert.assertThat(html)
            .attributeEquals("form.form", "method", "POST")
            .hasElement("form.form > input[type=hidden][name=_method][value=PUT]");
    }

    @Test
    @DisplayName("Form GET should not add method override")
    void testFormGetMethod() {
        Form form = Form.create()
            .withMethod(Method.GET);

        String html = form.render();

        HtmlAssert.assertThat(html)
            .attributeEquals("form.form", "method", "GET")
            .doesNotHaveElement("input[name=_method]");
    }

    @Test
    @DisplayName("Form should include CSRF token input")
    void testCsrfToken() {
        Form form = Form.create()
            .withCsrfToken("token123");

        String html = form.render();

        HtmlAssert.assertThat(html)
            .hasElement("form.form > input[type=hidden][name=_csrf][value=token123]");
    }

    @Test
    @DisplayName("Form should set HTMX CSRF header")
    void testHxPostCsrf() {
        Form form = Form.create()
            .withHxPostCsrf("/save", "token123");

        String html = form.render();

        HtmlAssert.assertThat(html)
            .attributeEquals("form.form", "hx-post", "/save")
            .attributeEquals("form.form", "hx-headers", "{\"X-CSRF-TOKEN\": \"token123\"}");
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

        HtmlAssert.assertThat(html)
            .hasElement("form#contact-form.form.compact")
            .attributeEquals("form#contact-form", "hx-get", "/load")
            .attributeEquals("form#contact-form", "hx-put", "/update")
            .attributeEquals("form#contact-form", "hx-delete", "/remove")
            .attributeEquals("form#contact-form", "hx-target", "#target")
            .attributeEquals("form#contact-form", "hx-swap", "outerHTML")
            .attributeEquals("form#contact-form", "hx-trigger", "submit")
            .attributeEquals("form#contact-form", "enctype", "multipart/form-data")
            .attributeEquals("form#contact-form", "novalidate", "");
    }

    @Test
    @DisplayName("Form should handle labeled and unlabeled fields")
    void testFormFieldLabels() {
        Form labeled = Form.create()
            .addField("Name", TextInput.create("name").withId("name-input"));
        Form unlabeled = Form.create()
            .addField("", TextInput.create("nickname").withId("nickname-input"));

        String labeledHtml = labeled.render();
        String unlabeledHtml = unlabeled.render();

        HtmlAssert.assertThat(labeledHtml)
            .hasElement("form.form > div.form-field > label.form-label")
            .elementTextEquals("label.form-label", "Name")
            .hasElement("form.form > div.form-field > input#name-input.form-input")
            .attributeEquals("input#name-input", "name", "name");

        HtmlAssert.assertThat(unlabeledHtml)
            .doesNotHaveElement("label.form-label")
            .hasElement("form.form > div.form-field > input#nickname-input.form-input")
            .attributeEquals("input#nickname-input", "name", "nickname");
    }
}
