package io.mindspice.demo.pages;

import io.mindspice.jhf.components.*;
import io.mindspice.jhf.components.display.*;
import io.mindspice.jhf.layout.*;
import io.mindspice.jhf.modules.*;
import org.springframework.stereotype.Component;

/**
 * Module Layouts page - demonstrates different ways to arrange modules horizontally.
 */
@Component
public class ModuleLayoutsPage implements DemoPage {

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1("Module Layout Patterns"))

                .addRow(row -> row.withChild(Alert.info(
                        "This page demonstrates different ways to arrange modules horizontally using the Row and Column system. " +
                        "All examples use ContentModule with text to show layout patterns.")))

                // Single Full-Width Module
                .addComponents(Header.H2("1. Single Full-Width Module"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        A module that spans the entire width:

                        ```java
                        .addRow(row -> row.withChild(
                            ContentModule.create()
                                .withTitle("Full Width")
                                .withContent("Content spans 100% width")
                        ));
                        ```
                        """)))
                .addRow(row -> row.withChild(
                        ContentModule.create()
                                .withTitle("Full Width Module")
                                .withContent("This module takes up the entire width of the content area. " +
                                        "Perfect for hero sections, full-width announcements, or standalone content.")
                ))

                // Two Modules Side-by-Side (50/50)
                .addComponents(Header.H2("2. Two Equal Modules (50/50)"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Two modules of equal width:

                        ```java
                        .addRow(row -> row
                            .withChild(ContentModule.create()
                                .withTitle("Left Module")
                                .withContent("..."))
                            .withChild(ContentModule.create()
                                .withTitle("Right Module")
                                .withContent("..."))
                        );
                        ```

                        Without specifying column widths, children automatically get equal space.
                        """)))
                .addRow(row -> row
                        .withChild(
                                ContentModule.create()
                                        .withTitle("Left Module")
                                        .withContent("This module takes up 50% of the width. Great for comparisons, " +
                                                "side-by-side content, or two equally important pieces of information.")
                        )
                        .withChild(
                                ContentModule.create()
                                        .withTitle("Right Module")
                                        .withContent("This module also takes up 50% of the width. The layout automatically " +
                                                "divides the available space equally between all direct children.")
                        )
                )

                // Two Modules (66/33) - Main + Sidebar
                .addComponents(Header.H2("3. Two Modules (66/33) - Main + Sidebar"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Asymmetric layout with main content and sidebar:

                        ```java
                        .addRow(row -> row
                            .withChild(new Column().withWidth(8).withChild(
                                ContentModule.create()
                                    .withTitle("Main Content")
                                    .withContent("...")
                            ))
                            .withChild(new Column().withWidth(4).withChild(
                                ContentModule.create()
                                    .withTitle("Sidebar")
                                    .withContent("...")
                            ))
                        );
                        ```

                        8 columns (66%) + 4 columns (33%) = 12 columns total
                        """)))
                .addRow(row -> row
                        .withChild(new Column().withWidth(8).withChild(
                                ContentModule.create()
                                        .withTitle("Main Content (8 columns)")
                                        .withContent("This is the primary content area taking up 8 out of 12 columns (66%). " +
                                                "Use this for article text, main features, or primary information.")
                        ))
                        .withChild(new Column().withWidth(4).withChild(
                                ContentModule.create()
                                        .withTitle("Sidebar (4 columns)")
                                        .withContent("This sidebar takes up 4 out of 12 columns (33%). " +
                                                "Perfect for navigation, related links, or supplementary information.")
                        ))
                )

                // Two Modules (33/66) - Reversed Layout
                .addComponents(Header.H2("4. Two Modules (33/66) - Sidebar First"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Reversed layout with sidebar on the left:

                        ```java
                        .addRow(row -> row
                            .withChild(new Column().withWidth(4).withChild(
                                ContentModule.create().withTitle("Left Sidebar")
                            ))
                            .withChild(new Column().withWidth(8).withChild(
                                ContentModule.create().withTitle("Main Content")
                            ))
                        );
                        ```
                        """)))
                .addRow(row -> row
                        .withChild(new Column().withWidth(4).withChild(
                                ContentModule.create()
                                        .withTitle("Left Sidebar (4 columns)")
                                        .withContent("Sidebar on the left can be useful for navigation menus, " +
                                                "table of contents, or filters.")
                        ))
                        .withChild(new Column().withWidth(8).withChild(
                                ContentModule.create()
                                        .withTitle("Main Content (8 columns)")
                                        .withContent("Main content on the right. This layout is common in documentation " +
                                                "sites and dashboards with left navigation.")
                        ))
                )

                // Three Equal Modules (33/33/33)
                .addComponents(Header.H2("5. Three Equal Modules (33/33/33)"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Three modules of equal width:

                        ```java
                        .addRow(row -> row
                            .withChild(ContentModule.create().withTitle("Left"))
                            .withChild(ContentModule.create().withTitle("Center"))
                            .withChild(ContentModule.create().withTitle("Right"))
                        );
                        ```

                        Or using explicit column widths (4+4+4=12):

                        ```java
                        .addRow(row -> row
                            .withChild(new Column().withWidth(4).withChild(module1))
                            .withChild(new Column().withWidth(4).withChild(module2))
                            .withChild(new Column().withWidth(4).withChild(module3))
                        );
                        ```
                        """)))
                .addRow(row -> row
                        .withChild(
                                ContentModule.create()
                                        .withTitle("Left Module")
                                        .withContent("First of three equal modules. Perfect for feature highlights, " +
                                                "service offerings, or team member profiles.")
                        )
                        .withChild(
                                ContentModule.create()
                                        .withTitle("Center Module")
                                        .withContent("Second module with equal width. The three-column layout is very " +
                                                "common in modern web design for showcasing features or benefits.")
                        )
                        .withChild(
                                ContentModule.create()
                                        .withTitle("Right Module")
                                        .withContent("Third and final module. Each takes up 33% of the available width, " +
                                                "creating a balanced, symmetric layout.")
                        )
                )

                // Four Modules (25/25/25/25)
                .addComponents(Header.H2("6. Four Equal Modules (25/25/25/25)"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Four modules in a row:

                        ```java
                        .addRow(row -> row
                            .withChild(new Column().withWidth(3).withChild(module1))
                            .withChild(new Column().withWidth(3).withChild(module2))
                            .withChild(new Column().withWidth(3).withChild(module3))
                            .withChild(new Column().withWidth(3).withChild(module4))
                        );
                        ```

                        Each module is 3 columns wide (3+3+3+3=12)
                        """)))
                .addRow(row -> row
                        .withChild(new Column().withWidth(3).withChild(
                                ContentModule.create()
                                        .withTitle("Module 1")
                                        .withContent("25% width. Great for statistics, metrics, or small feature cards.")
                        ))
                        .withChild(new Column().withWidth(3).withChild(
                                ContentModule.create()
                                        .withTitle("Module 2")
                                        .withContent("25% width. Four-column layouts work well for dashboards and data displays.")
                        ))
                        .withChild(new Column().withWidth(3).withChild(
                                ContentModule.create()
                                        .withTitle("Module 3")
                                        .withContent("25% width. Each module gets equal visual weight and importance.")
                        ))
                        .withChild(new Column().withWidth(3).withChild(
                                ContentModule.create()
                                        .withTitle("Module 4")
                                        .withContent("25% width. Note: On mobile, these will stack vertically for readability.")
                        ))
                )

                // Custom Widths (25/50/25)
                .addComponents(Header.H2("7. Custom Widths (25/50/25)"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Emphasize the center module:

                        ```java
                        .addRow(row -> row
                            .withChild(new Column().withWidth(3).withChild(leftModule))
                            .withChild(new Column().withWidth(6).withChild(centerModule))
                            .withChild(new Column().withWidth(3).withChild(rightModule))
                        );
                        ```

                        3 + 6 + 3 = 12 columns
                        """)))
                .addRow(row -> row
                        .withChild(new Column().withWidth(3).withChild(
                                ContentModule.create()
                                        .withTitle("Left (25%)")
                                        .withContent("Smaller supporting content on the left.")
                        ))
                        .withChild(new Column().withWidth(6).withChild(
                                ContentModule.create()
                                        .withTitle("Center (50%)")
                                        .withContent("The center module is emphasized by being twice as wide as the side modules. " +
                                                "This draws attention to the primary content while keeping context on the sides.")
                        ))
                        .withChild(new Column().withWidth(3).withChild(
                                ContentModule.create()
                                        .withTitle("Right (25%)")
                                        .withContent("Smaller supporting content on the right.")
                        ))
                )

                // Asymmetric Layout (20/60/20)
                .addComponents(Header.H2("8. Asymmetric Layout (20/60/20)"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Create visual hierarchy with varying widths:

                        ```java
                        .addRow(row -> row
                            .withChild(new Column().withWidth(2).withChild(leftModule))
                            .withChild(new Column().withWidth(8).withChild(centerModule))
                            .withChild(new Column().withWidth(2).withChild(rightModule))
                        );
                        ```

                        Note: 2 + 8 + 2 = 12 columns. The center is 4x wider than the sides!
                        """)))
                .addRow(row -> row
                        .withChild(new Column().withWidth(2).withChild(
                                ContentModule.create()
                                        .withTitle("Narrow")
                                        .withContent("Very narrow side column (16%).")
                        ))
                        .withChild(new Column().withWidth(8).withChild(
                                ContentModule.create()
                                        .withTitle("Wide Center (66%)")
                                        .withContent("The dominant central module that takes up most of the space. " +
                                                "This creates a strong focal point while the narrow side columns provide context or metadata.")
                        ))
                        .withChild(new Column().withWidth(2).withChild(
                                ContentModule.create()
                                        .withTitle("Narrow")
                                        .withContent("Very narrow side column (16%).")
                        ))
                )

                // Multiple Rows
                .addComponents(Header.H2("9. Multiple Rows with Different Layouts"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Combine different layouts on the same page:

                        ```java
                        Page.builder()
                            // First row: full width
                            .addRow(row -> row.withChild(fullWidthModule))

                            // Second row: two columns
                            .addRow(row -> row
                                .withChild(module1)
                                .withChild(module2))

                            // Third row: three columns
                            .addRow(row -> row
                                .withChild(module3)
                                .withChild(module4)
                                .withChild(module5))

                            .build();
                        ```
                        """)))
                .addRow(row -> row.withChild(
                        ContentModule.create()
                                .withTitle("Row 1: Full Width")
                                .withContent("This row contains a single full-width module.")
                ))
                .addRow(row -> row
                        .withChild(ContentModule.create()
                                .withTitle("Row 2: Left (50%)")
                                .withContent("First of two equal modules."))
                        .withChild(ContentModule.create()
                                .withTitle("Row 2: Right (50%)")
                                .withContent("Second of two equal modules."))
                )
                .addRow(row -> row
                        .withChild(ContentModule.create()
                                .withTitle("Row 3: Module A")
                                .withContent("First of three."))
                        .withChild(ContentModule.create()
                                .withTitle("Row 3: Module B")
                                .withContent("Second of three."))
                        .withChild(ContentModule.create()
                                .withTitle("Row 3: Module C")
                                .withContent("Third of three."))
                )

                // Best Practices
                .addComponents(Header.H2("Best Practices"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        ### Column Width Guidelines

                        - **Always sum to 12:** Column widths must add up to 12 (the grid total)
                        - **Common patterns:**
                          - 12 (full width)
                          - 6 + 6 (50/50)
                          - 8 + 4 (main + sidebar)
                          - 4 + 4 + 4 (three equal)
                          - 3 + 3 + 3 + 3 (four equal)
                          - 3 + 6 + 3 (emphasized center)

                        ### Responsive Behavior

                        - On mobile (< 480px), all columns stack vertically automatically
                        - Modules maintain their padding and margins on all screen sizes
                        - Test layouts on mobile to ensure readability

                        ### Visual Balance

                        - Use equal widths for items of equal importance
                        - Emphasize primary content with wider columns
                        - Keep narrow columns (1-2 width) for icons, numbers, or minimal content
                        - Maintain consistent gaps between modules

                        ### Performance

                        - Multiple modules on one row still load efficiently
                        - Each module is independently rendered server-side
                        - HTMX can refresh individual modules without affecting neighbors
                        """)))

                .build();

        return page.render();
    }
}
