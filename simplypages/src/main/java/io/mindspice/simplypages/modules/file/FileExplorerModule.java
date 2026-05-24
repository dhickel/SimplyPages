package io.mindspice.simplypages.modules.file;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.navigation.Breadcrumb;
import io.mindspice.simplypages.core.Module;

public class FileExplorerModule extends Module {
    protected final FileExplorerState state;
    protected final FileExplorerConfig config;
    protected final FileExplorerInspectorSpec inspectorSpec;

    protected FileExplorerModule(FileExplorerState state, FileExplorerConfig config, FileExplorerInspectorSpec inspectorSpec) {
        super("section");
        this.state = state;
        this.config = config;
        this.inspectorSpec = inspectorSpec == null ? FileExplorerInspectorSpec.defaults() : inspectorSpec;
        withClass("file-explorer-module");
    }

    public static FileExplorerModule create(FileExplorerState state, FileExplorerEndpoints endpoints) {
        return new FileExplorerModule(state, FileExplorerConfig.defaults(endpoints), FileExplorerInspectorSpec.defaults());
    }

    public static FileExplorerModule create(FileExplorerState state, FileExplorerConfig config) {
        return new FileExplorerModule(state, config, FileExplorerInspectorSpec.defaults());
    }

    @Override
    protected void buildContent() {
        withId(config.rootId());
        withChild(buildToolbar());
        withChild(buildBreadcrumb());
        Div body = new Div().withClass("file-explorer-body");
        body.withChild(buildListPane()).withChild(buildInspectorPane()).withChild(buildViewerPane());
        withChild(body);
        withChild(new Div().withId(config.modalContainerId()).withClass("file-explorer-modal-container"));
    }

    private Div buildToolbar() {
        Div toolbar = new Div().withClass("file-explorer-toolbar");
        toolbar.withChild(Header.H3(state.rootLabel() == null ? "File Explorer" : state.rootLabel()));
        if (state.currentPath() != null) {
            toolbar.withChild(new Paragraph(state.currentPath()).withClass("file-explorer-current-path"));
        }
        String currentPath = state.currentPath() == null || state.currentPath().isBlank() ? "." : state.currentPath();
        String listEndpoint = config.endpoints().list(currentPath);
        if (listEndpoint != null && !listEndpoint.isBlank()) {
            toolbar.withChild(Button.create("Refresh").withStyle(Button.ButtonStyle.SECONDARY).small()
                .withAttribute("hx-get", listEndpoint)
                .withAttribute("hx-target", "#" + config.listTargetId())
                .withAttribute("hx-swap", "outerHTML"));
        }
        if (config.allowCreateFolder()) {
            toolbar.withChild(modalButton("New Folder", "create-folder", currentPath));
        }
        if (config.allowCreateText()) {
            toolbar.withChild(modalButton("New Text", "create-text", currentPath));
        }
        if (config.allowCreateMarkdown()) {
            toolbar.withChild(modalButton("New Markdown", "create-markdown", currentPath));
        }
        if (state.toolbarActions() != null) {
            for (FileExplorerAction action : state.toolbarActions()) {
                toolbar.withChild(actionButton(action));
            }
        }
        return toolbar;
    }

    private Breadcrumb buildBreadcrumb() {
        Breadcrumb breadcrumb = Breadcrumb.create().withClass("file-explorer-breadcrumb");
        if (state.breadcrumbs() != null) {
            for (FileBreadcrumbItem item : state.breadcrumbs()) {
                if (item.active()) { breadcrumb.addActiveItem(item.label()); }
                else { breadcrumb.addItem(item.label(), config.endpoints().navigate(item.path())); }
            }
        }
        return breadcrumb;
    }

    private Div buildListPane() {
        Div list = new Div().withId(config.listTargetId()).withClass("file-explorer-list-pane file-explorer-" + config.explorerMode().name().toLowerCase());
        if (state.entries() == null || state.entries().isEmpty()) { return list.withChild(new Paragraph("No entries available.")); }
        for (FileEntryView entry : state.entries()) {
            Div row = new Div().withClass("file-explorer-entry" + (entry.selected() ? " selected" : ""));
            String openEndpoint = entry.directory() ? config.endpoints().navigate(entry.path()) : config.endpoints().view(entry.path());
            Button open = Button.create(entry.directory() ? "Open Folder" : "Open").withStyle(Button.ButtonStyle.SECONDARY).small();
            if (openEndpoint != null) {
                open.withAttribute("hx-get", openEndpoint)
                    .withAttribute("hx-target", entry.directory() ? "#" + config.listTargetId() : "#" + config.viewerTargetId())
                    .withAttribute("hx-swap", "outerHTML");
            }
            Button inspect = Button.create("Inspect").withStyle(Button.ButtonStyle.SECONDARY).small();
            if (config.endpoints().inspect(entry.path()) != null) {
                inspect.withAttribute("hx-get", config.endpoints().inspect(entry.path()))
                    .withAttribute("hx-target", "#" + config.inspectorTargetId())
                    .withAttribute("hx-swap", "outerHTML");
            }
            Div text = new Div().withClass("file-entry-text").withChild(Header.H4(entry.name())).withChild(new Paragraph(entry.summary() == null ? "" : entry.summary()));
            if (entry.sizeLabel() != null) { text.withChild(new Paragraph(entry.sizeLabel()).withClass("file-entry-size")); }
            Div tags = new Div().withClass("file-entry-tags");
            if (config.allowTags() && entry.tags() != null) { for (String tag : entry.tags()) { tags.withChild(new Div().withClass("tag").withInnerText(tag)); } }
            Div actions = new Div().withClass("file-entry-actions").withChild(open).withChild(inspect);
            if (config.allowDelete() && config.endpoints().modal(entry.path()) != null) {
                actions.withChild(modalButton("Delete", "delete", entry.path()));
            }
            if (config.allowRename() && config.endpoints().modal("rename", entry.path()) != null) {
                actions.withChild(modalButton("Rename", "rename", entry.path()));
            }
            if (config.allowCopyMove()) {
                if (config.endpoints().action("copy", entry.path()) != null) {
                    actions.withChild(Button.create("Copy").small()
                        .withAttribute("hx-get", config.endpoints().action("copy", entry.path()))
                        .withAttribute("hx-target", "#" + config.modalContainerId())
                        .withAttribute("hx-swap", "innerHTML"));
                }
                if (config.endpoints().action("move", entry.path()) != null) {
                    actions.withChild(Button.create("Move").small()
                        .withAttribute("hx-get", config.endpoints().action("move", entry.path()))
                        .withAttribute("hx-target", "#" + config.modalContainerId())
                        .withAttribute("hx-swap", "innerHTML"));
                }
            }
            if (entry.actions() != null) { for (FileExplorerAction action : entry.actions()) { actions.withChild(actionButton(action)); } }
            row.withChild(text).withChild(tags).withChild(actions);
            list.withChild(row);
        }
        return list;
    }

    private Div buildInspectorPane() {
        Div inspector = new Div().withId(config.inspectorTargetId()).withClass("file-explorer-inspector-pane").withChild(Header.H4(inspectorSpec.title()));
        if (state.inspectorContent() != null) { return inspector.withChild(state.inspectorContent()); }
        if (state.selectedEntry() != null) { return inspector.withChild(new Paragraph(state.selectedEntry().path())).withChild(new Paragraph(state.selectedEntry().type())); }
        return inspector.withChild(new Paragraph(inspectorSpec.emptyMessage()));
    }

    private Div buildViewerPane() {
        Div viewer = new Div().withId(config.viewerTargetId()).withClass("file-explorer-viewer-pane").withChild(Header.H4("Viewer"));
        return state.viewerContent() == null ? viewer.withChild(new Paragraph("Select a file to preview.")) : viewer.withChild(state.viewerContent());
    }

    protected Button actionButton(FileExplorerAction action) {
        Button button = Button.create(action.label()).small();
        if (action.cssClass() != null && !action.cssClass().isBlank()) { button.withClass(action.cssClass()); }
        if (action.endpoint() != null && !action.endpoint().isBlank()) { button.withAttribute("hx-" + action.method(), action.endpoint()); }
        if (action.hxTarget() != null && !action.hxTarget().isBlank()) { button.withAttribute("hx-target", action.hxTarget()); }
        if (action.hxSwap() != null && !action.hxSwap().isBlank()) { button.withAttribute("hx-swap", action.hxSwap()); }
        if (action.hxConfirm() != null && !action.hxConfirm().isBlank()) { button.withAttribute("hx-confirm", action.hxConfirm()); }
        return button;
    }

    protected Button modalButton(String label, String action, String path) {
        Button button = Button.create(label).small();
        String endpoint = config.endpoints().modal(action, path);
        if (endpoint != null && !endpoint.isBlank()) {
            button.withAttribute("hx-get", endpoint)
                .withAttribute("hx-target", "#" + config.modalContainerId())
                .withAttribute("hx-swap", "innerHTML");
        }
        return button;
    }

    public FileExplorerModule withPaneIds(String rootId, String listPaneId, String inspectorPaneId, String viewerPaneId, String modalContainerId) {
        return new FileExplorerModule(state, new FileExplorerConfig(
            config.endpoints(),
            config.explorerMode(),
            config.pickerMode(),
            rootId,
            listPaneId,
            inspectorPaneId,
            viewerPaneId,
            modalContainerId,
            config.pickerCallbackTargetId(),
            config.allowCreateFolder(),
            config.allowCreateText(),
            config.allowCreateMarkdown(),
            config.allowRename(),
            config.allowDelete(),
            config.allowCopyMove(),
            config.allowTags()
        ), inspectorSpec);
    }
}
