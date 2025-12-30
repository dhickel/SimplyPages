package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.display.*;
import io.mindspice.simplypages.components.navigation.*;
import io.mindspice.simplypages.layout.*;
import io.mindspice.simplypages.modules.*;
import org.springframework.stereotype.Component;

/**
 * Navigation components page - Link, NavBar, SideNav, Breadcrumb.
 */
@Component
public class NavigationPage implements DemoPage {

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1("Navigation Components"))
                .addRow(row -> row.withChild(Alert.info(
                        "Navigation components help users move through your application.")))

                // Link
                .addComponents(Header.H2("Links"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Link** component creates hyperlinks:

                        ```java
                        // Basic link
                        Link.create("/page", "Click Here");

                        // Link with HTMX (load without page refresh)
                        Link.create("/content", "Load Content")
                            .withHxGet("/api/content")
                            .withHxTarget("#main")
                            .withHxPushUrl(true);

                        // External link
                        Link.create("https://example.com", "External Site")
                            .withAttribute("target", "_blank");
                        ```
                        """)))
                .addRow(row -> {
                    Div linkExamples = new Div();
                    linkExamples.withChild(new Paragraph()
                            .withChild(Link.create("/home", "Home Page Link")));
                    linkExamples.withChild(new Paragraph()
                            .withChild(Link.create("/forms", "Forms Page")));
                    linkExamples.withChild(new Paragraph()
                            .withChild(Link.create("https://github.com", "External Link")
                                    .withAttribute("target", "_blank")));

                    row.withChild(ContentModule.create()
                            .withTitle("Link Examples")
                            .withCustomContent(linkExamples));
                })

                // NavBar
                .addComponents(Header.H2("Navigation Bar"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **NavBar** creates horizontal navigation menus:

                        ```java
                        NavBar.create()
                            .withBrand("My Site")
                            .addItem("Home", "/home", true)  // active
                            .addItem("About", "/about")
                            .addItem("Contact", "/contact")
                            .horizontal();
                        ```

                        Or use the builder for more features:

                        ```java
                        TopNavBuilder.create()
                            .withBrand("Cannabis Portal")
                            .addPortal("Research", "/research", true)
                            .addPortal("Forums", "/forums")
                            .addPortal("Journals", "/journals")
                            .build();
                        ```
                        """)))
                .addRow(row -> {
                    NavBar exampleNav = NavBar.create()
                            .withBrand("JHF Demo")
                            .addItem("Components", "/components", true)
                            .addItem("Forms", "/forms")
                            .addItem("Tables", "/tables")
                            .addItem("Modules", "/modules")
                            .horizontal();

                    row.withChild(ContentModule.create()
                            .withTitle("NavBar Example")
                            .withCustomContent(exampleNav));
                })

                // SideNav
                .addComponents(Header.H2("Side Navigation"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **SideNav** creates vertical sidebar navigation:

                        ```java
                        SideNav.create()
                            .addSection("Main")
                            .addItem("Dashboard", "/dashboard", true)
                            .addItem("Profile", "/profile")
                            .addSection("Data")
                            .addItem("Strains", "/strains")
                            .addItem("Journals", "/journals");
                        ```

                        Or use the builder:

                        ```java
                        SideNavBuilder.create()
                            .addSection("Research")
                            .addLink("Strains", "/strains", "🌿")
                            .addLink("Studies", "/studies", "📚")
                            .addSection("Community")
                            .addLink("Forums", "/forum", "💬")
                            .build();
                        ```
                        """)))
                .addRow(row -> {
                    SideNav exampleSideNav = SideNav.create()
                            .addSection("Content")
                            .addItem("Home", "/home", true)
                            .addItem("Components", "/components")
                            .addItem("Forms", "/forms")
                            .addSection("Data")
                            .addItem("Tables", "/tables")
                            .addItem("Display", "/display")
                            .addSection("Advanced")
                            .addItem("HTMX", "/htmx")
                            .addItem("Custom", "/custom");

                    row.withChild(new Column().withWidth(4).withChild(
                            ContentModule.create()
                                    .withTitle("SideNav Example")
                                    .withCustomContent(exampleSideNav)));
                })

                // Breadcrumbs
                .addComponents(Header.H2("Breadcrumbs"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Breadcrumb** shows navigation hierarchy:

                        ```java
                        Breadcrumb.create()
                            .addItem("Home", "/")
                            .addItem("Components", "/components")
                            .addItem("Navigation", "/navigation", true); // current
                        ```
                        """)))
                .addRow(row -> {
                    Breadcrumb exampleBreadcrumb = Breadcrumb.create()
                            .addItem("Home", "/home")
                            .addItem("Components", "/components")
                            .addActiveItem("Navigation");

                    row.withChild(ContentModule.create()
                            .withTitle("Breadcrumb Example")
                            .withCustomContent(exampleBreadcrumb));
                })

                .build();

        return page.render();
    }
}
