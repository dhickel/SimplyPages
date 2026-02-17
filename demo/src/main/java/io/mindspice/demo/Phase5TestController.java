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
import io.mindspice.simplypages.editing.Editable;
import io.mindspice.simplypages.editing.EditModalBuilder;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Container;
import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.modules.ContentModule;
import io.mindspice.simplypages.modules.EditableModule;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test controller for Phase 5: Framework-Level Button Styling and Bug Fixes.
 *
 * <p><strong>IMPORTANT STATE MANAGEMENT NOTES:</strong></p>
 * <ul>
 *   <li><strong>This is a TEST/DEMO controller</strong> - uses in-memory storage for simplicity</li>
 *   <li><strong>NOT production-ready:</strong> No database, no transactions, no concurrency control</li>
 *   <li><strong>Multi-user concerns:</strong> All users share same in-memory state (race conditions possible)</li>
 *   <li><strong>Transient state:</strong> All data lost on server restart</li>
 * </ul>
 *
 * <p><strong>For Production, You'd Need:</strong></p>
 * <ul>
 *   <li>Database persistence (JPA entities with @Entity, @Id, @Version)</li>
 *   <li>Optimistic locking (@Version field) or pessimistic locking</li>
 *   <li>User sessions or page ownership model</li>
 *   <li>Transaction management (@Transactional)</li>
 *   <li>Conflict resolution UI for concurrent edits</li>
 * </ul>
 *
 * <p>This controller demonstrates:</p>
 * <ul>
 *   <li><strong>Phase 5:</strong> EditableModule wrapper with framework CSS</li>
 *   <li><strong>Phase 5:</strong> Markdown toggle bug fix (persisted state)</li>
 *   <li><strong>Phase 5:</strong> Framework-level z-index and button styling</li>
 *   <li><strong>Phases 1-4:</strong> Modal, Editable, Auto-save, Constraints</li>
 * </ul>
 *
 * <p>Access at: <a href="http://localhost:8080/test/phase5">http://localhost:8080/test/phase5</a></p>
 */
@Controller
@RequestMapping("/test/phase5")
public class Phase5TestController {

    // Simple in-memory storage (NOT for production!)
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

    private static class TestRow {
        String id;
        int position;
        List<TestModule> modules = new ArrayList<>();

        TestRow(String id, int position) {
            this.id = id;
            this.position = position;
        }
    }

    private static class PageData {
        String pageId;
        List<TestRow> rows = new ArrayList<>();

        PageData(String pageId) {
            this.pageId = pageId;
        }
    }

    // WARNING: Shared mutable state - NOT thread-safe for real concurrent editing!
    private final Map<String, PageData> pages = new ConcurrentHashMap<>();
    private int idCounter = 20;

    public Phase5TestController() {
        initializeTestData();
    }

    private void initializeTestData() {
        PageData page = new PageData("phase5-test");

        // Row 1: Demonstrate framework styling and markdown toggle
        TestRow row1 = new TestRow("row-1", 0);
        row1.modules.add(new TestModule("module-1", "Framework Styling Demo",
                "# Phase 5: Framework-Level Buttons\n\n" +
                "✅ **Edit/delete buttons use framework CSS**\n" +
                "✅ **Z-index managed automatically**\n" +
                "✅ **No manual styling required**\n\n" +
                "Try clicking the ✏ button!",
                6));
        row1.modules.add(new TestModule("module-2", "Markdown Styling Test",
                "# All Markdown Elements\n\n" +
                "> This is a blockquote. It should have a blue left border and light blue background.\n" +
                "> Multiple lines in blockquote work too!\n\n" +
                "**Bold text** and *italic text* work perfectly.\n\n" +
                "## Lists Work Great\n\n" +
                "- First item\n" +
                "- Second item\n" +
                "  - Nested item\n" +
                "  - Another nested\n" +
                "- Third item\n\n" +
                "1. Numbered list\n" +
                "2. Second item\n" +
                "3. Third item\n\n" +
                "Inline `code` looks great. Here's a [link](https://example.com) too.\n\n" +
                "---\n\n" +
                "Horizontal rule above!",
                6));
        page.rows.add(row1);

        // Row 2: Demonstrate bug fixes
        TestRow row2 = new TestRow("row-2", 1);
        row2.modules.add(new TestModule("module-3", "Bug Fixes Verified",
                "# Bugs Fixed in Phase 5\n\n" +
                "1. ✅ Buttons render correctly (no more empty divs)\n" +
                "2. ✅ Framework CSS classes applied automatically\n" +
                "3. ✅ Z-index managed by framework\n" +
                "4. ✅ Proper render(RenderContext) override",
                12));
        page.rows.add(row2);

        // Row 3: Plain text example (markdown disabled)
        TestRow row3 = new TestRow("row-3", 2);
        row3.modules.add(new TestModule("module-4", "Consistent Styling",
                "All modules get the same clean, professional button styling.\n\n" +
                "No manual CSS required!\n\n" +
                "Framework handles positioning, z-index, colors, and hover states.",
                12));
        page.rows.add(row3);

        pages.put("phase5-test", page);
    }

    /**
     * Main test page.
     */
    @GetMapping
    @ResponseBody
    public String testPage() {
        Div modalContainer = new Div().withAttribute("id", "edit-modal-container");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Phase 5 Test - Framework Styling</title>\n");
        html.append("    <link rel=\"stylesheet\" href=\"/css/framework.css\">\n");
        html.append("    <script src=\"/webjars/htmx.org/dist/htmx.min.js\" defer></script>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append(renderPageContent());
        html.append(modalContainer.render());
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * Render page content (reusable for HTMX updates).
     */
    private String renderPageContent() {
        PageData page = pages.get("phase5-test");
        Container content = Container.create();

        // Header
        content.withChild(Header.H1("Phase 5 Test Page").withClass("mb-4"));

        // Phase 5 features
        Div features = new Div().withClass("mb-4");
        features.withChild(Alert.success(
                "<strong>Phase 5 Features:</strong><br>" +
                "✅ EditableModule wrapper with framework CSS classes<br>" +
                "✅ Framework-level z-index (buttons always visible)<br>" +
                "✅ Consistent button styling across all modules<br>" +
                "✅ Proper render(RenderContext) override for nested rendering"
        ));
        content.withChild(features);

        // State management warning
        Div warning = new Div().withClass("mb-4");
        warning.withChild(Alert.warning(
                "<strong>⚠️ State Management Note:</strong><br>" +
                "This test uses <strong>in-memory storage</strong> (not production-ready).<br>" +
                "For production: Use database, transactions, optimistic locking, and conflict resolution."
        ));
        content.withChild(warning);

        // Render rows with EditableModule
        for (TestRow row : page.rows) {
            Div rowSection = new Div()
                    .withClass("mb-4 p-3")
                    .withAttribute("style", "border: 1px dashed #dee2e6; border-radius: 4px;");

            Row moduleRow = new Row();
            for (TestModule module : row.modules) {
                // Create ContentModule with markdown setting
                ContentModule contentMod = ContentModule.create()
                        .withTitle(module.title)
                        .withContent(module.content);

                // PHASE 5: Wrap with EditableModule (framework styling!)
                EditableModule editableModule = EditableModule.wrap(contentMod)
                        .withEditUrl("/test/phase5/edit/" + module.id)
                        .withEditTitle("Edit")
                        .withDeleteUrl("/test/phase5/delete/" + module.id)
                        .withDeleteTarget("#page-content")
                        .withDeleteConfirm("Delete this module? (Row auto-deletes if last module)");

                // Visual wrapper (presentation only)
                Div visualWrapper = new Div()
                        .withClass("border rounded p-3")
                        .withAttribute("style", "background-color: #f8f9fa;")
                        .withChild(editableModule);

                Column col = Column.create().withWidth(module.width).withChild(visualWrapper);
                moduleRow.addColumn(col);
            }

            rowSection.withChild(moduleRow);

            // Add module button
            if (row.modules.size() < 3) {
                Div addModuleSection = new Div().withClass("text-center mt-3");
                Button addModuleBtn = Button.create("+ Add Module to Row")
                        .withStyle(Button.ButtonStyle.SECONDARY).small();
                addModuleBtn.withAttribute("hx-get", "/test/phase5/add-module-modal/" + row.id);
                addModuleBtn.withAttribute("hx-target", "#edit-modal-container");
                addModuleBtn.withAttribute("hx-swap", "innerHTML");
                addModuleSection.withChild(addModuleBtn);
                rowSection.withChild(addModuleSection);
            }

            content.withChild(rowSection);
        }

        // Add row button
        Div addRowSection = new Div().withClass("text-center mt-4");
        Button addRowBtn = Button.create("+ Add New Row")
                .withStyle(Button.ButtonStyle.PRIMARY);
        addRowBtn.withAttribute("hx-post", "/test/phase5/add-row");
        addRowBtn.withAttribute("hx-target", "#edit-modal-container");
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

        // Create ContentModule
        ContentModule contentMod = ContentModule.create()
                .withModuleId(moduleId)
                .withTitle(module.title)
                .withContent(module.content);

        Editable<ContentModule> adapter = contentMod;

        // Build combined form (module fields + width field)
        Div combinedForm = new Div();

        // Add module's edit fields (includes markdown toggle)
        combinedForm.withChild(adapter.buildEditView());

        // Add width field
        Div widthGroup = new Div().withClass("form-field mt-4");
        widthGroup.withChild(new Paragraph("Module Width:").withClass("form-label"));
        widthGroup.withChild(Select.create("width")
                .addOption("3", "1/4 (3/12)", module.width == 3)
                .addOption("4", "1/3 (4/12)", module.width == 4)
                .addOption("6", "1/2 (6/12)", module.width == 6)
                .addOption("8", "2/3 (8/12)", module.width == 8)
                .addOption("12", "Full (12/12)", module.width == 12));
        combinedForm.withChild(widthGroup);

        return EditModalBuilder.create()
                .withTitle("Edit Module (Phase 5 - State Persisted)")
                .withModuleId(moduleId)
                .withEditView(combinedForm)
                .withSaveUrl("/test/phase5/save/" + moduleId)
                .withDeleteUrl("/test/phase5/delete/" + moduleId)
                .withPageContainerId("page-content")
                .withModalContainerId("edit-modal-container")
                .build()
                .render();
    }

    /**
     * Save module
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

        // Save title and content
        module.title = formData.getOrDefault("title", module.title);
        module.content = formData.getOrDefault("content", module.content);

        // Save width
        if (formData.containsKey("width")) {
            try {
                module.width = Integer.parseInt(formData.get("width"));
            } catch (NumberFormatException e) {
                // Keep existing
            }
        }

        // OOB swaps to clear modal and update page
        String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
        String updatePage = renderPageContent().replace("<div id=\"page-content\">",
                "<div hx-swap-oob=\"true\" id=\"page-content\">");

        return clearModal + updatePage;
    }

    /**
     * Delete module - removes empty rows.
     */
    @DeleteMapping("/delete/{moduleId}")
    @ResponseBody
    public String deleteModule(@PathVariable String moduleId) {
        PageData page = pages.get("phase5-test");
        TestRow containingRow = null;

        for (TestRow row : page.rows) {
            if (row.modules.removeIf(m -> m.id.equals(moduleId))) {
                containingRow = row;
                break;
            }
        }

        // If row is empty, delete it
        if (containingRow != null && containingRow.modules.isEmpty()) {
            page.rows.remove(containingRow);
            for (int i = 0; i < page.rows.size(); i++) {
                page.rows.get(i).position = i;
            }
        }

        return renderPageContent();
    }

    /**
     * Add row endpoint.
     */
    @PostMapping("/add-row")
    @ResponseBody
    public String addRow() {
        PageData page = pages.get("phase5-test");
        String rowId = "row-" + (++idCounter);
        TestRow newRow = new TestRow(rowId, page.rows.size());
        page.rows.add(newRow);
        return showAddModuleModal(rowId);
    }

    /**
     * Show add module modal.
     */
    @GetMapping("/add-module-modal/{rowId}")
    @ResponseBody
    public String showAddModuleModal(@PathVariable String rowId) {
        Div body = new Div();

        Div titleGroup = new Div().withClass("form-field mb-3");
        titleGroup.withChild(new Paragraph("Title:").withClass("form-label"));
        titleGroup.withChild(TextInput.create("title").withPlaceholder("Module title"));
        body.withChild(titleGroup);

        Div contentGroup = new Div().withClass("form-field mb-3");
        contentGroup.withChild(new Paragraph("Content (Markdown):").withClass("form-label"));
        contentGroup.withChild(TextArea.create("content")
                .withPlaceholder("# Heading\n\nYour content...")
                .withRows(8));
        body.withChild(contentGroup);

        Div widthGroup = new Div().withClass("form-field mb-3");
        widthGroup.withChild(new Paragraph("Module Width:").withClass("form-label"));
        widthGroup.withChild(Select.create("width")
                .addOption("6", "1/2 (6/12)", true)
                .addOption("12", "Full (12/12)", false));
        body.withChild(widthGroup);

        Div footer = new Div().withClass("d-flex justify-content-end gap-2");
        Button cancelBtn = Button.create("Cancel").withStyle(Button.ButtonStyle.SECONDARY);
        cancelBtn.withAttribute("onclick", "document.getElementById('edit-modal-container').innerHTML = ''");
        footer.withChild(cancelBtn);

        Button addBtn = Button.create("Add Module").withStyle(Button.ButtonStyle.PRIMARY);
        addBtn.withAttribute("hx-post", "/test/phase5/add-module/" + rowId);
        addBtn.withAttribute("hx-swap", "none");
        addBtn.withAttribute("hx-include", ".modal-body input, .modal-body textarea, .modal-body select");
        footer.withChild(addBtn);

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
        PageData page = pages.get("phase5-test");
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

        String moduleId = "module-" + (++idCounter);
        String title = formData.getOrDefault("title", "New Module");
        String content = formData.getOrDefault("content", "New content");
        int width = Integer.parseInt(formData.getOrDefault("width", "6"));

        TestModule newModule = new TestModule(moduleId, title, content, width);
        row.modules.add(newModule);

        String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
        String updatePage = renderPageContent().replace("<div id=\"page-content\">",
                "<div hx-swap-oob=\"true\" id=\"page-content\">");

        return clearModal + updatePage;
    }

    private TestModule findModule(String id) {
        PageData page = pages.get("phase5-test");
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
