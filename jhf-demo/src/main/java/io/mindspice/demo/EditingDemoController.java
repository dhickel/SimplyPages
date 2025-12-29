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
        Map<String, Object> data = new HashMap<>();

        DemoModule(String id, String type) {
            this.id = id;
            this.type = type;
        }
    }

    private static class DemoRow {
        String id;
        List<DemoModule> modules = new ArrayList<>();

        DemoRow(String id) {
            this.id = id;
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

    // Page storage: pageId -> list of rows
    private final Map<String, List<DemoRow>> pages = new ConcurrentHashMap<>();

    // Pending edits storage for USER_EDIT mode
    private final Map<String, List<PendingEdit>> pendingEdits = new ConcurrentHashMap<>();

    private final AtomicInteger idCounter = new AtomicInteger(1);

    public EditingDemoController() {
        initializeDemoData();
    }

    private void initializeDemoData() {
        List<DemoRow> rows = new ArrayList<>();

        // Row 1: Two content modules
        DemoRow row1 = new DemoRow("row-1");

        DemoModule m1 = new DemoModule("module-1", "content");
        m1.data.put("title", "Welcome to Editing Demo");
        m1.data.put("content", "# Editing System\n\nClick **Edit** to modify this content!\n\n- In-place editing\n- HTMX updates\n- No page reloads\n\nThis module uses **OWNER_EDIT** mode - changes go live immediately.");
        row1.modules.add(m1);

        DemoModule m2 = new DemoModule("module-2", "content");
        m2.data.put("title", "Approval Workflow");
        m2.data.put("content", "# User Edits\n\nTry editing this module to see the approval workflow in action.\n\nThis module uses **USER_EDIT** mode - edits are staged for approval.\n\n**Pending edits will show in the sidebar.**");
        row1.modules.add(m2);

        rows.add(row1);

        // Row 2: Gallery
        DemoRow row2 = new DemoRow("row-2");

        DemoModule m3 = new DemoModule("module-3", "gallery");
        m3.data.put("title", "Sample Gallery");
        List<String> urls = List.of(
                "https://picsum.photos/400/300?random=1",
                "https://picsum.photos/400/300?random=2",
                "https://picsum.photos/400/300?random=3"
        );
        m3.data.put("urls", urls);
        row2.modules.add(m3);

        rows.add(row2);

        pages.put("demo-page", rows);
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
        List<DemoRow> rows = pages.get("demo-page");

        Div pageContainer = new Div()
                .withClass("editable-page-wrapper");

        for (DemoRow demoRow : rows) {
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

                // Manually add to row with calculated width
                int moduleCount = demoRow.modules.size();
                int colWidth = 12 / Math.max(1, moduleCount);
                Column col = Column.create().withWidth(colWidth).withChild(editableModule);
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

                addBtn.withAttribute("hx-get", "/editing-demo/api/rows/" + demoRow.id + "/add-module-form");
                addBtn.withAttribute("hx-target", "#add-module-modal");
                addBtn.withAttribute("hx-swap", "innerHTML");

                addModuleSection.withChild(addBtn);
                rowWrapper.withChild(addModuleSection);
            }

            pageContainer.withChild(rowWrapper);

            // Add insert row button after each row
            Div insertRowSection = new Div()
                    .withClass("insert-row-section");

            Button insertBtn = Button.create("+ Insert Row Below")
                    .withStyle(Button.ButtonStyle.LINK);

            insertBtn.withAttribute("hx-post", "/editing-demo/api/pages/demo-page/rows/insert");
            insertBtn.withAttribute("hx-target", "#page-demo-page");
            insertBtn.withAttribute("hx-swap", "outerHTML");

            insertRowSection.withChild(insertBtn);
            pageContainer.withChild(insertRowSection);
        }

        // Add final "Add Row at Bottom" button
        Div finalAddSection = new Div()
                .withClass("add-row-final");

        Button finalAddBtn = Button.create("+ Add Row at Bottom")
                .withStyle(Button.ButtonStyle.PRIMARY);

        finalAddBtn.withAttribute("hx-post", "/editing-demo/api/pages/demo-page/rows/add");
        finalAddBtn.withAttribute("hx-target", "#page-demo-page");
        finalAddBtn.withAttribute("hx-swap", "outerHTML");

        finalAddSection.withChild(finalAddBtn);
        pageContainer.withChild(finalAddSection);

        String pageHtml = pageContainer.render();

        // Add modal container and pending edits sidebar
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"row\" id=\"page-demo-page\">");
        html.append("<div class=\"col\" style=\"flex: 1 1 0;\">");
        html.append(pageHtml);
        html.append("<div id=\"add-module-modal\" class=\"mt-4\"></div>");
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

        return switch (dm.type) {
            case "content" -> renderContentForm(dm);
            case "gallery" -> renderGalleryForm(dm);
            default -> Alert.warning("Edit not supported").render();
        };
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

            // Return module with pending badge
            Module module = createModule(dm);
            EditableModule editable = EditableModule.wrap(module)
                    .withModuleId(moduleId)
                    .withEditUrl("/editing-demo/api/modules/" + moduleId + "/edit")
                    .withDeleteUrl("/editing-demo/api/modules/" + moduleId + "/delete")
                    .withEditMode(mode);

            String moduleHtml = editable.render();
            String pendingSidebar = renderPendingEditsSidebar();

            // Return both module and updated sidebar
            return "" +
                   "<div hx-swap-oob=\"true\" id=\"pending-edits-sidebar\">" + pendingSidebar + "</div>" +
                   moduleHtml;
        } else {
            // OWNER_EDIT: Apply changes immediately
            if ("content".equals(dm.type)) {
                dm.data.put("title", formData.get("title"));
                dm.data.put("content", formData.get("content"));
            }

            // Return updated editable module
            Module module = createModule(dm);
            EditableModule editable = EditableModule.wrap(module)
                    .withModuleId(moduleId)
                    .withEditUrl("/editing-demo/api/modules/" + moduleId + "/edit")
                    .withDeleteUrl("/editing-demo/api/modules/" + moduleId + "/delete")
                    .withEditMode(mode);

            return "" + editable.render();
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
        for (List<DemoRow> rows : pages.values()) {
            for (DemoRow row : rows) {
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

    @GetMapping("/api/rows/{rowId}/add-module-form")
    @ResponseBody
    public String showAddModuleForm(@PathVariable String rowId) {
        Div modal = new Div()
                .withClass("modal-content p-4")
                .withAttribute("style", "background-color: white; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);");

        modal.withChild(Header.H3("Add Module"));
        modal.withChild(new Paragraph("Select module type:").withClass("mb-3"));

        // Module type selector
        Select typeSelect = Select.create("moduleType")
                .addOption("content", "Content Module", true)
                .addOption("gallery", "Gallery Module", false);
        typeSelect.withClass("mb-3");
        typeSelect.withAttribute("style", "width: 100%;");
        modal.withChild(typeSelect);

        // Title field
        Div titleGroup = new Div().withClass("mb-3");
        titleGroup.withChild(new Paragraph("Title:").withClass("fw-bold mb-1"));
        TextInput titleInput = TextInput.create("title");
        titleInput.withPlaceholder("Module title");
        titleInput.withAttribute("style", "width: 100%;");
        titleGroup.withChild(titleInput);
        modal.withChild(titleGroup);

        // Content field (for content modules)
        Div contentGroup = new Div().withClass("mb-3");
        contentGroup.withChild(new Paragraph("Content (Markdown):").withClass("fw-bold mb-1"));
        TextArea contentArea = TextArea.create("content");
        contentArea.withPlaceholder("# Heading\n\nYour content here...");
        contentArea.withRows(10);
        contentArea.withAttribute("style", "width: 100%;");
        contentGroup.withChild(contentArea);
        modal.withChild(contentGroup);

        // Buttons
        Div buttons = new Div().withClass("d-flex gap-2");

        Button addBtn = Button.submit("Add Module").withStyle(Button.ButtonStyle.PRIMARY);
        addBtn.withAttribute("hx-post", "/editing-demo/api/rows/" + rowId + "/add-module");
        addBtn.withAttribute("hx-target", "#row-" + rowId);
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

        DemoModule newModule = new DemoModule(moduleId, type);
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
        List<DemoRow> rows = pages.get(pageId);
        if (rows == null) {
            return "" + Alert.danger("Page not found").render();
        }

        // Create new empty row
        String rowId = "row-" + idCounter.incrementAndGet();
        DemoRow newRow = new DemoRow(rowId);

        // Add a placeholder module
        String moduleId = "module-" + idCounter.incrementAndGet();
        DemoModule module = new DemoModule(moduleId, "content");
        module.data.put("title", "New Row");
        module.data.put("content", "Click **Edit** to add content to this new row!");
        newRow.modules.add(module);

        rows.add(newRow);

        // Return the entire page
        return "" + renderEditablePage();
    }

    @PostMapping("/api/pages/{pageId}/rows/insert")
    @ResponseBody
    public String insertRow(@PathVariable String pageId) {
        // For simplicity, just add at the end (same as add)
        return addRow(pageId);
    }

    // ===== HELPER METHODS =====

    private DemoModule findModule(String id) {
        for (List<DemoRow> rows : pages.values()) {
            for (DemoRow row : rows) {
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
        for (List<DemoRow> rows : pages.values()) {
            for (DemoRow row : rows) {
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
