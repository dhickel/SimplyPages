package io.mindspice.simplypages.modules.file;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FileExplorerModuleTest {
    @Test
    void rendersExplorerWithStableTargetsAndHtmxAttributes() {
        FileEntryView entry = new FileEntryView("1", "readme.md", "/docs/readme.md", "text", "1 KB", "Project README", List.of("docs"), false, true, List.of());
        FileExplorerState state = new FileExplorerState("Files", "/docs", List.of(new FileBreadcrumbItem("Root", "/", false)), List.of(entry), entry, List.of(), new Paragraph("Inspector"), new Paragraph("Viewer"), false, null);
        String html = FileExplorerModule.create(state, new FileExplorerEndpoints("/demo/nav", "/demo/inspect", "/demo/view", "/demo/select"))
            .withPaneIds("fx-root", "fx-list", "fx-inspector", "fx-viewer", "fx-modal")
            .render();
        assertTrue(html.contains("id=\"fx-root\""));
        assertTrue(html.contains("id=\"fx-list\""));
        assertTrue(html.contains("id=\"fx-inspector\""));
        assertTrue(html.contains("id=\"fx-viewer\""));
        assertTrue(html.contains("id=\"fx-modal\""));
        assertTrue(html.contains("hx-get=\"/demo/view?path=/docs/readme.md\""));
    }

    @Test
    void rendersPickerFooterAndSelectionAction() {
        FileEntryView entry = new FileEntryView("2", "images", "/images", "dir", null, "Folder", List.of(), true, true, List.of());
        FileExplorerState state = new FileExplorerState("Picker", "/", List.of(), List.of(entry), entry, List.of(), null, null, true, "/images");
        String html = FilePickerModule.create(state, new FileExplorerEndpoints("/demo/nav", "/demo/inspect", "/demo/view", "/demo/select"))
            .withPickerTargetId("picker-target")
            .render();
        assertTrue(html.contains("file-picker-footer"));
        assertTrue(html.contains("id=\"picker-target\""));
        assertTrue(html.contains("hx-post=\"/demo/select?path=/images\""));
    }
}
