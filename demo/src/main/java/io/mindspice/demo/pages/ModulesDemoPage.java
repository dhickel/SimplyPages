package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.ListItem;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.components.forum.ForumPost;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.modules.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ModulesDemoPage implements DemoPage {

    private record MetricRow(String name, String value) {}

    @Override
    public String render() {
        DataModule<MetricRow> dataModule = DataModule.create(MetricRow.class)
            .withTitle("DataModule")
            .withDataTable(io.mindspice.simplypages.components.display.DataTable.create(MetricRow.class)
                .addColumn("Metric", MetricRow::name)
                .addColumn("Value", MetricRow::value)
                .withData(List.of(new MetricRow("P95", "220ms"), new MetricRow("Errors", "0.03%"))));

        HeroModule heroModule = HeroModule.create()
            .withTitle("SimplyPages")
            .withSubtitle("Module-first composition")
            .withDescription("Compose reusable server-rendered sections with clear lifecycle boundaries.")
            .withPrimaryButton("Read Docs", "/docs")
            .centered();

        ContentModule contentModule = ContentModule.create()
            .withTitle("ContentModule")
            .withContent("Markdown-backed content for docs and editorial sections.");

        FormModule formModule = FormModule.create()
            .withTitle("FormModule")
            .withSubmitUrl("/demos/api/form-preview")
            .addField("Email", TextInput.email("moduleEmail").withPlaceholder("team@example.com"));

        GalleryModule galleryModule = GalleryModule.create()
            .withTitle("GalleryModule")
            .withColumns(2)
            .addImage("https://picsum.photos/id/1015/800/460", "mountain lake")
            .addImage("https://picsum.photos/id/1039/800/460", "forest river");

        ForumModule forumModule = ForumModule.create()
            .withTitle("ForumModule")
            .addPost(ForumPost.create().withAuthor("maintainer").withTitle("ForumModule").withContent("Thread-ready module."));

        SimpleListModule simpleListModule = SimpleListModule.create()
            .withTitle("SimpleListModule")
            .addItem(ListItem.create("One"))
            .addItem(ListItem.create("Two"));

        RichContentModule richContentModule = RichContentModule.create("RichContentModule")
            .addParagraph(new Paragraph("Mixed content blocks in a single module."))
            .addHeader(Header.H4("Subsection"));

        CalloutModule calloutModule = CalloutModule.create()
            .withTitle("CalloutModule")
            .withContent("Contextual notice block.")
            .success();

        ComparisonModule comparisonModule = ComparisonModule.create()
            .withTitle("ComparisonModule")
            .addColumn("Starter")
            .addColumn("Pro", true)
            .addRow("Support", "Community", "Priority");

        QuoteModule quoteModule = QuoteModule.create()
            .withTitle("QuoteModule")
            .withQuote("Keep dynamic values in RenderContext.")
            .withAuthor("SimplyPages")
            .centered();

        StatsModule statsModule = StatsModule.create()
            .withTitle("StatsModule")
            .withColumns(2)
            .addStat("124", "Components")
            .addStat("19", "Modules");

        TabsModule tabsModule = TabsModule.create()
            .withTitle("TabsModule")
            .addTab("Pattern A", "Replace one module target")
            .addTab("Pattern B", "OOB multi-target updates");

        TimelineModule timelineModule = TimelineModule.create()
            .withTitle("TimelineModule")
            .addEvent("2026-02", "Consolidation", "Reduced demo drift and route sprawl")
            .addEvent("2026-03", "Docs sync", "Align examples with framework contracts");

        AccordionModule accordionModule = AccordionModule.create()
            .withTitle("AccordionModule")
            .addItem("Build once", "Module structure belongs in buildContent")
            .addItem("Dynamic data", "Use Template + SlotKey + RenderContext")
            .withFirstExpanded();

        DynamicCardModule dynamicCardModule = DynamicCardModule.create()
            .withTitle("DynamicCardModule")
            .withCardContent("Runtime Card", "Mutable demo module");

        DynamicListModule dynamicListModule = DynamicListModule.create()
            .withTitle("DynamicListModule")
            .withListItems(List.of("A", "B", "C"));

        DynamicTableModule dynamicTableModule = DynamicTableModule.create()
            .withTitle("DynamicTableModule");

        EditableModule editableModule = EditableModule.wrap(
                ContentModule.create().withTitle("EditableModule").withContent("Decorator controls"))
            .withModuleId("editable-sample")
            .withEditUrl("/editing-demo/edit/module-1")
            .withDeleteUrl("/editing-demo/delete/module-1");

        return Page.builder()
            .addComponents(Header.H1("Modules Library"))

            .addRow(row -> row.withJustify("center").withChild(centered(heroModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(contentModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(richContentModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(formModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(dataModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(galleryModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(forumModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(simpleListModule)))

            .addRow(row -> row
                .withChild(new Column().withWidth(6).withChild(calloutModule))
                .withChild(new Column().withWidth(6).withChild(quoteModule)))
            .addRow(row -> row
                .withChild(new Column().withWidth(6).withChild(comparisonModule))
                .withChild(new Column().withWidth(6).withChild(statsModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(tabsModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(accordionModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(timelineModule)))

            .addRow(row -> row.withJustify("center").withChild(centered(dynamicCardModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(dynamicListModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(dynamicTableModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(editableModule)))

            .build()
            .render();
    }

    private Column centered(io.mindspice.simplypages.core.Component component) {
        return new Column().withWidth(10).withChild(component);
    }
}
