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
        List<FileEntryView> entries = demoEntries("/workspace/docs");
        FileExplorerState state = new FileExplorerState("Demo File Explorer", "/workspace/docs",
            List.of(new FileBreadcrumbItem("workspace", "/workspace", false), new FileBreadcrumbItem("docs", "/workspace/docs", true)),
            entries, entries.getFirst(), List.of(), new Paragraph("Inspector content supplied by app."), new Paragraph("Viewer content supplied by app."), false, null);
        FileExplorerEndpoints endpoints = new FileExplorerEndpoints(
            "/demos/file-explorer/list",
            "/demos/file-explorer/list",
            "/demos/file-explorer/view",
            "/demos/file-explorer/inspect",
            "/demos/file-explorer/modal/{action}",
            "/demos/file-explorer/action/{action}",
            "/demos/file-picker/select"
        );
        FileExplorerConfig config = new FileExplorerConfig(
            endpoints,
            FileExplorerMode.CARDS,
            FilePickerMode.FILES_OR_DIRECTORIES,
            "demo-file-explorer-root",
            "demo-file-explorer-list",
            "demo-file-explorer-inspector",
            "demo-file-explorer-viewer",
            "demo-file-explorer-modal",
            "demo-file-picker-value",
            true, true, true, true, true, true, true
        );
        FileExplorerModule module = FileExplorerModule.create(state, config);
        return Page.builder().addComponents(module).build().render();
    }
    public String renderPicker() {
        return renderPicker(FilePickerMode.FILES_OR_DIRECTORIES);
    }
    public String renderPicker(FilePickerMode pickerMode) {
        List<FileEntryView> entries = demoEntries("/workspace");
        FileExplorerState state = new FileExplorerState("Demo File Picker", "/workspace", List.of(new FileBreadcrumbItem("workspace", "/workspace", true)),
            entries, entries.get(1), List.of(), new Paragraph("Choose a file or folder."), null, true, "/workspace/docs/readme.md");
        FileExplorerEndpoints endpoints = new FileExplorerEndpoints(
            "/demos/file-picker/list",
            "/demos/file-picker/list",
            "/demos/file-picker/view",
            "/demos/file-picker/inspect",
            "/demos/file-picker/modal/{action}",
            "/demos/file-picker/action/{action}",
            "/demos/file-picker/select"
        );
        FileExplorerConfig config = new FileExplorerConfig(
            endpoints,
            FileExplorerMode.LIST,
            pickerMode,
            "demo-file-picker-root",
            "demo-file-picker-list",
            "demo-file-picker-inspector",
            "demo-file-picker-viewer",
            "demo-file-picker-modal",
            "demo-file-picker-value",
            false, false, false, false, true, false, true
        );
        FilePickerModule module = FilePickerModule.create(state, config);
        return Page.builder().addComponents(module).build().render();
    }
    public List<FileEntryView> demoEntries(String path) {
        if ("/workspace/images".equals(path)) {
            return List.of(
                new FileEntryView("3", "diagram.png", "/workspace/images/diagram.png", "image/png", "14 KB", "Image placeholder", List.of("assets", "image"), false, false, List.of()),
                new FileEntryView("4", "raw.bin", "/workspace/images/raw.bin", "application/octet-stream", "2 KB", "Binary placeholder", List.of("binary"), false, false, List.of())
            );
        }
        if ("/workspace".equals(path)) {
            return List.of(
                new FileEntryView("1", "docs", "/workspace/docs", "directory", null, "Documentation folder", List.of("notes"), true, false, List.of()),
                new FileEntryView("2", "images", "/workspace/images", "directory", null, "Image assets folder", List.of("assets"), true, true, List.of())
            );
        }
        return List.of(
            new FileEntryView("1", "readme.md", "/workspace/docs/readme.md", "text/markdown", "3 KB", "Quick start guide", List.of("docs", "markdown"), false, true, List.of()),
            new FileEntryView("2", "notes.txt", "/workspace/docs/notes.txt", "text/plain", "1 KB", "Plain text notes", List.of("notes"), false, false, List.of()),
            new FileEntryView("3", "images", "/workspace/images", "directory", null, "Folder", List.of("assets"), true, false, List.of())
        );
    }
}
