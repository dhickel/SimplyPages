package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.forms.*;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.modules.ContentModule;
import org.springframework.stereotype.Component;

@Component
public class BasicsFormsDemoPage implements DemoPage {

    @Override
    public String render() {
        Form sampleForm = Form.create()
            .withHxPost("/demos/api/form-preview")
            .withHxTarget("#form-preview")
            .withHxSwap("outerHTML")
            .addField("Name", TextInput.create("name").withPlaceholder("Ada Lovelace").required())
            .addField("Email", TextInput.email("email").withPlaceholder("ada@example.com").required())
            .addField("Role", Select.create("role")
                .addOption("engineer", "Engineer", true)
                .addOption("designer", "Designer")
                .addOption("operator", "Operator"))
            .addField("Plan", RadioGroup.create("plan")
                .addOption("starter", "Starter")
                .addOption("pro", "Pro")
                .addOption("enterprise", "Enterprise")
                .withSelectedValue("pro"))
            .addField("Notes", TextArea.create("notes").withRows(3).withPlaceholder("Short note"))
            .withChild(Checkbox.create("terms", "accepted").withLabel("Accept terms").required())
            .withChild(Button.submit("Submit"));

        Form inputTypes = Form.create()
            .addField("Text", TextInput.create("text_demo").withPlaceholder("Plain text input"))
            .addField("Email", TextInput.email("email_demo").withPlaceholder("team@simplypages.dev"))
            .addField("Password", TextInput.password("pw_demo").withPlaceholder("••••••••"))
            .addField("Number", TextInput.number("num_demo").withPlaceholder("42"))
            .addField("Date", TextInput.date("date_demo"));

        Form choiceInputs = Form.create()
            .addField("Priority", Select.create("priority")
                .addOption("normal", "Normal", true)
                .addOption("high", "High")
                .addOption("urgent", "Urgent"))
            .addField("Mode", RadioGroup.create("mode")
                .addOption("sync", "Sync")
                .addOption("async", "Async")
                .withSelectedValue("async"))
            .withChild(Checkbox.create("notify", "yes").withLabel("Notify reviewers"));

        return Page.builder()
            .addComponents(Header.H1("Basics & Forms"))
            .addComponents(new Markdown("""
                ## Sections
                - [Text & Primitive Components](#text-primitives)
                - [Form Components](#form-components)
                - [Composed Form Flow](#form-flow)
                """))

            .addRow(row -> row.withChild(new Div().withId("text-primitives").withChild(
                ContentModule.create()
                    .withTitle("Text & Primitive Components")
                    .withContent("Each primitive is shown with isolated examples, then grouped into practical compositions.")
            )))

            .addRow(row -> row
                .withChild(new Column().withWidth(3).withChild(section("Header", Header.H3("Header.H3 Example"))))
                .withChild(new Column().withWidth(3).withChild(section("Paragraph", new Paragraph("Paragraph escapes plain text by default for safe rendering."))))
                .withChild(new Column().withWidth(3).withChild(section("Code", Code.inline("ContentModule.create().withTitle(\"Example\")"))))
                .withChild(new Column().withWidth(3).withChild(section("Blockquote", Blockquote.create("Build static module structure once; render dynamic values with slots.")))))

            .addRow(row -> row
                .withChild(new Column().withWidth(3).withChild(section("Markdown", new Markdown("""
                    **Markdown** supports structured prose and lists.

                    - headings
                    - emphasis
                    - links
                    """))))
                .withChild(new Column().withWidth(3).withChild(section("RawHtml", new RawHtml("<p><strong>RawHtml:</strong> trusted markup passthrough.</p>"))))
                .withChild(new Column().withWidth(3).withChild(section("Div + TextNode", new Div()
                    .withChild(new Paragraph()
                        .withChild(new TextNode("TextNode supports inline composition inside "))
                        .withChild(Code.inline("Paragraph"))
                        .withChild(new TextNode(" content."))))))
                .withChild(new Column().withWidth(3).withChild(section("Divider + Spacer", new Div()
                    .withChild(new Paragraph("Content above divider"))
                    .withChild(Spacer.vertical().small())
                    .withChild(Divider.horizontal())
                    .withChild(Spacer.vertical().small())
                    .withChild(new Paragraph("Content below divider"))))))

            .addRow(row -> row.withChild(new Column().withWidth(6).withChild(section("Dropdown", Dropdown.create("Open menu")
                .addLink("Docs", "/docs")
                .addLink("Modules", "/demos/modules")
                .addDivider()
                .addLink("HTMX & Editing", "/demos/htmx-editing")
                .build()))))

            .addRow(row -> row.withChild(new Div().withId("form-components").withChild(
                ContentModule.create()
                    .withTitle("Form Components")
                    .withContent("Inputs are split by intent: typed values, choices, and action controls.")
            )))

            .addRow(row -> row
                .withChild(new Column().withWidth(6).withChild(section("Typed Inputs", inputTypes)))
                .withChild(new Column().withWidth(6).withChild(section("Choice Inputs", choiceInputs))))

            .addRow(row -> row
                .withChild(new Column().withWidth(4).withChild(section("Buttons", new Div()
                    .withChild(Button.submit("Primary"))
                    .withChild(Button.create("Secondary").withStyle(Button.ButtonStyle.SECONDARY))
                    .withChild(Button.create("Success").withStyle(Button.ButtonStyle.SUCCESS))
                    .withChild(Button.create("Danger").withStyle(Button.ButtonStyle.DANGER)))))
                .withChild(new Column().withWidth(4).withChild(section("Checkbox", Checkbox.create("audit", "yes").withLabel("Mark for audit"))))
                .withChild(new Column().withWidth(4).withChild(section("RadioGroup", RadioGroup.create("release")
                    .addOption("nightly", "Nightly")
                    .addOption("stable", "Stable")
                    .withSelectedValue("stable")))))

            .addRow(row -> row.withChild(new Div().withId("form-flow").withChild(
                ContentModule.create()
                    .withTitle("Composed Form Flow")
                    .withContent("End-to-end form example with HTMX preview target.")
            )))

            .addRow(row -> row
                .withChild(new Column().withWidth(8).withChild(section("Demo Form", sampleForm)))
                .withChild(new Column().withWidth(4).withChild(new Div().withId("form-preview")
                    .withChild(ContentModule.create()
                        .withTitle("Preview Target")
                        .withContent("Submit to render server-generated fragment output here.")))))

            .addRow(row -> row.withChild(section("Catalog Coverage", new Markdown("""
                Covered primitives and form controls on this page:

                Header, Paragraph, Markdown, Div, TextNode, RawHtml, Code, Blockquote, Divider, Spacer, Dropdown,
                Form, TextInput, TextArea, Select, Checkbox, RadioGroup, Button.
                """))))
            .build()
            .render();
    }

    private ContentModule section(String title, io.mindspice.simplypages.core.Component content) {
        return ContentModule.create()
            .withTitle(title)
            .withCustomContent(content);
    }
}
