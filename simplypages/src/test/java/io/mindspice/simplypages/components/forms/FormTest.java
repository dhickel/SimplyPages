package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.forms.Form.Method;
import io.mindspice.simplypages.core.Attribute;
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
import java.util.regex.Pattern;

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
    void csrfHeaderOrdinaryOutputAndFluentReturnsRemainCompatible() {
        Form form = Form.create();
        assertSame(form, form.withHxPostCsrf("/save?x=1&y=2", "token123"));
        assertEquals("<form class=\"form\" hx-post=\"/save?x=1&amp;y=2\" "
            + "hx-headers=\"{&#34;X-CSRF-TOKEN&#34;: &#34;token123&#34;}\"></form>", form.render());
        assertSame(form, form.withCsrfHeaderName("X-Custom"));
        assertSame(form, form.withHxPostCsrf("/custom", "custom-token"));
        assertEquals("{\"X-Custom\": \"custom-token\"}", Jsoup.parseBodyFragment(form.render())
            .selectFirst("form").attr("hx-headers"));
        assertSame(form, form.withCsrfToken("hidden"));
    }

    @Test
    void csrfHeaderNullAndEmptyNamesTokensAndUrlsKeepExistingStringSemantics() {
        for (String name : new String[] {null, "", "X-Custom"}) {
            for (String token : new String[] {null, "", "token"}) {
                for (String url : new String[] {null, "", "/save?x=1&y=%25"}) {
                    Form form = Form.create().withCsrfHeaderName(name).withHxPostCsrf(url, token);
                    var template = form.compile(); // Compile before any direct render.
                    String html = form.render();
                    assertEquals(html, template.render(RenderContext.empty()));
                    var element = Jsoup.parseBodyFragment(html).selectFirst("form");
                    assertEquals("{\"" + String.valueOf(name) + "\": \"" + String.valueOf(token) + "\"}",
                        element.attr("hx-headers"));
                    assertTrue(element.hasAttr("hx-post"));
                    assertEquals(url == null ? "" : url, element.attr("hx-post"));
                }
            }
        }
    }

    @Test
    void csrfHeaderJsonEscapesQuotesBackslashesAndInjectionShapesWithoutAddingMarkup() {
        String name = "X-\"\\-Header";
        String token = "token\", \"X-Injected\": \"yes\\tail\" onload=\"bad <script>bad</script>";
        Form form = Form.create().withCsrfHeaderName(name).withHxPostCsrf("/save", token)
            .withCsrfToken(token).withChild(Button.submit("Save"));
        var template = form.compile();
        String html = form.render();
        assertEquals(html, template.render(RenderContext.empty()));
        var document = Jsoup.parseBodyFragment(html);
        var element = document.selectFirst("form");
        assertEquals("{\"X-\\\"\\\\-Header\": \"token\\\", \\\"X-Injected\\\": \\\"yes\\\\tail"
            + "\\\" onload=\\\"bad <script>bad</script>\"}", element.attr("hx-headers"));
        assertEquals(List.of("class", "hx-post", "hx-headers"), element.attributes().asList().stream()
            .map(attribute -> attribute.getKey()).toList());
        assertEquals(List.of("form", "input", "button"), document.body().getAllElements().stream()
            .skip(1).map(org.jsoup.nodes.Element::tagName).toList());
        assertEquals(token, element.selectFirst("input[name=_csrf]").attr("value"));
        assertEquals(1, Pattern.compile(" hx-headers=\"").matcher(html).results().count());
        assertEquals(1, Pattern.compile("<form\\b").matcher(html).results().count());
        assertEquals(0, Pattern.compile("<script\\b| onload=\"").matcher(html).results().count());
    }

    @Test
    void csrfHeaderJsonLeavesPrintableAsciiEntityLooksAndPercentSequencesLiteral() {
        String name = "&quot;&#34;&amp;%22%5C'<>/ :";
        String token = "~ &quot; &#x0; &amp; %00 %22 %5C <tag> / '";
        Form form = Form.create().withCsrfHeaderName(name).withHxPostCsrf("/save", token);
        String html = form.render();
        assertEquals("{\"" + name + "\": \"" + token + "\"}", Jsoup.parseBodyFragment(html)
            .selectFirst("form").attr("hx-headers"));
        assertEquals(html, form.compile().render(RenderContext.empty()));
        assertEquals(0, Jsoup.parseBodyFragment(html).select("tag").size());
    }

    @Test
    void csrfHeaderJsonEncodesControlBoundariesBmpPairsAndIsolatedSurrogatesAsAscii() {
        String name = "A\u0000\b\t\n\f\r\u001f\u007f\u0080\u009f\u00ff\u20ac\uD83D\uDE00\uD800";
        String token = "B\u0001\u001e\u007e\u0100\u2028\u2029\uFFFF\uD83D\uDE00\uDC00";
        Form form = Form.create().withCsrfHeaderName(name).withHxPostCsrf("/unicode", token);
        var template = form.compile();
        String html = form.render();
        assertEquals(html, template.render(RenderContext.empty()));
        assertTrue(html.chars().allMatch(unit -> unit >= 0x20 && unit < 0x7f));
        assertEquals("{\"A\\u0000\\u0008\\u0009\\u000a\\u000c\\u000d\\u001f\\u007f\\u0080"
                + "\\u009f\\u00ff\\u20ac\\ud83d\\ude00\\ud800\": "
                + "\"B\\u0001\\u001e~\\u0100\\u2028\\u2029\\uffff\\ud83d\\ude00\\udc00\"}",
            Jsoup.parseBodyFragment(html).selectFirst("form").attr("hx-headers"));
    }

    @Test
    void csrfHeaderConfigurationIsCapturedPerCallAndRepeatedCallsReplaceAliasesInOrder() {
        Form form = Form.create();
        form.withAttribute("data-before", "one");
        form.withAttribute("HX-POST", "/old");
        form.withAttribute("HX-HEADERS", "old");
        form.withCsrfHeaderName("first").withHxPostCsrf("/first", "token-one");
        form.withAttribute("data-after", "two");
        var template = form.compile();
        String first = form.render();
        form.withCsrfHeaderName("second");
        assertEquals(first, form.render(), "Changing configuration must not rewrite an existing pair");
        assertEquals(first, template.render(RenderContext.empty()));

        form.withHxPostCsrf("/second", "token-two");
        String html = form.render();
        var element = Jsoup.parseBodyFragment(html).selectFirst("form");
        assertEquals("/second", element.attr("hx-post"));
        assertEquals("{\"second\": \"token-two\"}", element.attr("hx-headers"));
        assertEquals("one", element.attr("data-before"));
        assertEquals("two", element.attr("data-after"));
        assertEquals(List.of("class", "data-before", "data-after", "hx-post", "hx-headers"),
            element.attributes().asList().stream().map(attribute -> attribute.getKey()).toList());
        assertEquals(1, Pattern.compile("(?i) hx-headers=\"").matcher(html).results().count());
        assertEquals(1, Pattern.compile("(?i) hx-post=\"").matcher(html).results().count());
        assertEquals(first, template.render(RenderContext.empty()), "Static templates retain the old pair");
        assertEquals(html, form.compile().render(RenderContext.empty()), "Fresh templates see new configuration");
    }

    @Test
    void csrfHeaderOutputKeepsContextDispatchHiddenTokenAndRenderingPurity() {
        SlotKey<String> content = SlotKey.of("form-content");
        InspectableForm form = new InspectableForm();
        form.withHxPostCsrf("/save", "token\"\\&").withCsrfToken("token\"\\&")
            .withChild(new HtmlTag("span").withInnerText(content))
            .withChild(context -> new HtmlTag("em").withInnerText(content).render(context));
        List<Attribute> attributes = form.attributeSnapshot();
        List<Component> children = form.childSnapshot();
        var template = form.compile(); // No warm-up render.
        for (String text : List.of("request <one>&", "request two")) {
            RenderContext context = RenderContext.builder().with(content, text).build();
            String direct = form.render(context);
            for (int repeat = 0; repeat < 3; repeat++) {
                assertEquals(direct, template.render(context));
                assertEquals(direct, form.render(context));
                assertEquals(attributes, form.attributeSnapshot());
                assertEquals(children, form.childSnapshot());
            }
            var document = Jsoup.parseBodyFragment(direct);
            assertEquals(text, document.selectFirst("span").text());
            assertEquals(text, document.selectFirst("em").text());
            assertEquals("token\"\\&", document.selectFirst("input[name=_csrf]").attr("value"));
            assertEquals(1, document.select("form").size());
            assertEquals(1, document.select("input[name=_csrf]").size());
        }
    }

    @Test
    void csrfJsonHelperDoesNotChangeLogicalMethodActionOrExistingHiddenInputs() {
        InspectableForm form = new InspectableForm();
        form.withMethod(Method.PUT).withAction("/native?x=1&y=2")
            .withCsrfToken(null).withCsrfToken("").withCsrfToken("token\"\\&");
        List<Component> children = form.childSnapshot();
        form.withHxPostCsrf("/htmx?x=1&y=%25", null);
        assertEquals(children, form.childSnapshot());
        var template = form.compile();
        String html = form.render();
        assertEquals(html, template.render(RenderContext.empty()));
        var element = Jsoup.parseBodyFragment(html).selectFirst("form");
        assertEquals("POST", element.attr("method"));
        assertEquals("/native?x=1&y=2", element.attr("action"));
        assertEquals("/htmx?x=1&y=%25", element.attr("hx-post"));
        assertEquals("{\"X-CSRF-TOKEN\": \"null\"}", element.attr("hx-headers"));
        assertEquals("PUT", element.selectFirst("input[name=_method]").attr("value"));
        var csrf = element.select("input[name=_csrf]");
        assertEquals(3, csrf.size());
        assertTrue(csrf.get(0).hasAttr("value"));
        assertTrue(csrf.get(1).hasAttr("value"));
        assertEquals("", csrf.get(0).attr("value"));
        assertEquals("", csrf.get(1).attr("value"));
        assertEquals("token\"\\&", csrf.get(2).attr("value"));
        assertEquals(2, Pattern.compile(" value />").matcher(html).results().count());
    }

    private static class InspectableForm extends Form {
        List<Attribute> attributeSnapshot() { return List.copyOf(attributes); }
        List<Component> childSnapshot() { return List.copyOf(children); }
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
