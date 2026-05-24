package io.mindspice.simplypages.modules.file;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FileExplorerModuleTest {
    private static FileExplorerConfig defaultConfig(String root, String list, String inspector, String viewer, String modal) {
        return new FileExplorerConfig(
            new FileExplorerEndpoints("/demo/list", "/demo/nav", "/demo/view", "/demo/inspect", "/demo/modal/{action}", "/demo/action/{action}", "/demo/select"),
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
        assertTrue(html.contains("hx-get=\"/demo/list?path=%2Fdocs\""));
        assertTrue(html.contains("hx-get=\"/demo/modal/create-folder?path=%2Fdocs\""));
        assertTrue(html.contains("hx-get=\"/demo/modal/create-text?path=%2Fdocs\""));
        assertTrue(html.contains("hx-get=\"/demo/modal/create-markdown?path=%2Fdocs\""));
        assertTrue(html.contains("hx-get=\"/demo/modal/delete?path=%2Fdocs%2Freadme.md\""));
        assertTrue(html.contains("hx-get=\"/demo/modal/rename?path=%2Fdocs%2Freadme.md\""));
        assertTrue(html.contains("hx-get=\"/demo/action/copy?path=%2Fdocs%2Freadme.md\""));
        assertTrue(html.contains("hx-get=\"/demo/action/move?path=%2Fdocs%2Freadme.md\""));
    }

    @Test
    void rendersPickerFooterAndSelectionActionForAllowedMode() {
        FileEntryView entry = new FileEntryView("2", "readme.md", "/docs/readme.md", "text", null, "File", List.of(), false, true, List.of());
        FileExplorerState state = new FileExplorerState("Picker", "/", List.of(), List.of(entry), entry, List.of(), null, null, true, "/docs/readme.md");
        FileExplorerConfig config = new FileExplorerConfig(
            new FileExplorerEndpoints("/demo/list", "/demo/nav", "/demo/view", "/demo/inspect", "/demo/modal/{action}", "/demo/action/{action}", "/demo/select"),
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
            new FileExplorerEndpoints("/demo/list", "/demo/nav", "/demo/view", "/demo/inspect", "/demo/modal/{action}", "/demo/action/{action}", "/demo/select"),
            FileExplorerMode.LIST,
            FilePickerMode.DIRECTORIES,
            "picker-root", "picker-list", "picker-inspector", "picker-viewer", "picker-modal", "picker-target",
            false, false, false, false, false, false, false
        );
        String html = FilePickerModule.create(state, config).render();
        assertFalse(html.contains("hx-post=\"/demo/select?path=%2Fdocs%2Freadme.md\""));
    }

    @Test
    void pickerModesAllowDirectoriesAndFilesOrDirectories() {
        FileEntryView directory = new FileEntryView("3", "images", "/docs/images", "directory", null, "Folder", List.of(), true, true, List.of());
        FileExplorerState state = new FileExplorerState("Picker", "/docs", List.of(), List.of(directory), directory, List.of(), null, null, true, "/docs/images");
        FileExplorerConfig directoryConfig = new FileExplorerConfig(
            new FileExplorerEndpoints("/demo/list", "/demo/nav", "/demo/view", "/demo/inspect", "/demo/modal/{action}", "/demo/action/{action}", "/demo/select"),
            FileExplorerMode.LIST,
            FilePickerMode.DIRECTORIES,
            "picker-root", "picker-list", "picker-inspector", "picker-viewer", "picker-modal", "picker-target",
            false, false, false, false, false, false, false
        );
        String directoryHtml = FilePickerModule.create(state, directoryConfig).render();
        assertTrue(directoryHtml.contains("hx-post=\"/demo/select?path=%2Fdocs%2Fimages\""));

        FileExplorerConfig bothConfig = new FileExplorerConfig(
            directoryConfig.endpoints(),
            FileExplorerMode.LIST,
            FilePickerMode.FILES_OR_DIRECTORIES,
            "picker-root", "picker-list", "picker-inspector", "picker-viewer", "picker-modal", "picker-target",
            false, false, false, false, false, false, false
        );
        String bothHtml = FilePickerModule.create(state, bothConfig).render();
        assertTrue(bothHtml.contains("hx-post=\"/demo/select?path=%2Fdocs%2Fimages\""));
    }

    @Test
    void endpointTemplatesEncodeAndInterpolatePath() {
        FileExplorerEndpoints endpoints = new FileExplorerEndpoints(
            "/demo/list?view=compact",
            "/demo/nav/{path}",
            "/demo/view",
            "/demo/inspect",
            "/demo/modal/{action}?reason=manual",
            "/demo/action/{action}/{path}",
            "/demo/select"
        );
        String path = "/workspace/docs/a b&c#.md";
        assertTrue(endpoints.list(path).contains("path=%2Fworkspace%2Fdocs%2Fa+b%26c%23.md"));
        assertTrue(endpoints.navigate(path).equals("/demo/nav/%2Fworkspace%2Fdocs%2Fa%20b%26c%23.md"));
        assertTrue(endpoints.modal("rename", path).contains("/demo/modal/rename?reason=manual&path=%2Fworkspace%2Fdocs%2Fa+b%26c%23.md"));
        assertTrue(endpoints.action("move", path).equals("/demo/action/move/%2Fworkspace%2Fdocs%2Fa%20b%26c%23.md"));
    }

    @Test
    void escapesEntryLabelsTagsAndPaths() {
        FileEntryView entry = new FileEntryView("1", "<script>x</script>.md", "/docs/<bad>& file.md", "text", null, "Contains <tag>", List.of("<note>"), false, true, List.of());
        FileExplorerState state = new FileExplorerState("Files", "/docs", List.of(), List.of(entry), entry, List.of(), null, null, false, null);
        String html = FileExplorerModule.create(state, defaultConfig("root", "list", "inspector", "viewer", "modal")).render();
        assertFalse(html.contains("<script>x</script>"));
        assertTrue(html.contains("&lt;script&gt;x&lt;/script&gt;.md"));
        assertTrue(html.contains("%3Cbad%3E%26+file.md"));
        assertTrue(html.contains("&lt;note&gt;"));
    }

    @Test
    void suppressesControlsForBlankEndpointsAndDisabledFlags() {
        FileEntryView entry = new FileEntryView("1", "readme.md", "/docs/readme.md", "text", null, "File", List.of("docs"), false, true, List.of());
        FileExplorerState state = new FileExplorerState("Files", "/docs", List.of(), List.of(entry), entry, List.of(), null, null, false, null);
        FileExplorerConfig config = new FileExplorerConfig(
            new FileExplorerEndpoints("", "/demo/nav", "/demo/view", "/demo/inspect", "/demo/modal/{action}", "/demo/action/{action}", "/demo/select"),
            FileExplorerMode.CARDS,
            FilePickerMode.FILES_OR_DIRECTORIES,
            "root", "list", "inspector", "viewer", "modal", "picker-target",
            false, false, false, false, false, false, false
        );
        String html = FileExplorerModule.create(state, config).render();
        assertFalse(html.contains("hx-get=\"\""));
        assertFalse(html.contains("New Folder"));
        assertFalse(html.contains("New Text"));
        assertFalse(html.contains("New Markdown"));
        assertFalse(html.contains(">Delete<"));
        assertFalse(html.contains(">Rename<"));
        assertFalse(html.contains(">Copy<"));
        assertFalse(html.contains(">Move<"));
        assertFalse(html.contains("class=\"tag\""));
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
