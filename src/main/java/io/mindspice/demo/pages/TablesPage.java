package io.mindspice.demo.pages;

import io.mindspice.jhf.components.*;
import io.mindspice.jhf.components.display.*;
import io.mindspice.jhf.layout.*;
import io.mindspice.jhf.modules.*;
import org.springframework.stereotype.Component;

/**
 * Tables page - demonstrates Table and DataTable components.
 */
@Component
public class TablesPage implements DemoPage {

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1("Table Components"))
                .addRow(row -> row.withChild(Alert.success(
                        "Tables are perfect for displaying structured data in rows and columns.")))

                // Basic Table
                .addComponents(Header.H2("Basic Table"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Table** component for manual table construction:

                        ```java
                        Table.create()
                            .withHeaders("Name", "Type", "THC %", "Rating")
                            .addRow("Blue Dream", "Hybrid", "17-24%", "4.5/5")
                            .addRow("OG Kush", "Hybrid", "19-26%", "4.7/5")
                            .striped()      // Alternating row colors
                            .bordered()     // Cell borders
                            .hoverable();   // Highlight on hover
                        ```
                        """)))
                .addRow(row -> {
                    Table strainTable = Table.create()
                            .withHeaders("Strain", "Type", "THC %", "CBD %", "Rating")
                            .addRow("Blue Dream", "Hybrid", "17-24%", "0.1-0.2%", "⭐⭐⭐⭐½")
                            .addRow("OG Kush", "Hybrid", "19-26%", "0.1-0.3%", "⭐⭐⭐⭐⭐")
                            .addRow("Northern Lights", "Indica", "16-21%", "0.1%", "⭐⭐⭐⭐½")
                            .addRow("Sour Diesel", "Sativa", "20-25%", "0.2%", "⭐⭐⭐⭐")
                            .addRow("Girl Scout Cookies", "Hybrid", "18-28%", "0.1%", "⭐⭐⭐⭐⭐")
                            .addRow("Jack Herer", "Sativa", "18-24%", "0.1%", "⭐⭐⭐⭐½")
                            .striped()
                            .bordered()
                            .hoverable();

                    row.withChild(ContentModule.create()
                            .withTitle("Cannabis Strains Database")
                            .withCustomContent(strainTable));
                })

                // DataTable (type-safe)
                .addComponents(Header.H2("Type-Safe DataTable"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **DataTable** provides type-safe data display with method references:

                        ```java
                        DataTable<Strain> table = DataTable.create(Strain.class)
                            .addColumn("Name", Strain::getName)
                            .addColumn("Type", Strain::getType)
                            .addColumn("THC %", s -> s.getThcPercentage() + "%")
                            .withData(strainList)
                            .striped()
                            .hoverable();
                        ```

                        Benefits:
                        * Compile-time type checking
                        * Refactoring support
                        * Lambda expressions for custom formatting
                        * Automatic null handling

                        Note: This example uses mock data since we don't have a database connection.
                        """)))
                .addRow(row -> row.withChild(Alert.warning(
                        "DataTable requires actual Java objects. See the source code for DataModule " +
                        "examples with type-safe column definitions.")))

                .build();

        return page.render();
    }
}
