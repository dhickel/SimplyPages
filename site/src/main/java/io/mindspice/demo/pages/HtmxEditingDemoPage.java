package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.forms.Form;
import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.Slot;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.Template;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.modules.ContentModule;
import org.springframework.stereotype.Component;

@Component
public class HtmxEditingDemoPage implements DemoPage {

    public static final SlotKey<String> TITLE_SLOT = SlotKey.of("dynamic_title");
    public static final SlotKey<String> BODY_SLOT = SlotKey.of("dynamic_body");

    public static final Template CARD_TEMPLATE = Template.of(
        ContentModule.create()
            .withModuleId("dynamic-card-preview")
            .withTitle("Template Render")
            .withCustomContent(new Div()
                .withChild(new HtmlTag("h3").withChild(Slot.of(TITLE_SLOT)))
                .withChild(new Paragraph().withChild(Slot.of(BODY_SLOT))))
    );

    public static String renderTemplateCard(String title, String body) {
        return CARD_TEMPLATE.render(
            RenderContext.builder()
                .with(TITLE_SLOT, title)
                .with(BODY_SLOT, body)
                .build()
        );
    }

    @Override
    public String render() {
        Form templateForm = Form.create()
            .withHxPost("/demos/api/template-card")
            .withHxTarget("#dynamic-card-preview")
            .withHxSwap("outerHTML")
            .addField("Title", TextInput.create("title").withPlaceholder("Template title").required())
            .addField("Body", TextInput.create("body").withPlaceholder("Template body").required())
            .withChild(Button.submit("Render Template"));

        Button loadEditingButton = Button.create("Load editing demo fragment").withStyle(Button.ButtonStyle.INFO);
        loadEditingButton.withAttribute("hx-get", "/editing-demo");
        loadEditingButton.withAttribute("hx-target", "#editing-fragment");
        loadEditingButton.withAttribute("hx-swap", "innerHTML");

        return Page.builder()
            .addComponents(Header.H1("HTMX & Editing"))
            .addComponents(new Markdown("""
                ## Sections
                - [Template + SlotKey](#template)
                - [Editing Integration](#editing)
                """))
            .addRow(row -> row
                .withChild(new Column().withWidth(6).withChild(new Div().withId("template")
                    .withChild(ContentModule.create()
                        .withTitle("Template + SlotKey")
                        .withCustomContent(templateForm))))
                .withChild(new Column().withWidth(6).withChild(new RawHtml(renderTemplateCard(
                    "Initial Card", "Update this module with the form above.")))))
            .addRow(row -> row.withChild(new Div().withId("editing")
                .withChild(ContentModule.create()
                    .withTitle("Editing Integration")
                    .withCustomContent(new Div()
                        .withChild(Alert.info("Load the editing flow in-page. Endpoints remain canonical under /editing-demo."))
                        .withChild(Spacer.vertical().small())
                        .withChild(loadEditingButton)
                        .withChild(Spacer.vertical().small())
                        .withChild(new Div().withId("editing-fragment"))))))
            .build()
            .render();
    }
}
