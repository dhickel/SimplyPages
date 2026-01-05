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
import io.mindspice.simplypages.modules.ContentModule;
import io.mindspice.simplypages.modules.EditableModule;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test controller for Phase 6: Styling Improvements.
 *
 * <p><strong>Phase 6 Features Demonstrated:</strong></p>
 * <ul>
 *   <li><strong>Editable Module Wrapper:</strong> Better margin-bottom (24px) for proper spacing</li>
 *   <li><strong>Editable Row Wrapper:</strong> Padding, border-radius, hover effects (background color change)</li>
 *   <li><strong>Add Module Section:</strong> Professional dashed border buttons with hover states</li>
 *   <li><strong>Insert Row Section:</strong> Subtle styling with transparent background</li>
 *   <li><strong>Modal Forms:</strong> Proper spacing, labels, focus states (already implemented in Phase 1-2)</li>
 * </ul>
 *
 * <p><strong>CSS Improvements:</strong></p>
 * <ul>
 *   <li>Better margins and spacing throughout</li>
 *   <li>Professional color scheme (grays, blues)</li>
 *   <li>Smooth transitions on hover</li>
 *   <li>Enhanced visual hierarchy</li>
 * </ul>
 *
 * <p>Access at: <a href="http://localhost:8080/test/phase6">http://localhost:8080/test/phase6</a></p>
 */
@Controller
@RequestMapping("/test/phase6")
public class Phase6TestController {

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

    private final Map<String, PageData> pages = new ConcurrentHashMap<>();
    private int idCounter = 30;

    public Phase6TestController() {
        initializeTestData();
    }

    private void initializeTestData() {
        PageData page = new PageData("phase6-test");

        // Row 1: Demonstrate row hover effects
        TestRow row1 = new TestRow("row-1", 0);
        row1.modules.add(new TestModule("module-1", "Phase 6: Styling Improvements",
                "# Enhanced Visual Design\n\n" +
                "✅ **Editable row wrapper** - Now has padding, border-radius, and hover effect\n" +
                "✅ **Better spacing** - Modules have 24px margin-bottom\n" +
                "✅ **Professional colors** - Subtle grays with smooth transitions\n\n" +
                "**Try hovering over this entire row** - notice the subtle background color change!",
                6));
        row1.modules.add(new TestModule("module-2", "Form Improvements",
                "# Modal Form Styling\n\n" +
                "> Click the ✏ edit button to see improved form styling!\n\n" +
                "- Labels have proper spacing (8px margin-bottom)\n" +
                "- Inputs have focus states (blue border + shadow)\n" +
                "- Better padding and professional appearance\n" +
                "- Smooth transitions throughout",
                6));
        page.rows.add(row1);

        // Row 2: Demonstrate button styling
        TestRow row2 = new TestRow("row-2", 1);
        row2.modules.add(new TestModule("module-3", "Button Styling Demo",
                "# Professional Button Design\n\n" +
                "**Add Module buttons** (below) now feature:\n" +
                "- Dashed borders for visual clarity\n" +
                "- Neutral gray color scheme\n" +
                "- Hover effects with transform\n\n" +
                "**Insert Row buttons** use:\n" +
                "- Transparent background\n" +
                "- Subtle border styling\n" +
                "- Understated appearance",
                12));
        page.rows.add(row2);

        // Row 3: Another row to show consistent spacing
        TestRow row3 = new TestRow("row-3", 2);
        row3.modules.add(new TestModule("module-4", "Consistent Spacing",
                "All rows maintain 32px margin-bottom for clear visual separation.\n\n" +
                "Modules within rows have 24px margin-bottom.\n\n" +
                "This creates a professional, clean layout hierarchy!",
                12));
        page.rows.add(row3);

        pages.put("phase6-test", page);
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
        html.append("    <title>Phase 6 Test - Styling Improvements</title>\n");
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
        PageData page = pages.get("phase6-test");
        Container content = Container.create();

        // Header
        content.withChild(Header.H1("Phase 6 Test Page - Styling Improvements").withClass("mb-4"));

        // Phase 6 features alert
        Div features = new Div().withClass("mb-4");
        features.withChild(Alert.success(
                "<strong>Phase 6 CSS Improvements:</strong><br>" +
                "✅ Editable module wrapper: margin-bottom 24px<br>" +
                "✅ Editable row wrapper: padding 16px, border-radius 8px, hover effect<br>" +
                "✅ Add module buttons: dashed border, professional colors, hover transform<br>" +
                "✅ Insert row buttons: transparent background, subtle borders<br>" +
                "✅ Modal forms: proper spacing, labels, focus states (from Phase 1-2)"
        ));
        content.withChild(features);

        // Visual demo section
        Div demoInfo = new Div().withClass("mb-4");
        demoInfo.withChild(Alert.info(
                "<strong>🎨 Visual Demo:</strong><br>" +
                "Hover over rows to see the subtle background color transition!<br>" +
                "Click edit buttons to see improved form styling with proper spacing and focus states."
        ));
        content.withChild(demoInfo);

        // Render rows with editable-row-wrapper class
        for (TestRow row : page.rows) {
            // Use editable-row-wrapper class for Phase 6 styling
            Div rowWrapper = new Div()
                    .withClass("editable-row-wrapper");

            Row moduleRow = new Row();
            for (TestModule module : row.modules) {
                // Create ContentModule
                ContentModule contentMod = ContentModule.create()
                        .withTitle(module.title)
                        .withContent(module.content);

                // Wrap with EditableModule (Phase 5 framework styling)
                EditableModule editableModule = EditableModule.wrap(contentMod)
                        .withEditUrl("/test/phase6/edit/" + module.id)
                        .withEditTitle("Edit")
                        .withDeleteUrl("/test/phase6/delete/" + module.id)
                        .withDeleteTarget("#page-content")
                        .withDeleteConfirm("Delete this module?");

                Column col = Column.create().withWidth(module.width).withChild(editableModule);
                moduleRow.addColumn(col);
            }

            rowWrapper.withChild(moduleRow);

            // Add module section - uses .add-module-section class
            if (row.modules.size() < 3) {
                Div addModuleSection = new Div().withClass("add-module-section");
                Button addModuleBtn = Button.create("+ Add Module to Row")
                        .withStyle(Button.ButtonStyle.SECONDARY);
                addModuleBtn.withAttribute("hx-get", "/test/phase6/add-module-modal/" + row.id);
                addModuleBtn.withAttribute("hx-target", "#edit-modal-container");
                addModuleBtn.withAttribute("hx-swap", "innerHTML");
                addModuleSection.withChild(addModuleBtn);
                rowWrapper.withChild(addModuleSection);
            }

            content.withChild(rowWrapper);

            // Insert row section - uses .insert-row-section class
            Div insertRowSection = new Div().withClass("insert-row-section");
            Button insertRowBtn = Button.create("Insert Row Here")
                    .withStyle(Button.ButtonStyle.SECONDARY).small();
            insertRowBtn.withAttribute("hx-post", "/test/phase6/insert-row/" + row.position);
            insertRowBtn.withAttribute("hx-target", "#edit-modal-container");
            insertRowBtn.withAttribute("hx-swap", "innerHTML");
            insertRowSection.withChild(insertRowBtn);
            content.withChild(insertRowSection);
        }

        // Final add row button - uses .add-row-final class
        Div addRowFinal = new Div().withClass("add-row-final mt-4");
        Button addRowBtn = Button.create("+ Add New Row")
                .withStyle(Button.ButtonStyle.PRIMARY);
        addRowBtn.withAttribute("hx-post", "/test/phase6/add-row");
        addRowBtn.withAttribute("hx-target", "#edit-modal-container");
        addRowBtn.withAttribute("hx-swap", "innerHTML");
        addRowFinal.withChild(addRowBtn);
        content.withChild(addRowFinal);

        return "<div id=\"page-content\">" + content.render() + "</div>";
    }

    /**
     * Edit module endpoint - demonstrates improved form styling.
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

        EditAdapter<ContentModule> adapter = contentMod;

        // Build combined form
        Div combinedForm = new Div();

        // Module's edit fields
        combinedForm.withChild(adapter.buildEditView());

        // Width field (demonstrates form styling)
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
                .withTitle("Edit Module - Notice Form Styling!")
                .withModuleId(moduleId)
                .withEditView(combinedForm)
                .withSaveUrl("/test/phase6/save/" + moduleId)
                .withDeleteUrl("/test/phase6/delete/" + moduleId)
                .withPageContainerId("page-content")
                .withModalContainerId("edit-modal-container")
                .build()
                .render();
    }

    /**
     * Save module.
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

        module.title = formData.getOrDefault("title", module.title);
        module.content = formData.getOrDefault("content", module.content);

        if (formData.containsKey("width")) {
            try {
                module.width = Integer.parseInt(formData.get("width"));
            } catch (NumberFormatException e) {
                // Keep existing
            }
        }

        // OOB swaps
        String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
        String updatePage = renderPageContent().replace("<div id=\"page-content\">",
                "<div hx-swap-oob=\"true\" id=\"page-content\">");

        return clearModal + updatePage;
    }

    /**
     * Delete module.
     */
    @DeleteMapping("/delete/{moduleId}")
    @ResponseBody
    public String deleteModule(@PathVariable String moduleId) {
        PageData page = pages.get("phase6-test");
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
        PageData page = pages.get("phase6-test");
        String rowId = "row-" + (++idCounter);
        TestRow newRow = new TestRow(rowId, page.rows.size());
        page.rows.add(newRow);
        return showAddModuleModal(rowId);
    }

    /**
     * Insert row at position.
     */
    @PostMapping("/insert-row/{position}")
    @ResponseBody
    public String insertRow(@PathVariable int position) {
        PageData page = pages.get("phase6-test");
        String rowId = "row-" + (++idCounter);
        TestRow newRow = new TestRow(rowId, position);
        page.rows.add(position, newRow);

        // Renumber positions
        for (int i = 0; i < page.rows.size(); i++) {
            page.rows.get(i).position = i;
        }

        return showAddModuleModal(rowId);
    }

    /**
     * Show add module modal.
     */
    @GetMapping("/add-module-modal/{rowId}")
    @ResponseBody
    public String showAddModuleModal(@PathVariable String rowId) {
        Div body = new Div();

        Div titleGroup = new Div().withClass("form-field");
        titleGroup.withChild(new Paragraph("Title:").withClass("form-label"));
        titleGroup.withChild(TextInput.create("title").withPlaceholder("Module title"));
        body.withChild(titleGroup);

        Div contentGroup = new Div().withClass("form-field");
        contentGroup.withChild(new Paragraph("Content (Markdown):").withClass("form-label"));
        contentGroup.withChild(TextArea.create("content")
                .withPlaceholder("# Heading\n\nYour content...")
                .withRows(8));
        body.withChild(contentGroup);

        Div widthGroup = new Div().withClass("form-field");
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
        addBtn.withAttribute("hx-post", "/test/phase6/add-module/" + rowId);
        addBtn.withAttribute("hx-swap", "none");
        addBtn.withAttribute("hx-include", ".modal-body input, .modal-body textarea, .modal-body select");
        footer.withChild(addBtn);

        return Modal.create()
                .withTitle("Add Module - Notice Form Field Spacing!")
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
        PageData page = pages.get("phase6-test");
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
        PageData page = pages.get("phase6-test");
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
