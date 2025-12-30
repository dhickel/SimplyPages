package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.forms.*;
import io.mindspice.simplypages.layout.*;
import io.mindspice.simplypages.modules.*;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Dynamic content demo page showcasing HTMX integration.
 * 
 * This page demonstrates how to create dynamic modules that can be updated
 * independently using HTMX.
 */
@Component
public class DynamicDemoPage implements DemoPage {

    @Override
    public String render() {
        return renderWithContent("Default Card Title", "This is the default card content.",
                               Arrays.asList(
                                   new String[]{"Row 1 Col 1", "Row 1 Col 2", "Row 1 Col 3"},
                                   new String[]{"Row 2 Col 1", "Row 2 Col 2", "Row 2 Col 3"}
                               ),
                               Arrays.asList("Item 1", "Item 2", "Item 3", "Item 4"));
    }

    public String renderWithContent(String cardTitle, String cardContent, List<String[]> tableData, List<String> listItems) {
        Page page = Page.builder()
            .addComponents(Header.H1("Dynamic Content Demo"))
            .addRow(row -> row.withChild(new Markdown(
                """
                ## Dynamic Content with HTMX

                This page demonstrates how to create dynamic modules that can be updated independently
                using HTMX. The top row contains three modules (card, table, list) that
                can be updated individually using the form below.

                Each module can be updated via HTMX without refreshing the entire page.
                """)))

            // Top row with 3 modules
            .addRow(row -> {
                // Card module with dynamic content
                DynamicCardModule card = DynamicCardModule.create()
                    .withCardContent(cardTitle, cardContent)
                    .withModuleId("dynamic-card")
                    .withTitle("Dynamic Card");

                // Table module with dynamic content
                DynamicTableModule table = DynamicTableModule.create()
                    .withTableData(new String[]{"Column 1", "Column 2", "Column 3"}, tableData)
                    .withModuleId("dynamic-table")
                    .withTitle("Dynamic Table");

                // List module with dynamic content
                DynamicListModule list = DynamicListModule.create()
                    .withListItems(listItems)
                    .withModuleId("dynamic-list")
                    .withTitle("Dynamic List");

                row.withChild(card)
                   .withChild(table)
                   .withChild(list);
            })

            .addComponents(Spacer.vertical().large())

            // Bottom row with edit form
            .addRow(row -> row.withChild(Header.H2("Update Module Content").withBottomBar()))
            .addRow(row -> row.withChild(new Markdown(
                """
                Use the form below to update the content of any of the modules above.
                Select which module to update, enter new content, and click "Render".
                Only the selected module will be updated without refreshing the page.
                
                **Content Format:**
                - **Card Module**: Title in the first field, content in the second
                - **Table Module**: Comma-separated values for each row (e.g., "Col1,Col2,Col3")
                - **List Module**: One item per line
                """)))

            .addRow(row -> {
                Form form = Form.create()
                    .withHxPost("/api/dynamic-demo/update")
                    .withHxTarget("#dynamic-result")
                    .withHxSwap("innerHTML")
                    .withClass("row g-3");

                // Module selector dropdown
                Select moduleSelect = Select.create("module")
                    .withId("module-select")
                    .addOption("card", "Card Module")
                    .addOption("table", "Table Module")
                    .addOption("list", "List Module")
                    .withClass("form-select");

                // Text inputs for content
                TextInput titleInput = TextInput.create("title")
                    .withPlaceholder("Title (for card only)")
                    .withClass("form-control");

                TextArea contentInput = TextArea.create("content")
                    .withPlaceholder("Content (text for card, comma-separated rows for table, line-separated items for list)")
                    .withRows(4)
                    .withClass("form-control");

                // Submit button
                Button submitButton = Button.submit("Render")
                    .withStyle(Button.ButtonStyle.PRIMARY);

                form.addField("Module to Update", moduleSelect)
                    .addField("Title", titleInput)
                    .addField("Content", contentInput)
                    .addField("", submitButton);

                row.withChild(form);
            })

            .addRow(row -> {
                Div resultDiv = new Div()
                    .withAttribute("id", "dynamic-result")
                    .withClass("mt-3");
                row.withChild(resultDiv);
            })

            .build();

        return page.render();
    }
    
    // Methods to render individual modules for HTMX updates
    public String renderCardModule(String title, String content) {
        return DynamicCardModule.create()
            .withCardContent(title, content)
            .withModuleId("dynamic-card")
            .withTitle("Dynamic Card")
            .render();
    }
    
    public String renderTableModule(List<String[]> data) {
        return DynamicTableModule.create()
            .withTableData(new String[]{"Column 1", "Column 2", "Column 3"}, data)
            .withModuleId("dynamic-table")
            .withTitle("Dynamic Table")
            .render();
    }
    
    public String renderListModule(List<String> items) {
        return DynamicListModule.create()
            .withListItems(items)
            .withModuleId("dynamic-list")
            .withTitle("Dynamic List")
            .render();
    }
}