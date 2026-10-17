package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.Template;
import io.mindspice.simplypages.modules.ContentModule;
import io.mindspice.simplypages.testutil.HtmlAssert;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModalTest {

    @Test
    @DisplayName("Modal should render title, body, and footer in expected containers")
    void testModalRendering() {
        Modal modal = Modal.create()
            .withModalId("test-modal")
            .withTitle("Edit Item")
            .withBody(new Div().withInnerText("Body content"))
            .withFooter(Button.create("Save"));

        String html = modal.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.modal-backdrop#test-modal")
            .hasElement("div.modal-backdrop#test-modal > div.modal-container")
            .attributeEquals(".modal-backdrop", "data-close-on-backdrop", "true")
            .attributeEquals(".modal-backdrop", "data-close-on-escape", "true")
            .attributeEquals(".modal-container", "role", "dialog")
            .attributeEquals(".modal-container", "aria-modal", "true")
            .attributeEquals(".modal-container", "tabindex", "-1")
            .attributeEquals(".modal-container", "aria-labelledby", "test-modal-title")
            .attributeEquals(".modal-title", "id", "test-modal-title")
            .doesNotHaveElement(".modal-backdrop[onclick], .modal-backdrop[onkeydown], .modal-close[onclick]")
            .hasElement("div.modal-container > div.modal-header")
            .hasElement("div.modal-header > h3.modal-title")
            .hasElement("div.modal-container > div.modal-body")
            .hasElement("div.modal-container > div.modal-footer")
            .elementTextEquals("div.modal-header > h3.modal-title", "Edit Item")
            .elementTextEquals("div.modal-body", "Body content")
            .elementTextEquals("div.modal-footer > button", "Save")
            .attributeEquals("div.modal-footer > button", "type", "button")
            .childOrder("div.modal-container", "div.modal-header", "div.modal-body", "div.modal-footer");

        SnapshotAssert.assertMatches("display/modal/default", html);
    }

    @Test
    @DisplayName("Modal should allow disabling Escape key close")
    void testModalCloseOnEscapeDisabled() {
        String html = Modal.create()
            .withModalId("test-modal")
            .closeOnEscape(false)
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("div.modal-backdrop#test-modal")
            .attributeEquals("div.modal-backdrop#test-modal", "data-close-on-escape", "false")
            .attributeEquals(".modal-container", "aria-label", "Dialog")
            .doesNotHaveElement(".modal-backdrop[tabindex]");

        HtmlAssert.assertThat(html)
            .doesNotHaveElement("div.modal-backdrop[onkeydown]");

        SnapshotAssert.assertMatches("display/modal/escape-disabled", html);
    }

    @Test
    @DisplayName("Modal should render body/footer without header when close button and title are omitted")
    void testModalWithoutHeader() {
        String html = Modal.create()
            .withModalId("content-only-modal")
            .showCloseButton(false)
            .withBody(new Div().withInnerText("Body only"))
            .withFooter(Button.create("Close"))
            .render();

        HtmlAssert.assertThat(html)
            .doesNotHaveElement("div.modal-header")
            .attributeEquals(".modal-container", "role", "dialog")
            .attributeEquals(".modal-container", "aria-label", "Dialog")
            .attributeEquals(".modal-container", "tabindex", "-1")
            .doesNotHaveElement(".modal-container[aria-labelledby]")
            .hasElement("div.modal-body")
            .hasElement("div.modal-footer")
            .childOrder("div.modal-container", "div.modal-body", "div.modal-footer");

        SnapshotAssert.assertMatches("display/modal/content-only", html);
    }

    @Test
    @DisplayName("Modal should render full structure when nested in context-aware parent render paths")
    void testModalNestedInModuleRenderPath() {
        String html = ContentModule.create()
            .withTitle("Modal Host")
            .withCustomContent(Modal.create()
                .withModalId("nested-modal")
                .withTitle("Nested Modal")
                .withBody(new Div().withInnerText("Nested body")))
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("div.modal-backdrop#nested-modal")
            .hasElement("div.modal-backdrop#nested-modal > div.modal-container")
            .hasElement("div.modal-container > div.modal-body")
            .elementTextEquals("div.modal-body", "Nested body");
    }

    @Test
    @DisplayName("Modal body and footer should render slot-backed content with supplied context")
    void testModalContextAndTemplateRendering() {
        SlotKey<String> bodyText = SlotKey.of("modalBody");
        SlotKey<String> footerText = SlotKey.of("modalFooter");
        Modal modal = Modal.create()
            .withModalId("slot-modal")
            .withTitle("Slot Modal")
            .withBody(new Div().withInnerText(bodyText))
            .withFooter(new Div().withInnerText(footerText));
        RenderContext context = RenderContext.empty()
            .put(bodyText, "Body from context")
            .put(footerText, "Footer from context");

        String contextHtml = modal.render(context);
        String nestedHtml = new Div().withChild(modal).render(context);
        String templateHtml = Template.of(new Div().withChild(modal)).render(context);

        HtmlAssert.assertThat(contextHtml)
            .elementTextEquals("div.modal-body", "Body from context")
            .elementTextEquals("div.modal-footer", "Footer from context");

        HtmlAssert.assertThat(nestedHtml)
            .elementTextEquals("div.modal-body", "Body from context")
            .elementTextEquals("div.modal-footer", "Footer from context");

        HtmlAssert.assertThat(templateHtml)
            .elementTextEquals("div.modal-body", "Body from context")
            .elementTextEquals("div.modal-footer", "Footer from context");
    }

    @Test
    void flagsBlankTitleAndContextRemainIndependent() {
        var key = io.mindspice.simplypages.core.SlotKey.<String>of("modal-content");
        Modal modal = Modal.create().withModalId("context-modal").withTitle(" ")
            .showCloseButton(false).closeOnBackdrop(false).closeOnEscape(false)
            .withBody(io.mindspice.simplypages.core.Slot.of(key))
            .withFooter(io.mindspice.simplypages.core.Slot.of(key));
        String html = modal.compile().render(io.mindspice.simplypages.core.RenderContext.of(key, "<value>"));
        HtmlAssert.assertThat(html)
            .attributeEquals(".modal-backdrop", "data-close-on-backdrop", "false")
            .attributeEquals(".modal-backdrop", "data-close-on-escape", "false")
            .attributeEquals(".modal-container", "aria-label", "Dialog")
            .doesNotHaveElement(".modal-header, .modal-container[aria-labelledby], .modal-close, .modal-body value, .modal-footer value")
            .elementTextEquals(".modal-body", "<value>")
            .elementTextEquals(".modal-footer", "<value>");
    }

    @Test
    void titleIsEscapedAndIdsAreValidated() {
        String title = "<img src=x onerror=alert(1)> & \"title\"";
        HtmlAssert.assertThat(Modal.create().withModalId("safe-title").withTitle(title).render())
            .elementTextEquals("#safe-title-title", title)
            .doesNotHaveElement(".modal-title img")
            .attributeEquals(".modal-container", "aria-labelledby", "safe-title-title");
        for (String id : new String[]{null, "", "1modal", "bad'id", "bad id"}) {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Modal.create().withModalId(id));
        }
    }

    @Test void generatedModalIdsAreUniqueAcrossRapidCreation() {
        var ids = new java.util.HashSet<String>();
        for (int i = 0; i < 100; i++) {
            String id = org.jsoup.Jsoup.parse(Modal.create().render()).selectFirst(".modal-backdrop").id();
            org.junit.jupiter.api.Assertions.assertTrue(ids.add(id));
        }
    }
}
