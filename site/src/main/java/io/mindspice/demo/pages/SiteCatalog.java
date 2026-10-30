package io.mindspice.demo.pages;

import io.mindspice.demo.SiteShell;
import io.mindspice.simplypages.builders.*;
import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.display.*;
import io.mindspice.simplypages.components.forms.*;
import io.mindspice.simplypages.components.media.*;
import io.mindspice.simplypages.components.navigation.*;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.layout.*;
import io.mindspice.simplypages.modules.*;

import java.util.*;
import java.util.function.Supplier;

/** Explicit examples are also the catalog's coverage manifest. No classpath-generated claims. */
public final class SiteCatalog {
    public record Example(Class<?> symbol, String category, String route, String id, String purpose,
                          String source, Supplier<Component> preview, String docs) {
        public String api() { return "/javadocs/" + symbol.getName().replace('.', '/') + ".html"; }
        public String destination() { return route + "#" + id; }
    }
    private static final String CATALOG = "/docs/reference/components-and-modules-catalog.md";
    private static final String BUILDERS = "/docs/reference/builders-shell-nav-banner-accountbar.md";
    private static final List<Example> EXAMPLES = new ArrayList<>();
    static {
        String text = "/demos/components/text-structure";
        add(Div.class, "Text & structure", text, "Group related components without inserting trusted HTML.", "new Div().withChild(new Paragraph(\"A composed block\"))", () -> new Div().withChild(new Paragraph("A composed block")));
        add(Header.class, "Text & structure", text, "Choose a heading level appropriate to the document outline.", "Header.H3(\"A section heading\")", () -> Header.H3("A section heading"));
        add(Paragraph.class, "Text & structure", text, "Plain text is escaped, including angle brackets and ampersands.", "new Paragraph(\"Java < HTML & back again\")", () -> new Paragraph("Java < HTML & back again"));
        add(TextNode.class, "Text & structure", text, "Compose inline text with another component.", "new Paragraph().withChild(new TextNode(\"Use \" )).withChild(Code.inline(\"RenderContext\"))", () -> new Paragraph().withChild(new TextNode("Use ")).withChild(Code.inline("RenderContext")));
        add(RawHtml.class, "Text & structure", text, "Only application-owned trusted markup belongs in RawHtml.", "new RawHtml(\"<p><strong>Trusted application markup</strong></p>\")", () -> new RawHtml("<p><strong>Trusted application markup</strong></p>"));
        add(Markdown.class, "Text & structure", text, "Render structured prose using safe Markdown defaults.", "new Markdown(\"**Compose**, then render.\\n\\n- Java\\n- HTML\")", () -> new Markdown("**Compose**, then render.\n\n- Java\n- HTML"));
        add(Code.class, "Text & structure", text, "Inline and titled, language-marked block code.", "new Div().withChild(Code.inline(\"SlotKey<String>\")).withChild(Code.block(\"page.render(context);\").withLanguage(\"java\").withTitle(\"Render.java\"))", () -> new Div().withChild(Code.inline("SlotKey<String>" )).withChild(Code.block("page.render(context);").withLanguage("java").withTitle("Render.java")));
        add(Blockquote.class, "Text & structure", text, "A quotation within surrounding content.", "Blockquote.create(\"Keep request data in the render context.\")", () -> Blockquote.create("Keep request data in the render context."));
        add(Divider.class, "Text & structure", text, "Horizontal and vertical dividers derive their styling at render time.", "new Div().withChild(new Paragraph(\"Above\")).withChild(Divider.horizontal()).withChild(new Paragraph(\"Below\")).withChild(Divider.vertical())", () -> new Div().withChild(new Paragraph("Above")).withChild(Divider.horizontal()).withChild(new Paragraph("Below")).withChild(Divider.vertical()));
        add(Spacer.class, "Text & structure", text, "Explicit spacing between composed elements.", "new Div().withChild(new Paragraph(\"Before\")).withChild(Spacer.vertical().small()).withChild(new Paragraph(\"After\"))", () -> new Div().withChild(new Paragraph("Before")).withChild(Spacer.vertical().small()).withChild(new Paragraph("After")));
        add(ListItem.class, "Text & structure", text, "List items compose inside semantic lists.", "new HtmlTag(\"ul\").withChild(ListItem.create(\"Compose\")).withChild(ListItem.create(\"Render\"))", () -> new HtmlTag("ul").withChild(ListItem.create("Compose")).withChild(ListItem.create("Render")));

        String forms = "/demos/components/forms";
        add(Form.class, "Forms", forms, "Submit a real protected POST. Without JavaScript the same action returns server-rendered HTML.", "previewForm()", SiteCatalog::previewForm);
        add(TextInput.class, "Forms", forms, "Typed fields use native browser validation.", "Form.create().addField(\"Email\", TextInput.email(\"sample-email\")).addField(\"Date\", TextInput.date(\"sample-date\")).addField(\"Number\", TextInput.number(\"sample-number\")).addField(\"Password specimen (not stored)\", TextInput.password(\"sample-password\"))", () -> Form.create().addField("Email", TextInput.email("sample-email")).addField("Date", TextInput.date("sample-date")).addField("Number", TextInput.number("sample-number")).addField("Password specimen (not stored)", TextInput.password("sample-password")));
        add(TextArea.class, "Forms", forms, "Native multiline values, including a leading line break.", "Form.create().addField(\"Notes\", TextArea.create(\"notes\").withValue(\"\\nA leading line break\").withRows(3))", () -> Form.create().addField("Notes", TextArea.create("notes").withValue("\nA leading line break").withRows(3)));
        add(Select.class, "Forms", forms, "A native single-select choice.", "Form.create().addField(\"Priority\", Select.create(\"priority\").addOption(\"normal\", \"Normal\", true).addOption(\"high\", \"High\"))", () -> Form.create().addField("Priority", Select.create("priority").addOption("normal", "Normal", true).addOption("high", "High")));
        add(Checkbox.class, "Forms", forms, "A labeled native checkbox.", "Checkbox.create(\"notify\", \"yes\").withLabel(\"Notify reviewers\")", () -> Checkbox.create("notify", "yes").withLabel("Notify reviewers"));
        add(RadioGroup.class, "Forms", forms, "Mutually exclusive native choices.", "Form.create().addField(\"Delivery\", RadioGroup.create(\"delivery\").addOption(\"summary\", \"Summary\").addOption(\"all\", \"All updates\").withSelectedValue(\"summary\"))", () -> Form.create().addField("Delivery", RadioGroup.create("delivery").addOption("summary", "Summary").addOption("all", "All updates").withSelectedValue("summary")));
        add(Button.class, "Forms", forms, "A button performs a real fragment request; submit buttons are shown in the Form example.", "new Div().withChild(Button.create(\"Request server time\").withAttribute(\"hx-get\", \"/demos/fragments/time\").withAttribute(\"hx-target\", \"#button-result\")).withChild(new Div().withId(\"button-result\").withAttribute(\"role\", \"status\"))", () -> new Div().withChild(Button.create("Request server time").withAttribute("hx-get", "/demos/fragments/time").withAttribute("hx-target", "#button-result")).withChild(new Div().withId("button-result").withAttribute("role", "status")));

        String display = "/demos/components/display-data";
        add(Alert.class, "Display & data", display, "Informational, success, warning and danger states.", "new Div().withChild(Alert.info(\"Information\")).withChild(Alert.success(\"Saved\")).withChild(Alert.warning(\"Review needed\")).withChild(Alert.danger(\"Validation failed\"))", () -> new Div().withChild(Alert.info("Information")).withChild(Alert.success("Saved")).withChild(Alert.warning("Review needed")).withChild(Alert.danger("Validation failed")));
        add(Badge.class, "Display & data", display, "Short status labels with explicit text.", "new Paragraph().withChild(Badge.info(\"Draft\")).withChild(Badge.success(\"Reviewed\"))", () -> new Paragraph().withChild(Badge.info("Draft")).withChild(Badge.success("Reviewed")));
        add(io.mindspice.simplypages.components.display.Tag.class, "Display & data", display, "A content category, not a clickable filter.", "Tag.create(\"Java\").withColor(\"success\")", () -> io.mindspice.simplypages.components.display.Tag.create("Java").withColor("success"));
        add(Label.class, "Display & data", display, "A compact textual label.", "Label.create(\"Server-rendered\")", () -> Label.create("Server-rendered"));
        add(Spinner.class, "Display & data", display, "A visible loading indicator; this specimen is not a background request.", "Spinner.create().small()", () -> Spinner.create().small());
        add(ProgressBar.class, "Display & data", display, "A 72 percent specimen with semantic progress information.", "ProgressBar.create(72).success()", () -> ProgressBar.create(72).success());
        add(InfoBox.class, "Display & data", display, "An explicitly illustrative metric, not site telemetry.", "InfoBox.create().withTitle(\"Example queue\").withValue(\"12\")", () -> InfoBox.create().withTitle("Example queue").withValue("12"));
        add(Card.class, "Display & data", display, "A header/body surface.", "Card.create().withHeader(\"Review queue\").withBody(\"Three example documents await review.\")", () -> Card.create().withHeader("Review queue").withBody("Three example documents await review."));
        add(CardGrid.class, "Display & data", display, "Cards composed into a responsive grid.", "CardGrid.create().withColumns(2).addCard(Card.create().withHeader(\"Draft\").withBody(\"Write\")).addCard(Card.create().withHeader(\"Review\").withBody(\"Read\"))", () -> CardGrid.create().withColumns(2).addCard(Card.create().withHeader("Draft").withBody("Write")).addCard(Card.create().withHeader("Review").withBody("Read")));
        add(Table.class, "Display & data", display, "A manually specified table.", "Table.create().withHeaders(\"Boundary\", \"Owner\").addRow(\"Escaping\", \"Framework\").addRow(\"Authorization\", \"Application\").bordered()", () -> Table.create().withHeaders("Boundary", "Owner").addRow("Escaping", "Framework").addRow("Authorization", "Application").bordered());
        add(DataTable.class, "Display & data", display, "Columns are typed functions over application data.", "DataTable.create(String.class).addColumn(\"Stage\", value -> value).withData(List.of(\"Compose\", \"Render\")).striped()", () -> DataTable.create(String.class).addColumn("Stage", value -> value).withData(List.of("Compose", "Render")).striped());
        add(OrderedList.class, "Display & data", display, "An ordered workflow.", "OrderedList.create().addItem(\"Compose\").addItem(\"Compile\").addItem(\"Render\")", () -> OrderedList.create().addItem("Compose").addItem("Compile").addItem("Render"));
        add(UnorderedList.class, "Display & data", display, "An unordered collection.", "UnorderedList.create().addItem(\"Java\").addItem(\"HTML\")", () -> UnorderedList.create().addItem("Java").addItem("HTML"));
        add(Modal.class, "Display & data", display, "Load an actual framework modal, close it, and return focus to the trigger.", "Modal.create().withModalId(\"catalog-modal\").withTitle(\"A server-rendered dialog\").withBody(new Paragraph(\"Escape closes this dialog.\"))", () -> new Div().withChild(Button.create("Open dialog").withAttribute("hx-get", "/demos/fragments/modal").withAttribute("hx-target", "#catalog-modal-host")).withChild(new Div().withId("catalog-modal-host")));

        String navigation = "/demos/components/navigation";
        add(Link.class, "Navigation", navigation, "A real document link with a useful destination.", "Link.create(\"/docs/tutorials/first-page.md\", \"Build your first page\")", () -> Link.create("/docs/tutorials/first-page.md", "Build your first page"));
        add(Breadcrumb.class, "Navigation", navigation, "Semantic location within a hierarchy.", "Breadcrumb.create().addItem(\"Home\", \"/\").addItem(\"Demos\", \"/demos\").addActiveItem(\"Navigation\")", () -> Breadcrumb.create().addItem("Home", "/").addItem("Demos", "/demos").addActiveItem("Navigation"));
        add(NavBar.class, "Navigation", navigation, "Local specimen navigation uses real destinations.", "NavBar.create().withBrand(\"Example navigation\").addItem(\"Demos\", \"/demos\").addItem(\"Docs\", \"/docs\")", () -> NavBar.create().withBrand("Example navigation").addItem("Demos", "/demos").addItem("Docs", "/docs"));
        add(SideNav.class, "Navigation", navigation, "Grouped side navigation.", "SideNav.create().addSection(\"Learn\").addItem(\"Tutorials\", \"/docs/tutorials/README.md\").addItem(\"Reference\", \"/docs/reference/README.md\")", () -> SideNav.create().addSection("Learn").addItem("Tutorials", "/docs/tutorials/README.md").addItem("Reference", "/docs/reference/README.md"));
        add(Dropdown.class, "Navigation", navigation, "Open the menu with mouse or keyboard.", "Dropdown.create(\"Learning resources\").addLink(\"Tutorials\", \"/docs/tutorials/README.md\").addLink(\"Reference\", \"/docs/reference/README.md\").build()", () -> Dropdown.create("Learning resources").addLink("Tutorials", "/docs/tutorials/README.md").addLink("Reference", "/docs/reference/README.md").build());
        add(AccountWidget.class, "Navigation", navigation, "A simulated display widget, not authentication. Its profile link explains the application boundary.", "AccountWidget.createAuthenticated(\"Simulated Ada\").withProfileUrl(\"/docs/security/02-authwrapper-authorizationchecker-integration.md\").withLogoutUrl(\"/demos/components/navigation?persona=guest#accountwidget\")", () -> AccountWidget.createAuthenticated("Simulated Ada").withProfileUrl("/docs/security/02-authwrapper-authorizationchecker-integration.md").withLogoutUrl("/demos/components/navigation?persona=guest#accountwidget"));
        add(Icon.class, "Navigation", navigation, "A local CSS icon needs no external font library.", "Icon.custom(\"sp-site-leaf\").withAriaLabel(\"Leaf\")", () -> Icon.custom("sp-site-leaf").withAriaLabel("Leaf"));

        String media = "/demos/components/media";
        add(Image.class, "Media", media, "Local image with meaningful alternative text.", "Image.create(\"/media/landscape.svg\", \"Abstract forest and warm sun\")", () -> Image.create("/media/landscape.svg", "Abstract forest and warm sun"));
        add(Gallery.class, "Media", media, "A local image collection; no remote image service.", "Gallery.create().withColumns(2).addImage(\"/media/landscape.svg\", \"Forest\").addImage(\"/media/study.svg\", \"Geometric study\")", () -> Gallery.create().withColumns(2).addImage("/media/landscape.svg", "Forest").addImage("/media/study.svg", "Geometric study"));
        add(Audio.class, "Media", media, "Play a two-second locally generated tone. Playback is user-initiated.", "Audio.create(\"/media/tone.wav\").withControls()", () -> Audio.create("/media/tone.wav").withControls());
        add(Video.class, "Media", media, "Play a two-second local silent geometric animation.", "Video.create(\"/media/study.mp4\").withControls().withWidth(480)", () -> Video.create("/media/study.mp4").withControls().withWidth(480));

        String layout = "/demos/layout";
        add(Container.class, "Layout", layout, "A constrained content container.", "Container.create().withChild(new Paragraph(\"Contained text\"))", () -> Container.create().withChild(new Paragraph("Contained text")));
        add(Section.class, "Layout", layout, "A semantic section around related content.", "Section.create().withChild(Header.H3(\"Section heading\")).withChild(new Paragraph(\"Section content\"))", () -> Section.create().withChild(Header.H3("Section heading")).withChild(new Paragraph("Section content")));
        add(Row.class, "Layout", layout, "A row owns responsive column sizing.", "new Row().withChild(new Column().withWidth(6).withChild(Alert.info(\"Six columns\"))).withChild(new Column().withWidth(6).withChild(Alert.success(\"Six columns\")))", () -> new Row().withChild(new Column().withWidth(6).withChild(Alert.info("Six columns"))).withChild(new Column().withWidth(6).withChild(Alert.success("Six columns"))));
        add(Column.class, "Layout", layout, "Modules are sized by their containing column, not module width setters.", "new Row().withChild(new Column().withWidth(8).withChild(ContentModule.create().withTitle(\"Eight columns\").withContent(\"Module sizing belongs here.\")))", () -> new Row().withChild(new Column().withWidth(8).withChild(ContentModule.create().withTitle("Eight columns").withContent("Module sizing belongs here."))));
        add(Grid.class, "Layout", layout, "A grid of composed components.", "Grid.create().withColumns(2).addItems(Alert.info(\"One\"), Alert.success(\"Two\"))", () -> Grid.create().withColumns(2).addItems(Alert.info("One"), Alert.success("Two")));
        add(Page.class, "Layout", layout, "Page is content composition, not a nested HTML document.", "Page.builder().addComponents(Header.H3(\"Page content\"), new Paragraph(\"Rendered inside this shell.\")).build()", () -> Page.builder().addComponents(Header.H3("Page content"), new Paragraph("Rendered inside this shell.")).build());

        module(AccordionModule.class, "accordion", "Expandable sections with keyboard-accessible controls.", "AccordionModule.create().withTitle(\"Rendering model\").addItem(\"Structure\", \"Build once.\").addItem(\"Request data\", \"Render with a context.\").withFirstExpanded()", () -> AccordionModule.create().withTitle("Rendering model").addItem("Structure", "Build once.").addItem("Request data", "Render with a context.").withFirstExpanded());
        module(CalloutModule.class, "callout", "A contextual notice composed as a module.", "CalloutModule.create().withTitle(\"Review complete\").withContent(\"This is illustrative content.\").success()", () -> CalloutModule.create().withTitle("Review complete").withContent("This is illustrative content.").success());
        module(ComparisonModule.class, "comparison", "Compare two approaches without inventing product tiers.", "ComparisonModule.create().withTitle(\"Rendering choices\").addColumn(\"Static\").addColumn(\"Context\", true).addRow(\"Values\", \"Fixed\", \"Per request\")", () -> ComparisonModule.create().withTitle("Rendering choices").addColumn("Static").addColumn("Context", true).addRow("Values", "Fixed", "Per request"));
        module(ContentModule.class, "content", "Markdown-backed prose with an explicit title.", "ContentModule.create().withTitle(\"A readable interface\").withContent(\"Compose **Java components** into HTML.\")", () -> ContentModule.create().withTitle("A readable interface").withContent("Compose **Java components** into HTML."));
        module(DataModule.class, "data", "A module wraps a typed table.", "DataModule.create(String.class).withTitle(\"Stages\").withDataTable(DataTable.create(String.class).addColumn(\"Stage\", value -> value).withData(List.of(\"Compose\", \"Render\")))", () -> DataModule.create(String.class).withTitle("Stages").withDataTable(DataTable.create(String.class).addColumn("Stage", value -> value).withData(List.of("Compose", "Render"))));
        module(DynamicCardModule.class, "dynamic-card", "Submit a value to rebuild this request-owned dynamic card.", "DynamicCardModule.create().withTitle(\"Dynamic card\").withCardContent(\"Request value\", value)", () -> dynamicPreview("dynamic-card"));
        module(DynamicListModule.class, "dynamic-list", "Submit comma-separated values to render a different list.", "DynamicListModule.create().withTitle(\"Dynamic list\").withListItems(List.of(value.split(\",\")))", () -> dynamicPreview("dynamic-list"));
        module(DynamicTableModule.class, "dynamic-table", "Submit a value to replace actual table data.", "DynamicTableModule.create().withTitle(\"Dynamic table\").withTableData(new String[]{\"Value\"}, Collections.singletonList(new String[]{value}))", () -> dynamicPreview("dynamic-table"));
        module(FormModule.class, "form", "A high-level form using the same protected preview endpoint.", "FormModule.create().withTitle(\"Contact preview\").withForm(previewForm())", () -> FormModule.create().withTitle("Contact preview").withForm(previewForm()));
        module(GalleryModule.class, "gallery", "A titled local media collection.", "GalleryModule.create().withTitle(\"Local studies\").withColumns(2).addImage(\"/media/landscape.svg\", \"Forest\").addImage(\"/media/study.svg\", \"Study\")", () -> GalleryModule.create().withTitle("Local studies").withColumns(2).addImage("/media/landscape.svg", "Forest").addImage("/media/study.svg", "Study"));
        module(HeroModule.class, "hero", "A composable introduction with a real destination.", "HeroModule.create().withTitle(\"Build in Java\").withSubtitle(\"Render HTML\").withDescription(\"Keep your interface close to your application.\").withPrimaryButton(\"Read the tutorial\", \"/docs/tutorials/first-page.md\").centered()", () -> HeroModule.create().withTitle("Build in Java").withSubtitle("Render HTML").withDescription("Keep your interface close to your application.").withPrimaryButton("Read the tutorial", "/docs/tutorials/first-page.md").centered());
        module(QuoteModule.class, "quote", "A quotation and attribution.", "QuoteModule.create().withTitle(\"Design note\").withQuote(\"Make the data flow explicit.\").withAuthor(\"Example author\").centered()", () -> QuoteModule.create().withTitle("Design note").withQuote("Make the data flow explicit.").withAuthor("Example author").centered());
        module(RichContentModule.class, "rich-content", "Mixed typed components in one module.", "RichContentModule.create(\"A composed article\").addHeader(Header.H3(\"Composition\")).addParagraph(new Paragraph(\"Small pieces, one document.\"))", () -> RichContentModule.create("A composed article").addHeader(Header.H3("Composition")).addParagraph(new Paragraph("Small pieces, one document.")));
        module(SimpleListModule.class, "simple-list", "List items retain raw identities; positional editable descriptors are separate.", "SimpleListModule.create().withTitle(\"Reading list\").addItem(ListItem.create(\"Components\")).addItem(ListItem.create(\"Templates\"))", () -> SimpleListModule.create().withTitle("Reading list").addItem(ListItem.create("Components")).addItem(ListItem.create("Templates")));
        module(StatsModule.class, "stats", "Illustrative values, not measured site statistics.", "StatsModule.create().withTitle(\"Example review queue\").withColumns(2).addStat(\"12\", \"Drafts\").addStat(\"4\", \"Reviewed\")", () -> StatsModule.create().withTitle("Example review queue").withColumns(2).addStat("12", "Drafts").addStat("4", "Reviewed"));
        module(TabsModule.class, "tabs", "Switch between keyboard-navigable content panels.", "TabsModule.create().withTitle(\"Request lifecycle\").addTab(\"Compose\", \"Build static structure.\").addTab(\"Render\", \"Supply typed context values.\")", () -> TabsModule.create().withTitle("Request lifecycle").addTab("Compose", "Build static structure.").addTab("Render", "Supply typed context values."));
        module(TimelineModule.class, "timeline", "An illustrative content workflow, not a project roadmap.", "TimelineModule.create().withTitle(\"Example workflow\").addEvent(\"1\", \"Draft\", \"Write content.\").addEvent(\"2\", \"Review\", \"Check the result.\")", () -> TimelineModule.create().withTitle("Example workflow").addEvent("1", "Draft", "Write content.").addEvent("2", "Review", "Check the result."));
        linkedModule(EditableModule.class, "editable", "/demos/features/editing", "Editable wrappers, permissions, nested children and proposal approval in your temporary sandbox.");
        linkedModule(ChatModule.class, "chat", "/demos/features/chat", "Actual transcript, composer, serialized updates and finite SSE in your own temporary conversation.");

        builder(BannerBuilder.class, "banner", "A standalone banner with restrained application-owned colors.", "BannerBuilder.create().withTitle(\"Field notes\").withSubtitle(\"A Java-rendered publication\").withBackgroundColor(\"#245C48\").withTextColor(\"#FFFFFF\").build()", () -> BannerBuilder.create().withTitle("Field notes").withSubtitle("A Java-rendered publication").withBackgroundColor("#245C48").withTextColor("#FFFFFF").build());
        builder(TopNavBuilder.class, "top-nav", "A full-document navigation specimen.", "TopNavBuilder.create().withBrand(\"Field notes\").withHtmxNavigation(false).addPrimaryLink(\"Docs\", \"/docs\").addUtilityLink(\"Java API\", \"/api\").build()", () -> TopNavBuilder.create().withBrand("Field notes").withHtmxNavigation(false).addPrimaryLink("Docs", "/docs").addUtilityLink("Java API", "/api").build());
        builder(SideNavBuilder.class, "side-nav", "The builder produces framework SideNav markup and fragment navigation.", "new Div().withChild(SideNavBuilder.create().addSection(\"Examples\").addLink(\"Server time\", \"/demos/fragments/time\").withContentTarget(\"builder-nav-target\").build()).withChild(new Div().withId(\"builder-nav-target\"))", () -> new Div().withChild(SideNavBuilder.create().addSection("Examples").addLink("Server time", "/demos/fragments/time").withContentTarget("builder-nav-target").build()).withChild(new Div().withId("builder-nav-target")));
        builder(AccountBarBuilder.class, "account-bar", "Utility links and a clearly simulated account display.", "AccountBarBuilder.create().addLeftLink(\"Documentation\", \"/docs\").addRightItem(new Paragraph(\"Simulated visitor, not signed in\")).build()", () -> AccountBarBuilder.create().addLeftLink("Documentation", "/docs").addRightItem(new Paragraph("Simulated visitor, not signed in")).build());
        builder(ShellBuilder.class, "shell", "Open a complete local document, isolated from this site's shell.", "ShellBuilder.create().withPageTitle(\"Local shell preview\").withContent(new Paragraph(\"A complete document.\")).build()", () -> Link.create("/demos/builders/preview/shell", "Open full ShellBuilder preview"));
        builder(ShellTemplate.class, "shell-template", "Compile a shell and render its typed content slot in a separate local document.", "ShellBuilder.create().withPageTitle(\"Template preview\").buildTemplate().renderWithContent(new Paragraph(\"Content supplied at render time.\"))", () -> Link.create("/demos/builders/preview/shell-template", "Open full ShellTemplate preview"));
    }

    private SiteCatalog() {}
    private static void add(Class<?> type, String category, String route, String purpose, String source, Supplier<Component> preview) {
        EXAMPLES.add(new Example(type, category, route, type.getSimpleName().toLowerCase(Locale.ROOT), purpose, source, preview, CATALOG));
    }
    private static void module(Class<?> type, String slug, String purpose, String source, Supplier<Component> preview) {
        add(type, "Modules", "/demos/modules/" + slug, purpose, source, preview);
    }
    private static void linkedModule(Class<?> type, String slug, String route, String purpose) {
        add(type, "Modules", route, purpose,
            type.getSimpleName() + (type == ChatModule.class ? ".create()" : ".wrap(contentModule)"),
            () -> Link.create(route, "Open working " + type.getSimpleName() + " example"));
    }
    private static void builder(Class<?> type, String slug, String purpose, String source, Supplier<Component> preview) {
        EXAMPLES.add(new Example(type, "Builders", "/demos/builders/" + slug, type.getSimpleName().toLowerCase(Locale.ROOT), purpose, source, preview, BUILDERS));
    }
    public static List<Example> examples() { return List.copyOf(EXAMPLES); }
    public static Set<String> routes() { return new LinkedHashSet<>(EXAMPLES.stream().map(Example::route).toList()); }

    public static String directory(String category) {
        Div root = new Div().withClass("sp-site-directory");
        var groups = new LinkedHashMap<String, List<Example>>();
        EXAMPLES.stream().filter(e -> category == null || e.category().equals(category))
            .forEach(e -> groups.computeIfAbsent(e.category(), key -> new ArrayList<>()).add(e));
        groups.forEach((name, entries) -> {
            Div section = new Div().withClass("sp-site-directory-group").withChild(Header.H2(name));
            for (Example entry : entries) section.withChild(Link.create(entry.destination(), entry.symbol().getSimpleName()));
            root.withChild(section);
        });
        return root.render();
    }

    public static String render(String route) { return render(route, "ada"); }
    public static String render(String route, String persona) {
        List<Example> selected = EXAMPLES.stream().filter(e -> e.route().equals(route)).toList();
        if (selected.isEmpty()) return null;
        String title = selected.size() == 1 ? selected.getFirst().symbol().getSimpleName() : selected.getFirst().category();
        String intro = (selected.getFirst().symbol() == HeroModule.class || selected.getFirst().symbol() == BannerBuilder.class)
            ? new Div().withClass("sp-site-intro").withChild(Link.create("/demos", "Demos /"))
                .withChild(new Paragraph(title + " · the specimen supplies this page's H1"))
                .withChild(new Paragraph("Working framework components, paired with the Java that composes them.")).render()
            : SiteShell.intro(title, "Working framework components, paired with the Java that composes them.");
        Div root = new Div().withChild(new RawHtml(intro));
        HtmlTag nav = new HtmlTag("nav").withClass("sp-site-local-nav").withAttribute("aria-label", "Examples on this page");
        selected.forEach(e -> nav.withChild(Link.create("#" + e.id(), e.symbol().getSimpleName())));
        root.withChild(nav);
        for (Example example : selected) {
            if (example.symbol() == AccountWidget.class && "guest".equals(persona)) {
                example = new Example(example.symbol(), example.category(), example.route(), example.id(),
                    "Simulated guest display only. Login switches this request's display back to Simulated Ada; it does not authenticate.",
                    "AccountWidget.createGuest().withLoginUrl(\"/demos/components/navigation?persona=ada#accountwidget\").withSignupUrl(null)",
                    () -> AccountWidget.createGuest().withLoginUrl("/demos/components/navigation?persona=ada#accountwidget").withSignupUrl(null), example.docs());
            }
            root.withChild(section(example));
        }
        return root.render();
    }

    public static Component section(Example e) {
        HtmlTag section = new HtmlTag("section").withId(e.id()).withClass("sp-site-example")
            .withAttribute("data-catalog-symbol", e.symbol().getName())
            .withChild(Header.H2(e.symbol().getSimpleName())).withChild(new Paragraph(e.purpose()))
            .withChild(new Div().withClass("sp-site-example-grid")
                .withChild(new Div().withClass("sp-site-preview").withAttribute("data-example-preview", "true").withChild(e.preview().get()))
                .withChild(Code.block(source(e)).withLanguage("java").withTitle("Java composition")))
            .withChild(new Div().withClass("sp-site-reference-links")
                .withChild(Link.create(e.api(), e.symbol().getSimpleName() + " API"))
                .withChild(Link.create(e.docs(), "Documentation")));
        if (e.category().equals("Builders")) section.withChild(Link.create(
            e.route().replace("/demos/builders/", "/demos/builders/preview/"), "Open isolated full-document preview →"));
        return section;
    }

    private static String source(Example e) {
        if (e.symbol() == ShellBuilder.class || e.symbol() == ShellTemplate.class) {
            boolean template = e.symbol() == ShellTemplate.class;
            String title = template ? "Template preview" : "Local shell preview";
            String body = template ? "Content supplied at render time." : "A complete document.";
            String kind = template ? "shell-template" : "shell";
            return "Component content = new Div().withChild(Header.H1(\"" + title + "\"))\n    .withChild(new Paragraph(\"" + body + "\"))\n    .withChild(Link.create(\"/demos/builders/" + kind + "\", \"Back to the builder example\"));\nShellBuilder shell = ShellBuilder.create().withPageTitle(\"" + title + "\")\n    .withSideNav(SideNavBuilder.create().addSection(\"Preview navigation\")\n        .addLink(\"Chat\", \"/chat\").addLink(\"Documentation\", \"/docs\").build(), true);\n"
                + (template ? "shell.buildTemplate().renderWithContent(content);" : "shell.withContent(content).build();");
        }
        if (e.symbol() == Form.class || e.symbol() == FormModule.class) return "Form form = Form.create()\n    .withAction(\"/demos/api/form-preview\").withMethod(Form.Method.POST)\n    .withHxPost(\"/demos/api/form-preview\").withHxTarget(\"#form-preview\").withHxSwap(\"outerHTML\")\n    .addField(\"Name\", TextInput.create(\"name\").required())\n    .addField(\"Email\", TextInput.email(\"email\").required())\n    .addField(\"Role\", Select.create(\"role\").addOption(\"engineer\", \"Engineer\").addOption(\"designer\", \"Designer\"))\n    .withChild(Button.submit(\"Render preview\"))\n    .withChild(new Div().withId(\"form-preview\").withAttribute(\"role\", \"status\")\n        .withInnerText(\"Your server-rendered result appears here.\"));\n// Application SSR adds one _csrf field and inherited literal hx-headers.\n" + (e.symbol() == FormModule.class ? "FormModule.create().withTitle(\"Contact preview\").withForm(form);" : "form.render();");
        if (e.symbol() == Modal.class) return "new Div().withChild(Button.create(\"Open dialog\")\n    .withAttribute(\"hx-get\", \"/demos/fragments/modal\").withAttribute(\"hx-target\", \"#catalog-modal-host\"))\n    .withChild(new Div().withId(\"catalog-modal-host\"));\n// GET /demos/fragments/modal returns:\n" + e.source() + ".render();";
        if (Set.of(DynamicCardModule.class, DynamicListModule.class, DynamicTableModule.class).contains(e.symbol())) {
            String kind = e.route().substring(e.route().lastIndexOf('/') + 1);
            return "String value = \"Compose, Render\";\nComponent module = " + e.source() + ";\nnew Div().withChild(Form.create().withAction(\"/demos/api/dynamic\").withMethod(Form.Method.POST)\n    .withHxPost(\"/demos/api/dynamic\").withHxTarget(\"#dynamic-result\").withHxSwap(\"outerHTML\")\n    .withChild(new HtmlTag(\"input\", true).withAttribute(\"type\", \"hidden\")\n        .withAttribute(\"name\", \"kind\").withAttribute(\"value\", \"" + kind + "\"))\n    .addField(\"Value\", TextInput.create(\"value\").withValue(value).required())\n    .withChild(Button.submit(\"Update " + kind + "\")))\n    .withChild(new Div().withId(\"dynamic-result\").withAttribute(\"aria-live\", \"polite\").withChild(module));\n// POST /demos/api/dynamic selects the same module with the submitted value.";
        }
        return e.source() + ";";
    }

    public static Form previewForm() {
        return Form.create().withAction("/demos/api/form-preview").withMethod(Form.Method.POST)
            .withHxPost("/demos/api/form-preview").withHxTarget("#form-preview").withHxSwap("outerHTML")
            .addField("Name", TextInput.create("name").required())
            .addField("Email", TextInput.email("email").required())
            .addField("Role", Select.create("role").addOption("engineer", "Engineer").addOption("designer", "Designer"))
            .withChild(Button.submit("Render preview"))
            .withChild(new Div().withId("form-preview").withAttribute("role", "status").withInnerText("Your server-rendered result appears here."));
    }

    private static Component dynamicPreview(String kind) {
        return new Div().withChild(Form.create().withAction("/demos/api/dynamic").withMethod(Form.Method.POST)
            .withHxPost("/demos/api/dynamic").withHxTarget("#dynamic-result").withHxSwap("outerHTML")
            .withChild(new HtmlTag("input", true).withAttribute("type", "hidden").withAttribute("name", "kind").withAttribute("value", kind))
            .addField("Value", TextInput.create("value").withValue("Compose, Render").required())
            .withChild(Button.submit("Update " + kind)))
            .withChild(new RawHtml(dynamic(kind, "Compose, Render")));
    }
    public static String dynamic(String kind, String value) {
        Component module = switch (kind) {
            case "dynamic-card" -> DynamicCardModule.create().withTitle("Dynamic card").withCardContent("Request value", value);
            case "dynamic-list" -> DynamicListModule.create().withTitle("Dynamic list").withListItems(List.of(value.split(",")));
            case "dynamic-table" -> DynamicTableModule.create().withTitle("Dynamic table").withTableData(new String[]{"Value"}, Collections.singletonList(new String[]{value}));
            default -> throw new IllegalArgumentException("Unknown dynamic example");
        };
        return new Div().withId("dynamic-result").withAttribute("aria-live", "polite").withChild(module).render();
    }
}
