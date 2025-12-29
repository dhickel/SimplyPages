package io.mindspice.demo;


import io.mindspice.jhf.builders.ShellBuilder;
import io.mindspice.jhf.builders.SideNavBuilder;
import io.mindspice.jhf.builders.TopBannerBuilder;
import io.mindspice.jhf.components.Div;
import io.mindspice.jhf.components.Header;
import io.mindspice.jhf.components.Paragraph;
import io.mindspice.jhf.components.display.Alert;
import io.mindspice.jhf.components.forms.Button;
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
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Editing system demo - shows in-place editing, page builder, and module management.
 */
@Controller
@RequestMapping("/editing-demo")
public class EditingDemoController {

    // Simple in-memory storage
    private static class DemoModule {
        String id;
        String type;
        Map<String, Object> data = new HashMap<>();
    }

    private final Map<String, List<DemoModule>> pageModules = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    public EditingDemoController() {
        initializeDemoData();
    }

    private void initializeDemoData() {
        List<DemoModule> modules = new ArrayList<>();

        // Module 1: Welcome content
        DemoModule m1 = new DemoModule();
        m1.id = "module-1";
        m1.type = "content";
        m1.data.put("title", "Welcome to Editing Demo");
        m1.data.put("content", "# Editing System\n\nClick **Edit** above to modify this content!\n\n- In-place editing\n- HTMX updates\n- No page reloads");
        modules.add(m1);

        // Module 2: Gallery
        DemoModule m2 = new DemoModule();
        m2.id = "module-2";
        m2.type = "gallery";
        m2.data.put("title", "Sample Gallery");
        List<String> urls = List.of(
                "https://picsum.photos/400/300?random=1",
                "https://picsum.photos/400/300?random=2",
                "https://picsum.photos/400/300?random=3"
        );
        m2.data.put("urls", urls);
        modules.add(m2);

        pageModules.put("demo-1", modules);
    }

    @GetMapping
    @ResponseBody
    public String viewPage(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        response.setHeader("Vary", "HX-Request");

        List<DemoModule> modules = pageModules.get("demo-1");

        EditablePage page = EditablePage.create("demo-1");
        Row row = new Row();
        EditableRow editableRow = EditableRow.wrap(row, "row-1", "demo-1");

        for (DemoModule dm : modules) {
            Module module = createModule(dm);
            editableRow.addEditableModule(module, dm.id);
        }

        page.addEditableRow(editableRow);

        String content = page.render();

        // Add modal container
        content += "<div id=\"add-module-modal\" class=\"mt-4\"></div>";

        if (hxRequest != null) {
            return content;
        }

        return renderWithShell(content);
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

    // ===== EDIT ENDPOINTS =====

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

        Div form = new Div();
        form.withAttribute("id", dm.id);
        form.withClass("module content-module p-3");

        form.withChild(Header.H3("Edit Content"));

        // Title field
        Div titleGroup = new Div().withClass("mb-3");
        titleGroup.withChild(new Paragraph("Title:"));
        TextInput titleInput = TextInput.create("title");
        titleInput.withValue(title != null ? title : "");
        titleInput.withAttribute("style", "width: 100%; max-width: 600px;");
        titleGroup.withChild(titleInput);
        form.withChild(titleGroup);

        // Content field
        Div contentGroup = new Div().withClass("mb-3");
        contentGroup.withChild(new Paragraph("Content (Markdown):"));
        TextArea contentArea = TextArea.create("content");
        contentArea.withValue(content != null ? content : "");
        contentArea.withRows(15);
        contentArea.withAttribute("style", "width: 100%; max-width: 800px;");
        contentGroup.withChild(contentArea);
        form.withChild(contentGroup);

        // Buttons
        Div buttons = new Div().withClass("d-flex gap-2");

        Button saveBtn = Button.submit("Save");
        saveBtn.withAttribute("hx-post", "/editing-demo/api/modules/" + dm.id + "/update");
        saveBtn.withAttribute("hx-target", "#" + dm.id);
        saveBtn.withAttribute("hx-swap", "outerHTML");
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
        form.withChild(new Paragraph("Gallery editing demo - functionality can be extended"));

        Button cancelBtn = Button.create("Cancel").withStyle(Button.ButtonStyle.SECONDARY);
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
            return Alert.danger("Module not found").render();
        }

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
                .withEditMode(EditMode.OWNER_EDIT);

        return editable.render();
    }

    @GetMapping("/api/modules/{moduleId}/view")
    @ResponseBody
    public String viewModule(@PathVariable String moduleId) {
        DemoModule dm = findModule(moduleId);
        if (dm == null) {
            return Alert.danger("Module not found").render();
        }

        Module module = createModule(dm);
        EditableModule editable = EditableModule.wrap(module)
                .withModuleId(moduleId)
                .withEditUrl("/editing-demo/api/modules/" + moduleId + "/edit")
                .withDeleteUrl("/editing-demo/api/modules/" + moduleId + "/delete")
                .withEditMode(EditMode.OWNER_EDIT);

        return editable.render();
    }

    @DeleteMapping("/api/modules/{moduleId}/delete")
    @ResponseBody
    public String deleteModule(@PathVariable String moduleId) {
        List<DemoModule> modules = pageModules.get("demo-1");
        modules.removeIf(m -> m.id.equals(moduleId));
        return "";  // Empty - HTMX removes element
    }

    private DemoModule findModule(String id) {
        return pageModules.get("demo-1").stream()
                .filter(m -> m.id.equals(id))
                .findFirst()
                .orElse(null);
    }

    private String renderWithShell(String content) {
        String shell = ShellBuilder.create()
                .withPageTitle("JHF Editing Demo")
                .withTopBanner(
                        TopBannerBuilder.create()
                                .withTitle("Java HTML Framework - Editing Demo")
                                .withSubtitle("In-place editing with HTMX")
                                .withClass("banner-full-width")
                                .withBackgroundColor("#2c3e50")
                                .withTextColor("#ffffff")
                                .build()
                )
                .withSideNav(
                        SideNavBuilder.create()
                                .addSection("Editing Demo")
                                .addLink("Editable Page", "/editing-demo", "✏️")
                                .addSection("Main Demo")
                                .addLink("Back to Home", "/home", "🏠")
                                .build()
                )
                .build();

        return shell.replace("<div id=\"content-area\"></div>",
                "<div id=\"content-area\">" + content + "</div>");
    }
}
