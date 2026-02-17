package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.forms.*;
import io.mindspice.simplypages.layout.*;
import io.mindspice.simplypages.modules.*;
import org.springframework.stereotype.Component;

/**
 * Modules page - demonstrates the module system.
 */
@Component
public class ModulesPage implements DemoPage {

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1("Module System"))

                .addRow(row -> row.withChild(new Markdown(
                        """
                        ## What are Modules?

                        Modules are high-level components that combine multiple primitives into complete
                        functional units. Think of them as pre-built page sections with consistent
                        structure and styling.

                        ### Module vs Component

                        * **Component** - Single HTML element (Button, TextInput, Paragraph)
                        * **Module** - Multiple components combined (ContentModule = title + markdown + styling)

                        ### Built-in Modules

                        1. **ContentModule** - Display formatted text and Markdown
                        2. **FormModule** - Complete forms with structure
                        3. **DataModule** - Type-safe data tables
                        4. **GalleryModule** - Image galleries with captions
                        5. **ForumModule** - Discussion threads and posts

                        ### Why Use Modules?

                        * Consistent styling across your application
                        * Faster development (pre-built patterns)
                        * Easier maintenance (change module, update everywhere)
                        * Better composition (mix and match modules)
                        """)))

                // ContentModule
                .addComponents(Header.H2("Content Module"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **ContentModule** displays formatted text content with optional title:

                        ```java
                        ContentModule.create()
                            .withTitle("Section Title")
                            .withContent(\"\"\"
                                ## Markdown Support

                                Write **Markdown** and it renders as HTML!
                            \"\"\");
                        ```
                        """)))

                .addRow(row -> {
                    ContentModule contentModule = ContentModule.create()
                            .withTitle("Content Module Example")
                            .withModuleId("content-demo")
                            .withContent(
                                    """
                                    ## Features of ContentModule

                                    * Automatic title rendering
                                    * Full Markdown support
                                    * Consistent styling
                                    * Optional module ID for targeting

                                    ### Markdown Capabilities

                                    Write **bold**, *italic*, `code`, and more!

                                    * Lists
                                    * [Links](#)
                                    * Tables
                                    * Headers

                                    Perfect for documentation, blog posts, or any rich text content.
                                    """
                            );

                    row.withChild(contentModule);
                })

                // FormModule
                .addComponents(Header.H2("Form Module"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **FormModule** combines form components with structure and styling:

                        ```java
                        FormModule.create()
                            .withTitle("Contact Form")
                            .withDescription("Get in touch with us")
                            .addField("Name", TextInput.create("name").required())
                            .addField("Email", TextInput.email("email").required())
                            .addField("", Button.submit("Send"));
                        ```
                        """)))

                .addRow(row -> {
                    FormModule formModule = FormModule.create()
                            .withTitle("Sample Form Module")
                            .withDescription("This form demonstrates the FormModule structure")
                            .addField("Username", TextInput.create("username")
                                    .withPlaceholder("Choose a username")
                                    .required())
                            .addField("Email", TextInput.email("email")
                                    .withPlaceholder("your@email.com")
                                    .required())
                            .addField("Bio", TextArea.create("bio")
                                    .withRows(3)
                                    .withPlaceholder("Tell us about yourself"))
                            .addField("", Button.submit("Create Profile")
                                    .withStyle(Button.ButtonStyle.PRIMARY));

                    row.withChild(formModule);
                })

                // DataModule
                .addComponents(Header.H2("Data Module"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **DataModule** displays type-safe data tables:

                        ```java
                        DataModule.create(Product.class)
                            .withTitle("Product Database")
                            .withDataTable(
                                DataTable.create(Product.class)
                                    .addColumn("Name", Product::getName)
                                    .addColumn("Category", Product::getCategory)
                                    .withData(productList)
                            );
                        ```

                        Note: Requires actual Java objects for type-safe columns.
                        """)))

                // GalleryModule (shown earlier)
                .addComponents(Header.H2("Gallery Module"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **GalleryModule** creates image galleries:

                        ```java
                        GalleryModule.create()
                            .withTitle("Photo Gallery")
                            .withColumns(3)
                            .addImage("/img1.jpg", "Alt", "Caption")
                            .addImage("/img2.jpg", "Alt", "Caption");
                        ```

                        See the Gallery page for a full demonstration.
                        """)))

                // ForumModule (shown earlier)
                .addComponents(Header.H2("Forum Module"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **ForumModule** displays discussion threads:

                        ```java
                        ForumModule.create()
                            .withTitle("Recent Discussions")
                            .addPost(ForumPost.create()
                                .withAuthor("User")
                                .withTitle("Topic")
                                .withContent("Content..."));
                        ```

                        See the Forum page for examples.
                        """)))

                // Creating Custom Modules
                .addComponents(Header.H2("Creating Custom Modules"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Extend the `Module` base class to create your own:

                        ```java
                        public class StatsModule extends Module {
                            private Map<String, Integer> stats;

                            public StatsModule() {
                                super("div");
                                this.withClass("stats-module");
                            }

                            public static StatsModule create() {
                                return new StatsModule();
                            }

                            public StatsModule withStats(Map<String, Integer> stats) {
                                this.stats = stats;
                                return this;
                            }

                            @Override
                            protected void buildContent() {
                                if (title != null) {
                                    super.withChild(Header.H2(title));
                                }

                                Row row = new Row();
                                for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                                    row.withChild(InfoBox.create()
                                        .withTitle(entry.getKey())
                                        .withValue(entry.value().toString()));
                                }
                                super.withChild(row);
                            }
                        }
                        ```

                        See the Custom Components page for more details.
                        """)))

                .build();

        return page.render();
    }
}
