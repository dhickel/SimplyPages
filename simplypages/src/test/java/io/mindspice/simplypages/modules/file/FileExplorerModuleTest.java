package io.mindspice.simplypages.modules.file;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FileExplorerModuleTest {
    private static FileExplorerConfig defaultConfig(String root, String list, String inspector, String viewer, String modal) {
        return new FileExplorerConfig(
            new FileExplorerEndpoints("/demo/list", "/demo/nav", "/demo/view", "/demo/inspect", "/demo/modal/delete", "/demo/action/run", "/demo/select"),
            FileExplorerMode.CARDS,
            FilePickerMode.FILES_OR_DIRECTORIES,
            root, list, inspector, viewer, modal, "picker-target",
            true, true, true, true, true, true, true
        );
    }

    @Test
    void rendersExplorerWithStableTargetsAndHtmxAttributes() {
        FileEntryView entry = new FileEntryView("1", "readme.md", "/docs/readme.md", "text", "1 KB", "Project README", List.of("docs"), false, true, List.of());
        FileExplorerState state = new FileExplorerState("Files", "/docs", List.of(new FileBreadcrumbItem("Root", "/", false)), List.of(entry), entry, List.of(), new Paragraph("Inspector"), new Paragraph("Viewer"), false, null);
        String html = FileExplorerModule.create(state, defaultConfig("fx-root", "fx-list", "fx-inspector", "fx-viewer", "fx-modal")).render();
        assertTrue(html.contains("id=\"fx-root\""));
        assertTrue(html.contains("id=\"fx-list\""));
        assertTrue(html.contains("id=\"fx-inspector\""));
        assertTrue(html.contains("id=\"fx-viewer\""));
        assertTrue(html.contains("id=\"fx-modal\""));
        assertTrue(html.contains("hx-get=\"/demo/view?path=%2Fdocs%2Freadme.md\""));
        assertTrue(html.contains("hx-get=\"/demo/inspect?path=%2Fdocs%2Freadme.md\""));
        assertTrue(html.contains("hx-get=\"/demo/modal/delete?path=%2Fdocs%2Freadme.md\""));
        assertTrue(html.contains("hx-get=\"/demo/action/run?path=%2Fdocs%2Freadme.md\""));
    }

    @Test
    void rendersPickerFooterAndSelectionActionForAllowedMode() {
        FileEntryView entry = new FileEntryView("2", "readme.md", "/docs/readme.md", "text", null, "File", List.of(), false, true, List.of());
        FileExplorerState state = new FileExplorerState("Picker", "/", List.of(), List.of(entry), entry, List.of(), null, null, true, "/docs/readme.md");
        FileExplorerConfig config = new FileExplorerConfig(
            new FileExplorerEndpoints("/demo/list", "/demo/nav", "/demo/view", "/demo/inspect", "/demo/modal/delete", "/demo/action/run", "/demo/select"),
            FileExplorerMode.LIST,
            FilePickerMode.FILES,
            "picker-root", "picker-list", "picker-inspector", "picker-viewer", "picker-modal", "picker-target",
            false, false, false, false, false, false, false
        );
        String html = FilePickerModule.create(state, config).render();
        assertTrue(html.contains("file-picker-footer"));
        assertTrue(html.contains("id=\"picker-target\""));
        assertTrue(html.contains("hx-post=\"/demo/select?path=%2Fdocs%2Freadme.md\""));
    }

    @Test
    void pickerModeDirectoriesHidesSelectForFiles() {
        FileEntryView fileEntry = new FileEntryView("2", "readme.md", "/docs/readme.md", "text", null, "File", List.of(), false, true, List.of());
        FileExplorerState state = new FileExplorerState("Picker", "/", List.of(), List.of(fileEntry), fileEntry, List.of(), null, null, true, "/docs/readme.md");
        FileExplorerConfig config = new FileExplorerConfig(
            new FileExplorerEndpoints("/demo/list", "/demo/nav", "/demo/view", "/demo/inspect", "/demo/modal/delete", "/demo/action/run", "/demo/select"),
            FileExplorerMode.LIST,
            FilePickerMode.DIRECTORIES,
            "picker-root", "picker-list", "picker-inspector", "picker-viewer", "picker-modal", "picker-target",
            false, false, false, false, false, false, false
        );
        String html = FilePickerModule.create(state, config).render();
        assertFalse(html.contains("hx-post=\"/demo/select?path=%2Fdocs%2Freadme.md\""));
    }

    @Test
    void endpointTemplatesEncodeAndInterpolatePath() {
        FileExplorerEndpoints endpoints = new FileExplorerEndpoints(
            "/demo/list?view=compact",
            "/demo/nav/{path}",
            "/demo/view",
            "/demo/inspect",
            "/demo/modal/delete?reason=manual",
            "/demo/action/run/{path}",
            "/demo/select"
        );
        String path = "/workspace/docs/a b.md";
        assertTrue(endpoints.list(path).contains("path=%2Fworkspace%2Fdocs%2Fa+b.md"));
        assertTrue(endpoints.navigate(path).equals("/demo/nav/%2Fworkspace%2Fdocs%2Fa+b.md"));
        assertTrue(endpoints.modal(path).contains("reason=manual&path=%2Fworkspace%2Fdocs%2Fa+b.md"));
        assertTrue(endpoints.action(path).equals("/demo/action/run/%2Fworkspace%2Fdocs%2Fa+b.md"));
    }

    @Test
    void moduleRenderIsIdempotent() {
        FileEntryView entry = new FileEntryView("1", "readme.md", "/docs/readme.md", "text", null, "File", List.of(), false, true, List.of());
        FileExplorerState state = new FileExplorerState("Files", "/docs", List.of(), List.of(entry), entry, List.of(), null, null, false, null);
        FileExplorerModule module = FileExplorerModule.create(state, defaultConfig("root", "list", "inspector", "viewer", "modal"));
        String first = module.render();
        String second = module.render();
        assertTrue(first.equals(second));
    }
}
