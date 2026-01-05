package io.mindspice.demo;

import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.components.display.Modal;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.forms.TextArea;
import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.editing.EditAdapter;
import io.mindspice.simplypages.editing.EditModalBuilder;
import io.mindspice.simplypages.editing.ValidationResult;
import io.mindspice.simplypages.layout.Container;
import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.layout.Section;
import io.mindspice.simplypages.modules.ContentModule;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test controller for Phase 1-2: Modal Component and EditAdapter Interface.
 *
 * <p>This controller provides test pages to verify:</p>
 * <ul>
 *   <li>Modal component rendering and functionality</li>
 *   <li>EditAdapter interface with ContentModule</li>
 *   <li>EditModalBuilder helper</li>
 *   <li>ValidationResult handling</li>
 * </ul>
 *
 * <p>Access at: <a href="http://localhost:8080/test/phase1-2">http://localhost:8080/test/phase1-2</a></p>
 */
@Controller
@RequestMapping("/test/phase1-2")
public class Phase1And2TestController {

    // In-memory storage for test modules
    private final Map<String, TestModuleData> modules = new ConcurrentHashMap<>();

    /**
     * Test data for a module.
     */
    private static class TestModuleData {
        String id;
        String title;
        String content;

        TestModuleData(String id, String title, String content) {
            this.id = id;
            this.title = title;
            this.content = content;
        }
    }

    /**
     * Constructor - initialize test data.
     */
    public Phase1And2TestController() {
        // Create sample modules for testing
        modules.put("test-1", new TestModuleData(
                "test-1",
                "Sample Content Module",
                "# Welcome to Phase 1-2 Testing\n\nThis module demonstrates:\n- Modal rendering\n- EditAdapter interface\n- Form validation\n\n**Click the edit button to test editing!**"
        ));

        modules.put("test-2", new TestModuleData(
                "test-2",
                "Second Module",
                "This is another module for testing the editing system."
        ));
    }

    /**
     * Helper: Render page content (reusable for full page and HTMX updates).
     */
    private String renderPageContent() {
        Container content = Container.create();

        // Header
        content.withChild(
                Header.H1("Phase 1-2 Test Page")
                        .withClass("mb-4")
        );

        // Description
        Div description = new Div().withClass("mb-4");
        description.withChild(new Paragraph(
                "This page tests the Modal component and EditAdapter interface. " +
                "Click the buttons below to test different features."
        ));
        content.withChild(description);

        // Test buttons section
        Section testButtons = Section.create().withClass("mb-5");
        testButtons.withChild(Header.H2("Test Controls").withClass("mb-3"));

        Row buttonRow = new Row();
        buttonRow.withClass("gap-2 mb-4");

        // Test 1: Simple modal
        Button simpleModalBtn = Button.create("Test Simple Modal")
                .withStyle(Button.ButtonStyle.PRIMARY);
        simpleModalBtn.withAttribute("hx-get", "/test/phase1-2/modal/simple");
        simpleModalBtn.withAttribute("hx-target", "#edit-modal-container");
        simpleModalBtn.withAttribute("hx-swap", "innerHTML");
        buttonRow.withChild(simpleModalBtn);

        // Test 2: Form modal
        Button formModalBtn = Button.create("Test Form Modal")
                .withStyle(Button.ButtonStyle.SECONDARY);
        formModalBtn.withAttribute("hx-get", "/test/phase1-2/modal/form");
        formModalBtn.withAttribute("hx-target", "#edit-modal-container");
        formModalBtn.withAttribute("hx-swap", "innerHTML");
        buttonRow.withChild(formModalBtn);

        testButtons.withChild(buttonRow);
        content.withChild(testButtons);

        // Test modules section
        Section modulesSection = Section.create().withClass("mb-5");
        modulesSection.withChild(Header.H2("Test Modules (EditAdapter)").withClass("mb-3"));

        for (TestModuleData data : modules.values()) {
            Div moduleWrapper = new Div()
                    .withClass("position-relative border rounded p-3 mb-3")
                    .withAttribute("style", "background-color: #f8f9fa;");

            // Edit button (positioned absolutely)
            Button editBtn = Button.create("✏ Edit")
                    .withStyle(Button.ButtonStyle.LINK);
            editBtn.withClass("position-absolute");
            editBtn.withAttribute("style", "top: 10px; right: 10px; font-size: 1.5rem;");
            editBtn.withAttribute("hx-get", "/test/phase1-2/edit/" + data.id);
            editBtn.withAttribute("hx-target", "#edit-modal-container");
            editBtn.withAttribute("hx-swap", "innerHTML");
            moduleWrapper.withChild(editBtn);

            // Render the actual module
            ContentModule module = createModuleFromData(data);
            moduleWrapper.withChild(module);

            modulesSection.withChild(moduleWrapper);
        }

        content.withChild(modulesSection);

        // Results section
        Section resultsSection = Section.create();
        resultsSection.withChild(Header.H2("Test Results").withClass("mb-3"));
        resultsSection.withChild(
                new Div()
                        .withAttribute("id", "results-container")
                        .withClass("border rounded p-3")
                        .withAttribute("style", "min-height: 100px; background-color: #fff;")
                        .withChild(new Paragraph("Results will appear here...").withClass("text-muted"))
        );
        content.withChild(resultsSection);

        // Wrap in a div with ID for HTMX targeting
        return "<div id=\"page-content\">" + content.render() + "</div>";
    }

    /**
     * Main test page.
     */
    @GetMapping
    @ResponseBody
    public String testPage() {
        // Modal container (empty, populated by HTMX)
        Div modalContainer = new Div().withAttribute("id", "edit-modal-container");

        // Build simple HTML page
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Phase 1-2 Test</title>\n");
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
     * Test endpoint: Simple modal.
     */
    @GetMapping("/modal/simple")
    @ResponseBody
    public String simpleModal() {
        Div body = new Div();
        body.withChild(new Paragraph("This is a simple modal with no form elements."));
        body.withChild(new Paragraph("Click the X or backdrop to close."));
        body.withChild(Alert.info("Modal component is working!"));

        Div footer = new Div().withClass("d-flex justify-content-end");
        Button closeBtn = Button.create("Close")
                .withStyle(Button.ButtonStyle.SECONDARY);
        closeBtn.withAttribute("onclick", "document.getElementById('edit-modal-container').innerHTML = ''");
        footer.withChild(closeBtn);

        return Modal.create()
                .withTitle("Simple Modal Test")
                .withBody(body)
                .withFooter(footer)
                .render();
    }

    /**
     * Test endpoint: Form modal.
     */
    @GetMapping("/modal/form")
    @ResponseBody
    public String formModal() {
        Div body = new Div();

        // Name field
        Div nameGroup = new Div().withClass("form-field");
        nameGroup.withChild(new Paragraph("Name:").withClass("form-label"));
        nameGroup.withChild(TextInput.create("name").withValue("Test User"));
        body.withChild(nameGroup);

        // Message field
        Div messageGroup = new Div().withClass("form-field");
        messageGroup.withChild(new Paragraph("Message:").withClass("form-label"));
        messageGroup.withChild(TextArea.create("message").withValue("Test message").withRows(4));
        body.withChild(messageGroup);

        Div footer = new Div().withClass("d-flex justify-content-end gap-2");

        Button cancelBtn = Button.create("Cancel")
                .withStyle(Button.ButtonStyle.SECONDARY);
        cancelBtn.withAttribute("onclick", "document.getElementById('edit-modal-container').innerHTML = ''");
        footer.withChild(cancelBtn);

        Button submitBtn = Button.submit("Submit Test")
                .withStyle(Button.ButtonStyle.PRIMARY);
        submitBtn.withAttribute("hx-post", "/test/phase1-2/submit-test");
        submitBtn.withAttribute("hx-target", "#results-container");
        submitBtn.withAttribute("hx-swap", "innerHTML");
        submitBtn.withAttribute("hx-include", "closest .modal-body");
        footer.withChild(submitBtn);

        return Modal.create()
                .withTitle("Form Modal Test")
                .withBody(body)
                .withFooter(footer)
                .closeOnBackdrop(false)
                .render();
    }

    /**
     * Test endpoint: Submit form test.
     */
    @PostMapping("/submit-test")
    @ResponseBody
    public String submitTest(@RequestParam Map<String, String> formData) {
        Div results = new Div();
        results.withChild(Header.H3("Form Submission Test Results").withClass("mb-3"));
        results.withChild(Alert.success("Form submitted successfully!"));

        results.withChild(new Paragraph("<strong>Received data:</strong>"));
        Div dataList = new Div().withClass("ms-3");
        formData.forEach((key, value) -> {
            dataList.withChild(new Paragraph("• " + key + ": " + value));
        });
        results.withChild(dataList);

        // Clear modal and update results
        String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
        String updateResults = "<div hx-swap-oob=\"true\" id=\"results-container\" class=\"border rounded p-3\">" +
                results.render() + "</div>";

        return clearModal + updateResults;
    }

    /**
     * Test endpoint: Edit module using EditAdapter.
     */
    @GetMapping("/edit/{moduleId}")
    @ResponseBody
    public String editModule(@PathVariable String moduleId) {
        TestModuleData data = modules.get(moduleId);
        if (data == null) {
            return Alert.danger("Module not found: " + moduleId).render();
        }

        // Create module and cast to EditAdapter
        ContentModule module = createModuleFromData(data);
        EditAdapter<ContentModule> adapter = module;

        // Use EditModalBuilder
        return EditModalBuilder.create()
                .withTitle("Edit Module: " + data.title)
                .withModuleId(moduleId)
                .withEditView(adapter.buildEditView())
                .withSaveUrl("/test/phase1-2/save/" + moduleId)
                .withDeleteUrl("/test/phase1-2/delete/" + moduleId)
                .withPageContainerId("page-content")
                .withModalContainerId("edit-modal-container")
                .build()
                .render();
    }

    /**
     * Test endpoint: Save module edits.
     */
    @PostMapping("/save/{moduleId}")
    @ResponseBody
    public String saveModule(
            @PathVariable String moduleId,
            @RequestParam Map<String, String> formData
    ) {
        System.out.println("=== SAVE MODULE CALLED ===");
        System.out.println("Module ID: " + moduleId);
        System.out.println("Form Data: " + formData);

        TestModuleData data = modules.get(moduleId);
        if (data == null) {
            return Modal.create()
                    .withTitle("Error")
                    .withBody(Alert.danger("Module not found"))
                    .render();
        }

        // Create module and apply edits
        ContentModule module = createModuleFromData(data);
        EditAdapter<ContentModule> adapter = module;

        // Validate
        ValidationResult validation = adapter.validate(formData);
        if (!validation.isValid()) {
            return Modal.create()
                    .withTitle("Validation Error")
                    .withBody(Alert.danger("Validation failed: " + validation.getErrorsAsString()))
                    .render();
        }

        // Apply edits
        adapter.applyEdits(formData);

        // Update storage
        data.title = formData.getOrDefault("title", data.title);
        data.content = formData.getOrDefault("content", data.content);

        // Use OOB swaps for both modal clearing and page update
        String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";

        // Wrap page content in OOB swap div
        String pageContent = renderPageContent(); // Returns <div id="page-content">...</div>
        String updatePage = pageContent.replace("<div id=\"page-content\">",
                                                "<div hx-swap-oob=\"true\" id=\"page-content\">");

        String response = clearModal + updatePage;
        System.out.println("=== RESPONSE START ===");
        System.out.println(response.substring(0, Math.min(500, response.length())));
        System.out.println("=== RESPONSE END ===");

        return response;
    }

    /**
     * Test endpoint: Delete module.
     */
    @DeleteMapping("/delete/{moduleId}")
    @ResponseBody
    public String deleteModule(@PathVariable String moduleId) {
        TestModuleData removed = modules.remove(moduleId);

        if (removed == null) {
            return Alert.danger("Module not found").render();
        }

        // Use OOB swaps for both modal clearing and page update
        String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";

        // Wrap page content in OOB swap div
        String pageContent = renderPageContent();
        String updatePage = pageContent.replace("<div id=\"page-content\">",
                                                "<div hx-swap-oob=\"true\" id=\"page-content\">");

        return clearModal + updatePage;
    }

    /**
     * Helper: Create ContentModule from test data.
     */
    private ContentModule createModuleFromData(TestModuleData data) {
        ContentModule module = ContentModule.create()
                .withModuleId(data.id)
                .withTitle(data.title)
                .withContent(data.content);

        return module;
    }
}
