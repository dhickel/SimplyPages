package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.display.*;
import io.mindspice.simplypages.layout.*;
import io.mindspice.simplypages.modules.*;
import org.springframework.stereotype.Component;

/**
 * Components overview page - demonstrates all basic components.
 *
 * <p>Shows:</p>
 * <ul>
 *   <li>Header (H1-H6)</li>
 *   <li>Paragraph</li>
 *   <li>Div</li>
 *   <li>Markdown</li>
 *   <li>Image</li>
 *   <li>Lists (OrderedList, UnorderedList)</li>
 * </ul>
 */
@Component
public class ComponentsPage implements DemoPage {

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1("Basic Components"))
                .addRow(row -> row.withChild(Alert.info(
                        "Components are the fundamental building blocks of JHF. " +
                        "Each component represents an HTML element and can be composed with others.")))

                // Headers Section
                .addComponents(Header.H2("Headers"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Headers are created using `Header.H1()` through `Header.H6()`:

                        ```java
                        Header.H1("Main Title");
                        Header.H2("Section Title");
                        Header.H3("Subsection");
                        ```
                        """)))
                .addRow(row -> {
                    Div headers = new Div();
                    headers.withChild(Header.H1("This is H1"));
                    headers.withChild(Header.H2("This is H2"));
                    headers.withChild(Header.H3("This is H3"));
                    headers.withChild(Header.H4("This is H4"));
                    headers.withChild(Header.H5("This is H5"));
                    headers.withChild(Header.H6("This is H6"));
                    row.withChild(headers);
                })

                // Paragraphs and Div
                .addComponents(Header.H2("Paragraphs and Containers"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Paragraph** - For text content:
                        ```java
                        new Paragraph().withInnerText("Your text here")
                        ```

                        **Div** - Generic container for grouping:
                        ```java
                        new Div()
                            .withAttribute("class", "custom-class")
                            .withChild(someComponent)
                        ```
                        """)))
                .addRow(row -> {
                    Div example = new Div()
                            .withAttribute("class", "p-medium")
                            .withChild(new Paragraph().withInnerText(
                                    "This is a paragraph inside a div container. " +
                                    "Paragraphs are perfect for body text."))
                            .withChild(new Paragraph().withInnerText(
                                    "You can add multiple paragraphs, each will be its own <p> tag."));
                    row.withChild(example);
                })

                // Lists
                .addComponents(Header.H2("Lists"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Unordered Lists** (bullets):
                        ```java
                        UnorderedList.create()
                            .addItem("First item")
                            .addItem("Second item")
                        ```

                        **Ordered Lists** (numbered):
                        ```java
                        OrderedList.create()
                            .addItem("Step 1")
                            .addItem("Step 2")
                        ```
                        """)))
                .addRow(row -> row
                        .withChild(new Column().withWidth(6).withChild(
                                ContentModule.create()
                                        .withTitle("Unordered List")
                                        .withCustomContent(
                                                UnorderedList.create()
                                                        .addItem("Cannabis Sativa")
                                                        .addItem("Cannabis Indica")
                                                        .addItem("Cannabis Ruderalis")
                                                        .addItem("Hybrid Strains")
                                        )
                        ))
                        .withChild(new Column().withWidth(6).withChild(
                                ContentModule.create()
                                        .withTitle("Ordered List")
                                        .withCustomContent(
                                                OrderedList.create()
                                                        .addItem("Germination (1-7 days)")
                                                        .addItem("Seedling (2-3 weeks)")
                                                        .addItem("Vegetative (3-16 weeks)")
                                                        .addItem("Flowering (8-11 weeks)")
                                                        .addItem("Harvest")
                                        )
                        ))
                )

                // Markdown
                .addComponents(Header.H2("Markdown Component"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The **Markdown** component renders GitHub-Flavored Markdown to HTML:

                        ```java
                        new Markdown(\"\"\"
                            # Title
                            This is **bold** and *italic*
                            * Bullet point
                            [Link](https://example.com)
                        \"\"\")
                        ```

                        Supports: headers, lists, **bold**, *italic*, `code`, links, and tables!

                        | Feature | Supported |
                        |---------|-----------|
                        | Headers | ✅ |
                        | Lists | ✅ |
                        | Tables | ✅ |
                        | Code | ✅ |
                        """)))

                // Image
                .addComponents(Header.H2("Images"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Images use the `Image` component:

                        ```java
                        Image.create()
                            .withSrc("/path/to/image.jpg")
                            .withAlt("Description")
                            .withWidth(400)
                        ```
                        """)))
                .addRow(row -> row.withChild(
                        new Image("https://picsum.photos/800/300", "Sample image")
                                .withAttribute("class", "img-fluid")
                ))

                .build();

        return page.render();
    }
}
