package io.mindspice.demo;


import io.mindspice.jhf.builders.ShellBuilder;
import io.mindspice.jhf.builders.SideNavBuilder;
import io.mindspice.jhf.builders.TopBannerBuilder;
import io.mindspice.jhf.components.Div;
import io.mindspice.jhf.components.Header;
import io.mindspice.jhf.components.Paragraph;
import io.mindspice.jhf.components.display.Alert;
import io.mindspice.jhf.components.forms.Button;
import io.mindspice.jhf.components.forms.Select;
import io.mindspice.jhf.components.forms.TextArea;
import io.mindspice.jhf.components.forms.TextInput;
import io.mindspice.jhf.core.Module;
import io.mindspice.jhf.editing.EditMode;
import io.mindspice.jhf.editing.EditablePage;
import io.mindspice.jhf.editing.EditableRow;
import io.mindspice.jhf.editing.EditableModule;
import io.mindspice.jhf.layout.Column;
import io.mindspice.jhf.layout.Row;
import io.mindspice.jhf.modules.ContentModule;
import io.mindspice.jhf.modules.GalleryModule;
import io.mindspice.jhf.modules.DataModule;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Comprehensive editing system demo showcasing:
 * - In-place editing with HTMX
 * - Staging edits for approval (USER_EDIT mode)
 * - Owner edits (OWNER_EDIT mode - changes go live immediately)
 * - Adding rows dynamically
 * - Adding modules to rows
 * - Deleting modules and rows
 */
@Controller
@RequestMapping("/editing-demo")
public class EditingDemoController {

    // Simple in-memory storage
    private static class DemoModule {
        String id;
        String type;
        int width;  // 1-12 column width
        Map<String, Object> data = new HashMap<>();

        DemoModule(String id, String type, int width) {
            this.id = id;
            this.type = type;
            this.width = width;
        }
    }

    private static class DemoRow {
        String id;
        int position;  // Row order in page
        List<DemoModule> modules = new ArrayList<>();

        DemoRow(String id, int position) {
            this.id = id;
            this.position = position;
        }
    }

    private static class PendingEdit {
        String moduleId;
        String type;
        Map<String, String> changes;
        long timestamp;

        PendingEdit(String moduleId, String type, Map<String, String> changes) {
            this.moduleId = moduleId;
            this.type = type;
            this.changes = new HashMap<>(changes);
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static class PageData {
        String pageId;
        String userId;
        long lastModified;
        List<DemoRow> rows = new ArrayList<>();

        PageData(String pageId, String userId) {
            this.pageId = pageId;
            this.userId = userId;
            this.lastModified = System.currentTimeMillis();
        }

        // Flat structure for future DB migration
        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("pageId", pageId);
            map.put("userId", userId);
            map.put("lastModified", lastModified);

            List<Map<String, Object>> rowList = new ArrayList<>();
            for (DemoRow row : rows) {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("rowId", row.id);
                rowMap.put("position", row.position);

                List<Map<String, Object>> moduleList = new ArrayList<>();
                for (DemoModule module : row.modules) {
                    Map<String, Object> moduleMap = new HashMap<>();
                    moduleMap.put("moduleId", module.id);
                    moduleMap.put("type", module.type);
                    moduleMap.put("width", module.width);
                    moduleMap.put("data", new HashMap<>(module.data));
                    moduleList.add(moduleMap);
                }
                rowMap.put("modules", moduleList);
                rowList.add(rowMap);
            }
            map.put("rows", rowList);
            return map;
        }
    }

    // Page storage: pageId -> PageData
    private final Map<String, PageData> pages = new ConcurrentHashMap<>();
    private final Map<String, PageData> savedPages = new ConcurrentHashMap<>();

    // Pending edits storage for USER_EDIT mode
    private final Map<String, List<PendingEdit>> pendingEdits = new ConcurrentHashMap<>();

    private final AtomicInteger idCounter = new AtomicInteger(1);

    public EditingDemoController() {
        initializeDemoData();
    }

    private void initializeDemoData() {
        PageData pageData = new PageData("demo-page", "demo-user");

        // Row 1: Two content modules
        DemoRow row1 = new DemoRow("row-1", 0);

        DemoModule m1 = new DemoModule("module-1", "content", 6);  // Half width
        m1.data.put("title", "Welcome to Editing Demo");
        m1.data.put("content", "# Editing System\n\nClick **Edit** to modify this content!\n\n- In-place editing\n- HTMX updates\n- No page reloads\n\nThis module uses **OWNER_EDIT** mode - changes go live immediately.");
        row1.modules.add(m1);

        DemoModule m2 = new DemoModule("module-2", "content", 6);  // Half width
        m2.data.put("title", "Approval Workflow");
        m2.data.put("content", "# User Edits\n\nTry editing this module to see the approval workflow in action.\n\nThis module uses **USER_EDIT** mode - edits are staged for approval.\n\n**Pending edits will show in the sidebar.**");
        row1.modules.add(m2);

        pageData.rows.add(row1);

        // Row 2: Gallery
        DemoRow row2 = new DemoRow("row-2", 1);

        DemoModule m3 = new DemoModule("module-3", "gallery", 12);  // Full width
        m3.data.put("title", "Sample Gallery");
        List<String> urls = List.of(
                "https://picsum.photos/400/300?random=1",
                "https://picsum.photos/400/300?random=2",
                "https://picsum.photos/400/300?random=3"
        );
        m3.data.put("urls", urls);
        row2.modules.add(m3);

        pageData.rows.add(row2);

        pages.put("demo-page", pageData);
        pendingEdits.put("demo-page", new ArrayList<>());
    }

    @GetMapping
    @ResponseBody
    public String viewPage(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        response.setHeader("Vary", "HX-Request");

        String content = renderEditablePage();

        if (hxRequest != null) {
            return content;
        }

        return renderWithShell(content);
    }

    private String renderEditablePage() {
        PageData pageData = pages.get("demo-page");
        if (pageData == null) {
            return Alert.danger("Page not found").render();
        }

        Div pageContainer = new Div()
                .withClass("editable-page-wrapper");

        for (DemoRow demoRow : pageData.rows) {
            Row row = new Row();

            // Determine edit mode: module-1 is OWNER_EDIT, module-2 is USER_EDIT, rest are OWNER_EDIT
            for (DemoModule dm : demoRow.modules) {
                Module module = createModule(dm);
                EditMode mode = dm.id.equals("module-2") ? EditMode.USER_EDIT : EditMode.OWNER_EDIT;

                EditableModule editableModule = EditableModule.wrap(module)
                        .withModuleId(dm.id)
                        .withEditUrl("/editing-demo/api/modules/" + dm.id + "/edit")
                        .withDeleteUrl("/editing-demo/api/modules/" + dm.id + "/delete")
                        .withEditMode(mode);

                // Use module's configured width instead of calculating
                Column col = Column.create().withWidth(dm.width).withChild(editableModule);
                row.addColumn(col);
            }

            // Build custom row wrapper with add module button
            Div rowWrapper = new Div()
                    .withAttribute("id", "row-" + demoRow.id)
                    .withClass("editable-row-wrapper");

            rowWrapper.withChild(row);

            // Add "Add Module" button if space available
            if (demoRow.modules.size() < 3) {
                Div addModuleSection = new Div()
                        .withClass("add-module-section");

                Button addBtn = Button.create("+ Add Module")
                        .withStyle(Button.ButtonStyle.SECONDARY);

                addBtn.withAttribute("hx-get", "/editing-demo/api/rows/" + demoRow.id + "/add-module-modal");
                addBtn.withAttribute("hx-target", "#add-module-modal");
                addBtn.withAttribute("hx-swap", "innerHTML");

                addModuleSection.withChild(addBtn);
                rowWrapper.withChild(addModuleSection);
            }

            pageContainer.withChild(rowWrapper);

            // Add insert row button after each row
            Div insertRowSection = new Div()
                    .withClass("insert-row-section");

            Button insertBtn = Button.create("Add Row Below")
                    .withStyle(Button.ButtonStyle.LINK);

            insertBtn.withAttribute("hx-post", "/editing-demo/api/pages/demo-page/rows/insert?afterRowId=" + demoRow.id);
            insertBtn.withAttribute("hx-target", "#page-demo-page");
            insertBtn.withAttribute("hx-swap", "outerHTML");

            insertRowSection.withChild(insertBtn);
            pageContainer.withChild(insertRowSection);
        }

        String pageHtml = pageContainer.render();

        // Save/Load controls
        Div saveLoadControls = new Div()
                .withClass("save-load-controls mb-3 d-flex gap-2 justify-content-end");

        Button saveBtn = Button.create("Save Page")
                .withStyle(Button.ButtonStyle.SUCCESS);
        saveBtn.withAttribute("hx-post", "/editing-demo/api/pages/demo-page/save");
        saveBtn.withAttribute("hx-target", "#save-status");
        saveBtn.withAttribute("hx-swap", "innerHTML");

        Button loadBtn = Button.create("Load Saved")
                .withStyle(Button.ButtonStyle.SECONDARY);
        loadBtn.withAttribute("hx-post", "/editing-demo/api/pages/demo-page/load");
        loadBtn.withAttribute("hx-target", "#page-demo-page");
        loadBtn.withAttribute("hx-swap", "outerHTML");
        loadBtn.withAttribute("hx-confirm", "This will discard unsaved changes. Continue?");

        saveLoadControls.withChild(saveBtn);
        saveLoadControls.withChild(loadBtn);

        Div saveStatus = new Div().withAttribute("id", "save-status");

        // Add modal containers and pending edits sidebar
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"row\" id=\"page-demo-page\">");
        html.append("<div class=\"col\" style=\"flex: 1 1 0;\">");
        html.append("<div class=\"save-load-wrapper mb-3\">");
        html.append(saveLoadControls.render());
        html.append(saveStatus.render());
        html.append("</div>");
        html.append(pageHtml);
        html.append("<div id=\"add-module-modal\" class=\"mt-4\"></div>");
        html.append("<div id=\"edit-module-modal\" class=\"mt-4\"></div>");
        html.append("</div>");

        // Pending edits sidebar
        html.append("<div class=\"col-3\">");
        html.append(renderPendingEditsSidebar());
        html.append("</div>");
        html.append("</div>");

        return html.toString();
    }

    private String renderPendingEditsSidebar() {
        List<PendingEdit> pending = pendingEdits.get("demo-page");

        Div sidebar = new Div()
                .withClass("pending-edits-sidebar p-3")
                .withAttribute("id", "pending-edits-sidebar")
                .withAttribute("style", "background-color: #f8f9fa; border-radius: 8px; min-height: 200px;");

        sidebar.withChild(Header.H4("Pending Edits"));

        if (pending.isEmpty()) {
            sidebar.withChild(new Paragraph("No pending edits").withClass("text-muted"));
        } else {
            for (PendingEdit edit : pending) {
                Div editCard = new Div().withClass("card mb-2 p-2");
                editCard.withChild(new Paragraph("Module: " + edit.moduleId).withClass("fw-bold mb-1"));
                editCard.withChild(new Paragraph("Type: " + edit.type).withClass("small text-muted mb-2"));

                Div buttons = new Div().withClass("d-flex gap-1");

                Button approveBtn = Button.create("Approve").withStyle(Button.ButtonStyle.SUCCESS).small();
                approveBtn.withAttribute("hx-post", "/editing-demo/api/pending/" + edit.moduleId + "/approve");
                approveBtn.withAttribute("hx-target", "#page-demo-page");
                approveBtn.withAttribute("hx-swap", "outerHTML");

                Button rejectBtn = Button.create("Reject").withStyle(Button.ButtonStyle.DANGER).small();
                rejectBtn.withAttribute("hx-delete", "/editing-demo/api/pending/" + edit.moduleId + "/reject");
                rejectBtn.withAttribute("hx-target", "#pending-edits-sidebar");
                rejectBtn.withAttribute("hx-swap", "outerHTML");

                buttons.withChild(approveBtn);
                buttons.withChild(rejectBtn);
                editCard.withChild(buttons);

                sidebar.withChild(editCard);
            }
        }

        return sidebar.render();
    }

    private Module createModule(DemoModule dm) {
        return switch (dm.type) {
            case "content" -> ContentModule.create()
                    .withTitle((String) dm.data.get("title"))
                    .withContent((String) dm.data.get("content"));

            case "gallery" -> {
                GalleryModule gallery = GalleryModule.create()
                        .withTitle((String) dm.data.get("title"));

                @SuppressWarnings("unchecked")
                List<String> urls = (List<String>) dm.data.get("urls");
                if (urls != null) {
                    for (int i = 0; i < urls.size(); i++) {
                        gallery.addImage(urls.get(i), "Image " + (i + 1));
                    }
                }
                yield gallery;
            }

            default -> ContentModule.create()
                    .withTitle("Unknown")
                    .withContent("Type: " + dm.type);
        };
    }

    // ===== MODULE EDIT ENDPOINTS =====

    @GetMapping("/api/modules/{moduleId}/edit")
    @ResponseBody
    public String editModule(@PathVariable String moduleId) {
        DemoModule dm = findModule(moduleId);
        if (dm == null) {
            return Alert.danger("Module not found").render();
        }
        return renderEditModal(dm);
    }

    private String renderEditModal(DemoModule dm) {
        Div modal = new Div()
                .withClass("modal-content p-4")
                .withAttribute("style", "background-color: white; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); max-width: 600px;");

        modal.withChild(Header.H3("Edit Module"));

        EditMode mode = dm.id.equals("module-2") ? EditMode.USER_EDIT : EditMode.OWNER_EDIT;
        if (mode == EditMode.USER_EDIT) {
            modal.withChild(Alert.info("Changes will be staged for approval").withClass("mb-3"));
        }

        // Width controls
        Div widthSection = new Div().withClass("mb-3");
        widthSection.withChild(new Paragraph("Width:").withClass("fw-bold mb-2"));

        // Preset buttons
        Div presetButtons = new Div().withClass("d-flex gap-2 mb-2");
        String[] presets = {"Full:12", "3/4:9", "1/2:6", "1/4:3"};
        for (String preset : presets) {
            String[] parts = preset.split(":");
            Button presetBtn = Button.create(parts[0])
                    .withStyle(Button.ButtonStyle.SECONDARY).small();
            presetBtn.withAttribute("type", "button");
            presetBtn.withAttribute("onclick",
                    "document.getElementById('module-width-edit').value=" + parts[1]);
            presetButtons.withChild(presetBtn);
        }
        widthSection.withChild(presetButtons);

        // Precise width input
        Div preciseWidth = new Div().withClass("d-flex gap-2 align-items-center");
        preciseWidth.withChild(new Paragraph("Precise:").withClass("mb-0"));
        TextInput widthInput = TextInput.create("width");
        widthInput.withAttribute("type", "number");
        widthInput.withAttribute("min", "1");
        widthInput.withAttribute("max", "12");
        widthInput.withValue(String.valueOf(dm.width));
        widthInput.withAttribute("id", "module-width-edit");
        widthInput.withAttribute("style", "width: 80px;");
        preciseWidth.withChild(widthInput);
        preciseWidth.withChild(new Paragraph("/ 12 columns").withClass("mb-0 text-muted small"));
        widthSection.withChild(preciseWidth);
        modal.withChild(widthSection);

        // Type-specific fields
        if ("content".equals(dm.type)) {
            Div titleGroup = new Div().withClass("mb-3");
            titleGroup.withChild(new Paragraph("Title:").withClass("fw-bold mb-1"));
            TextInput titleInput = TextInput.create("title");
            titleInput.withValue((String) dm.data.getOrDefault("title", ""));
            titleInput.withAttribute("style", "width: 100%;");
            titleGroup.withChild(titleInput);
            modal.withChild(titleGroup);

            Div contentGroup = new Div().withClass("mb-3");
            contentGroup.withChild(new Paragraph("Content (Markdown):").withClass("fw-bold mb-1"));
            TextArea contentArea = TextArea.create("content");
            contentArea.withValue((String) dm.data.getOrDefault("content", ""));
            contentArea.withRows(15);
            contentArea.withAttribute("style", "width: 100%;");
            contentGroup.withChild(contentArea);
            modal.withChild(contentGroup);
        } else if ("gallery".equals(dm.type)) {
            modal.withChild(new Paragraph("Gallery editing - simplified for demo")
                    .withClass("text-muted mb-3"));
        }

        // Buttons
        Div buttons = new Div().withClass("d-flex gap-2 justify-content-end");

        Button saveBtn = Button.submit("Save Changes").withStyle(Button.ButtonStyle.PRIMARY);
        saveBtn.withAttribute("hx-post", "/editing-demo/api/modules/" + dm.id + "/update");
        saveBtn.withAttribute("hx-target", "#page-demo-page");
        saveBtn.withAttribute("hx-swap", "outerHTML");
        saveBtn.withAttribute("hx-include", "closest .modal-content");
        buttons.withChild(saveBtn);

        Button cancelBtn = Button.create("Cancel").withStyle(Button.ButtonStyle.SECONDARY);
        cancelBtn.withAttribute("hx-get", "/editing-demo/api/clear-modal");
        cancelBtn.withAttribute("hx-target", "#edit-module-modal");
        cancelBtn.withAttribute("hx-swap", "innerHTML");
        buttons.withChild(cancelBtn);

        modal.withChild(buttons);

        return modal.render();
    }

    private String renderContentForm(DemoModule dm) {
        String title = (String) dm.data.get("title");
        String content = (String) dm.data.get("content");

        // Determine edit mode
        EditMode mode = dm.id.equals("module-2") ? EditMode.USER_EDIT : EditMode.OWNER_EDIT;

        Div form = new Div();
        form.withAttribute("id", dm.id);
        form.withClass("module content-module p-3");

        form.withChild(Header.H3("Edit Content"));

        if (mode == EditMode.USER_EDIT) {
            form.withChild(Alert.info("Changes will be staged for approval").withClass("mb-3"));
        }

        // Title field
        Div titleGroup = new Div().withClass("mb-3");
        titleGroup.withChild(new Paragraph("Title:").withClass("fw-bold mb-1"));
        TextInput titleInput = TextInput.create("title");
        titleInput.withValue(title != null ? title : "");
        titleInput.withAttribute("style", "width: 100%; max-width: 600px;");
        titleGroup.withChild(titleInput);
        form.withChild(titleGroup);

        // Content field
        Div contentGroup = new Div().withClass("mb-3");
        contentGroup.withChild(new Paragraph("Content (Markdown):").withClass("fw-bold mb-1"));
        TextArea contentArea = TextArea.create("content");
        contentArea.withValue(content != null ? content : "");
        contentArea.withRows(15);
        contentArea.withAttribute("style", "width: 100%; max-width: 800px;");
        contentGroup.withChild(contentArea);
        form.withChild(contentGroup);

        // Buttons
        Div buttons = new Div().withClass("d-flex gap-2");

        Button saveBtn = Button.submit("Save Changes");
        saveBtn.withAttribute("hx-post", "/editing-demo/api/modules/" + dm.id + "/update");
        saveBtn.withAttribute("hx-target", "#" + dm.id);
        saveBtn.withAttribute("hx-swap", "outerHTML");
        saveBtn.withAttribute("hx-include", "closest .module");
        buttons.withChild(saveBtn);

        Button cancelBtn = Button.create("Cancel").withStyle(Button.ButtonStyle.SECONDARY);
        cancelBtn.withAttribute("hx-get", "/editing-demo/api/modules/" + dm.id + "/view");
        cancelBtn.withAttribute("hx-target", "#" + dm.id);
        cancelBtn.withAttribute("hx-swap", "outerHTML");
        buttons.withChild(cancelBtn);

        form.withChild(buttons);

        return form.render();
    }

    private String renderGalleryForm(DemoModule dm) {
        Div form = new Div();
        form.withAttribute("id", dm.id);
        form.withClass("module gallery-module p-3");

        form.withChild(Header.H3("Edit Gallery"));
        form.withChild(new Paragraph("Gallery editing - simplified for demo").withClass("text-muted mb-3"));

        Button cancelBtn = Button.create("Back to View").withStyle(Button.ButtonStyle.SECONDARY);
        cancelBtn.withAttribute("hx-get", "/editing-demo/api/modules/" + dm.id + "/view");
        cancelBtn.withAttribute("hx-target", "#" + dm.id);
        cancelBtn.withAttribute("hx-swap", "outerHTML");
        form.withChild(cancelBtn);

        return form.render();
    }

    @PostMapping("/api/modules/{moduleId}/update")
    @ResponseBody
    public String updateModule(
            @PathVariable String moduleId,
            @RequestParam Map<String, String> formData
    ) {
        DemoModule dm = findModule(moduleId);
        if (dm == null) {
            return "" + Alert.danger("Module not found").render();
        }

        // Determine edit mode
        EditMode mode = moduleId.equals("module-2") ? EditMode.USER_EDIT : EditMode.OWNER_EDIT;

        if (mode == EditMode.USER_EDIT) {
            // Stage edit for approval
            List<PendingEdit> pending = pendingEdits.get("demo-page");

            // Remove any existing pending edit for this module
            pending.removeIf(p -> p.moduleId.equals(moduleId));

            // Add new pending edit
            pending.add(new PendingEdit(moduleId, dm.type, formData));

            // Clear modal and re-render page to show pending status
            String pendingSidebar = renderPendingEditsSidebar();
            return "" +
                   "<div hx-swap-oob=\"true\" id=\"edit-module-modal\"></div>" +
                   "<div hx-swap-oob=\"true\" id=\"pending-edits-sidebar\">" + pendingSidebar + "</div>" +
                   "<div hx-swap-oob=\"true\" id=\"page-demo-page\">" + renderEditablePage() + "</div>";
        } else {
            // OWNER_EDIT: Apply changes immediately
            // Update width if provided
            if (formData.containsKey("width")) {
                try {
                    int width = Integer.parseInt(formData.get("width"));
                    if (width >= 1 && width <= 12) {
                        dm.width = width;
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid width
                }
            }

            // Update content based on type
            if ("content".equals(dm.type)) {
                dm.data.put("title", formData.get("title"));
                dm.data.put("content", formData.get("content"));
            }

            // Clear modal and re-render entire page to reflect width changes
            return "" +
                   "<div hx-swap-oob=\"true\" id=\"edit-module-modal\"></div>" +
                   "<div hx-swap-oob=\"true\" id=\"page-demo-page\">" + renderEditablePage() + "</div>";
        }
    }

    @GetMapping("/api/modules/{moduleId}/view")
    @ResponseBody
    public String viewModule(@PathVariable String moduleId) {
        DemoModule dm = findModule(moduleId);
        if (dm == null) {
            return "" + Alert.danger("Module not found").render();
        }

        EditMode mode = moduleId.equals("module-2") ? EditMode.USER_EDIT : EditMode.OWNER_EDIT;

        Module module = createModule(dm);
        EditableModule editable = EditableModule.wrap(module)
                .withModuleId(moduleId)
                .withEditUrl("/editing-demo/api/modules/" + moduleId + "/edit")
                .withDeleteUrl("/editing-demo/api/modules/" + moduleId + "/delete")
                .withEditMode(mode);

        return "" + editable.render();
    }

    @DeleteMapping("/api/modules/{moduleId}/delete")
    @ResponseBody
    public String deleteModule(@PathVariable String moduleId) {
        for (PageData pageData : pages.values()) {
            for (DemoRow row : pageData.rows) {
                row.modules.removeIf(m -> m.id.equals(moduleId));
            }
        }
        return "";  // Empty - HTMX removes element
    }

    // ===== PENDING EDIT APPROVAL ENDPOINTS =====

    @PostMapping("/api/pending/{moduleId}/approve")
    @ResponseBody
    public String approvePendingEdit(@PathVariable String moduleId) {
        List<PendingEdit> pending = pendingEdits.get("demo-page");
        PendingEdit edit = pending.stream()
                .filter(p -> p.moduleId.equals(moduleId))
                .findFirst()
                .orElse(null);

        if (edit == null) {
            return "" + Alert.warning("Pending edit not found").render();
        }

        // Apply the changes
        DemoModule dm = findModule(moduleId);
        if (dm != null && "content".equals(dm.type)) {
            dm.data.put("title", edit.changes.get("title"));
            dm.data.put("content", edit.changes.get("content"));
        }

        // Remove from pending
        pending.remove(edit);

        // Return the entire page to refresh everything
        return "" + renderEditablePage();
    }

    @DeleteMapping("/api/pending/{moduleId}/reject")
    @ResponseBody
    public String rejectPendingEdit(@PathVariable String moduleId) {
        List<PendingEdit> pending = pendingEdits.get("demo-page");
        pending.removeIf(p -> p.moduleId.equals(moduleId));

        return "" + renderPendingEditsSidebar();
    }

    // ===== ADD MODULE ENDPOINTS =====

    @GetMapping("/api/rows/{rowId}/add-module-modal")
    @ResponseBody
    public String showAddModuleModal(@PathVariable String rowId) {
        Div modal = new Div()
                .withClass("modal-content p-4")
                .withAttribute("style", "background-color: white; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); max-width: 600px;");

        modal.withChild(Header.H3("Add Module"));

        // Module type dropdown
        Div typeGroup = new Div().withClass("mb-3");
        typeGroup.withChild(new Paragraph("Module Type:").withClass("fw-bold mb-1"));
        Select typeSelect = Select.create("moduleType")
                .addOption("content", "Content (Markdown)", true)
                .addOption("gallery", "Gallery (Images)", false)
                .addOption("callout", "Callout Box", false)
                .addOption("form", "Form", false)
                .addOption("hero", "Hero Banner", false)
                .addOption("stats", "Statistics", false)
                .addOption("timeline", "Timeline", false)
                .addOption("quote", "Quote Block", false);
        typeSelect.withAttribute("style", "width: 100%;");
        typeGroup.withChild(typeSelect);
        modal.withChild(typeGroup);

        // Width controls
        Div widthSection = new Div().withClass("mb-3");
        widthSection.withChild(new Paragraph("Width:").withClass("fw-bold mb-2"));

        // Preset buttons
        Div presetButtons = new Div().withClass("d-flex gap-2 mb-2");
        String[] presets = {"Full:12", "3/4:9", "1/2:6", "1/4:3"};
        for (String preset : presets) {
            String[] parts = preset.split(":");
            Button presetBtn = Button.create(parts[0])
                    .withStyle(Button.ButtonStyle.SECONDARY).small();
            presetBtn.withAttribute("type", "button");
            presetBtn.withAttribute("onclick",
                    "document.getElementById('module-width').value=" + parts[1]);
            presetButtons.withChild(presetBtn);
        }
        widthSection.withChild(presetButtons);

        // Precise width input
        Div preciseWidth = new Div().withClass("d-flex gap-2 align-items-center");
        preciseWidth.withChild(new Paragraph("Precise:").withClass("mb-0"));
        TextInput widthInput = TextInput.create("width");
        widthInput.withAttribute("type", "number");
        widthInput.withAttribute("min", "1");
        widthInput.withAttribute("max", "12");
        widthInput.withValue("6");
        widthInput.withAttribute("id", "module-width");
        widthInput.withAttribute("style", "width: 80px;");
        preciseWidth.withChild(widthInput);
        preciseWidth.withChild(new Paragraph("/ 12 columns").withClass("mb-0 text-muted small"));
        widthSection.withChild(preciseWidth);
        modal.withChild(widthSection);

        // Title field
        Div titleGroup = new Div().withClass("mb-3");
        titleGroup.withChild(new Paragraph("Title:").withClass("fw-bold mb-1"));
        TextInput titleInput = TextInput.create("title");
        titleInput.withPlaceholder("Module title");
        titleInput.withAttribute("style", "width: 100%;");
        titleGroup.withChild(titleInput);
        modal.withChild(titleGroup);

        // Content field
        Div contentGroup = new Div().withClass("mb-3");
        contentGroup.withChild(new Paragraph("Content:").withClass("fw-bold mb-1"));
        TextArea contentArea = TextArea.create("content");
        contentArea.withPlaceholder("# Heading\n\nYour content here...");
        contentArea.withRows(8);
        contentArea.withAttribute("style", "width: 100%;");
        contentGroup.withChild(contentArea);
        modal.withChild(contentGroup);

        // Buttons
        Div buttons = new Div().withClass("d-flex gap-2 justify-content-end");

        Button addBtn = Button.submit("Add Module").withStyle(Button.ButtonStyle.PRIMARY);
        addBtn.withAttribute("hx-post", "/editing-demo/api/rows/" + rowId + "/add-module");
        addBtn.withAttribute("hx-target", "#page-demo-page");
        addBtn.withAttribute("hx-swap", "outerHTML");
        addBtn.withAttribute("hx-include", "closest .modal-content");
        buttons.withChild(addBtn);

        Button cancelBtn = Button.create("Cancel").withStyle(Button.ButtonStyle.SECONDARY);
        cancelBtn.withAttribute("hx-get", "/editing-demo/api/clear-modal");
        cancelBtn.withAttribute("hx-target", "#add-module-modal");
        cancelBtn.withAttribute("hx-swap", "innerHTML");
        buttons.withChild(cancelBtn);

        modal.withChild(buttons);

        return "" + modal.render();
    }

    @GetMapping("/api/clear-modal")
    @ResponseBody
    public String clearModal() {
        return "";
    }

    @PostMapping("/api/rows/{rowId}/add-module")
    @ResponseBody
    public String addModule(
            @PathVariable String rowId,
            @RequestParam Map<String, String> formData
    ) {
        DemoRow row = findRow(rowId);
        if (row == null) {
            return "" + Alert.danger("Row not found").render();
        }

        if (row.modules.size() >= 3) {
            return "" + Alert.warning("Maximum 3 modules per row").render();
        }

        // Create new module
        String moduleId = "module-" + idCounter.incrementAndGet();
        String type = formData.getOrDefault("moduleType", "content");

        // Get and validate width
        int width;
        try {
            width = Integer.parseInt(formData.getOrDefault("width", "6"));
            if (width < 1 || width > 12) {
                return "" + Alert.danger("Width must be between 1 and 12").render();
            }
        } catch (NumberFormatException e) {
            width = 6;  // Default to half width
        }

        DemoModule newModule = new DemoModule(moduleId, type, width);
        newModule.data.put("title", formData.getOrDefault("title", "New Module"));

        if ("content".equals(type)) {
            newModule.data.put("content", formData.getOrDefault("content", "New content"));
        } else if ("gallery".equals(type)) {
            newModule.data.put("urls", List.of(
                "https://picsum.photos/400/300?random=" + idCounter.get()
            ));
        }

        row.modules.add(newModule);

        // Re-render the entire page to update the row
        String pageHtml = renderEditablePage();

        // Clear the modal
        return "" +
               "<div hx-swap-oob=\"true\" id=\"add-module-modal\"></div>" +
               "<div hx-swap-oob=\"true\" id=\"page-demo-page\">" + pageHtml + "</div>";
    }

    // ===== ADD/INSERT ROW ENDPOINTS =====

    @PostMapping("/api/pages/{pageId}/rows/add")
    @ResponseBody
    public String addRow(@PathVariable String pageId) {
        PageData pageData = pages.get(pageId);
        if (pageData == null) {
            return "" + Alert.danger("Page not found").render();
        }

        // Create new empty row at end
        String rowId = "row-" + idCounter.incrementAndGet();
        int newPosition = pageData.rows.size();
        DemoRow newRow = new DemoRow(rowId, newPosition);

        // Add a placeholder module
        String moduleId = "module-" + idCounter.incrementAndGet();
        DemoModule module = new DemoModule(moduleId, "content", 12);  // Full width
        module.data.put("title", "New Row");
        module.data.put("content", "Click **Edit** to add content to this new row!");
        newRow.modules.add(module);

        pageData.rows.add(newRow);

        // Return the entire page
        return "" + renderEditablePage();
    }

    @PostMapping("/api/pages/{pageId}/rows/insert")
    @ResponseBody
    public String insertRow(
            @PathVariable String pageId,
            @RequestParam(required = false) String afterRowId
    ) {
        PageData pageData = pages.get(pageId);
        if (pageData == null) {
            return "" + Alert.danger("Page not found").render();
        }

        String rowId = "row-" + idCounter.incrementAndGet();
        int newPosition;

        if (afterRowId == null) {
            // No afterRowId - add at end
            newPosition = pageData.rows.size();
        } else {
            // Find the row to insert after
            int afterIndex = -1;
            for (int i = 0; i < pageData.rows.size(); i++) {
                if (pageData.rows.get(i).id.equals(afterRowId)) {
                    afterIndex = i;
                    break;
                }
            }

            if (afterIndex == -1) {
                // Row not found, add at end
                newPosition = pageData.rows.size();
            } else {
                // Insert after the found row
                newPosition = afterIndex + 1;
            }
        }

        DemoRow newRow = new DemoRow(rowId, newPosition);

        // Add placeholder module
        String moduleId = "module-" + idCounter.incrementAndGet();
        DemoModule module = new DemoModule(moduleId, "content", 12);  // Full width
        module.data.put("title", "New Row");
        module.data.put("content", "Click **Edit** to customize this row!");
        newRow.modules.add(module);

        // Insert at correct position
        pageData.rows.add(newPosition, newRow);

        // Renumber positions after insertion
        for (int i = 0; i < pageData.rows.size(); i++) {
            pageData.rows.get(i).position = i;
        }

        return "" + renderEditablePage();
    }

    // ===== SAVE/LOAD ENDPOINTS =====

    @PostMapping("/api/pages/{pageId}/save")
    @ResponseBody
    public String savePage(@PathVariable String pageId) {
        PageData current = pages.get(pageId);
        if (current == null) {
            return "" + Alert.danger("Page not found").render();
        }

        // Deep copy to saved state
        PageData saved = new PageData(current.pageId, current.userId);
        saved.lastModified = System.currentTimeMillis();

        for (DemoRow row : current.rows) {
            DemoRow rowCopy = new DemoRow(row.id, row.position);
            for (DemoModule module : row.modules) {
                DemoModule moduleCopy = new DemoModule(module.id, module.type, module.width);
                moduleCopy.data.putAll(module.data);
                rowCopy.modules.add(moduleCopy);
            }
            saved.rows.add(rowCopy);
        }

        savedPages.put(pageId, saved);

        return "" + Alert.success("Page saved! Last saved: " + new java.util.Date(saved.lastModified))
                .render();
    }

    @PostMapping("/api/pages/{pageId}/load")
    @ResponseBody
    public String loadPage(@PathVariable String pageId) {
        PageData saved = savedPages.get(pageId);
        if (saved == null) {
            return "" + Alert.warning("No saved state found").render();
        }

        // Deep copy from saved to current
        PageData loaded = new PageData(saved.pageId, saved.userId);
        loaded.lastModified = saved.lastModified;

        for (DemoRow row : saved.rows) {
            DemoRow rowCopy = new DemoRow(row.id, row.position);
            for (DemoModule module : row.modules) {
                DemoModule moduleCopy = new DemoModule(module.id, module.type, module.width);
                moduleCopy.data.putAll(module.data);
                rowCopy.modules.add(moduleCopy);
            }
            loaded.rows.add(rowCopy);
        }

        pages.put(pageId, loaded);
        pendingEdits.put(pageId, new ArrayList<>());  // Clear pending edits

        return "" + renderEditablePage();
    }

    // ===== HELPER METHODS =====

    private DemoModule findModule(String id) {
        for (PageData pageData : pages.values()) {
            for (DemoRow row : pageData.rows) {
                for (DemoModule m : row.modules) {
                    if (m.id.equals(id)) {
                        return m;
                    }
                }
            }
        }
        return null;
    }

    private DemoRow findRow(String id) {
        for (PageData pageData : pages.values()) {
            for (DemoRow row : pageData.rows) {
                if (row.id.equals(id)) {
                    return row;
                }
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
                                .withSubtitle("In-place editing, approval workflow, and dynamic page building")
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
                                .addLink("In-Place Editing", "#", "📝")
                                .addLink("Approval Workflow", "#", "✅")
                                .addLink("Add Modules", "#", "➕")
                                .addLink("Add Rows", "#", "📊")
                                .addSection("Main Demo")
                                .addLink("Back to Home", "/home", "🏠")
                                .build()
                )
                .build();

        // Replace the HTMX-loading content-area with our actual content
        return shell.replaceAll("<div id=\"content-area\"[^>]*>", "<div id=\"content-area\">")
                .replace("<div id=\"content-area\"></div>",
                        "<div id=\"content-area\">" + content + "</div>");
    }
}
