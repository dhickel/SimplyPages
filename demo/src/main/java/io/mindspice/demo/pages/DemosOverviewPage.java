package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Markdown;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.modules.ContentModule;
import org.springframework.stereotype.Component;

@Component
public class DemosOverviewPage implements DemoPage {

    @Override
    public String render() {
        return Page.builder()
            .addComponents(Header.H1("SimplyPages Demo Overview"))
            .addRow(row -> row.withChild(new Column().withWidth(8).withChild(
                ContentModule.create()
                    .withTitle("Purpose")
                    .withContent("""
                        This demo is the public framework surface for contributors evaluating component coverage,
                        module composition patterns, and HTMX/editing integration.

                        Each page is intentionally organized into section rows with 2-4 examples per row so layout behavior
                        is visible without overcrowding.
                        """))))
            .addRow(row -> row.withChild(new Column().withWidth(4).withChild(
                ContentModule.create()
                    .withTitle("Quick Navigation")
                    .withCustomContent(new Markdown("""
                        - [Basics & Forms](/demos/basics-forms)
                        - [Display & Data](/demos/display-data)
                        - [Modules](/demos/modules)
                        - [HTMX & Editing](/demos/htmx-editing)
                        """)))))
            .addRow(row -> row
                .withChild(new Column().withWidth(6).withChild(ContentModule.create()
                    .withTitle("Basics & Forms")
                    .withContent("Core primitives, text rendering, form controls, and end-to-end form composition.")))
                .withChild(new Column().withWidth(6).withChild(ContentModule.create()
                    .withTitle("Display & Data")
                    .withContent("Status UI, tables, cards, media, navigation, and forum-oriented components."))))
            .addRow(row -> row
                .withChild(new Column().withWidth(6).withChild(ContentModule.create()
                    .withTitle("Modules")
                    .withContent("All module types with practical row layouts and grouped variant examples.")))
                .withChild(new Column().withWidth(6).withChild(ContentModule.create()
                    .withTitle("HTMX & Editing")
                    .withContent("Template + SlotKey + RenderContext flow plus integration with editing endpoints."))))
            .build()
            .render();
    }
}
