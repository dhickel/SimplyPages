package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.display.*;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.layout.Row;
import org.springframework.stereotype.Component;

/**
 * Demo page showcasing newly added components.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>Spacer - Vertical and horizontal spacing</li>
 *   <li>Divider - Visual separators</li>
 *   <li>Spinner - Loading indicators</li>
 *   <li>ProgressBar - Progress indicators</li>
 *   <li>Blockquote - Quoted text</li>
 *   <li>Code - Code snippets</li>
 *   <li>Icon - Icon wrappers</li>
 *   <li>Enhanced Header - Alignment and bars</li>
 *   <li>Enhanced Paragraph - Alignment</li>
 * </ul>
 */
@Component
public class NewComponentsPage implements DemoPage {

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1("New Components").center())

                // Introduction
                .addRow(row -> row.withChild(new Markdown(
                        """
                        # Recently Added Components

                        This page showcases all the components that were recently added to the framework
                        to bring it to feature parity with major web frameworks.
                        """
                )))

                // Spacer Component
                .addRow(row -> row.withChild(Header.H2("Spacer Component").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The **Spacer** component creates vertical or horizontal spacing between elements.

                        **Usage:**
                        ```java
                        Spacer.vertical().small();    // 16px
                        Spacer.vertical().medium();   // 32px (default)
                        Spacer.vertical().large();    // 48px
                        Spacer.vertical().extraLarge(); // 64px
                        Spacer.vertical().custom("100px");
                        ```
                        """
                )))
                .addRow(row -> {
                    Div demo = new Div().withClass("demo-box");
                    demo.withChild(new Paragraph("First element"));
                    demo.withChild(Spacer.vertical().large());
                    demo.withChild(new Paragraph("Second element (48px gap above)"));
                    row.withChild(demo);
                })

                .addComponents(Spacer.vertical().large())

                // Divider Component
                .addRow(row -> row.withChild(Header.H2("Divider Component").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The **Divider** component creates horizontal or vertical visual separators.

                        **Usage:**
                        ```java
                        Divider.horizontal();           // Basic divider
                        Divider.horizontal().thick();   // Thicker line
                        Divider.horizontal().dashed();  // Dashed style
                        Divider.horizontal().withColor("#cccccc");
                        Divider.horizontal().withText("OR"); // Divider with text
                        ```
                        """
                )))
                .addRow(row -> {
                    Div demo = new Div().withClass("demo-box");
                    demo.withChild(new Paragraph("Content above divider"));
                    demo.withChild(Divider.horizontal());
                    demo.withChild(new Paragraph("Content below divider"));
                    demo.withChild(Spacer.vertical().medium());
                    demo.withChild(Divider.horizontal().thick());
                    demo.withChild(Spacer.vertical().medium());
                    demo.withChild(Divider.horizontal().dashed());
                    row.withChild(demo);
                })

                .addComponents(Spacer.vertical().large())

                // Spinner Component
                .addRow(row -> row.withChild(Header.H2("Spinner Component").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The **Spinner** component displays loading indicators.

                        **Usage:**
                        ```java
                        Spinner.create();                    // Medium spinner
                        Spinner.create().small();            // Small spinner
                        Spinner.create().large();            // Large spinner
                        Spinner.create().withMessage("Loading data...");
                        Spinner.create().withColor("primary");
                        ```
                        """
                )))
                .addRow(row -> {
                    Row spinnerRow = new Row();
                    spinnerRow.withChild(Spinner.create().small());
                    spinnerRow.withChild(Spinner.create().medium());
                    spinnerRow.withChild(Spinner.create().large());
                    spinnerRow.withChild(Spinner.create().withMessage("Loading..."));
                    row.withChild(spinnerRow);
                })

                .addComponents(Spacer.vertical().large())

                // ProgressBar Component
                .addRow(row -> row.withChild(Header.H2("ProgressBar Component").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The **ProgressBar** component shows completion progress.

                        **Usage:**
                        ```java
                        ProgressBar.create(75);              // 75% complete
                        ProgressBar.create(60).withLabel("60% Complete");
                        ProgressBar.create(90).success();    // Green color
                        ProgressBar.create(45).warning();    // Yellow color
                        ProgressBar.create(25).error();      // Red color
                        ProgressBar.create(50).striped().animated();
                        ```
                        """
                )))
                .addRow(row -> {
                    Div demo = new Div().withClass("demo-box");
                    demo.withChild(new Paragraph("Default progress:"));
                    demo.withChild(ProgressBar.create(75));
                    demo.withChild(Spacer.vertical().small());

                    demo.withChild(new Paragraph("Success (with label):"));
                    demo.withChild(ProgressBar.create(90).success().withLabel("90%"));
                    demo.withChild(Spacer.vertical().small());

                    demo.withChild(new Paragraph("Warning:"));
                    demo.withChild(ProgressBar.create(60).warning());
                    demo.withChild(Spacer.vertical().small());

                    demo.withChild(new Paragraph("Error:"));
                    demo.withChild(ProgressBar.create(30).error());
                    demo.withChild(Spacer.vertical().small());

                    demo.withChild(new Paragraph("Striped & Animated:"));
                    demo.withChild(ProgressBar.create(50).striped().animated());

                    row.withChild(demo);
                })

                .addComponents(Spacer.vertical().large())

                // Blockquote Component
                .addRow(row -> row.withChild(Header.H2("Blockquote Component").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The **Blockquote** component displays quoted text with citations.

                        **Usage:**
                        ```java
                        Blockquote.create("Quote text here");
                        Blockquote.create("Quote text")
                            .withCitation("Author Name")
                            .withSource("Publication, 2024");
                        ```
                        """
                )))
                .addRow(row -> {
                    Div demo = new Div().withClass("demo-box");
                    demo.withChild(
                            Blockquote.create("The best way to predict the future is to invent it.")
                                    .withCitation("Alan Kay")
                    );
                    demo.withChild(Spacer.vertical().medium());
                    demo.withChild(
                            Blockquote.create("Collaborative research yields better results and more reliable data.")
                                    .withCitation("Dr. Jane Smith")
                                    .withSource("Journal of Cannabis Research, 2024")
                    );
                    row.withChild(demo);
                })

                .addComponents(Spacer.vertical().large())

                // Code Component
                .addRow(row -> row.withChild(Header.H2("Code Component").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The **Code** component displays inline code or code blocks.

                        **Usage:**
                        ```java
                        Code.inline("System.out.println()");
                        Code.block(codeString);
                        Code.block(codeString).withLanguage("java");
                        Code.block(codeString).withLanguage("java").withTitle("Example.java");
                        ```
                        """
                )))
                .addRow(row -> {
                    Div demo = new Div().withClass("demo-box");
                    demo.withChild(new Paragraph("Inline code example: ").withInnerText("Use "));
                    demo.withChild(Code.inline("System.out.println()"));
                    demo.withChild(new Paragraph(" to print."));

                    demo.withChild(Spacer.vertical().medium());
                    demo.withChild(new Paragraph("Code block:"));
                    demo.withChild(Code.block(
                            """
                            public static void main(String[] args) {
                                System.out.println("Hello, World!");
                            }
                            """
                    ).withLanguage("java"));

                    row.withChild(demo);
                })

                .addComponents(Spacer.vertical().large())

                // Icon Component
                .addRow(row -> row.withChild(Header.H2("Icon Component").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The **Icon** component provides wrappers for icon libraries.

                        **Usage:**
                        ```java
                        Icon.fontAwesome("user");
                        Icon.fontAwesome("check").large();
                        Icon.material("settings");
                        Icon.bootstrap("heart-fill");
                        Icon.fontAwesome("info-circle").withAriaLabel("Information");
                        ```

                        **Note:** This demo uses emoji placeholders. In production, include your icon library CSS.
                        """
                )))
                .addRow(row -> {
                    Div demo = new Div().withClass("demo-box");
                    demo.withChild(new Paragraph("Icon sizes: "));
                    demo.withChild(new Paragraph("👤 (small) 👤 (medium) 👤 (large) 👤 (xl)"));
                    row.withChild(demo);
                })

                .addComponents(Spacer.vertical().large())

                // Enhanced Header Component
                .addRow(row -> row.withChild(Header.H2("Enhanced Header Component").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Headers** now support alignment and top/bottom bars.

                        **Usage:**
                        ```java
                        Header.H1("Title").center();
                        Header.H2("Section").withBottomBar();
                        Header.H1("Important").withTopBar().withBottomBar();
                        Header.H2("Custom").withTopBar(Divider.horizontal().thick());
                        ```
                        """
                )))
                .addRow(row -> {
                    Div demo = new Div().withClass("demo-box");
                    demo.withChild(Header.H3("Left Aligned (Default)").left());
                    demo.withChild(Header.H3("Center Aligned").center());
                    demo.withChild(Header.H3("Right Aligned").right());
                    demo.withChild(Spacer.vertical().medium());
                    demo.withChild(Header.H3("With Bottom Bar").withBottomBar());
                    demo.withChild(Spacer.vertical().medium());
                    demo.withChild(Header.H3("With Top and Bottom Bars").withTopBar().withBottomBar());
                    row.withChild(demo);
                })

                .addComponents(Spacer.vertical().large())

                // Enhanced Paragraph Component
                .addRow(row -> row.withChild(Header.H2("Enhanced Paragraph Component").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Paragraphs** now support alignment options.

                        **Usage:**
                        ```java
                        new Paragraph("Text").left();
                        new Paragraph("Text").center();
                        new Paragraph("Text").right();
                        new Paragraph("Text").justify();
                        ```
                        """
                )))
                .addRow(row -> {
                    Div demo = new Div().withClass("demo-box");
                    demo.withChild(new Paragraph("This paragraph is left-aligned (default).").left());
                    demo.withChild(new Paragraph("This paragraph is center-aligned.").center());
                    demo.withChild(new Paragraph("This paragraph is right-aligned.").right());
                    demo.withChild(new Paragraph("This paragraph is justified with enough text to demonstrate justification across multiple lines. The text should spread evenly across the width.").justify());
                    row.withChild(demo);
                })

                .build();

        return page.render();
    }
}
