package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.forms.Form.Method;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.testutil.HtmlAssert;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

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
            .withHxScrollTargetTop()
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
            .attributeEquals("form#contact-form", "data-sp-scroll-top", "target")
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
            .attributeEquals("label.form-label", "for", "name-input")
            .hasElement("form.form > div.form-field > input#name-input.form-input")
            .attributeEquals("input#name-input", "name", "name");

        HtmlAssert.assertThat(unlabeledHtml)
            .doesNotHaveElement("label.form-label")
            .hasElement("form.form > div.form-field > input#nickname-input.form-input")
            .attributeEquals("input#nickname-input", "name", "nickname");
    }

    @ParameterizedTest
    @ValueSource(strings = {"input", "textarea", "select", "button"})
    void missingSingleControlIdsAreAssignedDuringAssemblyAndStayStable(String type) {
        HtmlTag control = singleControl(type);
        assertNull(control.getId());
        Form form = Form.create().addField("Field", control);
        String id = control.getId();
        assertNotNull(id, "Assembly, not rendering, must assign the control ID");
        assertFalse(id.isEmpty());

        var template = form.compile(); // Compile before the first direct render.
        String html = form.render();
        assertSingleControlLabel(html, id, type);
        assertEquals(html, template.render(RenderContext.empty()));
        assertEquals(html, form.render());
        assertEquals(html, template.render(RenderContext.empty()));
        assertEquals(id, control.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"input", "textarea", "select", "button"})
    void explicitSingleControlIdsArePreserved(String type) {
        HtmlTag control = singleControl(type).withId(type + "-explicit");
        Form form = Form.create().addField("Field", control);
        assertEquals(type + "-explicit", control.getId());
        assertSingleControlLabel(form.render(), control.getId(), type);
    }

    @ParameterizedTest
    @ValueSource(strings = {"input", "textarea", "select", "button"})
    void rawSingleControlIdsArePreserved(String type) {
        HtmlTag control = singleControl(type).withAttribute("ID", type + "-raw");
        Form form = Form.create().addField("Field", control);
        assertEquals(type + "-raw", control.getId());
        assertSingleControlLabel(form.render(), control.getId(), type);
    }

    @ParameterizedTest
    @ValueSource(strings = {"input", "textarea", "select", "button"})
    void emptySingleControlIdsAreReplacedWithUsableIds(String type) {
        HtmlTag control = singleControl(type).withId("");
        Form form = Form.create().addField("Field", control);
        assertFalse(control.getId().isEmpty());
        assertSingleControlLabel(form.render(), control.getId(), type);
    }

    @Test
    void replacingAnEmptyCheckboxInputIdAlsoUpdatesItsExistingOptionLabel() {
        Checkbox checkbox = Checkbox.create("agree", "yes").withId("").withLabel("Accept terms");
        Form form = Form.create().addField("Terms", checkbox);
        String id = checkbox.getId();
        assertFalse(id.isEmpty());
        var doc = Jsoup.parseBodyFragment(form.render());
        assertEquals(id, doc.selectFirst("input").id());
        assertEquals(id, doc.selectFirst("label.form-label").attr("for"));
        assertEquals(id, doc.selectFirst("label.checkbox-label").attr("for"));
        assertEquals(form.render(), form.compile().render(RenderContext.empty()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"default", "explicit", "raw-wrapper", "explicit-and-raw-wrapper", "own-label"})
    void checkboxFieldLabelTargetsInternalInputNotWrapper(String mode) {
        Checkbox checkbox = Checkbox.create("agree", "yes");
        if (mode.contains("explicit")) {
            checkbox.withId("agree-input");
        }
        if (mode.contains("raw-wrapper")) {
            checkbox.withAttribute("ID", "agree-wrapper");
        }
        if (mode.equals("own-label")) {
            checkbox.withLabel("Existing option label");
        }
        String previousId = checkbox.getId();
        Form form = Form.create().addField("Terms", checkbox);
        String inputId = checkbox.getId();
        assertNotNull(inputId);
        if (previousId != null) {
            assertEquals(previousId, inputId);
        }
        if (mode.contains("raw-wrapper")) {
            assertNotEquals("agree-wrapper", inputId);
        }
        // A label added after field assembly must use the same internal input ID.
        checkbox.withLabel("Existing option label");
        var template = form.compile();
        String html = form.render();
        var doc = Jsoup.parseBodyFragment(html);
        var wrapper = doc.selectFirst(".form-checkbox");
        assertNotNull(wrapper);
        assertEquals(mode.contains("raw-wrapper") ? "agree-wrapper" : "", wrapper.id());
        assertEquals(inputId, wrapper.selectFirst("input").id());
        assertEquals(inputId, doc.selectFirst("label.form-label").attr("for"));
        assertEquals(inputId, wrapper.selectFirst("label.checkbox-label").attr("for"));
        assertEquals("Existing option label", wrapper.selectFirst("label").text());
        assertEquals(html, template.render(RenderContext.empty()));
        assertEquals(html, form.render());
        assertEquals(inputId, checkbox.getId());
    }

    @Test
    void generatedIdsAreUniqueAcrossFieldsAndIndependentFormsWithRepeatedNames() {
        TextInput first = TextInput.create("same");
        TextInput second = TextInput.create("same");
        TextInput third = TextInput.create("same");
        Checkbox fourth = Checkbox.create("same", "yes");
        Form one = Form.create().addField("First", first).addField("Second", second);
        Form two = Form.create().addField("Third", third).addField("Fourth", fourth);
        var ids = List.of(first.getId(), second.getId(), third.getId(), fourth.getId());
        assertEquals(4, new HashSet<>(ids).size());
        var doc = Jsoup.parseBodyFragment(one.render() + two.render());
        for (var label : doc.select("label.form-label")) {
            assertEquals("input", doc.getElementById(label.attr("for")).tagName());
        }
    }

    @Test
    void omittedLabelsDoNotAssignIdsOrReplaceApplicationNames() {
        TextInput text = TextInput.create("name");
        Checkbox checkbox = Checkbox.create("agree", "yes");
        RadioGroup radios = RadioGroup.create("choice").addOption("yes", "Yes");
        radios.withAttribute("aria-label", "Application choice");
        Form form = Form.create()
            .addField(null, text)
            .addField("", checkbox)
            .addField(null, radios);
        assertNull(text.getId());
        assertNull(checkbox.getId());
        HtmlAssert.assertThat(form.render())
            .doesNotHaveElement(".form-label")
            .doesNotHaveElement(".form-radio-group[aria-labelledby]")
            .attributeEquals(".form-radio-group", "aria-label", "Application choice");
    }

    @Test
    void fieldLabelsAndExplicitIdReferencesRemainEscaped() {
        String id = "field-\"<&";
        Form form = Form.create().addField("<script>&Name", TextInput.create("name").withId(id));
        var doc = Jsoup.parseBodyFragment(form.render());
        assertEquals(id, doc.selectFirst("input").id());
        assertEquals(id, doc.selectFirst("label").attr("for"));
        assertEquals("<script>&Name", doc.selectFirst("label").text());
        assertTrue(doc.select("script").isEmpty());
        assertEquals(form.render(), form.compile().render(RenderContext.empty()));
    }

    @Test
    void customComponentsAreNotRenderedOrInferredDuringAssembly() {
        AtomicInteger renders = new AtomicInteger();
        SlotKey<String> text = SlotKey.of("custom-field");
        Component custom = context -> {
            renders.incrementAndGet();
            return new HtmlTag("span").withInnerText(text).render(context);
        };
        Form form = Form.create().addField("Custom caption", custom);
        assertEquals(0, renders.get());
        var template = form.compile();
        assertEquals(0, renders.get());
        RenderContext context = RenderContext.builder().with(text, "Request content").build();
        String html = form.render(context);
        assertEquals(html, template.render(context));
        assertEquals(2, renders.get());
        HtmlAssert.assertThat(html)
            .hasElement("span.form-label")
            .elementTextEquals("span.form-label", "Custom caption")
            .doesNotHaveElement("label.form-label, .form-label[for], .form-label[id]")
            .elementTextEquals(".form-field > span:not(.form-label)", "Request content");
    }

    @Test
    void customTagsAndMultiControlFieldsKeepApplicationOwnedLabeling() {
        HtmlTag rawInput = new HtmlTag("input", true).withAttribute("name", "raw");
        Div multi = new Div();
        multi.withId("address");
        multi.withChild(new HtmlTag("label").withAttribute("for", "city").withInnerText("City"));
        multi.withChild(TextInput.create("city").withId("city"));
        multi.withChild(new HtmlTag("label").withAttribute("for", "zip").withInnerText("ZIP"));
        multi.withChild(TextInput.create("zip").withId("zip"));
        Form form = Form.create().addField("Raw input caption", rawInput).addField("Address", multi);
        assertNull(rawInput.getId());
        assertEquals("address", multi.getId());
        String html = form.render();
        HtmlAssert.assertThat(html)
            .hasElementCount("span.form-label", 2)
            .doesNotHaveElement("label.form-label, .form-label[for], #address[aria-labelledby], input[name=raw][id]")
            .attributeEquals("#address > label:nth-of-type(1)", "for", "city")
            .attributeEquals("#address > label:nth-of-type(2)", "for", "zip")
            .elementTextEquals("#address > label:nth-of-type(1)", "City")
            .elementTextEquals("#address > label:nth-of-type(2)", "ZIP");
        assertEquals(html, form.compile().render(RenderContext.empty()));
    }

    @Test
    void knownSingleControlFieldsSnapshot() {
        Form form = Form.create()
            .addField("Name", TextInput.create("name").withId("name"))
            .addField("Notes", TextArea.create("notes").withId("notes").withValue("Notes & details"))
            .addField("Choice", Select.create("choice").withId("choice").addOption("a", "Alpha"))
            .addField("Terms", Checkbox.create("agree", "yes").withId("agree").withLabel("Accept terms"))
            .addField("Action", Button.submit("Save").withId("save"));
        String html = form.render();
        assertEquals(html, form.compile().render(RenderContext.empty()));
        assertEquals("Save", Jsoup.parseBodyFragment(html).selectFirst("button").text());
        SnapshotAssert.assertMatches("forms/form/known-single-controls", html);
    }

    private static HtmlTag singleControl(String type) {
        return switch (type) {
            case "input" -> TextInput.create("field");
            case "textarea" -> TextArea.create("field");
            case "select" -> Select.create("field").addOption("a", "Alpha");
            case "button" -> Button.submit("Save");
            default -> throw new IllegalArgumentException(type);
        };
    }

    private static void assertSingleControlLabel(String html, String id, String type) {
        var doc = Jsoup.parseBodyFragment(html);
        var label = doc.selectFirst("label.form-label");
        assertNotNull(label);
        assertEquals(id, label.attr("for"));
        assertNotNull(doc.getElementById(id));
        assertEquals(type, doc.getElementById(id).tagName());
    }
}
