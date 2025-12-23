package io.mindspice.demo.pages;

import io.mindspice.jhf.components.forms.Button;
import io.mindspice.jhf.components.forms.Form;
import io.mindspice.jhf.components.forms.TextArea;
import io.mindspice.jhf.components.forms.Button.ButtonType;
import io.mindspice.jhf.components.*;
import io.mindspice.jhf.core.Component; // Explicit import
import io.mindspice.jhf.core.*;
import io.mindspice.jhf.modules.ContentModule;
import io.mindspice.jhf.layout.Row;
import io.mindspice.jhf.layout.Column;
import io.mindspice.jhf.layout.Page;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@org.springframework.stereotype.Component
public class AdvancedRenderingPage implements DemoPage {

    // --- Pattern B: Static Template Definition ---

    // We define a simple module for the Pattern B demo (High Performance List)
    private static final SlotKey<String> ITEM_TEXT = SlotKey.of("text");

    // A single item template (Pattern B)
    private static final Template ITEM_TEMPLATE = Template.of(
        new Div()
            .withClass("card mb-2 shadow-sm")
            .withChild(new Div().withClass("card-body p-3")
                .withChild(Slot.of(ITEM_TEXT))
            )
    );

    // Template for Composite Pattern (mimics a full module)
    private static final Template COMPOSITE_MODULE_TEMPLATE = Template.of(
        new Div().withClass("card h-100 shadow-sm")
            .withChild(
                new Div().withClass("card-header bg-light fw-bold")
                    .withChild(Slot.of(ITEM_TEXT)) // Use the text slot as title
            )
            .withChild(
                new Div().withClass("card-body")
                    .withInnerText("This is a pre-compiled template module rendered within a runtime layout.")
            )
    );

    // --- Wiki-Style Editing Demo Templates ---

    // Slots for the wiki module
    private static final SlotKey<String> WIKI_CONTENT = SlotKey.of("content");

    // 1. Display View
    private static final Template WIKI_DISPLAY_TEMPLATE = Template.of(
        new ContentModule()
            .withTitle("Wiki Article")
            .withCustomContent(
                new Div()
                    .withChild(new Paragraph().withChild(Slot.of(WIKI_CONTENT)))
                    .withChild(
                        new Button("Edit")
                            .withClass("btn btn-primary mt-2")
                            .withAttribute("hx-get", "/demo/wiki/edit")
                            .withAttribute("hx-swap", "outerHTML")
                            .withAttribute("hx-target", "closest .module") // Replace the entire module
                    )
            )
    );

    // 2. Edit View (Form)
    // We construct the TextArea carefully to inject the slot as child (which renders as value)
    private static final Component WIKI_EDIT_FORM = new Form()
        .withAttribute("hx-post", "/demo/wiki/save")
        .withAttribute("hx-swap", "outerHTML")
        .withAttribute("hx-target", "closest .module")
        .withChild(
            // TextArea constructor sets name
            new TextArea("content")
                .withClass("form-control mb-2")
                .withRows(4)
                .withChild(Slot.of(WIKI_CONTENT)) // Slot as child renders as textarea value
        )
        .withChild(
            new Button("Save Changes")
                .withType(ButtonType.SUBMIT)
                .withClass("btn btn-success me-2")
        )
        .withChild(
            new Button("Cancel")
                .withType(ButtonType.BUTTON)
                .withClass("btn btn-secondary")
                .withAttribute("hx-get", "/demo/wiki/display") // Re-fetch display view
                .withAttribute("hx-swap", "outerHTML")
                .withAttribute("hx-target", "closest .module")
        );

    private static final Template WIKI_EDIT_TEMPLATE = Template.of(
        new ContentModule()
            .withTitle("Editing Article")
            .withCustomContent(WIKI_EDIT_FORM)
    );

    @Override
    public String render() {
        return Page.builder()
            .addComponents(Header.H1("Advanced Rendering Patterns"))
            .addComponents(new Paragraph().withInnerText("This page demonstrates the dual-pattern architecture for handling dynamic content."))

            // --- Section 1: Pattern A (Dynamic Structure) ---
            .addComponents(renderPatternASection(false)) // Default to simple

            // --- Section 2: Pattern B (Dynamic Data) ---
            .addComponents(renderPatternBSection())

            // --- Section 3: Composite Pattern ---
            .addComponents(renderCompositeSection())

            // --- Section 4: Wiki-Style Editing ---
            .addComponents(renderWikiSection())

            .build()
            .render();
    }

    /**
     * Pattern A: Request-Scoped Composition
     * Builds a different layout based on a condition (simulated by a boolean).
     */
    public Component renderPatternASection(boolean complexLayout) {
        return new ContentModule()
            .withTitle("Pattern A: Dynamic Structure")
            .withCustomContent(
                new Div()
                    .withChild(new Paragraph().withInnerText("Layout changes based on condition (click toggle below)."))
                    .withChild(renderPatternAInner(complexLayout))
            );
    }

    /**
     * Helper to render the inner content of Pattern A.
     * Separated to avoid duplicating the outer module when updating via HTMX.
     */
    public Div renderPatternAInner(boolean complexLayout) {
        Div container = new Div().withAttribute("id", "pattern-a-container");

        // Control to toggle layout
        container.withChild(
            new Button("Toggle Layout")
                .withClass("btn btn-outline-primary mb-3")
                .withAttribute("hx-get", "/demo/advanced/layout?complex=" + !complexLayout)
                .withAttribute("hx-target", "#pattern-a-container")
                .withAttribute("hx-swap", "outerHTML")
        );

        if (complexLayout) {
            // Complex Layout: 2 Columns
            container.withChild(
                new Row()
                    .withChild(Column.create().withWidth(8).withChild(
                        new Div().withClass("p-3 bg-info text-white").withInnerText("Complex Layout - Main Content (8 cols)")
                    ))
                    .withChild(Column.create().withWidth(4).withChild(
                        new Div().withClass("p-3 bg-dark text-white").withInnerText("Sidebar (4 cols)")
                    ))
            );
        } else {
            // Simple Layout: 1 Column
            container.withChild(
                new Row()
                    .withChild(Column.create().withWidth(12).withChild(
                        new Div().withClass("p-3 bg-light border").withInnerText("Simple Layout - Single Column (12 cols)")
                    ))
            );
        }
        return container;
    }

    /**
     * Pattern B: Pre-compiled Templates
     * Renders a list of items using a pre-compiled template for high performance.
     */
    private Component renderPatternBSection() {
        // Generate data for 5 items
        List<Component> items = IntStream.range(1, 6)
            .mapToObj(i -> {
                RenderContext ctx = RenderContext.builder()
                    .with(ITEM_TEXT, "Dynamic Item #" + i)
                    .build();
                // Use the new TemplateComponent!
                return TemplateComponent.of(ITEM_TEMPLATE, ctx);
            })
            .collect(Collectors.toList());

        Div listContainer = new Div();
        items.forEach(listContainer::withChild);

        return new ContentModule()
            .withTitle("Pattern B: Dynamic Data")
            .withCustomContent(
                new Div()
                    .withChild(new Paragraph().withInnerText("High-performance rendering using Templates and RenderContext."))
                    .withChild(listContainer)
            );
    }

    /**
     * Composite Pattern
     * Combines Pattern A (Page Layout) and Pattern B (Module Rendering).
     * We determine the layout at runtime, but use templates for the content.
     */
    private Component renderCompositeSection() {
        // Simulate a runtime layout config
        List<String> layoutConfig = List.of("Module A", "Module B", "Module C");

        Row row = new Row();

        for (String modName : layoutConfig) {
            // Pattern A: Create column structure
            Column col = Column.create().withWidth(4);

            // Pattern B: Use TemplateComponent for content
            RenderContext ctx = RenderContext.builder()
                .with(ITEM_TEXT, modName)
                .build();

            col.withChild(TemplateComponent.of(COMPOSITE_MODULE_TEMPLATE, ctx));
            row.withChild(col);
        }

        return new ContentModule()
            .withTitle("Composite Pattern")
            .withCustomContent(
                new Div()
                    .withChild(new Paragraph().withInnerText("Runtime layout structure containing pre-compiled template modules."))
                    .withChild(row)
            );
    }

    /**
     * Wiki-Style Editing
     * Uses HTMX to swap between Display and Edit templates.
     */
    private Component renderWikiSection() {
        // Initial render uses the Display Template
        // In a real app, this would fetch from DB.
        String initialContent = "This is a wiki article. Click edit to modify me.";
        return renderWikiDisplay(initialContent);
    }

    // --- Helper methods for Controller to call ---

    public static TemplateComponent renderWikiDisplay(String content) {
        RenderContext ctx = RenderContext.builder()
             .with(WIKI_CONTENT, content)
             .build();
        return TemplateComponent.of(WIKI_DISPLAY_TEMPLATE, ctx);
    }

    public static TemplateComponent renderWikiEdit(String content) {
        RenderContext ctx = RenderContext.builder()
             .with(WIKI_CONTENT, content)
             .build();
        return TemplateComponent.of(WIKI_EDIT_TEMPLATE, ctx);
    }

}
