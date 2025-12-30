package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.display.*;
import io.mindspice.simplypages.components.forms.*;
import io.mindspice.simplypages.layout.*;
import io.mindspice.simplypages.modules.*;
import org.springframework.stereotype.Component;

/**
 * Forms page - comprehensive form components demonstration.
 *
 * <p>Demonstrates all form components:</p>
 * <ul>
 *   <li>TextInput (text, email, password, number, date)</li>
 *   <li>TextArea</li>
 *   <li>Select (dropdowns)</li>
 *   <li>Checkbox</li>
 *   <li>RadioGroup</li>
 *   <li>Button (submit, button, reset)</li>
 *   <li>Form composition</li>
 *   <li>CSS width constraints (withMaxWidth, withMinWidth, withWidth)</li>
 * </ul>
 */
@Component
public class FormsPage implements DemoPage {

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1("Form Components"))
                .addRow(row -> row.withChild(Alert.info(
                        "Forms are first-class citizens in JHF. Build complex forms with validation, " +
                        "styling, and HTMX integration using fluent APIs.")))

                // TextInput Examples
                .addComponents(Header.H2("Text Inputs"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **TextInput** supports different input types with width constraints:

                        ```java
                        // Regular text
                        TextInput.create("username")
                            .withPlaceholder("Enter username")
                            .withMaxWidth("300px")
                            .required();

                        // Email with validation (wider for email addresses)
                        TextInput.email("email")
                            .withPlaceholder("user@example.com")
                            .withMaxWidth("400px")
                            .required();

                        // Password (masked)
                        TextInput.password("password")
                            .withMaxWidth("300px")
                            .required();

                        // Number input (compact)
                        TextInput.number("age")
                            .withPlaceholder("Enter age")
                            .withMaxWidth("150px");

                        // Date picker
                        TextInput.date("birthdate")
                            .withMaxWidth("200px");
                        ```
                        """)))
                .addRow(row -> {
                    Form inputExamples = Form.create()
                            .addField("Text Input", TextInput.create("text_example")
                                    .withPlaceholder("Regular text input")
                                    .withMaxWidth("300px"))
                            .addField("Email", TextInput.email("email_example")
                                    .withPlaceholder("your@email.com")
                                    .withMaxWidth("400px"))
                            .addField("Password", TextInput.password("password_example")
                                    .withPlaceholder("Enter password")
                                    .withMaxWidth("300px"))
                            .addField("Number", TextInput.number("number_example")
                                    .withPlaceholder("Enter a number")
                                    .withMaxWidth("150px"))
                            .addField("Date", TextInput.date("date_example")
                                    .withMaxWidth("200px"));

                    row.withChild(ContentModule.create()
                            .withTitle("Input Types")
                            .withCustomContent(inputExamples));
                })

                // TextArea
                .addComponents(Header.H2("Text Area"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **TextArea** for multi-line text input with width constraint:

                        ```java
                        TextArea.create("description")
                            .withRows(5)
                            .withPlaceholder("Enter detailed description...")
                            .withMaxWidth("600px")
                            .required();
                        ```
                        """)))
                .addRow(row -> {
                    Form textAreaForm = Form.create()
                            .addField("Comments", TextArea.create("comments")
                                    .withRows(4)
                                    .withPlaceholder("Enter your comments here...")
                                    .withMaxWidth("600px"));

                    row.withChild(ContentModule.create()
                            .withTitle("Text Area Example")
                            .withCustomContent(textAreaForm));
                })

                // Select Dropdowns
                .addComponents(Header.H2("Select Dropdowns"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Select** components create dropdown menus:

                        ```java
                        Select.create("product_category")
                            .addOption("category_a", "Category A")
                            .addOption("category_b", "Category B")
                            .addOption("category_c", "Category C")
                            .withSelectedValue("category_b");
                        ```
                        """)))
                .addRow(row -> {
                    Form selectForm = Form.create()
                            .addField("Product Category", Select.create("product_category")
                                    .addOption("category_a", "Category A")
                                    .addOption("category_b", "Category B")
                                    .addOption("category_c", "Category C")
                                    .withMaxWidth("250px"))
                            .addField("Processing Stage", Select.create("processing_stage")
                                    .addOption("stage1", "Initial Stage")
                                    .addOption("stage2", "Processing")
                                    .addOption("stage3", "Quality Check")
                                    .addOption("stage4", "Ready for Distribution")
                                    .withMaxWidth("300px"));

                    row.withChild(ContentModule.create()
                            .withTitle("Dropdown Examples")
                            .withCustomContent(selectForm));
                })

                // Checkboxes
                .addComponents(Header.H2("Checkboxes"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Checkbox** for boolean options:

                        ```java
                        Checkbox.create("agree", "yes")
                            .withLabel("I agree to the terms")
                            .checked();
                        ```
                        """)))
                .addRow(row -> {
                    Form checkboxForm = Form.create()
                            .addField("Preferences", Checkbox.create("premium", "yes")
                                    .withLabel("Premium features only"))
                            .addField("", Checkbox.create("advanced", "yes")
                                    .withLabel("Advanced options"))
                            .addField("", Checkbox.create("newsletter", "yes")
                                    .withLabel("Subscribe to newsletter")
                                    .checked());

                    row.withChild(ContentModule.create()
                            .withTitle("Checkbox Examples")
                            .withCustomContent(checkboxForm));
                })

                // Radio Groups
                .addComponents(Header.H2("Radio Groups"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **RadioGroup** for mutually exclusive options:

                        ```java
                        RadioGroup.create("experience")
                            .addOption("beginner", "Beginner")
                            .addOption("intermediate", "Intermediate")
                            .addOption("expert", "Expert")
                            .withSelectedValue("beginner")
                            .inline();  // Display horizontally
                        ```
                        """)))
                .addRow(row -> {
                    RadioGroup experienceLevel = RadioGroup.create("experience")
                            .addOption("beginner", "Beginner")
                            .addOption("intermediate", "Intermediate")
                            .addOption("advanced", "Advanced")
                            .addOption("expert", "Expert")
                            .withSelectedValue("beginner");

                    RadioGroup processingMethod = RadioGroup.create("method")
                            .addOption("standard", "Standard")
                            .addOption("accelerated", "Accelerated")
                            .addOption("precision", "Precision")
                            .inline();

                    Form radioForm = Form.create()
                            .addField("Experience Level", experienceLevel)
                            .addField("Processing Method", processingMethod);

                    row.withChild(ContentModule.create()
                            .withTitle("Radio Group Examples")
                            .withCustomContent(radioForm));
                })

                // Buttons
                .addComponents(Header.H2("Buttons"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Button** components with different styles:

                        ```java
                        // Submit button
                        Button.submit("Submit Form")
                            .withStyle(Button.ButtonStyle.PRIMARY);

                        // Regular button
                        Button.button("Click Me")
                            .withStyle(Button.ButtonStyle.SUCCESS);

                        // Reset button
                        Button.reset("Clear Form")
                            .withStyle(Button.ButtonStyle.SECONDARY);
                        ```

                        Available styles: PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, INFO
                        """)))
                .addRow(row -> {
                    Div buttonExamples = new Div();
                    buttonExamples.withChild(Button.submit("Primary")
                            .withStyle(Button.ButtonStyle.PRIMARY));
                    buttonExamples.withChild(Button.create("Secondary")
                            .withStyle(Button.ButtonStyle.SECONDARY));
                    buttonExamples.withChild(Button.create("Success")
                            .withStyle(Button.ButtonStyle.SUCCESS));
                    buttonExamples.withChild(Button.create("Danger")
                            .withStyle(Button.ButtonStyle.DANGER));
                    buttonExamples.withChild(Button.create("Warning")
                            .withStyle(Button.ButtonStyle.WARNING));
                    buttonExamples.withChild(Button.create("Info")
                            .withStyle(Button.ButtonStyle.INFO));

                    row.withChild(ContentModule.create()
                            .withTitle("Button Styles")
                            .withCustomContent(buttonExamples));
                })

                // Complete Form Example
                .addComponents(Header.H2("Complete Form Example"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Putting it all together - a complete data collection form with width constraints:

                        ```java
                        Form dataEntryForm = Form.create()
                            .withId("data-entry-form")
                            .addField("Date", TextInput.date("entry_date")
                                .withMaxWidth("200px")
                                .required())
                            .addField("Product Name", TextInput.create("product_name")
                                .withMaxWidth("350px")
                                .required())
                            .addField("Processing Stage", Select.create("stage")
                                .withMaxWidth("300px")
                                .addOption("stage1", "Initial Stage")
                                .addOption("stage2", "Processing")
                                .addOption("stage3", "Quality Check"))
                            .addField("Notes", TextArea.create("notes")
                                .withRows(5)
                                .withMaxWidth("700px"))
                            .addField("", Button.submit("Save Entry")
                                .withStyle(Button.ButtonStyle.PRIMARY)
                                .withMinWidth("180px"));
                        ```
                        """)))
                .addRow(row -> {
                    Form dataCollectionForm = Form.create()
                            .withId("data-collection-form")
                            .addField("Entry Date", TextInput.date("entry_date")
                                    .required()
                                    .withMaxWidth("200px"))
                            .addField("Product Name", TextInput.create("product_name")
                                    .withPlaceholder("e.g., Product Alpha")
                                    .required()
                                    .withMaxWidth("350px"))
                            .addField("Processing Stage", Select.create("processing_stage")
                                    .addOption("stage1", "Initial Stage (1-3 days)")
                                    .addOption("stage2", "Processing (3-16 days)")
                                    .addOption("stage3", "Quality Check (8-11 days)")
                                    .addOption("stage4", "Ready for Distribution")
                                    .withMaxWidth("300px"))
                            .addField("Temperature (°F)", TextInput.number("temperature")
                                    .withPlaceholder("70-85")
                                    .withMaxWidth("150px"))
                            .addField("Humidity (%)", TextInput.number("humidity")
                                    .withPlaceholder("40-70")
                                    .withMaxWidth("150px"))
                            .addField("Notes", TextArea.create("notes")
                                    .withRows(5)
                                    .withPlaceholder("Observations, issues, or changes...")
                                    .withMaxWidth("700px"))
                            .addField("Public Entry", Checkbox.create("public", "yes")
                                    .withLabel("Share with community")
                                    .checked())
                            .addField("", Button.submit("Save Data Entry")
                                    .withStyle(Button.ButtonStyle.PRIMARY)
                                    .withMinWidth("180px"));

                    row.withChild(ContentModule.create()
                            .withTitle("Data Collection Entry Form")
                            .withCustomContent(dataCollectionForm));
                })

                .build();

        return page.render();
    }
}
