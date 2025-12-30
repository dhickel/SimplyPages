package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.display.*;
import io.mindspice.simplypages.layout.*;
import io.mindspice.simplypages.modules.*;
import org.springframework.stereotype.Component;

/**
 * Cards and info boxes page.
 */
@Component
public class CardsPage implements DemoPage {

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1("Cards & Data Display"))

                .addRow(row -> row.withChild(new Markdown(
                        """
                        Cards group related content and actions in a flexible container.

                        ## Card Component

                        ```java
                        Card.create()
                            .withHeader("Title")
                            .withBody("Main content here")
                            .withFooter("Footer text");
                        ```
                        """)))

                // Single Card
                .addRow(row -> {
                    Card exampleCard = Card.create()
                            .withHeader("Welcome Card")
                            .withBody("This is the main content of the card. Cards are perfect for grouping related information.")
                            .withFooter("Card footer - additional info or actions");

                    row.withChild(new Column().withWidth(6).withChild(exampleCard));
                })

                // Card Grid
                .addComponents(Header.H2("Card Grid"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **CardGrid** arranges multiple cards in a responsive grid:

                        ```java
                        CardGrid.create()
                            .withColumns(3)
                            .addCard(Card.create()
                                .withHeader("Card 1")
                                .withBody("Content"))
                            .addCard(Card.create()
                                .withHeader("Card 2")
                                .withBody("Content"));
                        ```
                        """)))

                .addRow(row -> {
                    CardGrid grid = CardGrid.create()
                            .withColumns(3)
                            .addCard(Card.create()
                                    .withHeader("Product Database")
                                    .withBody("Browse our comprehensive database of products with detailed information on specifications, properties, and processing methods.")
                                    .withFooter("1,234 products available"))
                            .addCard(Card.create()
                                    .withHeader("Data Journals")
                                    .withBody("Document your research journey and learn from others. Share data, track progress, and get community feedback.")
                                    .withFooter("567 active journals"))
                            .addCard(Card.create()
                                    .withHeader("Research Papers")
                                    .withBody("Access peer-reviewed research on scientific methods, data collection techniques, and practical applications.")
                                    .withFooter("89 papers published"))
                            .addCard(Card.create()
                                    .withHeader("Community Forums")
                                    .withBody("Join discussions with researchers worldwide. Ask questions, share experiences, and learn.")
                                    .withFooter("12,345 members"))
                            .addCard(Card.create()
                                    .withHeader("Research Guides")
                                    .withBody("Step-by-step guides for data collection, from planning to completion. Includes beginner and advanced techniques.")
                                    .withFooter("45 guides available"))
                            .addCard(Card.create()
                                    .withHeader("Data Analysis")
                                    .withBody("Visualize and analyze research data. Track results, optimize conditions, and improve outcomes over time.")
                                    .withFooter("Coming soon"));

                    row.withChild(grid);
                })

                // InfoBox
                .addComponents(Header.H2("Info Boxes"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **InfoBox** displays statistics and key metrics:

                        ```java
                        InfoBox.create()
                            .withIcon("📊")
                            .withTitle("Total Users")
                            .withValue("12,345");
                        ```
                        """)))

                .addRow(row -> {
                    Row infoBoxRow = new Row()
                            .withComponents(
                                    InfoBox.create()
                                            .withIcon("👥")
                                            .withTitle("Total Users")
                                            .withValue("12,345"),
                                    InfoBox.create()
                                            .withIcon("📊")
                                            .withTitle("Active Projects")
                                            .withValue("3,456"),
                                    InfoBox.create()
                                            .withIcon("💬")
                                            .withTitle("Forum Posts")
                                            .withValue("98,765"),
                                    InfoBox.create()
                                            .withIcon("📚")
                                            .withTitle("Research Papers")
                                            .withValue("1,234")
                            );

                    row.withChild(ContentModule.create()
                            .withTitle("Platform Statistics")
                            .withCustomContent(infoBoxRow));
                })

                .build();

        return page.render();
    }
}
