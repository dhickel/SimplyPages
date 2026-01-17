package io.mindspice.demo;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.components.display.Modal;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.forms.Select;
import io.mindspice.simplypages.components.forms.TextArea;
import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.editing.EditAdapter;
import io.mindspice.simplypages.editing.EditModalBuilder;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Container;
import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.layout.Section;
import io.mindspice.simplypages.modules.ContentModule;
import io.mindspice.simplypages.modules.EditableModule;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test controller for Phase 3-4: Auto-Save Architecture and Row/Module Constraints.
 *
 * <p>This controller demonstrates:</p>
 * <ul>
 *   <li><strong>Phase 3:</strong> Auto-save (no manual save/load buttons)</li>
 *   <li><strong>Phase 4:</strong> Row/module constraints (rows require modules, empty rows auto-delete)</li>
 *   <li>Proper modal overlay usage (single container, Modal.create())</li>
 * </ul>
 *
 * <p>Access at: <a href="http://localhost:8080/test/phase3-4">http://localhost:8080/test/phase3-4</a></p>
 */
@Controller
@RequestMapping("/test/phase3-4")
public class Phase3And4TestController {

    // Simple in-memory storage
    private static class TestRow {
        String id;
        int position;
        List<TestModule> modules = new ArrayList<>();

        TestRow(String id, int position) {
            this.id = id;
            this.position = position;
        }
    }

    private static class TestModule {
        String id;
        String title;
        String content;
        int width;  // 1-12 column width

        TestModule(String id, String title, String content, int width) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.width = width;
        }
    }

    private static class PageData {
        String pageId;
        List<TestRow> rows = new ArrayList<>();

        PageData(String pageId) {
            this.pageId = pageId;
        }
    }

    private final Map<String, PageData> pages = new ConcurrentHashMap<>();
    private int idCounter = 10;

    public Phase3And4TestController() {
        initializeTestData();
    }

    private void initializeTestData() {
        PageData page = new PageData("test-page");

        // Row 1: Two modules
        TestRow row1 = new TestRow("row-1", 0);
        row1.modules.add(new TestModule("module-1", "Auto-Save Demo",
                "# Phase 3: Auto-Save Architecture\n\nEdit this module and see changes save **immediately** without manual save buttons!\n\n✨ No save/load buttons needed",
                6));
        row1.modules.add(new TestModule("module-2", "Row Constraints",
                "# Phase 4: Row/Module Constraints\n\n- Try deleting this module (the last in this row)\n- The entire row will auto-delete!\n- Try adding a new row below",
                6));
        page.rows.add(row1);

        // Row 2: Single full-width module
        TestRow row2 = new TestRow("row-2", 1);
        row2.modules.add(new TestModule("module-3", "Modal Overlays",
                "# Proper Modal Usage\n\nAll modals use:\n- Single container (`#edit-modal-container`)\n- `Modal.create()` component\n- Proper overlay with backdrop\n\n**Click edit to see!**",
                12));
        page.rows.add(row2);

        pages.put("test-page", page);
    }

    /**
     * Main test page.
     */
    @GetMapping
    @ResponseBody
    public String testPage() {
        // Single modal container for ALL modals (critical!)
        Div modalContainer = new Div().withAttribute("id", "edit-modal-container");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Phase 3-4 Test</title>\n");
        html.append("    <link rel=\"stylesheet\" href=\"/css/framework.css\">\n");
        html.append("    <script src=\"/webjars/htmx.org/dist/htmx.min.js\" defer></script>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append(renderPageContent());
        html.append(modalContainer.render());  // Single container at end
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * Render page content (reusable for HTMX updates).
     */
    private String renderPageContent() {
        PageData page = pages.get("test-page");
        Container content = Container.create();

        // Header
        content.withChild(Header.H1("Phase 3-4 Test Page").withClass("mb-4"));

        // Description
        Div description = new Div().withClass("mb-4");
        description.withChild(Alert.info(
                "<strong>Phase 3:</strong> Auto-save enabled (no manual save/load buttons)<br>" +
                "<strong>Phase 4:</strong> Row/module constraints (rows require modules, empty rows auto-delete)"
        ));
        content.withChild(description);

        // NO Save/Load buttons (Phase 3: auto-save)
        content.withChild(new Paragraph("<em>Notice: No save/load buttons! All changes save immediately.</em>")
                .withClass("text-muted mb-4"));

        // Render rows
        for (TestRow row : page.rows) {
            Div rowSection = new Div()
                    .withClass("mb-4 p-3")
                    .withAttribute("style", "border: 1px dashed #dee2e6; border-radius: 4px;");

            Row moduleRow = new Row();
            for (TestModule module : row.modules) {
                // Create module content
                ContentModule contentMod = ContentModule.create()
                        .withTitle(module.title)
                        .withContent(module.content);

                // Wrap with EditableModule (framework-level styling!)
                EditableModule editableModule = EditableModule.wrap(contentMod)
                        .withEditUrl("/test/phase3-4/edit/" + module.id)
                        .withEditTitle("Edit (auto-saves immediately)")
                        .withDeleteUrl("/test/phase3-4/delete/" + module.id)
                        .withDeleteTarget("#page-content")
                        .withDeleteConfirm("Delete this module? (If last in row, row will also be deleted)");

                // Add visual wrapper for border and background (presentation)
                Div visualWrapper = new Div()
                        .withClass("border rounded p-3")
                        .withAttribute("style", "background-color: #f8f9fa;")
                        .withChild(editableModule);

                Column col = Column.create().withWidth(module.width).withChild(visualWrapper);
                moduleRow.addColumn(col);
            }

            rowSection.withChild(moduleRow);

            // Add module button (if space available)
            if (row.modules.size() < 3) {
                Div addModuleSection = new Div().withClass("text-center mt-3");
                Button addModuleBtn = Button.create("+ Add Module to Row")
                        .withStyle(Button.ButtonStyle.SECONDARY).small();
                addModuleBtn.withAttribute("hx-get", "/test/phase3-4/add-module-modal/" + row.id);
                addModuleBtn.withAttribute("hx-target", "#edit-modal-container");  // Single container!
                addModuleBtn.withAttribute("hx-swap", "innerHTML");
                addModuleSection.withChild(addModuleBtn);
                rowSection.withChild(addModuleSection);
            }

            content.withChild(rowSection);
        }

        // Add row button (Phase 4: shows modal immediately, no placeholder)
        Div addRowSection = new Div().withClass("text-center mt-4");
        Button addRowBtn = Button.create("+ Add New Row (shows modal)")
                .withStyle(Button.ButtonStyle.PRIMARY);
        addRowBtn.withAttribute("hx-post", "/test/phase3-4/add-row");
        addRowBtn.withAttribute("hx-target", "#edit-modal-container");  // Returns modal!
        addRowBtn.withAttribute("hx-swap", "innerHTML");
        addRowSection.withChild(addRowBtn);
        content.withChild(addRowSection);

        return "<div id=\"page-content\">" + content.render() + "</div>";
    }

    /**
     * Edit module endpoint - returns Modal overlay.
     */
    @GetMapping("/edit/{moduleId}")
    @ResponseBody
    public String editModule(@PathVariable String moduleId) {
        TestModule module = findModule(moduleId);
        if (module == null) {
            return Modal.create()
                    .withTitle("Error")
                    .withBody(Alert.danger("Module not found"))
                    .render();
        }

        ContentModule contentMod = ContentModule.create()
                .withModuleId(moduleId)
                .withTitle(module.title)
                .withContent(module.content);

        EditAdapter<ContentModule> adapter = contentMod;

        // Build combined form (module fields + width field)
        Div combinedForm = new Div();

        // Add module's own edit fields
        combinedForm.withChild(adapter.buildEditView());

        // Add width field (layout concern)
        Div widthGroup = new Div().withClass("form-field mt-4");
        widthGroup.withChild(new Paragraph("Module Width:").withClass("form-label"));
        widthGroup.withChild(Select.create("width")
                .addOption("3", "1/4 (3/12)", module.width == 3)
                .addOption("4", "1/3 (4/12)", module.width == 4)
                .addOption("6", "1/2 (6/12)", module.width == 6)
                .addOption("8", "2/3 (8/12)", module.width == 8)
                .addOption("12", "Full (12/12)", module.width == 12));
        combinedForm.withChild(widthGroup);

        // Use EditModalBuilder (wraps Modal.create())
        return EditModalBuilder.create()
                .withTitle("Edit Module (Auto-Save)")
                .withModuleId(moduleId)
                .withEditView(combinedForm)
                .withSaveUrl("/test/phase3-4/save/" + moduleId)
                .withDeleteUrl("/test/phase3-4/delete/" + moduleId)
                .withPageContainerId("page-content")
                .withModalContainerId("edit-modal-container")
                .build()
                .render();
    }

    /**
     * Save module - auto-save (Phase 3).
     */
    @PostMapping("/save/{moduleId}")
    @ResponseBody
    public String saveModule(@PathVariable String moduleId, @RequestParam Map<String, String> formData) {
        TestModule module = findModule(moduleId);
        if (module == null) {
            return Modal.create()
                    .withTitle("Error")
                    .withBody(Alert.danger("Module not found"))
                    .render();
        }

        // Auto-save immediately (Phase 3)
        module.title = formData.getOrDefault("title", module.title);
        module.content = formData.getOrDefault("content", module.content);

        // Save width if provided
        if (formData.containsKey("width")) {
            try {
                module.width = Integer.parseInt(formData.get("width"));
            } catch (NumberFormatException e) {
                // Keep existing width if invalid
            }
        }

        // Use OOB swaps to clear modal and update page
        String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
        String updatePage = renderPageContent().replace("<div id=\"page-content\">",
                "<div hx-swap-oob=\"true\" id=\"page-content\">");

        return clearModal + updatePage;
    }

    /**
     * Delete module - removes empty rows (Phase 4).
     */
    @DeleteMapping("/delete/{moduleId}")
    @ResponseBody
    public String deleteModule(@PathVariable String moduleId) {
        PageData page = pages.get("test-page");
        TestRow containingRow = null;

        // Find and remove module
        for (TestRow row : page.rows) {
            if (row.modules.removeIf(m -> m.id.equals(moduleId))) {
                containingRow = row;
                break;
            }
        }

        // Phase 4: If row is now empty, delete it
        if (containingRow != null && containingRow.modules.isEmpty()) {
            page.rows.remove(containingRow);

            // Renumber positions
            for (int i = 0; i < page.rows.size(); i++) {
                page.rows.get(i).position = i;
            }
        }

        // Return updated page
        return renderPageContent();
    }

    /**
     * Add row endpoint - returns modal (Phase 4: no placeholder).
     */
    @PostMapping("/add-row")
    @ResponseBody
    public String addRow() {
        PageData page = pages.get("test-page");

        // Create empty row
        String rowId = "row-" + (++idCounter);
        TestRow newRow = new TestRow(rowId, page.rows.size());
        page.rows.add(newRow);

        // Phase 4: Immediately show modal to add first module (row requires module)
        return showAddModuleModal(rowId);
    }

    /**
     * Show add module modal - uses Modal.create() for proper overlay.
     */
    @GetMapping("/add-module-modal/{rowId}")
    @ResponseBody
    public String showAddModuleModal(@PathVariable String rowId) {
        Div body = new Div();

        // Title field
        Div titleGroup = new Div().withClass("form-field mb-3");
        titleGroup.withChild(new Paragraph("Title:").withClass("form-label"));
        titleGroup.withChild(TextInput.create("title").withPlaceholder("Module title"));
        body.withChild(titleGroup);

        // Content field
        Div contentGroup = new Div().withClass("form-field mb-3");
        contentGroup.withChild(new Paragraph("Content (Markdown):").withClass("form-label"));
        contentGroup.withChild(TextArea.create("content")
                .withPlaceholder("# Heading\n\nYour content here...")
                .withRows(8));
        body.withChild(contentGroup);

        // Width field
        Div widthGroup = new Div().withClass("form-field mb-3");
        widthGroup.withChild(new Paragraph("Module Width:").withClass("form-label"));
        widthGroup.withChild(Select.create("width")
                .addOption("3", "1/4 (3/12)", false)
                .addOption("4", "1/3 (4/12)", false)
                .addOption("6", "1/2 (6/12)", true)
                .addOption("8", "2/3 (8/12)", false)
                .addOption("12", "Full (12/12)", false));
        body.withChild(widthGroup);

        // Footer buttons
        Div footer = new Div().withClass("d-flex justify-content-end gap-2");

        Button cancelBtn = Button.create("Cancel")
                .withStyle(Button.ButtonStyle.SECONDARY);
        cancelBtn.withAttribute("onclick", "document.getElementById('edit-modal-container').innerHTML = ''");
        footer.withChild(cancelBtn);

        Button addBtn = Button.create("Add Module")  // NOT Button.submit()!
                .withStyle(Button.ButtonStyle.PRIMARY);
        addBtn.withAttribute("hx-post", "/test/phase3-4/add-module/" + rowId);
        addBtn.withAttribute("hx-swap", "none");  // Use OOB swaps
        addBtn.withAttribute("hx-include", ".modal-body input, .modal-body textarea, .modal-body select");
        footer.withChild(addBtn);

        // Use Modal.create() for proper overlay
        return Modal.create()
                .withTitle("Add Module to Row")
                .withBody(body)
                .withFooter(footer)
                .render();
    }

    /**
     * Add module to row.
     */
    @PostMapping("/add-module/{rowId}")
    @ResponseBody
    public String addModule(@PathVariable String rowId, @RequestParam Map<String, String> formData) {
        PageData page = pages.get("test-page");
        TestRow row = page.rows.stream()
                .filter(r -> r.id.equals(rowId))
                .findFirst()
                .orElse(null);

        if (row == null) {
            return Modal.create()
                    .withTitle("Error")
                    .withBody(Alert.danger("Row not found"))
                    .render();
        }

        // Create new module
        String moduleId = "module-" + (++idCounter);
        String title = formData.getOrDefault("title", "New Module");
        String content = formData.getOrDefault("content", "New content");
        int width = Integer.parseInt(formData.getOrDefault("width", "6"));

        TestModule newModule = new TestModule(moduleId, title, content, width);
        row.modules.add(newModule);

        // Use OOB swaps
        String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
        String updatePage = renderPageContent().replace("<div id=\"page-content\">",
                "<div hx-swap-oob=\"true\" id=\"page-content\">");

        return clearModal + updatePage;
    }

    /**
     * Helper: Find module by ID.
     */
    private TestModule findModule(String id) {
        PageData page = pages.get("test-page");
        for (TestRow row : page.rows) {
            for (TestModule module : row.modules) {
                if (module.id.equals(id)) {
                    return module;
                }
            }
        }
        return null;
    }
}
