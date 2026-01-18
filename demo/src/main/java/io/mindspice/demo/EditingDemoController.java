package io.mindspice.demo;


import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.builders.SideNavBuilder;
import io.mindspice.simplypages.builders.TopBannerBuilder;
import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.components.display.Modal;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.forms.Checkbox;
import io.mindspice.simplypages.components.forms.Select;
import io.mindspice.simplypages.components.forms.TextArea;
import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.editing.Editable;
import io.mindspice.simplypages.editing.EditMode;
import io.mindspice.simplypages.editing.EditModalBuilder;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Container;
import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.modules.ContentModule;
import io.mindspice.simplypages.modules.SimpleListModule;
import io.mindspice.simplypages.components.ListItem;
import io.mindspice.simplypages.modules.EditableModule;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Editing demo page reimplemented with Phase 6.5 patterns:
 * - Single modal container (#edit-modal-container)
 * - OOB swaps for save/delete
 * - Row insert opens add-module modal
 * - Permission flags for modules and rows
 */
@Controller
@RequestMapping("/editing-demo")
public class EditingDemoController {

    private static final String PAGE_ID = "demo-page";
    private static final String PAGE_CONTAINER_ID = "page-content";
    private static final String MODAL_CONTAINER_ID = "edit-modal-container";

    private static class DemoModule {
        String id;
        String title;
        String content;
        int width;
        boolean useMarkdown = true;
        boolean canEdit = true;
        boolean canDelete = true;
        EditMode editMode = EditMode.OWNER_EDIT;

        DemoModule(String id, String title, String content, int width) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.width = width;
        }
    }

    private static class DemoRow {
        String id;
        int position;
        boolean canAddModule = true;
        List<DemoModule> modules = new ArrayList<>();

        DemoRow(String id, int position) {
            this.id = id;
            this.position = position;
        }
    }

    private static class PageData {
        String pageId;
        List<DemoRow> rows = new ArrayList<>();

        PageData(String pageId) {
            this.pageId = pageId;
        }
    }

    private static class PendingEdit {
        String moduleId;
        Map<String, String> changes;
        long timestamp;

        PendingEdit(String moduleId, Map<String, String> changes) {
            this.moduleId = moduleId;
            this.changes = new HashMap<>(changes);
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final Map<String, PageData> pages = new ConcurrentHashMap<>();
    private final Map<String, List<PendingEdit>> pendingEdits = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(100);

    public EditingDemoController() {
        initializeDemoData();
    }

    private void initializeDemoData() {
        PageData pageData = new PageData(PAGE_ID);

        DemoRow row1 = new DemoRow("row-1", 0);
        DemoModule intro = new DemoModule("module-1", "Welcome to the Editing Demo",
                "# Phase 6.5 Patterns\n\n" +
                "This page mirrors the gold-standard behavior from `/test/phase6-5`.\n\n" +
                "- Single modal container\n" +
                "- OOB swaps for save/delete\n" +
                "- Row insert opens add-module modal\n\n" +
                "Edit this module to see changes apply immediately.", 8);
        row1.modules.add(intro);

        DemoModule userEdit = new DemoModule("module-2", "Approval Workflow (User Edit)",
                "# USER_EDIT Mode\n\n" +
                "Edits to this module are staged for approval.\n\n" +
                "Open the **Pending Edits** modal to approve or reject.", 4);
        userEdit.editMode = EditMode.USER_EDIT;
        userEdit.canDelete = false;
        row1.modules.add(userEdit);
        pageData.rows.add(row1);

        DemoRow row2 = new DemoRow("row-2", 1);
        DemoModule locked = new DemoModule("module-3", "Fully Locked Module",
                "# Locked\n\n" +
                "No edit or delete buttons appear for this module.", 4);
        locked.canEdit = false;
        locked.canDelete = false;
        row2.modules.add(locked);

        DemoModule editOnly = new DemoModule("module-4", "Edit-Only Module",
                "# Edit Only\n\n" +
                "Can be edited but not deleted.", 4);
        editOnly.canDelete = false;
        row2.modules.add(editOnly);

        DemoModule deleteOnly = new DemoModule("module-5", "Delete-Only Module",
                "# Delete Only\n\n" +
                "Can be deleted but not edited.", 4);
        deleteOnly.canEdit = false;
        row2.modules.add(deleteOnly);
        pageData.rows.add(row2);

        DemoRow row3 = new DemoRow("row-3", 2);
        row3.canAddModule = false;
        DemoModule rowLocked = new DemoModule("module-6", "Row Locked (No Add Module)",
                "# Row Locking\n\n" +
                "This row disables the add-module control.\n\n" +
                "Modules can still be edited or deleted as allowed.", 12);
        row3.modules.add(rowLocked);
        pageData.rows.add(row3);

        DemoRow row4 = new DemoRow("row-4", 3);
        DemoModule widthDemo = new DemoModule("module-7", "Width Controls",
                "# Resize Me\n\n" +
                "Use the width dropdown in the edit modal to change this column.", 6);
        row4.modules.add(widthDemo);

        DemoModule markdownDemo = new DemoModule("module-8", "Markdown Toggle",
                "This module currently renders plain text. Toggle **Render as Markdown** to see the difference.", 6);
        markdownDemo.useMarkdown = false;
        row4.modules.add(markdownDemo);
        pageData.rows.add(row4);

        DemoRow row5 = new DemoRow("row-5", 4);
        DemoModule listDemo = new DemoModule("module-9", "Nested Editing Demo",
                "Simple List Module", 12);
        // Using 'content' field to store serialized list for demo simplicity or just ignoring it
        listDemo.content = "Item 1,Item 2,Item 3";
        row5.modules.add(listDemo);
        pageData.rows.add(row5);

        pages.put(PAGE_ID, pageData);
        pendingEdits.put(PAGE_ID, new ArrayList<>());
    }

    @GetMapping
    @ResponseBody
    public String viewPage(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        response.setHeader("Vary", "HX-Request");

        String content = renderPageView();

        if (hxRequest != null) {
            return content;
        }

        return renderWithShell(content);
    }

    private String renderPageView() {
        Div modalContainer = new Div().withAttribute("id", MODAL_CONTAINER_ID);
        return renderPageContent() + modalContainer.render();
    }

    private String renderPageContent() {
        PageData pageData = pages.get(PAGE_ID);
        if (pageData == null) {
            return Alert.danger("Page not found").render();
        }

        Container content = Container.create();

        content.withChild(Header.H1("Editing Demo (Phase 6.5 Patterns)").withClass("mb-3"));

        content.withChild(Alert.success("Gold-standard patterns: single modal container, OOB swaps, row inserts, and permission flags.")
                .withClass("mb-3"));
        content.withChild(Alert.info("Try editing modules, adding modules, inserting rows, and approving pending edits.")
                .withClass("mb-4"));

        List<PendingEdit> pending = getPendingEdits();
        Div controls = new Div().withClass("d-flex justify-content-end gap-2 mb-4");
        Button pendingBtn = Button.create("Pending Edits (" + pending.size() + ")")
                .withStyle(Button.ButtonStyle.INFO)
                .small();
        pendingBtn.withAttribute("hx-get", "/editing-demo/pending-edits");
        pendingBtn.withAttribute("hx-target", "#" + MODAL_CONTAINER_ID);
        pendingBtn.withAttribute("hx-swap", "innerHTML");
        controls.withChild(pendingBtn);
        content.withChild(controls);

        if (!pending.isEmpty()) {
            content.withChild(Alert.warning("Pending edits are waiting for approval.")
                    .withClass("mb-4"));
        }

        for (DemoRow demoRow : pageData.rows) {
            Div rowWrapper = new Div().withClass("editable-row-wrapper");
            Row moduleRow = new Row();

            for (DemoModule module : demoRow.modules) {
                io.mindspice.simplypages.core.Module displayModule;

                if (module.title.equals("Nested Editing Demo")) {
                     SimpleListModule listModule = SimpleListModule.create()
                            .withModuleId(module.id)
                            .withTitle(module.title);

                     if (module.content != null && !module.content.isEmpty()) {
                         for (String item : module.content.split(",")) {
                             listModule.addItem(ListItem.create(item.trim()));
                         }
                     }
                     displayModule = listModule;
                } else {
                    ContentModule contentModule = ContentModule.create()
                            .withModuleId(module.id)
                            .withTitle(module.title)
                            .withContent(module.content);

                    if (!module.useMarkdown) {
                        contentModule.disableMarkdown();
                    }
                    displayModule = contentModule;
                }

                EditableModule editableModule = EditableModule.wrap(displayModule)
                        .withEditUrl("/editing-demo/edit/" + module.id)
                        .withDeleteUrl("/editing-demo/delete/" + module.id)
                        .withDeleteTarget("#" + PAGE_CONTAINER_ID)
                        .withDeleteSwap("none")
                        .withDeleteConfirm("Delete this module?")
                        .withCanEdit(module.canEdit)
                        .withCanDelete(module.canDelete)
                        .withEditMode(module.editMode);

                Column col = Column.create().withWidth(module.width).withChild(editableModule);
                moduleRow.addColumn(col);
            }

            rowWrapper.withChild(moduleRow);

            if (demoRow.canAddModule && demoRow.modules.size() < 3) {
                Div addModuleSection = new Div().withClass("add-module-section");
                Button addModuleBtn = Button.create("+ Add Module to Row")
                        .withStyle(Button.ButtonStyle.SECONDARY);
                addModuleBtn.withAttribute("hx-get", "/editing-demo/add-module-modal/" + demoRow.id);
                addModuleBtn.withAttribute("hx-target", "#" + MODAL_CONTAINER_ID);
                addModuleBtn.withAttribute("hx-swap", "innerHTML");
                addModuleSection.withChild(addModuleBtn);
                rowWrapper.withChild(addModuleSection);
            }

            content.withChild(rowWrapper);

            Div insertRowSection = new Div().withClass("insert-row-section");
            Button insertRowBtn = Button.create("+ Insert Row Below")
                    .withStyle(Button.ButtonStyle.SECONDARY)
                    .small();
            insertRowBtn.withAttribute("hx-post", "/editing-demo/insert-row/" + demoRow.position);
            insertRowBtn.withAttribute("hx-target", "#" + MODAL_CONTAINER_ID);
            insertRowBtn.withAttribute("hx-swap", "innerHTML");
            insertRowSection.withChild(insertRowBtn);
            content.withChild(insertRowSection);
        }

        return "<div id=\"" + PAGE_CONTAINER_ID + "\">" + content.render() + "</div>";
    }

    @GetMapping("/edit/{moduleId}")
    @ResponseBody
    public String editModule(
            @PathVariable String moduleId,
            @RequestParam(required = false) String editMode
    ) {
        DemoModule module = findModule(moduleId);
        if (module == null) {
            return Modal.create().withTitle("Error")
                    .withBody(Alert.danger("Module not found"))
                    .render();
        }

        EditMode mode = resolveEditMode(editMode, module);
        return buildEditModal(module, mode);
    }

    @PostMapping("/save/{moduleId}")
    @ResponseBody
    public String saveModule(
            @PathVariable String moduleId,
            @RequestParam Map<String, String> formData,
            @RequestParam(required = false) String editMode
    ) {
        DemoModule module = findModule(moduleId);
        if (module == null) {
            return Modal.create().withTitle("Error")
                    .withBody(Alert.danger("Module not found"))
                    .render();
        }

        EditMode mode = resolveEditMode(editMode, module);
        if (mode == EditMode.USER_EDIT) {
            List<PendingEdit> pending = getPendingEdits();
            pending.removeIf(p -> p.moduleId.equals(moduleId));
            pending.add(new PendingEdit(moduleId, buildChangeSet(formData)));
            return buildOobResponse();
        }

        applyModuleEdits(module, formData);
        return buildOobResponse();
    }

    @DeleteMapping("/delete/{moduleId}")
    @ResponseBody
    public String deleteModule(@PathVariable String moduleId) {
        PageData pageData = pages.get(PAGE_ID);
        if (pageData == null) {
            return Alert.danger("Page not found").render();
        }

        DemoRow containingRow = null;
        for (DemoRow row : pageData.rows) {
            if (row.modules.removeIf(m -> m.id.equals(moduleId))) {
                containingRow = row;
                break;
            }
        }

        if (containingRow != null && containingRow.modules.isEmpty()) {
            pageData.rows.remove(containingRow);
            for (int i = 0; i < pageData.rows.size(); i++) {
                pageData.rows.get(i).position = i;
            }
        }

        return buildOobResponse();
    }

    @PostMapping("/insert-row/{position}")
    @ResponseBody
    public String insertRow(@PathVariable int position) {
        PageData pageData = pages.get(PAGE_ID);
        if (pageData == null) {
            return Alert.danger("Page not found").render();
        }

        String rowId = "row-" + idCounter.incrementAndGet();
        DemoRow newRow = new DemoRow(rowId, position + 1);
        pageData.rows.add(position + 1, newRow);

        for (int i = 0; i < pageData.rows.size(); i++) {
            pageData.rows.get(i).position = i;
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
                .addOption("3", "1/4 (3/12)", false)
                .addOption("4", "1/3 (4/12)", false)
                .addOption("6", "1/2 (6/12)", true)
                .addOption("8", "2/3 (8/12)", false)
                .addOption("12", "Full (12/12)", false));
        body.withChild(widthGroup);

        Div modeGroup = new Div().withClass("form-field");
        modeGroup.withChild(new Paragraph("Edit Mode:").withClass("form-label"));
        modeGroup.withChild(Select.create("editMode")
                .addOption("OWNER_EDIT", "Owner Edit (immediate)", true)
                .addOption("USER_EDIT", "User Edit (approval)", false));
        body.withChild(modeGroup);

        Div permGroup = new Div().withClass("form-field");
        permGroup.withChild(new Paragraph("Permissions:").withClass("form-label"));
        Div permRow = new Div().withClass("d-flex gap-3 flex-wrap");
        permRow.withChild(Checkbox.create("canEdit", "true").withLabel("Allow edit").checked());
        permRow.withChild(Checkbox.create("canDelete", "true").withLabel("Allow delete").checked());
        permRow.withChild(Checkbox.create("useMarkdown", "true").withLabel("Render as Markdown").checked());
        permGroup.withChild(permRow);
        body.withChild(permGroup);

        Div footer = new Div().withClass("d-flex justify-content-end gap-2");
        Button cancelBtn = Button.create("Cancel").withStyle(Button.ButtonStyle.SECONDARY);
        cancelBtn.withAttribute("onclick",
                "document.getElementById('" + MODAL_CONTAINER_ID + "').innerHTML = ''");
        footer.withChild(cancelBtn);

        Button addBtn = Button.create("Add Module").withStyle(Button.ButtonStyle.PRIMARY);
        addBtn.withAttribute("hx-post", "/editing-demo/add-module/" + rowId);
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
    public String addModule(
            @PathVariable String rowId,
            @RequestParam Map<String, String> formData
    ) {
        DemoRow row = findRow(rowId);
        if (row == null) {
            return Modal.create().withTitle("Error")
                    .withBody(Alert.danger("Row not found"))
                    .render();
        }

        if (!row.canAddModule) {
            return Modal.create().withTitle("Error")
                    .withBody(Alert.warning("This row is locked and cannot add modules."))
                    .render();
        }

        if (row.modules.size() >= 3) {
            return Modal.create().withTitle("Error")
                    .withBody(Alert.warning("Maximum 3 modules per row"))
                    .render();
        }

        String moduleId = "module-" + idCounter.incrementAndGet();
        String title = safeText(formData.getOrDefault("title", "New Module"));
        String content = safeText(formData.getOrDefault("content", "New content"));
        int width = parseWidth(formData.get("width"), 6);

        DemoModule newModule = new DemoModule(moduleId, title, content, width);
        newModule.useMarkdown = formData.containsKey("useMarkdown");
        newModule.canEdit = formData.containsKey("canEdit");
        newModule.canDelete = formData.containsKey("canDelete");
        newModule.editMode = resolveEditMode(formData.get("editMode"), newModule);
        row.modules.add(newModule);

        return buildOobResponse();
    }

    @GetMapping("/pending-edits")
    @ResponseBody
    public String showPendingEditsModal() {
        List<PendingEdit> pending = getPendingEdits();

        Div body = new Div();
        if (pending.isEmpty()) {
            body.withChild(Alert.info("No pending edits."));
        } else {
            for (PendingEdit edit : pending) {
                Div editCard = new Div().withClass("card p-3 mb-3");
                editCard.withChild(new Paragraph("Module: " + edit.moduleId).withClass("fw-bold mb-1"));
                editCard.withChild(new Paragraph("Fields: " + summarizeChanges(edit))
                        .withClass("text-muted small mb-2"));

                Div buttons = new Div().withClass("d-flex gap-2");
                Button approveBtn = Button.create("Approve")
                        .withStyle(Button.ButtonStyle.SUCCESS)
                        .small();
                approveBtn.withAttribute("hx-post", "/editing-demo/pending-edits/" + edit.moduleId + "/approve");
                approveBtn.withAttribute("hx-swap", "none");
                buttons.withChild(approveBtn);

                Button rejectBtn = Button.create("Reject")
                        .withStyle(Button.ButtonStyle.DANGER)
                        .small();
                rejectBtn.withAttribute("hx-delete", "/editing-demo/pending-edits/" + edit.moduleId + "/reject");
                rejectBtn.withAttribute("hx-swap", "none");
                buttons.withChild(rejectBtn);

                editCard.withChild(buttons);
                body.withChild(editCard);
            }
        }

        Div footer = new Div().withClass("d-flex justify-content-end");
        Button closeBtn = Button.create("Close").withStyle(Button.ButtonStyle.SECONDARY);
        closeBtn.withAttribute("onclick",
                "document.getElementById('" + MODAL_CONTAINER_ID + "').innerHTML = ''");
        footer.withChild(closeBtn);

        return Modal.create()
                .withTitle("Pending Edits")
                .withBody(body)
                .withFooter(footer)
                .render();
    }

    @PostMapping("/pending-edits/{moduleId}/approve")
    @ResponseBody
    public String approvePendingEdit(@PathVariable String moduleId) {
        List<PendingEdit> pending = getPendingEdits();
        PendingEdit edit = pending.stream()
                .filter(p -> p.moduleId.equals(moduleId))
                .findFirst()
                .orElse(null);

        if (edit == null) {
            return Modal.create().withTitle("Error")
                    .withBody(Alert.warning("Pending edit not found"))
                    .render();
        }

        DemoModule module = findModule(moduleId);
        if (module != null) {
            applyPendingEdit(module, edit);
        }
        pending.remove(edit);

        return buildOobResponse();
    }

    @DeleteMapping("/pending-edits/{moduleId}/reject")
    @ResponseBody
    public String rejectPendingEdit(@PathVariable String moduleId) {
        List<PendingEdit> pending = getPendingEdits();
        pending.removeIf(p -> p.moduleId.equals(moduleId));
        return buildOobResponse();
    }

    private String buildEditModal(DemoModule module, EditMode mode) {
        Editable<?> editable;

        if (module.title.equals("Nested Editing Demo")) {
            SimpleListModule listModule = SimpleListModule.create()
                    .withModuleId(module.id)
                    .withTitle(module.title);
             if (module.content != null && !module.content.isEmpty()) {
                 for (String item : module.content.split(",")) {
                     listModule.addItem(ListItem.create(item.trim()));
                 }
             }
             editable = listModule;
        } else {
            ContentModule contentMod = ContentModule.create()
                    .withModuleId(module.id)
                    .withTitle(module.title)
                    .withContent(module.content);
            if (!module.useMarkdown) {
                contentMod.disableMarkdown();
            }
            editable = contentMod;
        }

        Div combinedForm = new Div();

        if (mode == EditMode.USER_EDIT) {
            combinedForm.withChild(Alert.info("Edits will be staged for approval.")
                    .withClass("mb-3"));
        }

        // Add module property fields
        combinedForm.withChild(editable.buildEditView());

        // Custom extra fields for demo (width)
        Div widthGroup = new Div().withClass("form-field mt-4");
        widthGroup.withChild(new Paragraph("Module Width:").withClass("form-label"));
        widthGroup.withChild(Select.create("width")
                .addOption("3", "1/4 (3/12)", module.width == 3)
                .addOption("4", "1/3 (4/12)", module.width == 4)
                .addOption("6", "1/2 (6/12)", module.width == 6)
                .addOption("8", "2/3 (8/12)", module.width == 8)
                .addOption("12", "Full (12/12)", module.width == 12));
        combinedForm.withChild(widthGroup);

        EditModalBuilder builder = EditModalBuilder.create()
                .withTitle("Edit Module")
                .withModuleId(module.id)
                .withEditView(combinedForm) // Pass the combined form directly
                .withEditable(editable)     // Pass editable to enable child editing if supported
                .withSaveUrl(buildSaveUrl(module.id, mode))
                .withDeleteUrl(buildDeleteUrl(module.id, mode))
                .withChildEditUrl("/editing-demo/edit-child/" + module.id + "/{id}")
                .withChildDeleteUrl("/editing-demo/delete-child/" + module.id + "/{id}")
                .withPageContainerId(PAGE_CONTAINER_ID)
                .withModalContainerId(MODAL_CONTAINER_ID);

        if (!module.canDelete) {
            builder.hideDelete();
        }

        return builder.build().render();
    }

    private String buildSaveUrl(String moduleId, EditMode mode) {
        if (mode == null) {
            return "/editing-demo/save/" + moduleId;
        }
        return "/editing-demo/save/" + moduleId + "?editMode=" + mode.name();
    }

    private String buildDeleteUrl(String moduleId, EditMode mode) {
        if (mode == null) {
            return "/editing-demo/delete/" + moduleId;
        }
        return "/editing-demo/delete/" + moduleId + "?editMode=" + mode.name();
    }

    private String buildOobResponse() {
        String clearModal = "<div hx-swap-oob=\"true\" id=\"" + MODAL_CONTAINER_ID + "\"></div>";
        String updatePage = renderPageContent().replace(
                "<div id=\"" + PAGE_CONTAINER_ID + "\">",
                "<div hx-swap-oob=\"true\" id=\"" + PAGE_CONTAINER_ID + "\">"
        );
        return clearModal + updatePage;
    }

    private Map<String, String> buildChangeSet(Map<String, String> formData) {
        Map<String, String> changes = new HashMap<>();
        if (formData.containsKey("title")) {
            changes.put("title", safeText(formData.get("title")));
        }
        if (formData.containsKey("content")) {
            changes.put("content", safeText(formData.get("content")));
        }
        if (formData.containsKey("width")) {
            changes.put("width", formData.get("width"));
        }
        changes.put("useMarkdown", String.valueOf(formData.containsKey("useMarkdown")));
        return changes;
    }

    private void applyModuleEdits(DemoModule module, Map<String, String> formData) {
        if (formData.containsKey("title")) {
            module.title = safeText(formData.get("title"));
        }
        if (formData.containsKey("content")) {
            module.content = safeText(formData.get("content"));
        }
        if (formData.containsKey("width")) {
            module.width = parseWidth(formData.get("width"), module.width);
        }
        module.useMarkdown = formData.containsKey("useMarkdown");
    }

    private void applyPendingEdit(DemoModule module, PendingEdit edit) {
        Map<String, String> changes = edit.changes;
        if (changes.containsKey("title")) {
            module.title = changes.get("title");
        }
        if (changes.containsKey("content")) {
            module.content = changes.get("content");
        }
        if (changes.containsKey("width")) {
            module.width = parseWidth(changes.get("width"), module.width);
        }
        if (changes.containsKey("useMarkdown")) {
            module.useMarkdown = Boolean.parseBoolean(changes.get("useMarkdown"));
        }
    }

    private String summarizeChanges(PendingEdit edit) {
        List<String> fields = new ArrayList<>();
        if (edit.changes.containsKey("title")) {
            fields.add("title");
        }
        if (edit.changes.containsKey("content")) {
            fields.add("content");
        }
        if (edit.changes.containsKey("width")) {
            fields.add("width");
        }
        if (edit.changes.containsKey("useMarkdown")) {
            fields.add("markdown");
        }
        return String.join(", ", fields);
    }

    private String safeText(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private int parseWidth(String width, int fallback) {
        if (width == null) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(width);
            if (parsed >= 1 && parsed <= 12) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // Keep fallback
        }
        return fallback;
    }

    private EditMode resolveEditMode(String editMode, DemoModule module) {
        if (editMode != null) {
            try {
                return EditMode.valueOf(editMode);
            } catch (IllegalArgumentException ignored) {
                // Use module default
            }
        }
        if (module != null && module.editMode != null) {
            return module.editMode;
        }
        return EditMode.OWNER_EDIT;
    }

    private List<PendingEdit> getPendingEdits() {
        return pendingEdits.computeIfAbsent(PAGE_ID, key -> new ArrayList<>());
    }

    private DemoModule findModule(String id) {
        PageData pageData = pages.get(PAGE_ID);
        if (pageData == null) {
            return null;
        }
        for (DemoRow row : pageData.rows) {
            for (DemoModule module : row.modules) {
                if (module.id.equals(id)) {
                    return module;
                }
            }
        }
        return null;
    }

    private DemoRow findRow(String id) {
        PageData pageData = pages.get(PAGE_ID);
        if (pageData == null) {
            return null;
        }
        for (DemoRow row : pageData.rows) {
            if (row.id.equals(id)) {
                return row;
            }
        }
        return null;
    }

    private String renderWithShell(String content) {
        String shell = ShellBuilder.create()
                .withPageTitle("JHF Editing Demo")
                .withTopBanner(
                        TopBannerBuilder.create()
                                .withTitle("Java HTML Framework - Editing Demo")
                                .withSubtitle("Phase 6.5 patterns with permissions, row locking, and approval workflow")
                                .withClass("banner-full-width")
                                .withBackgroundColor("#2c3e50")
                                .withTextColor("#ffffff")
                                .build()
                )
                .withSideNav(
                        SideNavBuilder.create()
                                .addSection("Editing Demo")
                                .addLink("Editable Page", "/editing-demo", "✏️")
                                .addSection("Features")
                                .addLink("Permissions & Locks", "#", "🔒")
                                .addLink("Add Modules", "#", "➕")
                                .addLink("Insert Rows", "#", "📊")
                                .addLink("Approval Workflow", "#", "✅")
                                .addSection("Main Demo")
                                .addLink("Back to Home", "/home", "🏠")
                                .build()
                )
                .build();

        return shell.replaceAll("<div id=\"content-area\"[^>]*>", "<div id=\"content-area\">")
                .replace("<div id=\"content-area\"></div>",
                        "<div id=\"content-area\">" + content + "</div>");
    }
}
