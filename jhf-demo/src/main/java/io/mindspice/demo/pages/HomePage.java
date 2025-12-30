package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.display.*;
import io.mindspice.simplypages.layout.*;
import io.mindspice.simplypages.modules.*;
import org.springframework.stereotype.Component;

/**
 * Home page - Framework overview and introduction.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>Page.builder() pattern</li>
 *   <li>Header components</li>
 *   <li>Markdown component for rich text</li>
 *   <li>Basic page structure</li>
 * </ul>
 */
@Component
public class HomePage implements DemoPage {

    @Override
    public String render() {
        Page homePage = Page.builder()
                // Add main title using H1 header
                .addComponents(Header.H1("Java HTML Framework"))

                // Add introduction section with Markdown
                .addRow(row -> row.withChild(new Markdown(
                        """
                        # Welcome to the Java HTML Framework Tutorial!

                        This interactive demo showcases every component, module, and pattern available in JHF.
                        Use the navigation menu to explore different features.

                        ## What is JHF?

                        JHF is a lightweight, domain-specific framework for building server-side rendered web
                        applications with minimal JavaScript. Perfect for:

                        * Research portals and data-heavy applications
                        * Community platforms (forums, journals, discussions)
                        * Content management systems
                        * Any application where Java developers need to build UIs without deep web experience

                        ## Key Features

                        * **Java-Native:** Build UIs entirely in Java using fluent, type-safe APIs
                        * **Server-Side Rendering:** No complex frontend build process
                        * **Markdown Support:** Write content in Markdown, rendered automatically
                        * **HTMX Integration:** Dynamic updates without full page reloads
                        * **Modular Architecture:** Compose complex UIs from reusable components
                        * **Type Safety:** Compile-time checking for your UI code

                        ## Framework Architecture

                        ```
                        Component (interface) ← All renderable elements
                        │
                        ├── HtmlTag ← Base class for HTML elements
                        │   ├── Basic (Div, Paragraph, Header)
                        │   ├── Forms (TextInput, Button, Form)
                        │   ├── Display (Table, Card, Alert)
                        │   ├── Media (Gallery, Video, Audio)
                        │   ├── Navigation (Link, NavBar, SideNav)
                        │   └── Layout (Row, Column, Grid, Page)
                        │
                        └── Module ← High-level compositions
                            ├── ContentModule
                            ├── FormModule
                            ├── DataModule
                            ├── GalleryModule
                            └── ForumModule
                        ```

                        ## Quick Start Example

                        ```java
                        @GetMapping("/my-page")
                        @ResponseBody
                        public String myPage() {
                            Page page = Page.builder()
                                .addComponents(Header.H1("My Page"))
                                .addRow(row -> row.withChild(
                                    new Markdown("Content here!")
                                ))
                                .build();

                            return page.render();
                        }
                        ```

                        ## Navigate the Demo

                        Use the sidebar to explore:
                        * **Components** - Basic building blocks
                        * **Forms** - User input components
                        * **Display** - Data visualization
                        * **Modules** - High-level compositions
                        * **HTMX** - Dynamic behavior patterns
                        * **Custom** - Extending the framework

                        All source code for these demos is in `DemoController.java` - check it out to see
                        exactly how each example is built!
                        """
                )))

                // Add feature highlights with InfoBoxes
                .addRow(row -> {
                    Row infoBoxRow = new Row()
                            .withComponents(
                                    InfoBox.create()
                                            .withIcon("🎨")
                                            .withTitle("50+ Components")
                                            .withValue("Ready to Use"),
                                    InfoBox.create()
                                            .withIcon("📦")
                                            .withTitle("5 Module Types")
                                            .withValue("High-Level"),
                                    InfoBox.create()
                                            .withIcon("⚡")
                                            .withTitle("HTMX Powered")
                                            .withValue("Dynamic Updates")
                            );

                    row.withChild(ContentModule.create()
                            .withTitle("Framework Stats")
                            .withCustomContent(infoBoxRow));
                })

                .build();

        return homePage.render();
    }
}
