package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.display.*;
import io.mindspice.simplypages.layout.*;
import io.mindspice.simplypages.modules.*;
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
                            .withHeaders("Name", "Category", "Property A", "Rating")
                            .addRow("Product Alpha", "Category B", "17-24", "4.5/5")
                            .addRow("Product Beta", "Category B", "19-26", "4.7/5")
                            .striped()      // Alternating row colors
                            .bordered()     // Cell borders
                            .hoverable();   // Highlight on hover
                        ```
                        """)))
                .addRow(row -> {
                    Table productTable = Table.create()
                            .withHeaders("Product", "Category", "Property A", "Property B", "Rating")
                            .addRow("Product Alpha", "Category B", "17-24", "0.1-0.2", "⭐⭐⭐⭐½")
                            .addRow("Product Beta", "Category B", "19-26", "0.1-0.3", "⭐⭐⭐⭐⭐")
                            .addRow("Product Gamma", "Category A", "16-21", "0.1", "⭐⭐⭐⭐½")
                            .addRow("Product Delta", "Category C", "20-25", "0.2", "⭐⭐⭐⭐")
                            .addRow("Product Epsilon", "Category B", "18-28", "0.1", "⭐⭐⭐⭐⭐")
                            .addRow("Product Zeta", "Category C", "18-24", "0.1", "⭐⭐⭐⭐½")
                            .striped()
                            .bordered()
                            .hoverable();

                    row.withChild(ContentModule.create()
                            .withTitle("Product Database")
                            .withCustomContent(productTable));
                })

                // DataTable (type-safe)
                .addComponents(Header.H2("Type-Safe DataTable"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **DataTable** provides type-safe data display with method references:

                        ```java
                        DataTable<Product> table = DataTable.create(Product.class)
                            .addColumn("Name", Product::getName)
                            .addColumn("Category", Product::getCategory)
                            .addColumn("Property A", p -> p.getPropertyA() + "")
                            .withData(productList)
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
