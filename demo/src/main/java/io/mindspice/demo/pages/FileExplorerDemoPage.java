package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.modules.file.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FileExplorerDemoPage implements DemoPage {
    @Override
    public String render() { return renderExplorer(); }
    public String renderExplorer() {
        List<FileEntryView> entries = demoEntries();
        FileExplorerState state = new FileExplorerState("Demo File Explorer", "/workspace/docs",
            List.of(new FileBreadcrumbItem("workspace", "/workspace", false), new FileBreadcrumbItem("docs", "/workspace/docs", true)),
            entries, entries.getFirst(), List.of(), new Paragraph("Inspector content supplied by app."), new Paragraph("Viewer content supplied by app."), false, null);
        FileExplorerModule module = FileExplorerModule.create(state, new FileExplorerEndpoints("/demos/file-explorer/list", "/demos/file-explorer/inspect", "/demos/file-explorer/view", "/demos/file-picker/select"))
            .withPaneIds("demo-file-explorer-root", "demo-file-explorer-list", "demo-file-explorer-inspector", "demo-file-explorer-viewer", "demo-file-explorer-modal");
        return Page.builder().addComponents(module).build().render();
    }
    public String renderPicker() {
        List<FileEntryView> entries = demoEntries();
        FileExplorerState state = new FileExplorerState("Demo File Picker", "/workspace", List.of(new FileBreadcrumbItem("workspace", "/workspace", true)),
            entries, entries.get(1), List.of(), new Paragraph("Choose a file or folder."), null, true, "/workspace/docs/readme.md");
        FilePickerModule module = FilePickerModule.create(state, new FileExplorerEndpoints("/demos/file-explorer/list", "/demos/file-explorer/inspect", "/demos/file-explorer/view", "/demos/file-picker/select"))
            .withPickerTargetId("demo-file-picker-value");
        module.withPaneIds("demo-file-picker-root", "demo-file-picker-list", "demo-file-picker-inspector", "demo-file-picker-viewer", "demo-file-picker-modal");
        return Page.builder().addComponents(module).build().render();
    }
    private List<FileEntryView> demoEntries() {
        return List.of(
            new FileEntryView("1", "readme.md", "/workspace/docs/readme.md", "text/markdown", "3 KB", "Quick start guide", List.of("docs", "markdown"), false, true, List.of()),
            new FileEntryView("2", "images", "/workspace/images", "directory", null, "Folder", List.of("assets"), true, false, List.of())
        );
    }
}
