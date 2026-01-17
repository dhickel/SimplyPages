package io.mindspice.demo.pages;

import io.mindspice.jhf.components.*;
import io.mindspice.jhf.components.display.*;
import io.mindspice.jhf.components.forms.*;
import io.mindspice.jhf.layout.*;
import io.mindspice.jhf.modules.*;
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
                        Select.create("strain_type")
                            .addOption("sativa", "Sativa")
                            .addOption("indica", "Indica")
                            .addOption("hybrid", "Hybrid")
                            .withSelectedValue("hybrid");
                        ```
                        """)))
                .addRow(row -> {
                    Form selectForm = Form.create()
                            .addField("Strain Type", Select.create("strain_type")
                                    .addOption("sativa", "Sativa")
                                    .addOption("indica", "Indica")
                                    .addOption("hybrid", "Hybrid")
                                    .withMaxWidth("250px"))
                            .addField("Growth Stage", Select.create("growth_stage")
                                    .addOption("seed", "Seedling")
                                    .addOption("veg", "Vegetative")
                                    .addOption("flower", "Flowering")
                                    .addOption("harvest", "Ready to Harvest")
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
                            .addField("Preferences", Checkbox.create("organic", "yes")
                                    .withLabel("Organic growing only"))
                            .addField("", Checkbox.create("indoor", "yes")
                                    .withLabel("Indoor growing"))
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

                    RadioGroup growMethod = RadioGroup.create("method")
                            .addOption("soil", "Soil")
                            .addOption("hydro", "Hydroponic")
                            .addOption("aero", "Aeroponic")
                            .inline();

                    Form radioForm = Form.create()
                            .addField("Experience Level", experienceLevel)
                            .addField("Growing Method", growMethod);

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
                        Putting it all together - a complete grow journal entry form with width constraints:

                        ```java
                        Form growJournalForm = Form.create()
                            .withId("grow-journal-form")
                            .addField("Date", TextInput.date("entry_date")
                                .withMaxWidth("200px")
                                .required())
                            .addField("Strain", TextInput.create("strain_name")
                                .withMaxWidth("350px")
                                .required())
                            .addField("Growth Stage", Select.create("stage")
                                .withMaxWidth("300px")
                                .addOption("seed", "Seedling")
                                .addOption("veg", "Vegetative")
                                .addOption("flower", "Flowering"))
                            .addField("Notes", TextArea.create("notes")
                                .withRows(5)
                                .withMaxWidth("700px"))
                            .addField("", Button.submit("Save Entry")
                                .withStyle(Button.ButtonStyle.PRIMARY)
                                .withMinWidth("180px"));
                        ```
                        """)))
                .addRow(row -> {
                    Form growJournalForm = Form.create()
                            .withId("grow-journal-form")
                            .addField("Entry Date", TextInput.date("entry_date")
                                    .required()
                                    .withMaxWidth("200px"))
                            .addField("Strain Name", TextInput.create("strain_name")
                                    .withPlaceholder("e.g., Blue Dream")
                                    .required()
                                    .withMaxWidth("350px"))
                            .addField("Growth Stage", Select.create("growth_stage")
                                    .addOption("seedling", "Seedling (1-3 weeks)")
                                    .addOption("vegetative", "Vegetative (3-16 weeks)")
                                    .addOption("flowering", "Flowering (8-11 weeks)")
                                    .addOption("harvest", "Ready to Harvest")
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
                            .addField("", Button.submit("Save Journal Entry")
                                    .withStyle(Button.ButtonStyle.PRIMARY)
                                    .withMinWidth("180px"));

                    row.withChild(ContentModule.create()
                            .withTitle("Grow Journal Entry Form")
                            .withCustomContent(growJournalForm));
                })

                .build();

        return page.render();
    }
}
