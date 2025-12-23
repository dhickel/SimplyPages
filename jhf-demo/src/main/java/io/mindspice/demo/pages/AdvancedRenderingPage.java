package io.mindspice.demo.pages;

import io.mindspice.jhf.builders.ShellBuilder;
import io.mindspice.jhf.components.Header;
import io.mindspice.jhf.components.Paragraph;
import io.mindspice.jhf.components.display.Card;
import io.mindspice.jhf.core.*;
import io.mindspice.jhf.layout.Column;
import io.mindspice.jhf.layout.Container;
import io.mindspice.jhf.layout.Page;
import io.mindspice.jhf.layout.Row;

public class AdvancedRenderingPage {

    // Define SlotKeys as constants
    private static final SlotKey<String> TITLE_KEY = SlotKey.of("title");
    private static final SlotKey<String> CONTENT_KEY = SlotKey.of("content");

    // Define a static template for a module
    private static final Template CARD_TEMPLATE;

    static {
        Card card = Card.create()
                .withHeader(Slot.of(TITLE_KEY))
                .withBody(Slot.of(CONTENT_KEY));
        CARD_TEMPLATE = Template.of(card);
    }

    public static String render(boolean isProUser) {
        // Return just the content for injection into the shell
        Component mainContent = buildContent(isProUser);
        return mainContent.render();
    }

    private static Component buildContent(boolean isProUser) {
        Container container = Container.create();

        container.withChild(Header.H1("Advanced Rendering Patterns"));
        container.withChild(new Paragraph().withInnerText("Demonstrating Pattern A (Structure) and Pattern B (Templates)."));

        // Pattern A: Request-Scoped Composition
        // Conditional layout based on user role
        Row row = Row.create();

        if (isProUser) {
            row.withChild(Column.create().withWidth(8).withChild(
                createTemplateComponent("Pro Dashboard", "Welcome to the advanced view.")
            ));
            row.withChild(Column.create().withWidth(4).withChild(
                createTemplateComponent("Quick Actions", "Pro user actions here.")
            ));
        } else {
            row.withChild(Column.create().withWidth(12).withChild(
                createTemplateComponent("Standard Dashboard", "Welcome to the standard view.")
            ));
        }

        container.withChild(row);

        return container;
    }

    // Helper to use Pattern B: Pre-compiled Templates
    private static TemplateComponent createTemplateComponent(String title, String content) {
        RenderContext context = RenderContext.create()
                .set(TITLE_KEY, title)
                .set(CONTENT_KEY, content);

        // Pattern B + TemplateComponent
        return TemplateComponent.of(CARD_TEMPLATE, context);
    }
}
