package io.mindspice.demo;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.components.display.Modal;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.editing.AuthWrapper;
import io.mindspice.simplypages.editing.Editable;
import io.mindspice.simplypages.editing.EditModalBuilder;
import io.mindspice.simplypages.editing.ValidationResult;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Container;
import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.modules.ContentModule;
import io.mindspice.simplypages.modules.EditableModule;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Phase 8 Test Controller - AuthWrapper Pattern Demo
 * <p>
 * Demonstrates the AuthWrapper utility for protecting edit operations with
 * authorization checks. Shows different permission scenarios and how to
 * integrate auth checks with the editing system.
 * </p>
 */
@Controller
@RequestMapping("/test/phase8")
public class Phase8TestController {

    // Simulated user permissions (in real app, this would come from database/auth service)
    private final Map<String, UserRole> userRoles = new HashMap<>();
    private final Map<String, TestModule> modules = new HashMap<>();

    public Phase8TestController() {
        // Initialize test users with different roles
        userRoles.put("admin", UserRole.ADMIN);
        userRoles.put("editor", UserRole.EDITOR);
        userRoles.put("viewer", UserRole.VIEWER);

        // Initialize test modules
        modules.put("module-1", new TestModule("module-1", "Public Content",
                "This module can be edited by editors and admins", false));
        modules.put("module-2", new TestModule("module-2", "Admin Only Content",
                "This module can only be edited by admins", false));
        modules.put("module-3", new TestModule("module-3", "Read-Only Content",
                "This module cannot be edited by anyone", false));
    }

    enum UserRole {
        ADMIN,   // Can edit and delete everything
        EDITOR,  // Can edit most content, but not admin-only
        VIEWER   // Can only view content
    }

    static class TestModule {
        String id;
        String title;
        String content;
        boolean useMarkdown;

        TestModule(String id, String title, String content, boolean useMarkdown) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.useMarkdown = useMarkdown;
        }
    }

    /**
     * Main test page showing different auth scenarios
     */
    @GetMapping
    @ResponseBody
    public String showTestPage(@RequestParam(defaultValue = "viewer") String user) {
        Div modalContainer = new Div().withAttribute("id", "edit-modal-container");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Phase 8 Test - AuthWrapper Pattern</title>\n");
        html.append("    <link rel=\"stylesheet\" href=\"/css/framework.css\">\n");
        html.append("    <script src=\"/webjars/htmx.org/dist/htmx.min.js\" defer></script>\n");
        html.append("</head>\n<body>\n");
        html.append(renderPageContent(user));
        html.append(modalContainer.render());
        html.append("</body>\n</html>\n");

        return html.toString();
    }

    private String renderPageContent(String user) {
        UserRole role = userRoles.getOrDefault(user, UserRole.VIEWER);
        Container content = Container.create();

        content.withChild(Header.H1("Phase 8: AuthWrapper Pattern Demo").withClass("mb-4"));

        content.withChild(Alert.info("Current User: <strong>" + user + "</strong> (Role: " + role + ")")
                .withAttribute("style", "margin-bottom: 20px;"));

        content.withChild(createUserSwitcher(user));

        content.withChild(Header.H2("Authorization Scenarios").withClass("mt-4 mb-3"));

        content.withChild(createScenarioExplanation(role));

        // Module 1: Public Content (Editors and Admins can edit)
        Row row1 = new Row();
        row1.withChild(Column.create().withWidth(12)
                .withChild(createEditableModule("module-1", user, role)));
        content.withChild(row1);

        // Module 2: Admin Only Content
        Row row2 = new Row();
        row2.withChild(Column.create().withWidth(12)
                .withChild(createEditableModule("module-2", user, role)));
        content.withChild(row2);

        // Module 3: Read-Only Content (Nobody can edit)
        Row row3 = new Row();
        row3.withChild(Column.create().withWidth(12)
                .withChild(createEditableModule("module-3", user, role)));
        content.withChild(row3);

        return content.render();
    }

    private Div createUserSwitcher(String currentUser) {
        Div switcher = new Div()
                .withClass("user-switcher")
                .withAttribute("style", "margin-bottom: 30px; padding: 15px; background: #f8f9fa; border-radius: 4px;");

        switcher.withChild(new Paragraph("Switch User: "));

        for (String user : new String[]{"admin", "editor", "viewer"}) {
            switcher.withChild(
                    Button.create(user.substring(0, 1).toUpperCase() + user.substring(1))
                            .withAttribute("onclick", "window.location.href='/test/phase8?user=" + user + "'")
                            .withAttribute("style",
                                    "margin-right: 10px;" +
                                            (user.equals(currentUser) ? " font-weight: bold; background: #007bff; color: white;" : "")
                            )
            );
        }

        return switcher;
    }

    private Div createScenarioExplanation(UserRole role) {
        Div explanation = new Div()
                .withAttribute("style", "margin-bottom: 20px; padding: 15px; background: #e7f3ff; border-left: 4px solid #2196F3;");

        String message = switch (role) {
            case ADMIN -> "As an <strong>ADMIN</strong>, you can edit both Module 1 and Module 2. Module 3 is read-only for everyone.";
            case EDITOR -> "As an <strong>EDITOR</strong>, you can only edit Module 1. Module 2 is admin-only, and Module 3 is read-only.";
            case VIEWER -> "As a <strong>VIEWER</strong>, you cannot edit any modules. Try clicking an edit button to see the authorization error.";
        };

        explanation.withChild(new Paragraph(message));
        return explanation;
    }

    private EditableModule createEditableModule(String moduleId, String username, UserRole role) {
        TestModule data = modules.get(moduleId);
        if (data == null) {
            ContentModule placeholder = ContentModule.create()
                    .withModuleId(moduleId)
                    .withTitle("Module Removed")
                    .withContent("This module was deleted during the demo session.");

            return EditableModule.wrap(placeholder)
                    .withCanEdit(false)
                    .withCanDelete(false);
        }

        ContentModule module = ContentModule.create()
                .withModuleId(moduleId)
                .withTitle(data.title)
                .withContent(data.content);

        if (!data.useMarkdown) {
            module.disableMarkdown();
        }

        boolean canEdit = canUserEdit(moduleId, username);
        boolean canDelete = canUserDelete(moduleId, username);

        return EditableModule.wrap(module)
                .withEditUrl(canEdit ? "/test/phase8/api/modules/" + moduleId + "/edit?user=" + username : null)
                .withDeleteUrl(canDelete ? "/test/phase8/api/modules/" + moduleId + "/delete?user=" + username : null)
                .withCanEdit(canEdit)
                .withCanDelete(canDelete);
    }

    /**
     * Authorization logic - determines if user can edit a module
     */
    private boolean canUserEdit(String moduleId, String username) {
        UserRole role = userRoles.getOrDefault(username, UserRole.VIEWER);

        return switch (moduleId) {
            case "module-1" -> role == UserRole.ADMIN || role == UserRole.EDITOR;  // Public: editors and admins
            case "module-2" -> role == UserRole.ADMIN;  // Admin only
            case "module-3" -> false;  // Read-only for everyone
            default -> false;
        };
    }

    /**
     * Authorization logic - determines if user can delete a module
     */
    private boolean canUserDelete(String moduleId, String username) {
        UserRole role = userRoles.getOrDefault(username, UserRole.VIEWER);

        return switch (moduleId) {
            case "module-1", "module-2" -> role == UserRole.ADMIN;  // Only admins can delete
            case "module-3" -> false;  // Nobody can delete read-only content
            default -> false;
        };
    }

    /**
     * Edit endpoint - protected with AuthWrapper
     */
    @GetMapping("/api/modules/{id}/edit")
    @ResponseBody
    public String editModule(
            @PathVariable String id,
            @RequestParam String user
    ) {
        // AuthWrapper.requireForEdit checks authorization before showing edit modal
        return AuthWrapper.requireForEdit(
                () -> canUserEdit(id, user),
                () -> {
                    TestModule data = modules.get(id);
                    if (data == null) {
                        return Modal.create()
                                .withTitle("Error")
                                .withBody(Alert.danger("Module not found"))
                                .render();
                    }

                    ContentModule module = ContentModule.create()
                            .withTitle(data.title)
                            .withContent(data.content);

                    if (!data.useMarkdown) {
                        module.disableMarkdown();
                    }

                    Editable<ContentModule> adapter = module;

                    return EditModalBuilder.create()
                            .withTitle("Edit Module")
                            .withEditView(adapter.buildEditView())
                            .withSaveUrl("/test/phase8/api/modules/" + id + "/update?user=" + user)
                            .withDeleteUrl("/test/phase8/api/modules/" + id + "/delete?user=" + user)
                            .build()
                            .render();
                },
                "You do not have permission to edit this content"  // Custom error message
        );
    }

    /**
     * Update endpoint - protected with AuthWrapper
     */
    @PostMapping("/api/modules/{id}/update")
    @ResponseBody
    public String updateModule(
            @PathVariable String id,
            @RequestParam String user,
            @RequestParam Map<String, String> formData
    ) {
        return AuthWrapper.requireForEdit(
                () -> canUserEdit(id, user),
                () -> {
                    TestModule data = modules.get(id);
                    if (data == null) {
                        return Modal.create()
                                .withTitle("Error")
                                .withBody(Alert.danger("Module not found"))
                                .render();
                    }

                    ContentModule module = ContentModule.create()
                            .withTitle(data.title)
                            .withContent(data.content);

                    if (!data.useMarkdown) {
                        module.disableMarkdown();
                    }

                    Editable<ContentModule> adapter = module;

                    // Validate
                    ValidationResult validation = adapter.validate(formData);
                    if (!validation.isValid()) {
                        return Modal.create()
                                .withTitle("Validation Error")
                                .withBody(Alert.danger(String.join(", ", validation.getErrors())))
                                .render();
                    }

                    // Apply edits
                    adapter.applyEdits(formData);

                    // Update storage
                    data.title = formData.get("title");
                    data.content = formData.get("content");
                    data.useMarkdown = "on".equals(formData.get("useMarkdown"));

                    // Return OOB swaps: clear modal, refresh body
                    String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
                    String refreshBody = renderPageContent(user).replace("<div class=\"container\">",
                            "<div hx-swap-oob=\"outerHTML\" class=\"container\">");

                    return clearModal + refreshBody;
                }
        );
    }

    /**
     * Delete endpoint - protected with AuthWrapper
     */
    @DeleteMapping("/api/modules/{id}/delete")
    @ResponseBody
    public String deleteModule(
            @PathVariable String id,
            @RequestParam String user
    ) {
        return AuthWrapper.requireForDelete(
                () -> canUserDelete(id, user),
                () -> {
                    modules.remove(id);

                    // Return OOB swaps: clear modal, refresh body
                    String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
                    String refreshBody = renderPageContent(user).replace("<div class=\"container\">",
                            "<div hx-swap-oob=\"outerHTML\" class=\"container\">");

                    return clearModal + refreshBody;
                }
        );
    }
}
