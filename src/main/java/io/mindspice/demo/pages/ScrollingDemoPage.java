package io.mindspice.demo.pages;

import io.mindspice.jhf.components.*;
import io.mindspice.jhf.components.display.*;
import io.mindspice.jhf.layout.Page;
import org.springframework.stereotype.Component;

/**
 * Demo page for independent scrolling layout.
 * Creates a page with its own scrollbar independent of the shell.
 */
@Component
public class ScrollingDemoPage implements DemoPage {

    @Override
    public String render() {
        return Page.builder()
                .withIndependentScrolling()
                .addComponents(
                        Header.H1("Independent Scrolling Demo").withClass("mb-4"),
                        Alert.info("This page has its own scrollbar and scrolls independently from the shell and navbar. Scroll down to see more content!"),

                        // Generate lots of content to demonstrate scrolling
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

                        Alert.success("You've reached the end! Notice how the navbar and sidebar stay fixed while only this content area scrolled.")
                )
                .build()
                .render();
    }

    private io.mindspice.jhf.core.Component generateSection(int number) {
        return new Div()
                .withClass("mb-5")
                .withChild(Header.H2("Section " + number).withClass("mb-3"))
                .withChild(new Paragraph("This is section " + number + ". Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                        "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
                        "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."))
                .withChild(new Paragraph("Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
                        "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."))
                .withChild(
                        new Card()
                                .withHeader("Card in Section " + number)
                                .withBody(new Paragraph("This card is part of section " + number + ". " +
                                        "Independent scrolling allows for app-like experiences where the main navigation stays fixed " +
                                        "while content scrolls freely."))
                );
    }
}
