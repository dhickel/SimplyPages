package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerTest {

    @Test
    @DisplayName("Container should render default and size classes")
    void testContainerSizes() {
        String defaultHtml = Container.create().render();
        String fluidHtml = Container.fluid().render();

        assertTrue(defaultHtml.contains("class=\"container\""));
        assertTrue(fluidHtml.contains("container container-fluid"));
    }

    @Test
    @DisplayName("Container should render custom class and children")
    void testContainerWithClassAndChild() {
        String html = Container.create()
            .withClass("custom")
            .withChild(new Paragraph("Content"))
            .render();

        assertTrue(html.contains("class=\"container custom\""));
        assertTrue(html.contains("Content"));
    }
}
