package io.mindspice.demo.pages;

import io.mindspice.jhf.components.*;
import io.mindspice.jhf.components.display.*;
import io.mindspice.jhf.components.navigation.*;
import io.mindspice.jhf.layout.Page;
import org.springframework.stereotype.Component;

/**
 * Demo page for sticky sidebar layout.
 * Creates a page with a sticky sidebar that follows the user as they scroll.
 */
@Component
public class StickySidebarDemoPage implements DemoPage {

    @Override
    public String render() {
        // Build the sidebar content
        io.mindspice.jhf.core.Component sidebarContent = new Div()
                .withChild(Header.H3("Table of Contents").withClass("mb-3"))
                .withChild(
                        new Div()
                                .withAttribute("class", "list-unstyled")
                                .withChild(new Div().withClass("mb-2").withChild(new Link("#section-1", "→ Section 1")))
                                .withChild(new Div().withClass("mb-2").withChild(new Link("#section-2", "→ Section 2")))
                                .withChild(new Div().withClass("mb-2").withChild(new Link("#section-3", "→ Section 3")))
                                .withChild(new Div().withClass("mb-2").withChild(new Link("#section-4", "→ Section 4")))
                                .withChild(new Div().withClass("mb-2").withChild(new Link("#section-5", "→ Section 5")))
                                .withChild(new Div().withClass("mb-2").withChild(new Link("#section-6", "→ Section 6")))
                                .withChild(new Div().withClass("mb-2").withChild(new Link("#section-7", "→ Section 7")))
                                .withChild(new Div().withClass("mb-2").withChild(new Link("#section-8", "→ Section 8")))
                                .withChild(new Div().withClass("mb-2").withChild(new Link("#section-9", "→ Section 9")))
                                .withChild(new Div().withClass("mb-2").withChild(new Link("#section-10", "→ Section 10")))
                )
                .withChild(Divider.horizontal().withClass("my-4"))
                .withChild(Header.H4("Sticky Features").withClass("mb-3"))
                .withChild(
                        new Div()
                                .withAttribute("class", "list mb-3")
                                .withChild(new Div().withInnerText("• Follows scroll"))
                                .withChild(new Div().withInnerText("• Own scrollbar"))
                                .withChild(new Div().withInnerText("• Responsive"))
                )
                .withChild(
                        Alert.info("This sidebar stays visible as you scroll through the main content!")
                );

        return Page.builder()
                .withStickySidebar(sidebarContent, 9, 3)
                .addComponents(
                        Header.H1("Sticky Sidebar Demo").withClass("mb-4"),
                        Alert.info("Scroll down to see the sidebar on the right follow you!"),

                        // Generate sections with IDs for navigation
                        generateSection(1),
                        generateSection(2),
                        generateSection(3),
                        generateSection(4),
                        generateSection(5),
                        generateSection(6),
                        generateSection(7),
                        generateSection(8),
                        generateSection(9),
                        generateSection(10),

                        Alert.success("Notice how the sidebar stayed visible and followed you all the way down!")
                )
                .build()
                .render();
    }

    private io.mindspice.jhf.core.Component generateSection(int number) {
        return new Div()
                .withAttribute("id", "section-" + number)
                .withClass("mb-5")
                .withChild(Header.H2("Section " + number).withClass("mb-3"))
                .withChild(new Paragraph("This is section " + number + ". Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                        "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
                        "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."))
                .withChild(new Paragraph("Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
                        "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."))
                .withChild(new Paragraph("The sticky sidebar on the right contains a table of contents and stays visible as you scroll. " +
                        "This is perfect for long-form content, documentation, or any page where you want persistent navigation."))
                .withChild(
                        new Card()
                                .withHeader("Card in Section " + number)
                                .withBody(new Paragraph("This card is part of section " + number + ". " +
                                        "Try clicking the links in the sidebar to jump between sections!"))
                );
    }
}
