package io.mindspice.demo.pages;

import io.mindspice.jhf.components.*;
import io.mindspice.jhf.layout.*;
import org.springframework.stereotype.Component;

/**
 * Custom components page - shows how to extend the framework.
 */
@Component
public class CustomPage implements DemoPage {

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1("Creating Custom Components & Modules"))

                .addRow(row -> row.withChild(new Markdown(
                        """
                        ## Extending JHF

                        JHF is designed to be extended. Create custom components and modules to fit your
                        specific needs while maintaining the framework's patterns.

                        ## Custom Component

                        Extend `HtmlTag` for new HTML elements:

                        ```java
                        public class ProgressBar extends HtmlTag {
                            private int percentage;

                            public ProgressBar() {
                                super("div");
                                this.withAttribute("class", "progress-bar");
                            }

                            public static ProgressBar create() {
                                return new ProgressBar();
                            }

                            public ProgressBar withPercentage(int percentage) {
                                this.percentage = percentage;
                                this.withAttribute("style",
                                    "width: " + percentage + "%");
                                return this;
                            }

                            @Override
                            public String render() {
                                this.withInnerText(percentage + "%");
                                return super.render();
                            }
                        }

                        // Usage:
                        ProgressBar progress = ProgressBar.create()
                            .withPercentage(75);
                        ```

                        ## Custom Module

                        Extend `Module` for complex compositions:

                        ```java
                        public class StrainCardModule extends Module {
                            private String strainName;
                            private String type;
                            private double thcPercent;
                            private String imageUrl;

                            public StrainCardModule() {
                                super("div");
                                this.withClass("strain-card-module");
                            }

                            public static StrainCardModule create() {
                                return new StrainCardModule();
                            }

                            public StrainCardModule withStrain(String name,
                                                                String type,
                                                                double thc,
                                                                String imgUrl) {
                                this.strainName = name;
                                this.type = type;
                                this.thcPercent = thc;
                                this.imageUrl = imgUrl;
                                return this;
                            }

                            @Override
                            protected void buildContent() {
                                Card card = Card.create()
                                    .withHeader(strainName)
                                    .withBody(new Div()
                                        .withChild(Image.create()
                                            .withSrc(imageUrl)
                                            .withWidth(200))
                                        .withChild(new Paragraph()
                                            .withInnerText("Type: " + type))
                                        .withChild(new Paragraph()
                                            .withInnerText("THC: " + thcPercent + "%"))
                                    )
                                    .withFooter(Button.create("View Details")
                                        .withStyle(Button.ButtonStyle.PRIMARY));

                                super.withChild(card);
                            }
                        }

                        // Usage:
                        StrainCardModule strain = StrainCardModule.create()
                            .withTitle("Featured Strain")
                            .withStrain("Blue Dream", "Hybrid", 21.5, "/img/blue-dream.jpg");
                        ```

                        ## Extension via Interfaces

                        Add cross-cutting concerns:

                        ```java
                        public interface Editable {
                            String getEditUrl();
                            Component renderEditButton();
                        }

                        public class EditableContentModule extends ContentModule
                                                            implements Editable {
                            private String editUrl;

                            public EditableContentModule withEditUrl(String url) {
                                this.editUrl = url;
                                return this;
                            }

                            @Override
                            public String getEditUrl() {
                                return editUrl;
                            }

                            @Override
                            public Component renderEditButton() {
                                return Button.create("Edit")
                                    .withAttribute("hx-get", editUrl)
                                    .withAttribute("hx-target", "#edit-modal");
                            }

                            @Override
                            protected void buildContent() {
                                super.buildContent();

                                // Add edit button if URL provided
                                if (editUrl != null) {
                                    super.withChild(renderEditButton());
                                }
                            }
                        }
                        ```

                        ## Builder Pattern Utilities

                        Create builders for common patterns:

                        ```java
                        public class DashboardPageBuilder {
                            private final Page.PageBuilder pageBuilder = Page.builder();
                            private String title;
                            private List<InfoBox> stats = new ArrayList<>();
                            private Component mainContent;

                            public DashboardPageBuilder withTitle(String title) {
                                this.title = title;
                                return this;
                            }

                            public DashboardPageBuilder addStat(InfoBox stat) {
                                stats.add(stat);
                                return this;
                            }

                            public DashboardPageBuilder withMainContent(Component content) {
                                this.mainContent = content;
                                return this;
                            }

                            public Page build() {
                                pageBuilder.addComponents(Header.H1(title));

                                if (!stats.isEmpty()) {
                                    Row statsRow = new Row();
                                    stats.forEach(statsRow::withChild);
                                    pageBuilder.addRow(row -> row.withChild(statsRow));
                                }

                                if (mainContent != null) {
                                    pageBuilder.addRow(row -> row.withChild(mainContent));
                                }

                                return pageBuilder.build();
                            }
                        }

                        // Usage:
                        Page dashboard = new DashboardPageBuilder()
                            .withTitle("My Dashboard")
                            .addStat(InfoBox.create().withTitle("Users").withValue("123"))
                            .addStat(InfoBox.create().withTitle("Posts").withValue("456"))
                            .withMainContent(ContentModule.create()
                                .withTitle("Recent Activity")
                                .withContent("..."))
                            .build();
                        ```

                        ## Best Practices

                        1. **Follow Existing Patterns**
                           * Provide static `create()` factory method
                           * Use fluent API (return `this`)
                           * Extend `HtmlTag` or `Module` appropriately

                        2. **Keep It Simple**
                           * Don't over-abstract
                           * Composition over inheritance
                           * Single responsibility

                        3. **Document Your Components**
                           * Add JavaDoc with usage examples
                           * Include code examples in documentation
                           * Show real-world use cases

                        4. **Test Your Components**
                           * Test HTML rendering
                           * Verify attribute handling
                           * Check edge cases

                        5. **Security First**
                           * Escape user input
                           * Validate parameters
                           * Use `HtmlUtils.htmlEscape()` for user content

                        ```java
                        import org.springframework.web.util.HtmlUtils;

                        public CustomComponent withUserContent(String userInput) {
                            String safe = HtmlUtils.htmlEscape(userInput);
                            this.withInnerText(safe);
                            return this;
                        }
                        ```
                        """)))

                .build();

        return page.render();
    }
}
