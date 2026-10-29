[How-to index](README.md) | [Learning path](../getting-started/README.md)

# Build a user-editable page

## Goal

Let an authenticated resource owner edit one briefing through a modal. Save validated
plain text, refresh the displayed briefing, and close the modal without a page reload.
This deliberately does not implement deletion, row insertion, or an approval queue.

## Prerequisites

- SimplyPages **1.1.1** installed locally, JDK 25 at runtime, and Spring MVC.
  Complete the [learning path](../getting-started/README.md) through
  [serving pages and fragments](serve-pages-and-fragments.md) first.
- A component-scanned application package. The example uses the demo's
  `io.mindspice.demo.learning` package and otherwise imports only public framework APIs.
- An application identity provider that supplies a genuine authenticated `Principal`.
  Provision an identity named `operations-owner`, or change the seeded resource owner
  to an existing identity. A request parameter is not an identity provider.
- Application CSRF middleware that validates every POST, plus your application's
  HTMX request hook that sends its issued token. Configure this before exposing the
  controller. Configure private responses with `Cache-Control: no-store` in that
  middleware too. This example is not authentication or CSRF configuration.
- The shell's `/css/framework.css`, `/js/framework.js`, and
  `/webjars/htmx.org/dist/htmx.min.js` must be served by your application.

## 1. Store values, not component trees

The sample stores one immutable record in memory. It is **non-durable**: restarting
loses edits. Saves are serialized within this controller; concurrent editors still
have last-writer-wins behavior. A durable application should load, authorize, validate,
and persist within its own transaction, adding a version check if needed.

Save `EditableBriefingController.java` beneath your application's component-scan
root. In the repository demo the source path would be
`demo/src/main/java/io/mindspice/demo/learning/EditableBriefingController.java`.
For a separate application, change the package consistently; do not treat the
private demo's selectable editing modes as an identity provider.

```java
package io.mindspice.demo.learning;

import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.components.display.Modal;
import io.mindspice.simplypages.editing.EditModalBuilder;
import io.mindspice.simplypages.editing.ValidationResult;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.modules.ContentModule;
import io.mindspice.simplypages.modules.EditableModule;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(value = "/learn/editable-page", produces = "text/html")
public class EditableBriefingController {
    private static final String BASE = "/learn/editable-page";
    private record Briefing(String owner, String title, String content) {}
    private final AtomicReference<Briefing> stored = new AtomicReference<>(
            new Briefing("operations-owner", "Shift briefing", "All services available."));

    @GetMapping
    public String page(Principal principal) {
        Briefing data = stored.get();
        requireOwner(principal, data);
        Page page = Page.builder().addComponents(
                Header.H1("Operations briefing"),
                new Div().withId("briefing-page").withChild(editor(data)),
                new Div().withId("briefing-modal-host")).build();
        return ShellBuilder.create().withPageTitle("Operations briefing")
                .withContent(page).build();
    }

    @GetMapping("/edit")
    public String edit(Principal principal) {
        Briefing data = stored.get();
        requireOwner(principal, data);
        return modal(module(data), null).render();
    }

    @PostMapping("/save")
    public synchronized String save(Principal principal,
            @RequestParam Map<String, String> form) {
        Briefing before = stored.get();
        requireOwner(principal, before);
        // Whitelist plain-text fields: never accept the Markdown checkbox here.
        Map<String, String> values = Map.of(
                "title", form.getOrDefault("title", ""),
                "content", form.getOrDefault("content", ""));
        ContentModule candidate = module(before);
        ValidationResult result = candidate.validate(values);
        String error = values.get("title").isBlank() ? "Title is required." : null;
        if (!result.isValid()) {
            error = String.join(" ", result.getErrors());
        }
        if (error != null) {
            ContentModule submitted = ContentModule.create()
                    .withModuleId("briefing-content")
                    .withTitle(values.get("title")).withContent(values.get("content"))
                    .disableMarkdown();
            return new Div().withId("briefing-modal-host")
                    .withAttribute("hx-swap-oob", "innerHTML")
                    .withChild(modal(submitted, error)).render();
        }
        candidate.applyEdits(values);
        Briefing saved = new Briefing(before.owner(), candidate.getTitle(),
                values.get("content"));
        stored.set(saved);
        return new Div().withId("briefing-page")
                .withAttribute("hx-swap-oob", "innerHTML")
                .withChild(editor(saved)).render()
                + new Div().withId("briefing-modal-host")
                .withAttribute("hx-swap-oob", "innerHTML").render();
    }

    private static void requireOwner(Principal principal, Briefing data) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (!data.owner().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private static ContentModule module(Briefing data) {
        return ContentModule.create().withModuleId("briefing-content")
                .withTitle(data.title()).withContent(data.content()).disableMarkdown();
    }

    private static EditableModule editor(Briefing data) {
        return EditableModule.wrap(module(data)).withModuleId("briefing-editor")
                .withEditButton("Edit briefing").withEditUrl(BASE + "/edit")
                .withEditTarget("#briefing-modal-host").withCanDelete(false);
    }

    private static Modal modal(ContentModule module, String error) {
        EditModalBuilder builder = EditModalBuilder.create()
                .withTitle("Edit briefing").withModuleId("briefing-content")
                .withSaveUrl(BASE + "/save").withPageContainerId("briefing-page")
                .withModalContainerId("briefing-modal-host").hideDelete();
        if (error == null) {
            builder.withEditable(module);
        } else {
            builder.withEditView(new Div().withChild(Alert.danger(error))
                    .withChild(module.buildEditView()));
        }
        return builder.build().withModalId("briefing-dialog");
    }
}
```

## 2. Connect the edit and save responses

`ContentModule` implements `Editable<ContentModule>`. Its edit view supplies the title,
content, and Markdown fields. This application accepts only title and content and
always renders plain text. Checking “Render as Markdown” therefore has no effect;
remove that control with an application-specific edit view if that would confuse users.
Do not pass arbitrary rich HTML through `withUnsafeHtml` instead.

`EditModalBuilder` needs an editable source **or** an explicit edit view, and a save
URL. Above, the normal response uses `withEditable`; validation failure uses an
explicit view to add an escaped error while preserving submitted values.

The GET response fills `#briefing-modal-host` with `innerHTML`. Save Properties uses
`hx-swap="none"`, so its response consists of explicit out-of-band updates:

- `briefing-page`: replace its contents with a newly built editor.
- `briefing-modal-host`: clear its contents after success; replace the modal on error.

The module ID (`briefing-content`), decorator ID (`briefing-editor`), page target,
modal host, and modal backdrop (`briefing-dialog`) are all distinct. Do not give a
wrapper and its child the same ID. Keep only one edit modal open: the builder's input
inclusion selector covers `.edit-properties-section` controls across the document.

Validation failures intentionally return HTTP 200 with error HTML; HTMX processes
the OOB swaps. Unauthorized requests return 401/403, not a successful edit response.
Your application should present those security failures through its own HTTP/HTMX
error handling. The endpoints are modal fragments, not ordinary full-page form posts.

## 3. Check the result

With the application's authenticated session and CSRF integration enabled:

1. Open `/learn/editable-page` as the resource owner. Select **Edit briefing**.
2. Change both fields and save. The heading/body change and the modal disappears.
   Reload: the values remain until the process restarts.
3. Submit blank content, then a title longer than 200 characters. Each must retain
   the submitted fields, show an error, and leave stored content unchanged.
4. Save `<script>alert(1)</script>` as content. It must display as text, not execute.
5. Independently request the page, GET edit, and POST save without authentication,
   and as a different authenticated principal. Expect 401 and 403 respectively.
   Query parameters such as `editMode=OWNER_EDIT` must not change these decisions.
6. Remove or corrupt the application's CSRF token. Its middleware must reject save
   before this handler runs. Hiding a button is not evidence that an endpoint is safe.

For a direct Java smoke check, construct `new EditableBriefingController()` and
use a `Principal` whose `getName()` returns `operations-owner`. Call `page(owner)`,
`edit(owner)`, and `save(owner, Map.of("title", "Updated briefing", "content", "Ready."))`.
The save returns the page update and modal-clear OOB regions. A subsequent
`page(owner)` contains the saved text. A blank-content save retains its draft in
the modal without changing that page. Null and non-owner principals must throw
401 and 403 respectively. Direct calls do not exercise CSRF middleware; check that
separately over HTTP.

## 4. Scale only when you own the generated endpoints

For a full page builder, `EditablePage.create(pageId)` and
`EditableRow.wrap(row, rowId, pageId).addEditableModule(module, moduleId)` emit more
controls. In this release their default URLs include:

- POST `/api/pages/{pageId}/rows/insert?position=0` for an empty page, and positions
  `1..N` after existing rows.
- GET `/api/pages/{pageId}/rows/{rowId}/add-module-form`.
- GET `/api/pages/{pageId}/modules/{moduleId}/edit` and DELETE the corresponding
  `/delete` URL; the row wrappers use the default `#edit-modal-container` target.

Those controls do not create HTTP handlers. Implement authorization, CSRF checks,
position/ID validation, storage, and matching response targets for every emitted
operation before using them. Reconstruct mutable pages/rows/modules per request.

`EditMode.USER_EDIT` is URL metadata indicating an intended approval flow. It does
not stage changes, persist an approval request, or enforce a permission. Derive
policy from the authenticated user/resource; implement any queue and approval
handlers in the application, rather than trusting the submitted mode.

## Related references

- [Owner/user approval pattern](../patterns/04-editing-workflows-owner-user-approval.md)
- [Editing API](../reference/editing-api-reference.md)
- [Security boundaries](../security/01-security-boundaries-and-safe-rendering.md)
- [Authorization integration](../security/02-authwrapper-authorizationchecker-integration.md)
- [Local editing exercise](../getting-started/03-editing-system-first-implementation.md)
