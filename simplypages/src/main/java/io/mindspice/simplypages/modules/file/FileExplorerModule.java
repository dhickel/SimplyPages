package io.mindspice.simplypages.modules.file;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.navigation.Breadcrumb;
import io.mindspice.simplypages.core.Module;

public class FileExplorerModule extends Module {
    protected final FileExplorerState state;
    protected final FileExplorerEndpoints endpoints;
    protected final FileExplorerInspectorSpec inspectorSpec;
    protected String rootId = "file-explorer-root";
    protected String listPaneId = "file-explorer-list";
    protected String inspectorPaneId = "file-explorer-inspector";
    protected String viewerPaneId = "file-explorer-viewer";
    protected String modalContainerId = "file-explorer-modal";

    protected FileExplorerModule(FileExplorerState state, FileExplorerEndpoints endpoints, FileExplorerInspectorSpec inspectorSpec) {
        super("section");
        this.state = state;
        this.endpoints = endpoints;
        this.inspectorSpec = inspectorSpec == null ? FileExplorerInspectorSpec.defaults() : inspectorSpec;
        withClass("file-explorer-module");
    }

    public static FileExplorerModule create(FileExplorerState state, FileExplorerEndpoints endpoints) {
        return new FileExplorerModule(state, endpoints, FileExplorerInspectorSpec.defaults());
    }

    public FileExplorerModule withPaneIds(String rootId, String listPaneId, String inspectorPaneId, String viewerPaneId, String modalContainerId) {
        this.rootId = rootId;
        this.listPaneId = listPaneId;
        this.inspectorPaneId = inspectorPaneId;
        this.viewerPaneId = viewerPaneId;
        this.modalContainerId = modalContainerId;
        return this;
    }

    @Override
    protected void buildContent() {
        withId(rootId);
        withChild(buildToolbar());
        withChild(buildBreadcrumb());
        Div body = new Div().withClass("file-explorer-body");
        body.withChild(buildListPane()).withChild(buildInspectorPane()).withChild(buildViewerPane());
        withChild(body);
        withChild(new Div().withId(modalContainerId).withClass("file-explorer-modal-container"));
    }

    private Div buildToolbar() {
        Div toolbar = new Div().withClass("file-explorer-toolbar");
        toolbar.withChild(Header.H3(state.rootLabel() == null ? "File Explorer" : state.rootLabel()));
        if (state.currentPath() != null) {
            toolbar.withChild(new Paragraph(state.currentPath()).withClass("file-explorer-current-path"));
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
                if (item.active()) { breadcrumb.addActiveItem(item.label()); } else { breadcrumb.addItem(item.label(), withPath(endpoints.navigateEndpoint(), item.path())); }
            }
        }
        return breadcrumb;
    }

    private Div buildListPane() {
        Div list = new Div().withId(listPaneId).withClass("file-explorer-list-pane");
        if (state.entries() == null || state.entries().isEmpty()) { return list.withChild(new Paragraph("No entries available.")); }
        for (FileEntryView entry : state.entries()) {
            Div row = new Div().withClass("file-explorer-entry" + (entry.selected() ? " selected" : ""));
            String openEndpoint = entry.directory() ? endpoints.navigateEndpoint() : endpoints.viewerEndpoint();
            Button open = Button.create(entry.directory() ? "Open Folder" : "Open").withStyle(Button.ButtonStyle.SECONDARY).small();
            if (openEndpoint != null) {
                open.withAttribute("hx-get", withPath(openEndpoint, entry.path()))
                    .withAttribute("hx-target", entry.directory() ? "#" + listPaneId : "#" + viewerPaneId)
                    .withAttribute("hx-swap", "outerHTML");
            }
            Div text = new Div().withClass("file-entry-text").withChild(Header.H4(entry.name())).withChild(new Paragraph(entry.summary() == null ? "" : entry.summary()));
            if (entry.sizeLabel() != null) { text.withChild(new Paragraph(entry.sizeLabel()).withClass("file-entry-size")); }
            Div tags = new Div().withClass("file-entry-tags");
            if (entry.tags() != null) { for (String tag : entry.tags()) { tags.withChild(new Div().withClass("tag").withInnerText(tag)); } }
            Div actions = new Div().withClass("file-entry-actions").withChild(open);
            if (entry.actions() != null) { for (FileExplorerAction action : entry.actions()) { actions.withChild(actionButton(action)); } }
            row.withChild(text).withChild(tags).withChild(actions);
            list.withChild(row);
        }
        return list;
    }

    private Div buildInspectorPane() {
        Div inspector = new Div().withId(inspectorPaneId).withClass("file-explorer-inspector-pane").withChild(Header.H4(inspectorSpec.title()));
        if (state.inspectorContent() != null) { return inspector.withChild(state.inspectorContent()); }
        if (state.selectedEntry() != null) { return inspector.withChild(new Paragraph(state.selectedEntry().path())).withChild(new Paragraph(state.selectedEntry().type())); }
        return inspector.withChild(new Paragraph(inspectorSpec.emptyMessage()));
    }

    private Div buildViewerPane() {
        Div viewer = new Div().withId(viewerPaneId).withClass("file-explorer-viewer-pane").withChild(Header.H4("Viewer"));
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

    protected String withPath(String endpoint, String path) {
        if (endpoint == null || path == null) { return endpoint; }
        return endpoint + (endpoint.contains("?") ? "&" : "?") + "path=" + path;
    }
}
