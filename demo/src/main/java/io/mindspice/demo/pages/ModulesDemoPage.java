package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.chat.*;
import io.mindspice.simplypages.components.display.PollingPanel;
import io.mindspice.simplypages.components.display.StatusBadge;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.components.navigation.HtmxTabNav;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.modules.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ModulesDemoPage implements DemoPage {

    private record MetricRow(String name, String value) {}

    private record DemoBlock(
        String id,
        String label,
        String kind,
        boolean open,
        io.mindspice.simplypages.core.Component content
    ) implements EmbeddedBlockData {}

    private record DemoTranscriptEntry(
        String id,
        String title,
        String body,
        String actor,
        String timestamp,
        String status,
        List<DemoBlock> embeddedBlocks
    ) implements TranscriptEntryData {}

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

        AssistantChatModule assistantWorkspace = AssistantChatModule.create()
            .withModuleId("assistant-workspace-demo")
            .withTitle("Assistant Workspace Primitives")
            .withDescription("Reusable assistant workspace modules compose room navigation, timeline transcript, polling status, and HTMX tabs.")
            .withRoomList(ChatRoomListModule.create()
                .withTitle("Rooms")
                .withDescription("Conversation buttons target a server-owned room fragment.")
                .withTarget("#assistant-room-fragment")
                .withSwap("outerHTML")
                .withActiveRoom("planning")
                .addRoom("planning", "Planning", "2 open items", "/demos/api/assistant/room?room=planning")
                .addRoom("handoff", "Handoff", "ready", "/demos/api/assistant/room?room=handoff")
                .addRoom("review", "Review", "waiting", "/demos/api/assistant/room?room=review"))
            .withToolbar(new Div()
                .withChild(HtmxTabNav.create("assistant-workspace-tabs", "#assistant-tab-panel")
                    .withActiveKey("summary")
                    .addTab("summary", "Summary", "/demos/api/assistant/tab?tab=summary")
                    .addTab("tools", "Tools", "/demos/api/assistant/tab?tab=tools")
                    .addTab("activity", "Activity", "/demos/api/assistant/tab?tab=activity"))
                .withChild(new Div()
                    .withId("assistant-tab-panel")
                    .withChild(assistantTabPanel("summary"))))
            .withChat(ChatModule.create()
                .withTitle("Room Timeline")
                .withDescription("The chat module carries application-owned transport metadata while the transcript renderer owns structure.")
                .withUiConfig(new ChatUiConfig(
                    "demo-planning-room",
                    ChatTransportMode.POLLING,
                    "/demos/api/assistant/room?room=planning",
                    "",
                    "#assistant-room-fragment",
                    "outerHTML",
                    15000
                ))
                .withTranscript(assistantRoomFragment("planning"))
                .withComposer(new Div()
                    .withClass("chat-demo-composer")
                    .withChild(TextInput.create("message").withPlaceholder("Draft a message"))
                    .withChild(Button.create("Send").withStyle(Button.ButtonStyle.PRIMARY))))
            .withSidePanel(new Div()
                .withChild(assistantStatusPanel())
                .withChild(new Div()
                    .withClass("assistant-workspace-badges")
                    .withChild(StatusBadge.success("Ready"))
                    .withChild(StatusBadge.busy("Running"))
                    .withChild(StatusBadge.warning("Needs review"))));

        MasterDetailBrowserModule workspaceBrowser = MasterDetailBrowserModule.create()
            .withModuleId("assistant-workspace-browser")
            .withTitle("MasterDetailBrowserModule")
            .withDescription("Item buttons load app-owned detail fragments into a stable target.")
            .withTarget("#workspace-detail-panel")
            .withActiveKey("workspace")
            .addItem("workspace", "Workspace", "files and outputs", "/demos/api/workspace/detail?item=workspace")
            .addItem("runs", "Runs", "latest activity", "/demos/api/workspace/detail?item=runs")
            .addItem("settings", "Settings", "configuration", "/demos/api/workspace/detail?item=settings")
            .withDetail(workspaceDetail("workspace"));

        TimelineTranscriptModule transcriptModule = TimelineTranscriptModule.create()
            .withModuleId("assistant-transcript-demo")
            .withTitle("TimelineTranscriptModule")
            .withDescription("Transcript entries can include status badges and embedded disclosure blocks.")
            .withTranscript(transcript(List.of(
                new DemoTranscriptEntry(
                    "demo-1",
                    "Plan accepted",
                    "The assistant selected a two-step implementation path.",
                    "Manager",
                    "09:30",
                    "ready",
                    List.of(new DemoBlock(
                        "block-1",
                        "Plan detail",
                        "markdown",
                        true,
                        new Markdown("- Inspect APIs\n- Add demo coverage\n- Validate fragments")
                    ))
                ),
                new DemoTranscriptEntry(
                    "demo-2",
                    "Worker update",
                    "Demo fragments are rendered server-side and swapped with HTMX.",
                    "Worker",
                    "09:42",
                    "running",
                    List.of()
                )
            )));

        return Page.builder()
            .addComponents(Header.H1("Modules Library"))

            .addRow(row -> row.withJustify("center").withChild(centered(heroModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(contentModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(richContentModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(formModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(dataModule)))
            .addRow(row -> row.withJustify("center").withChild(centered(galleryModule)))
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
            .addRow(row -> row.withJustify("center").withChild(centered(assistantWorkspace)))
            .addRow(row -> row.withJustify("center").withChild(centered(workspaceBrowser)))
            .addRow(row -> row.withJustify("center").withChild(centered(transcriptModule)))

            .build()
            .render();
    }

    private Column centered(io.mindspice.simplypages.core.Component component) {
        return new Column().withWidth(10).withChild(component);
    }

    public static io.mindspice.simplypages.core.Component assistantRoomFragment(String room) {
        String normalizedRoom = room == null || room.isBlank() ? "planning" : room;
        return new Div()
            .withId("assistant-room-fragment")
            .withChild(new HtmlTag("h3").withInnerText(roomTitle(normalizedRoom)))
            .withChild(transcript(roomEntries(normalizedRoom)));
    }

    public static io.mindspice.simplypages.core.Component assistantStatusPanel() {
        return PollingPanel.create("assistant-status-panel", "/demos/api/assistant/status")
            .everySeconds(15)
            .withChild(new HtmlTag("h3").withInnerText("Status"))
            .withChild(new Paragraph("PollingPanel refreshes this server-owned status fragment."))
            .withChild(StatusBadge.busy("Polling"));
    }

    public static io.mindspice.simplypages.core.Component assistantTabPanel(String tab) {
        String normalizedTab = tab == null || tab.isBlank() ? "summary" : tab;
        return switch (normalizedTab) {
            case "tools" -> new Div()
                .withClass("assistant-tab-panel-body")
                .withChild(StatusBadge.info("3 tools"))
                .withChild(new Paragraph("Tool availability, permissions, and execution status stay app-owned."));
            case "activity" -> new Div()
                .withClass("assistant-tab-panel-body")
                .withChild(StatusBadge.busy("Live"))
                .withChild(new Paragraph("Activity fragments can be swapped independently from the chat transcript."));
            default -> new Div()
                .withClass("assistant-tab-panel-body")
                .withChild(StatusBadge.success("Stable"))
                .withChild(new Paragraph("This tab panel demonstrates HtmxTabNav targeting a reusable content region."));
        };
    }

    public static io.mindspice.simplypages.core.Component workspaceDetail(String item) {
        String normalizedItem = item == null || item.isBlank() ? "workspace" : item;
        return switch (normalizedItem) {
            case "runs" -> detailPanel("Runs", "Latest run: demo-validation, status: complete", StatusBadge.success("Complete"));
            case "settings" -> detailPanel("Settings", "Model, tools, and workspace policy are rendered as app-owned detail data.", StatusBadge.info("Configured"));
            default -> detailPanel("Workspace", "Files, outputs, and pinned context appear in the detail target.", StatusBadge.busy("Active"));
        };
    }

    private static io.mindspice.simplypages.core.Component transcript(List<DemoTranscriptEntry> entries) {
        return TimelineTranscriptRenderer.<DemoTranscriptEntry, Void>builder()
            .withEmptyStateText("No demo timeline entries.")
            .build()
            .render(entries, null);
    }

    private static List<DemoTranscriptEntry> roomEntries(String room) {
        return switch (room) {
            case "handoff" -> List.of(new DemoTranscriptEntry(
                "handoff-1",
                "Handoff ready",
                "The worker attached demo routes and validation notes.",
                "Worker",
                "10:10",
                "ready",
                List.of()
            ));
            case "review" -> List.of(new DemoTranscriptEntry(
                "review-1",
                "Review requested",
                "Validator should inspect module composition and HTMX attributes.",
                "Reviewer",
                "10:20",
                "waiting",
                List.of(new DemoBlock(
                    "review-block",
                    "Checklist",
                    "markdown",
                    false,
                    new Markdown("- Room list swaps\n- Polling status\n- Master detail fragments")
                ))
            ));
            default -> List.of(
                new DemoTranscriptEntry(
                    "planning-1",
                    "Planning started",
                    "A focused assistant workspace demo was selected for `/demos/modules`.",
                    "Assistant",
                    "10:00",
                    "running",
                    List.of()
                ),
                new DemoTranscriptEntry(
                    "planning-2",
                    "Context captured",
                    "Room, tab, status, transcript, and browser primitives are visible on one page.",
                    "Assistant",
                    "10:05",
                    "ready",
                    List.of(new DemoBlock(
                        "planning-context",
                        "Included primitives",
                        "markdown",
                        true,
                        new Markdown("`AssistantChatModule`, `ChatRoomListModule`, `PollingPanel`, `HtmxTabNav`, and `MasterDetailBrowserModule`.")
                    ))
                )
            );
        };
    }

    private static io.mindspice.simplypages.core.Component detailPanel(
        String title,
        String body,
        io.mindspice.simplypages.core.Component badge
    ) {
        return new Div()
            .withClass("workspace-detail-card")
            .withChild(new HtmlTag("h3").withInnerText(title))
            .withChild(badge)
            .withChild(new Paragraph(body));
    }

    private static String roomTitle(String room) {
        return switch (room) {
            case "handoff" -> "Handoff Room";
            case "review" -> "Review Room";
            default -> "Planning Room";
        };
    }
}
