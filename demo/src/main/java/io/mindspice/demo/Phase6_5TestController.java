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
 * Test controller for Phase 6.5: Module Locking & Permission System.
 */
@Controller
@RequestMapping("/test/phase6-5")
public class Phase6_5TestController {

    private static class TestModule {
        String id;
        String title;
        String content;
        int width;
        boolean canEdit = true;
        boolean canDelete = true;

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
        boolean canAddModule = true;
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
    private int idCounter = 100;

    public Phase6_5TestController() {
        initializeTestData();
    }

    private void initializeTestData() {
        PageData page = new PageData("phase6-5-test");

        // Row 1: Fully locked module
        TestRow row1 = new TestRow("row-1", 0);
        TestModule lockedMod = new TestModule("module-1", "🔒 Fully Locked Module",
                "# Protected Content\n\nThis module is fully locked - no edit or delete buttons.\n\n" +
                "**Use Case:** Site branding, required legal text, template headers", 12);
        lockedMod.canEdit = false;
        lockedMod.canDelete = false;
        row1.modules.add(lockedMod);
        page.rows.add(row1);

        // Row 2: Edit-only module + normal module
        TestRow row2 = new TestRow("row-2", 1);
        TestModule editOnlyMod = new TestModule("module-2", "✏ Edit-Only Module",
                "# Editable But Not Deletable\n\nThis module can be edited but not deleted.\n\n" +
                "**Use Case:** Core content that must stay but can be updated", 6);
        editOnlyMod.canDelete = false;
        row2.modules.add(editOnlyMod);

        TestModule normalMod1 = new TestModule("module-3", "Normal Module",
                "**Comparison:** This normal module has both edit and delete buttons.", 6);
        row2.modules.add(normalMod1);
        page.rows.add(row2);

        // Row 3: Locked row (cannot add modules)
        TestRow row3 = new TestRow("row-3", 2);
        row3.canAddModule = false;
        TestModule rowLockedInfo = new TestModule("module-4", "🔒 Locked Row",
                "# This Row is Locked\n\n" +
                "Notice: **No 'Add Module' button** appears below!\n\n" +
                "The modules can still be edited, but you cannot add new modules to this row.", 12);
        row3.modules.add(rowLockedInfo);
        page.rows.add(row3);

        // Row 4: Normal unlocked row
        TestRow row4 = new TestRow("row-4", 3);
        TestModule normalMod2 = new TestModule("module-5", "Normal Unlocked Row",
                "# Comparison Row\n\nThis is a normal, unlocked row with 'Add Module' button below.", 12);
        row4.modules.add(normalMod2);
        page.rows.add(row4);

        pages.put("phase6-5-test", page);
    }

    @GetMapping
    @ResponseBody
    public String testPage() {
        Div modalContainer = new Div().withAttribute("id", "edit-modal-container");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Phase 6.5 Test - Module Locking</title>\n");
        html.append("    <link rel=\"stylesheet\" href=\"/css/framework.css\">\n");
        html.append("    <script src=\"/webjars/htmx.org/dist/htmx.min.js\" defer></script>\n");
        html.append("</head>\n<body>\n");
        html.append(renderPageContent());
        html.append(modalContainer.render());
        html.append("</body>\n</html>\n");

        return html.toString();
    }

    private String renderPageContent() {
        PageData page = pages.get("phase6-5-test");
        Container content = Container.create();

        content.withChild(Header.H1("Phase 6.5 - Module & Row Locking").withClass("mb-4"));

        Div features = new Div().withClass("mb-4");
        features.withChild(Alert.success(
                "<strong>Phase 6.5 Features:</strong><br>" +
                "✅ Module locking (.withCanEdit(false), .withCanDelete(false))<br>" +
                "✅ Row locking (.withCanAddModule(false))<br>" +
                "✅ Framework-level permission enforcement"));
        content.withChild(features);

        Div demoInfo = new Div().withClass("mb-4");
        demoInfo.withChild(Alert.info(
                "<strong>🔒 Try the locking features:</strong><br>" +
                "Notice which modules have edit/delete buttons!<br>" +
                "Row 3 is locked - no 'Add Module' button appears."));
        content.withChild(demoInfo);

        // Render rows
        for (TestRow row : page.rows) {
            Div rowWrapper = new Div().withClass("editable-row-wrapper");

            Row moduleRow = new Row();
            for (TestModule module : row.modules) {
                ContentModule contentMod = ContentModule.create()
                        .withTitle(module.title)
                        .withContent(module.content);

                EditableModule editableModule = EditableModule.wrap(contentMod)
                        .withEditUrl("/test/phase6-5/edit/" + module.id)
                        .withDeleteUrl("/test/phase6-5/delete/" + module.id)
                        .withDeleteTarget("#page-content")
                        .withDeleteConfirm("Delete this module?")
                        .withCanEdit(module.canEdit)
                        .withCanDelete(module.canDelete);

                Column col = Column.create().withWidth(module.width).withChild(editableModule);
                moduleRow.addColumn(col);
            }

            rowWrapper.withChild(moduleRow);

            // Add module section (only if row permits)
            if (row.canAddModule && row.modules.size() < 3) {
                Div addModuleSection = new Div().withClass("add-module-section");
                Button addModuleBtn = Button.create("+ Add Module to Row")
                        .withStyle(Button.ButtonStyle.SECONDARY);
                addModuleBtn.withAttribute("hx-get", "/test/phase6-5/add-module-modal/" + row.id);
                addModuleBtn.withAttribute("hx-target", "#edit-modal-container");
                addModuleBtn.withAttribute("hx-swap", "innerHTML");
                addModuleSection.withChild(addModuleBtn);
                rowWrapper.withChild(addModuleSection);
            }

            content.withChild(rowWrapper);

            // Insert row section
            Div insertRowSection = new Div().withClass("insert-row-section");
            Button insertRowBtn = Button.create("+ Insert Row Below")
                    .withStyle(Button.ButtonStyle.SECONDARY).small();
            insertRowBtn.withAttribute("hx-post", "/test/phase6-5/insert-row/" + row.position);
            insertRowBtn.withAttribute("hx-target", "#edit-modal-container");
            insertRowBtn.withAttribute("hx-swap", "innerHTML");
            insertRowSection.withChild(insertRowBtn);
            content.withChild(insertRowSection);
        }

        return "<div id=\"page-content\">" + content.render() + "</div>";
    }

    @GetMapping("/edit/{moduleId}")
    @ResponseBody
    public String editModule(@PathVariable String moduleId) {
        TestModule module = findModule(moduleId);
        if (module == null) {
            return Modal.create().withTitle("Error")
                    .withBody(Alert.danger("Module not found")).render();
        }

        ContentModule contentMod = ContentModule.create()
                .withModuleId(moduleId)
                .withTitle(module.title)
                .withContent(module.content);

        Editable<ContentModule> adapter = contentMod;
        Div combinedForm = new Div();
        combinedForm.withChild(adapter.buildEditView());

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
                .withTitle("Edit Module")
                .withModuleId(moduleId)
                .withEditView(combinedForm)
                .withSaveUrl("/test/phase6-5/save/" + moduleId)
                .withDeleteUrl("/test/phase6-5/delete/" + moduleId)
                .withPageContainerId("page-content")
                .withModalContainerId("edit-modal-container")
                .build()
                .render();
    }

    @PostMapping("/save/{moduleId}")
    @ResponseBody
    public String saveModule(@PathVariable String moduleId, @RequestParam Map<String, String> formData) {
        TestModule module = findModule(moduleId);
        if (module == null) {
            return Modal.create().withTitle("Error")
                    .withBody(Alert.danger("Module not found")).render();
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

        String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
        String updatePage = renderPageContent().replace("<div id=\"page-content\">",
                "<div hx-swap-oob=\"true\" id=\"page-content\">");

        return clearModal + updatePage;
    }

    @DeleteMapping("/delete/{moduleId}")
    @ResponseBody
    public String deleteModule(@PathVariable String moduleId) {
        PageData page = pages.get("phase6-5-test");
        TestRow containingRow = null;

        for (TestRow row : page.rows) {
            if (row.modules.removeIf(m -> m.id.equals(moduleId))) {
                containingRow = row;
                break;
            }
        }

        if (containingRow != null && containingRow.modules.isEmpty()) {
            page.rows.remove(containingRow);
            for (int i = 0; i < page.rows.size(); i++) {
                page.rows.get(i).position = i;
            }
        }

        return renderPageContent();
    }

    @PostMapping("/insert-row/{position}")
    @ResponseBody
    public String insertRow(@PathVariable int position) {
        PageData page = pages.get("phase6-5-test");
        String rowId = "row-" + (++idCounter);
        TestRow newRow = new TestRow(rowId, position + 1);
        page.rows.add(position + 1, newRow);

        for (int i = 0; i < page.rows.size(); i++) {
            page.rows.get(i).position = i;
        }

        return showAddModuleModal(rowId);
    }

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
        addBtn.withAttribute("hx-post", "/test/phase6-5/add-module/" + rowId);
        addBtn.withAttribute("hx-swap", "none");
        addBtn.withAttribute("hx-include", ".modal-body input, .modal-body textarea, .modal-body select");
        footer.withChild(addBtn);

        return Modal.create()
                .withTitle("Add Module")
                .withBody(body)
                .withFooter(footer)
                .render();
    }

    @PostMapping("/add-module/{rowId}")
    @ResponseBody
    public String addModule(@PathVariable String rowId, @RequestParam Map<String, String> formData) {
        PageData page = pages.get("phase6-5-test");
        TestRow row = page.rows.stream()
                .filter(r -> r.id.equals(rowId))
                .findFirst()
                .orElse(null);

        if (row == null) {
            return Modal.create().withTitle("Error")
                    .withBody(Alert.danger("Row not found")).render();
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
        PageData page = pages.get("phase6-5-test");
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
