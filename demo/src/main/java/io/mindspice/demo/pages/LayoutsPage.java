package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.display.*;
import io.mindspice.simplypages.layout.*;
import org.springframework.stereotype.Component;

/**
 * Layouts page - demonstrates the layout system.
 */
@Component
public class LayoutsPage implements DemoPage {

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1("Layout System"))

                .addRow(row -> row.withChild(Alert.info(
                        "JHF provides a flexible layout system with rows, columns, and grids.")))

                // Page Structure
                .addRow(row -> row.withChild(new Markdown(
                        """
                        ## Page Structure

                        Every page is built using `Page.builder()`:

                        ```java
                        Page.builder()
                            .addComponents(Header.H1("Title"))
                            .addRow(row -> row.withChild(...))
                            .build();
                        ```

                        Pages contain rows, rows contain columns, columns contain components.
                        """)))

                // Rows
                .addComponents(Header.H2("Rows"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Row** is a horizontal container using a 12-column grid:

                        ```java
                        .addRow(row -> row
                            .withChild(column1)
                            .withChild(column2)
                        );
                        ```

                        Or add components directly (equal-width columns):

                        ```java
                        .addRow(row -> row.withComponents(comp1, comp2, comp3));
                        ```
                        """)))

                // Columns
                .addComponents(Header.H2("Columns"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Column** divides rows into 12 equal parts:

                        ```java
                        new Column().withWidth(8)  // 8/12 = 66%
                        new Column().withWidth(4)  // 4/12 = 33%
                        ```

                        Without width specified, columns are equal-width.
                        """)))

                // Two-Column Layout
                .addComponents(Header.H2("Two-Column Layout (8-4)"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Classic main content + sidebar:

                        ```java
                        .addRow(row -> row
                            .withChild(new Column().withWidth(8)
                                .withChild(mainContent))
                            .withChild(new Column().withWidth(4)
                                .withChild(sidebar))
                        );
                        ```
                        """)))
                .addRow(row -> row
                        .withChild(new Column().withWidth(8).withChild(
                                Card.create()
                                        .withHeader("Main Content Area (8 columns)")
                                        .withBody("This takes up 8/12 = 66% of the width. Perfect for main content.")
                        ))
                        .withChild(new Column().withWidth(4).withChild(
                                Card.create()
                                        .withHeader("Sidebar (4 columns)")
                                        .withBody("This takes up 4/12 = 33% of the width. Great for navigation or supplementary info.")
                        ))
                )

                // Three Equal Columns
                .addComponents(Header.H2("Three Equal Columns"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        ```java
                        .addRow(row -> row.withComponents(
                            new Markdown("Column 1"),
                            new Markdown("Column 2"),
                            new Markdown("Column 3")
                        ));
                        ```
                        """)))
                .addRow(row -> row
                        .withComponents(
                                Card.create().withBody("Equal width column 1"),
                                Card.create().withBody("Equal width column 2"),
                                Card.create().withBody("Equal width column 3")
                        ))

                // Four Column Layout
                .addComponents(Header.H2("Four Column Layout (3-3-3-3)"))
                .addRow(row -> row.withComponents(
                        Card.create().withHeader("25%").withBody("3/12 columns"),
                        Card.create().withHeader("25%").withBody("3/12 columns"),
                        Card.create().withHeader("25%").withBody("3/12 columns"),
                        Card.create().withHeader("25%").withBody("3/12 columns")
                ))

                // Grid Layout
                .addComponents(Header.H2("CSS Grid Layout"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Grid** uses CSS Grid for more complex layouts:

                        ```java
                        Grid.create()
                            .withColumns(4)  // 4 columns
                            .addItem(card1)
                            .addItem(card2)
                            .addItem(card3);
                        ```

                        Items automatically wrap to new rows.
                        """)))
                .addRow(row -> {
                    Grid grid = Grid.create()
                            .withColumns(4)
                            .addItem(InfoBox.create().withIcon("1").withTitle("Grid Item"))
                            .addItem(InfoBox.create().withIcon("2").withTitle("Grid Item"))
                            .addItem(InfoBox.create().withIcon("3").withTitle("Grid Item"))
                            .addItem(InfoBox.create().withIcon("4").withTitle("Grid Item"))
                            .addItem(InfoBox.create().withIcon("5").withTitle("Grid Item"))
                            .addItem(InfoBox.create().withIcon("6").withTitle("Grid Item"))
                            .addItem(InfoBox.create().withIcon("7").withTitle("Grid Item"))
                            .addItem(InfoBox.create().withIcon("8").withTitle("Grid Item"));

                    row.withChild(grid);
                })

                // Container
                .addComponents(Header.H2("Container"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Container** constrains content width for readability:

                        ```java
                        Container container = new Container()
                            .withChild(Header.H1("Constrained Content"))
                            .withChild(paragraph);
                        ```
                        """)))

                // Section
                .addComponents(Header.H2("Section"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Section** creates semantic page divisions:

                        ```java
                        Section hero = new Section()
                            .withAttribute("class", "hero-section")
                            .withChild(Header.H1("Hero Title"));
                        ```
                        """)))

                .build();

        return page.render();
    }
}
