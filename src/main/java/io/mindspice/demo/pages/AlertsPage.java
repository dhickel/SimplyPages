package io.mindspice.demo.pages;

import io.mindspice.jhf.components.*;
import io.mindspice.jhf.components.display.*;
import io.mindspice.jhf.layout.*;
import io.mindspice.jhf.modules.*;
import org.springframework.stereotype.Component;

/**
 * Alerts, badges, and tags page.
 */
@Component
public class AlertsPage implements DemoPage {

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1("Alerts, Badges & Tags"))

                .addRow(row -> row.withChild(new Markdown(
                        """
                        Components for displaying notifications, labels, and status indicators.

                        ## Alerts

                        ```java
                        Alert.info("Informational message");
                        Alert.success("Success message");
                        Alert.warning("Warning message");
                        Alert.danger("Error message");
                        ```
                        """)))

                .addRow(row -> {
                    Div alertsDiv = new Div();
                    alertsDiv.withChild(Alert.info("This is an informational alert. Use for general notifications."));
                    alertsDiv.withChild(Alert.success("Success! Your changes have been saved successfully."));
                    alertsDiv.withChild(Alert.warning("Warning: Please review your settings before continuing."));
                    alertsDiv.withChild(Alert.danger("Error: Unable to process your request. Please try again."));

                    row.withChild(ContentModule.create()
                            .withTitle("Alert Styles")
                            .withCustomContent(alertsDiv));
                })

                // Badges
                .addComponents(Header.H2("Badges"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Badge** components highlight status or counts:

                        ```java
                        Badge.primary("Primary");
                        Badge.secondary("Secondary");
                        Badge.success("Success");
                        Badge.danger("Danger");
                        Badge.warning("Warning");
                        Badge.info("Info");
                        ```
                        """)))

                .addRow(row -> {
                    Div badgesDiv = new Div();
                    Paragraph p = new Paragraph("Status Badges: ");
                    p.withChild(Badge.primary("Primary"));
                    p.withChild(Badge.secondary("Secondary"));
                    p.withChild(Badge.success("Active"));
                    p.withChild(Badge.danger("Critical"));
                    p.withChild(Badge.warning("Pending"));
                    p.withChild(Badge.info("Info"));
                    badgesDiv.withChild(p);

                    Paragraph p2 = new Paragraph();
                    p2.withChild(new TextNode("Notification Badges: "));
                    p2.withChild(Badge.danger("5"));
                    p2.withChild(new TextNode(" new messages "));
                    p2.withChild(Badge.warning("12"));
                    p2.withChild(new TextNode(" pending approvals "));
                    p2.withChild(Badge.success("✓"));
                    p2.withChild(new TextNode(" verified"));
                    badgesDiv.withChild(p2);

                    row.withChild(ContentModule.create()
                            .withTitle("Badge Examples")
                            .withCustomContent(badgesDiv));
                })

                // Tags
                .addComponents(Header.H2("Tags"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Tag** components for categorization and labeling:

                        ```java
                        Tag.create("Label Text");
                        ```

                        Use tags to categorize content, show strain types, or filter options.
                        """)))

                .addRow(row -> {
                    Div tagsDiv = new Div();

                    Paragraph strainTypes = new Paragraph("Strain Types: ");
                    strainTypes.withChild(Tag.create("Indica"));
                    strainTypes.withChild(Tag.create("Sativa"));
                    strainTypes.withChild(Tag.create("Hybrid"));
                    tagsDiv.withChild(strainTypes);

                    Paragraph characteristics = new Paragraph("Characteristics: ");
                    characteristics.withChild(Tag.create("High THC"));
                    characteristics.withChild(Tag.create("CBD Rich"));
                    characteristics.withChild(Tag.create("Autoflower"));
                    characteristics.withChild(Tag.create("Feminized"));
                    tagsDiv.withChild(characteristics);

                    Paragraph growMethods = new Paragraph("Growing Methods: ");
                    growMethods.withChild(Tag.create("Indoor"));
                    growMethods.withChild(Tag.create("Outdoor"));
                    growMethods.withChild(Tag.create("Greenhouse"));
                    growMethods.withChild(Tag.create("Hydroponic"));
                    growMethods.withChild(Tag.create("Organic"));
                    tagsDiv.withChild(growMethods);

                    row.withChild(ContentModule.create()
                            .withTitle("Tag Examples")
                            .withCustomContent(tagsDiv));
                })

                .build();

        return page.render();
    }
}
