package io.mindspice.demo;


import io.mindspice.simplypages.builders.BannerBuilder;
import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.builders.SideNavBuilder;
import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.RawHtml;
import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.components.display.Modal;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.forms.Checkbox;
import io.mindspice.simplypages.components.forms.Form;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.components.forms.Select;
import io.mindspice.simplypages.components.forms.TextArea;
import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.editing.Editable;
import io.mindspice.simplypages.editing.EditMode;
import io.mindspice.simplypages.editing.EditModalBuilder;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.modules.ContentModule;
import io.mindspice.simplypages.modules.SimpleListModule;
import io.mindspice.simplypages.components.ListItem;
import io.mindspice.simplypages.modules.EditableModule;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.NumberUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
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
@RequestMapping(value = "/editing-demo", produces = "text/html;charset=UTF-8")
public class EditingDemoController {

    private static final String PAGE_ID = "demo-page";
    private static final String PAGE_CONTAINER_ID = "page-content";
    private static final String MODAL_CONTAINER_ID = "edit-modal-container";
    private static final String PRIVATE_WARNING = "Private shared, non-durable sandbox. Flags and selectable OWNER/USER modes are simulations, not authentication. Approval is a sandbox action. No CSRF, rate protection or production security guarantees.";

    private enum ModuleKind { CONTENT, LIST }

    private static class DemoModule {
        final ModuleKind kind;
        String id;
        String title;
        String content;
        int width;
        boolean useMarkdown = true;
        boolean canEdit = true;
        boolean canDelete = true;
        EditMode editMode = EditMode.OWNER_EDIT;

        DemoModule(String id, String title, String content, int width) {
            this(id, title, content, width, ModuleKind.CONTENT);
        }

        DemoModule(String id, String title, String content, int width, ModuleKind kind) {
            this.kind = kind;
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

    // All state access and rendered snapshots share this monitor; HTTP writes follow return.
    private final Object stateLock = new Object();
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
                "This is a shared, non-durable private editing sandbox.\n\n" +
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
                "Simple List Module", 12, ModuleKind.LIST);
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
        return inState(() -> {
        varyHx(response);

        if (!pages.containsKey(PAGE_ID)) {
            response.setStatus(404);
        }
        String content = renderPageView();

        if ("true".equalsIgnoreCase(hxRequest)) {
            return content;
        }

        return renderWithShell(content);
        });
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

        Page content = Page.builder().build();

        content.withChild(Header.H1("Editing Demo (Phase 6.5 Patterns)").withClass("mb-3"));

        content.withChild(Alert.warning(PRIVATE_WARNING)
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

                if (module.kind == ModuleKind.LIST) {
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
            @RequestParam(required = false) String editMode,
            HttpServletResponse response
    ) {
        return inState(() -> {
        DemoModule module = findModule(moduleId);
        if (module == null) return stateError(response, 404, "Module not found");
        if (!module.canEdit) return stateError(response, 403, "This module does not allow editing.");

        EditMode mode = resolveEditMode(editMode, module);
        return buildEditModal(module, mode);
        });
    }

    @PostMapping("/save/{moduleId}")
    @ResponseBody
    public String saveModule(
            @PathVariable String moduleId,
            @RequestParam Map<String, String> formData,
            @RequestParam(required = false) String editMode,
            HttpServletResponse response
    ) {
        return inState(() -> {
        DemoModule module = findModule(moduleId);
        if (module == null) return stateError(response, 404, "Module not found");
        if (!module.canEdit) return stateError(response, 403, "This module does not allow editing.");

        EditMode mode = resolveEditMode(editMode, module);
        if (mode == EditMode.USER_EDIT) {
            List<PendingEdit> pending = getPendingEdits();
            pending.removeIf(p -> p.moduleId.equals(moduleId));
            pending.add(new PendingEdit(moduleId, buildChangeSet(formData)));
            return buildOobResponse();
        }

        applyModuleEdits(module, formData);
        return buildOobResponse();
        });
    }

    @DeleteMapping("/delete/{moduleId}")
    @ResponseBody
    public String deleteModule(@PathVariable String moduleId, HttpServletResponse response) {
        return inState(() -> {
        PageData pageData = pages.get(PAGE_ID);
        if (pageData == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return Alert.danger("Page not found").render();
        }
        DemoModule module = findModule(moduleId);
        if (module == null) return stateError(response, 404, "Module not found");
        if (!module.canDelete) return stateError(response, 403, "This module does not allow deletion.");
        getPendingEdits().removeIf(edit -> edit.moduleId.equals(moduleId));

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
        });
    }

    @PostMapping("/insert-row/{position}")
    @ResponseBody
    public String insertRow(@PathVariable String position, HttpServletResponse response) {
        return inState(() -> {
        varyHx(response);
        PageData pageData = pages.get(PAGE_ID);
        if (pageData == null) {
            response.setStatus(404);
            return Alert.danger("Page not found").render();
        }
        final int index;
        try {
            index = NumberUtils.parseNumber(position, Integer.class);
        } catch (NumberFormatException invalid) {
            response.setStatus(400);
            return Alert.danger("Insertion position must be an integer.").render();
        }
        if (index < -1 || index >= pageData.rows.size()) {
            response.setStatus(400);
            return Alert.danger("Insertion position is outside the current page.").render();
        }

        String rowId = "row-" + idCounter.incrementAndGet();
        DemoRow newRow = new DemoRow(rowId, index + 1);
        pageData.rows.add(index + 1, newRow);

        for (int i = 0; i < pageData.rows.size(); i++) {
            pageData.rows.get(i).position = i;
        }

        return showAddModuleModal(rowId, response);
        });
    }

    @GetMapping("/add-module-modal/{rowId}")
    @ResponseBody
    public String showAddModuleModal(@PathVariable String rowId, HttpServletResponse response) {
        return inState(() -> {
        DemoRow row = findRow(rowId);
        if (row == null) return stateError(response, 404, "Row not found");
        if (!row.canAddModule) return stateError(response, 403, "This row cannot add modules.");
        if (row.modules.size() >= 3) return stateError(response, 409, "Maximum 3 modules per row");
        Div body = new Div().withChild(Alert.warning(PRIVATE_WARNING));

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
        });
    }

    @PostMapping("/add-module/{rowId}")
    @ResponseBody
    public String addModule(
            @PathVariable String rowId,
            @RequestParam Map<String, String> formData,
            HttpServletResponse response
    ) {
        return inState(() -> {
        DemoRow row = findRow(rowId);
        if (row == null) return stateError(response, 404, "Row not found");
        if (!row.canAddModule) return stateError(response, 403, "This row cannot add modules.");
        if (row.modules.size() >= 3) return stateError(response, 409, "Maximum 3 modules per row");

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
        });
    }

    @GetMapping("/pending-edits")
    @ResponseBody
    public String showPendingEditsModal() {
        return inState(() -> {
        List<PendingEdit> pending = getPendingEdits();

        Div body = new Div().withChild(Alert.warning(PRIVATE_WARNING));
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
        });
    }

    @PostMapping("/pending-edits/{moduleId}/approve")
    @ResponseBody
    public String approvePendingEdit(@PathVariable String moduleId, HttpServletResponse response) {
        return inState(() -> {
        List<PendingEdit> pending = getPendingEdits();
        PendingEdit edit = pending.stream()
                .filter(p -> p.moduleId.equals(moduleId))
                .findFirst()
                .orElse(null);

        DemoModule module = findModule(moduleId);
        if (edit == null || module == null) return stateError(response, 404, "Module or pending edit not found");
        if (!module.canEdit) return stateError(response, 403, "This module does not allow approval.");
        applyPendingEdit(module, edit);
        pending.remove(edit);

        return buildOobResponse();
        });
    }

    @DeleteMapping("/pending-edits/{moduleId}/reject")
    @ResponseBody
    public String rejectPendingEdit(@PathVariable String moduleId, HttpServletResponse response) {
        return inState(() -> {
        List<PendingEdit> pending = getPendingEdits();
        if (pending.stream().noneMatch(edit -> edit.moduleId.equals(moduleId))) {
            return stateError(response, 404, "Pending edit not found");
        }
        pending.removeIf(p -> p.moduleId.equals(moduleId));
        return buildOobResponse();
        });
    }

    private String inState(java.util.function.Supplier<String> operation) {
        synchronized (stateLock) {
            return operation.get();
        }
    }

    private String stateError(HttpServletResponse response, int status, String message) {
        response.setStatus(status);
        return Modal.create().withTitle("Private Sandbox")
                .withBody(new Div().withChild(Alert.warning(PRIVATE_WARNING)).withChild(Alert.danger(message))).render();
    }

    private String buildEditModal(DemoModule module, EditMode mode) {
        Editable<?> editable;

        if (module.kind == ModuleKind.LIST) {
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

        Div combinedForm = new Div().withChild(Alert.warning(PRIVATE_WARNING));

        if (mode == EditMode.USER_EDIT) {
            combinedForm.withChild(Alert.info("Edits will be staged for approval. Child changes start from live content and replace the whole pending proposal; prior pending child and property changes are discarded.")
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
                .withChildEditUrl(module.canEdit ? childUrlTemplate("edit-child", module.id, mode) : null)
                .withChildDeleteUrl(module.canEdit ? childUrlTemplate("delete-child", module.id, mode) : null)
                .withPageContainerId(PAGE_CONTAINER_ID)
                .withModalContainerId(MODAL_CONTAINER_ID);

        if (!module.canDelete) {
            builder.hideDelete();
        }

        return builder.build().render();
    }

    @GetMapping("/edit-child/{moduleId}/{childId}")
    @ResponseBody
    public String editChild(@PathVariable String moduleId, @PathVariable String childId,
            @RequestParam(required = false) String editMode, HttpServletResponse response) {
        return inState(() -> {
            EditMode mode = resolveEditMode(editMode, findModule(moduleId));
            try {
                ChildTarget child = resolveChild(moduleId, childId);
                return buildChildModal(moduleId, childId, mode, child.items().get(child.index()), null);
            } catch (ChildFailure failure) {
                return childError(moduleId, childId, mode, "", failure, response);
            }
        });
    }

    @PostMapping("/save-child/{moduleId}/{childId}")
    @ResponseBody
    public String saveChild(@PathVariable String moduleId, @PathVariable String childId,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) String editMode, HttpServletResponse response) {
        return inState(() -> {
        EditMode mode = resolveEditMode(editMode, findModule(moduleId));
        try {
            ChildTarget child = resolveChild(moduleId, childId);
            String label = text == null ? "" : text.trim();
            if (label.isBlank() || label.contains(",")) {
                throw new ChildFailure(400, "Enter nonblank text without commas (this demo stores a CSV list).");
            }
            List<String> candidate = new ArrayList<>(child.items());
            candidate.set(child.index(), label);
            storeChildContent(child.module(), candidate, mode);
            return buildOobResponse();
        } catch (ChildFailure failure) {
            return childError(moduleId, childId, mode, text, failure, response);
        }
        });
    }

    @DeleteMapping("/delete-child/{moduleId}/{childId}")
    @ResponseBody
    public String deleteChild(@PathVariable String moduleId, @PathVariable String childId,
            @RequestParam(required = false) String editMode, HttpServletResponse response) {
        return inState(() -> {
        EditMode mode = resolveEditMode(editMode, findModule(moduleId));
        try {
            ChildTarget child = resolveChild(moduleId, childId);
            List<String> candidate = new ArrayList<>(child.items());
            candidate.remove(child.index());
            storeChildContent(child.module(), candidate, mode);
            // Generated child DELETE controls otherwise default to an in-band innerHTML swap.
            response.setHeader("HX-Reswap", "none");
            return buildOobResponse();
        } catch (ChildFailure failure) {
            return childError(moduleId, childId, mode, "", failure, response);
        }
        });
    }

    private record ChildTarget(DemoModule module, List<String> items, int index) { }

    private static class ChildFailure extends RuntimeException {
        final int status;

        ChildFailure(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    private ChildTarget resolveChild(String moduleId, String childId) {
        if (!childId.matches("item-(0|[1-9][0-9]*)")) {
            throw new ChildFailure(400, "Invalid positional child ID.");
        }
        final int index;
        try {
            index = Integer.parseInt(childId.substring(5));
        } catch (NumberFormatException invalid) {
            throw new ChildFailure(400, "Child position is too large.");
        }
        DemoModule module = findModule(moduleId);
        if (module == null || module.kind != ModuleKind.LIST) {
            throw new ChildFailure(404, "Current list module not found.");
        }
        if (!module.canEdit) {
            throw new ChildFailure(403, "This module does not allow child editing.");
        }
        List<String> items = visibleItems(module.content);
        if (index >= items.size()) {
            throw new ChildFailure(404, "Child position is not in the current list.");
        }
        return new ChildTarget(module, items, index);
    }

    private List<String> visibleItems(String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        for (String item : content.split(",")) {
            items.add(item.trim());
        }
        return items;
    }

    private void storeChildContent(DemoModule module, List<String> candidate, EditMode mode) {
        String content = String.join(",", candidate);
        if (!visibleItems(content).equals(candidate)) {
            throw new ChildFailure(400, "This change would lose a retained empty item in CSV storage. Edit that item first.");
        }
        if (mode == EditMode.USER_EDIT) {
            List<PendingEdit> pending = getPendingEdits();
            pending.removeIf(edit -> edit.moduleId.equals(module.id));
            pending.add(new PendingEdit(module.id, Map.of("content", content)));
        } else {
            module.content = content;
        }
    }

    private String childError(String moduleId, String childId, EditMode mode, String text,
            ChildFailure failure, HttpServletResponse response) {
        response.setStatus(failure.status);
        response.setHeader("X-Editing-Demo-Modal", "true");
        response.setHeader("HX-Reswap", "innerHTML");
        return buildChildModal(moduleId, childId, mode, text, failure.getMessage());
    }

    private String buildChildModal(String moduleId, String childId, EditMode mode, String text, String error) {
        Div body = new Div().withAttribute("style", "overflow-wrap: anywhere")
                .withChild(Alert.warning(PRIVATE_WARNING));
        if (error != null) {
            body.withChild(new Div().withId("editing-child-error").withAttribute("role", "alert")
                    .withChild(Alert.danger(error)));
        }
        body.withChild(new Paragraph("Child IDs describe current list positions, not persistent identities. Labels must be nonblank and comma-free."));
        if (mode == EditMode.USER_EDIT) {
            body.withChild(Alert.warning("Submitting starts from live content and replaces the whole pending proposal. Prior pending child and property changes are discarded."));
        }
        String url = childUrlTemplate("save-child", moduleId, mode)
                .replace("{id}", UriUtils.encode(childId, StandardCharsets.UTF_8));
        Form form = Form.create().withId("editing-child-form").withMethod(Form.Method.POST)
                .withAction(url).withHxPost(url).withHxTarget("#" + MODAL_CONTAINER_ID).withHxSwap("none");
        Div field = new Div().withClass("form-field");
        field.withChild(new HtmlTag("label").withClass("form-label")
                .withAttribute("for", "editing-child-text").withInnerText("Text"));
        TextArea input = TextArea.create("text").withRows(3).withWrap("soft")
                .withValue(text);
        input.withId("editing-child-text");
        input.withAttribute("autofocus", "autofocus");
        if (error != null) {
            input.withAttribute("aria-invalid", "true");
            input.withAttribute("aria-describedby", "editing-child-error");
        }
        field.withChild(input);
        form.withChild(field);
        form.withChild(Button.create("Save Item").withAttribute("type", "submit"));
        body.withChild(form);
        Button cancel = Button.create("Cancel").withStyle(Button.ButtonStyle.SECONDARY);
        cancel.withAttribute("onclick", "document.getElementById('" + MODAL_CONTAINER_ID + "').innerHTML = ''");
        return Modal.create().withTitle("Edit List Item").withBody(body).withFooter(cancel)
                .closeOnBackdrop(false).render();
    }

    private String childUrlTemplate(String action, String moduleId, EditMode mode) {
        // Encode raw IDs, including reserved characters such as matrix-parameter ';'.
        // Only the framework-owned child placeholder remains unencoded until substitution.
        return "/editing-demo/" + action + "/" + UriUtils.encode(moduleId, StandardCharsets.UTF_8)
                + "/{id}?editMode=" + mode.name();
    }

    @ModelAttribute
    private void renderedResponse(HttpServletResponse response) {
        varyHx(response);
    }

    private void varyHx(HttpServletResponse response) {
        List<String> tokens = new ArrayList<>();
        for (String value : response.getHeaders("Vary")) {
            for (String token : value.split(",")) {
                if (!token.isBlank()) tokens.add(token.trim());
            }
        }
        if (tokens.stream().noneMatch(token -> token.equalsIgnoreCase("HX-Request"))) {
            tokens.add("HX-Request");
        }
        response.setHeader("Vary", String.join(", ", tokens));
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
        String text = value == null ? "" : value;
        return text.length() > 5000 ? text.substring(0, 5000) : text;
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
        return ShellBuilder.create()
                .withPageTitle("SimplyPages Editing Demo")
                .withTopBanner(
                        BannerBuilder.create()
                                .withLayout(BannerBuilder.BannerLayout.HORIZONTAL)
                                .withTitle("SimplyPages - Editing Demo")
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
                .withContent(new RawHtml(content))
                .addCustomJs("/js/editing-demo.js")
                .build();
    }
}
