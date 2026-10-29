[How-to index](README.md) | [Learning path](../getting-started/README.md)

# Handle form submissions

## Goal

Build a contact-address preview form with server-side validation, preserved values,
inline errors, and a confirmation. It deliberately **does not** send email, create an
account, or persist a subscription. Both ordinary browser posts and HTMX submissions
use the same validation rules.

## Prerequisites

- SimplyPages **1.1.1** installed locally, Spring MVC, and JDK 25 at runtime.
  Complete the [learning path](../getting-started/README.md) through
  [serving pages and fragments](serve-pages-and-fragments.md) first.
- A component-scanned package; the example fits the demo learning package below.
- Application authentication that supplies a real `Principal` for GET and POST.
- Existing application CSRF middleware. For this example, its integration contract
  is: expose the current token as servlet request attribute `appCsrfToken`, accept
  it in POST field `_csrf`, and validate it **before** invoking the POST handler.
  Adapt these two names to your existing security integration. Merely setting a
  request attribute is not CSRF validation. No Spring Security dependency is needed
  to compile this controller, and the controller does not install security middleware.
  Configure `Cache-Control: no-store` for these authenticated routes in that
  middleware, including the initial GET and rejected security responses.
- Serve the shell's framework CSS/JS and HTMX WebJar paths.

## 1. Define the server rules

After stripping leading/trailing whitespace, the name must contain 1–80 characters.
The email must contain 1–254 characters and match the explicitly limited ASCII shape
below: a local part, `@`, and a dotted domain. This is an application input rule, not
an assertion of mailbox existence or a complete international email parser.

Save `ContactPreviewController.java` beneath your application's component-scan
root. In the repository demo the source path would be
`demo/src/main/java/io/mindspice/demo/learning/ContactPreviewController.java`.
Change the package consistently for a separate application. The private demo
itself does not supply the authentication/CSRF prerequisites above.

```java
package io.mindspice.demo.learning;

import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.display.Label;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.forms.Form;
import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.components.navigation.Link;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(value = "/learn/contact-preview", produces = "text/html")
public class ContactPreviewController {
    private static final String URL = "/learn/contact-preview";
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+");

    @GetMapping
    public String get(Principal principal,
            @RequestAttribute("appCsrfToken") String csrfToken) {
        requireAuthenticated(principal);
        return document(form("", "", Map.of(), csrfToken));
    }

    @PostMapping
    public ResponseEntity<String> post(Principal principal,
            @RequestAttribute("appCsrfToken") String csrfToken,
            @RequestParam(name = "name", defaultValue = "") String name,
            @RequestParam(name = "email", defaultValue = "") String email,
            @RequestHeader(name = "HX-Request", defaultValue = "false") String hxRequest) {
        requireAuthenticated(principal);
        String cleanName = name.strip();
        String cleanEmail = email.strip();
        Map<String, String> errors = new LinkedHashMap<>();
        if (cleanName.isEmpty() || cleanName.length() > 80) {
            errors.put("name", "Enter a name of 1–80 characters.");
        }
        if (cleanEmail.isEmpty() || cleanEmail.length() > 254
                || !EMAIL.matcher(cleanEmail).matches()) {
            errors.put("email", "Enter an ASCII email address with a dotted domain, at most 254 characters.");
        }
        boolean hx = "true".equals(hxRequest);
        Div region;
        if (errors.isEmpty()) {
            region = new Div().withId("contact-result")
                    .withAttribute("role", "status")
                    .withChild(Header.H2("Address preview confirmed"))
                    .withChild(new Paragraph("Name: " + cleanName))
                    .withChild(new Paragraph("Email: " + cleanEmail))
                    .withChild(new Paragraph("Nothing was sent or stored."))
                    .withChild(Link.create(URL, "Preview another address"));
        } else {
            // Preserve the original strings, not only their stripped versions.
            region = form(name, email, errors, csrfToken);
        }
        HttpStatus status = !hx && !errors.isEmpty()
                ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return ResponseEntity.status(status)
                .header("Vary", "HX-Request")
                .header("Cache-Control", "no-store")
                .body(hx ? region.render() : document(region));
    }

    private static Div form(String name, String email,
            Map<String, String> errors, String csrfToken) {
        Form form = Form.create().withId("contact-form")
                .withAction(URL).withMethod(Form.Method.POST)
                .withHxPost(URL).withHxTarget("#contact-result")
                .withHxSwap("outerHTML").withCsrfToken(csrfToken)
                .withChild(field("name", "Name", name, errors.get("name"), false))
                .withChild(field("email", "Email", email, errors.get("email"), true))
                .withChild(Button.submit("Confirm address preview"));
        return new Div().withId("contact-result")
                .withChild(Header.H1("Preview a contact address"))
                .withChild(new Paragraph("This preview does not subscribe you to anything."))
                .withChild(form);
    }

    private static Div field(String name, String label, String value,
            String error, boolean email) {
        String id = "contact-" + name;
        TextInput input = (email ? TextInput.email(name) : TextInput.create(name))
                .withId(id).withValue(value).required().withMaxLength(email ? 254 : 80);
        Div field = new Div().withClass("form-field")
                .withChild(Label.create(label).forInput(id).required())
                .withChild(input);
        if (error != null) {
            input.withAttribute("aria-invalid", "true");
            input.withAttribute("aria-describedby", id + "-error");
            field.withChild(new Paragraph(error).withId(id + "-error")
                    .withAttribute("role", "alert"));
        }
        return field;
    }

    private static String document(Div region) {
        return ShellBuilder.create().withPageTitle("Contact address preview")
                .withContent(region).build();
    }

    private static void requireAuthenticated(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
```

## 2. Keep the response shape matched to the request

The form has an ordinary `action` and POST `method`, plus HTMX attributes. With
JavaScript disabled, the browser receives a complete document: invalid input returns
400 with the rendered form, and valid input returns 200 with the confirmation.

With HTMX loaded, `#contact-result` is replaced using `outerHTML`. Every fragment,
including confirmation, therefore has exactly one root with that same ID. Invalid
HTMX submissions deliberately return **200**, allowing default HTMX response handling
to swap in the error form. Do not change this to 422 and assume HTMX will display it;
using 4xx for validation requires explicit application response-handling configuration.
Only exact `HX-Request: true` selects the fragment; missing, `false`, or other values
receive a document. `Vary: HX-Request` distinguishes the representations. The header
selects presentation only, never authorization.

Authentication/CSRF failures are different from input errors: the application should
reject them with the appropriate security status and handle their UI separately.
Do not relabel a rejected security check as a successful validation response.

`required`, `type="email"`, and `maxlength` help the browser, but callers can bypass
them. The POST checks are authoritative. New controls are built on every render;
no failed request can leave values in a shared form object. `Label` text, errors,
confirmation text, and input attributes use escaped rendering, not raw HTML.

## 3. Check the result

1. Open `/learn/contact-preview` in an authenticated session. Confirm that selecting
   each label focuses its associated input.
2. Submit `Ada Lovelace` and `ada@example.org`. Expect the confirmation and the
   explicit statement that nothing was sent or stored. Follow **Preview another
   address** to return to the implemented GET route.
3. Bypass browser validation using developer tools, then submit whitespace-only
   name/email, an 81-character name, a 255-character email, and `ada@localhost`.
   Expect field-specific errors, preserved input, and no confirmation.
4. Submit a name such as `<b>Ada</b>` with a valid email. The confirmation must show
   literal angle brackets, not bold markup. On a failed submission, quoted input
   values must remain inside the input attribute rather than creating new attributes.
5. Check the network response with HTMX: one `contact-result` fragment, status 200,
   and no nested `html` or `body`. Repeat with JavaScript disabled: full documents,
   status 400 on invalid input and 200 on confirmation.
6. Remove the CSRF field or use a different session's token. The application's
   middleware must reject the POST. Without authentication, GET and POST must fail.

For a direct Java smoke check, construct `new ContactPreviewController()` and use
an application `Principal` and a nonempty example token. Call `get(principal, token)`,
then `post(principal, token, "Ada Lovelace", "ada@example.org", "true")` for a
confirmation fragment. Call `post(principal, token, " ", "ada@localhost", "true")`
for a 200 error fragment with preserved fields; changing the last argument to
`"false"` returns a full document with status 400. A null principal must throw 401.
These calls exercise the handlers, not the servlet token issuer/validator; verify
that integration over HTTP before using the routes.

The preview is safe to repeat because it has no durable side effect. When adapting
it to a real write, add application-owned authorization and persistence, and choose
an idempotency/redirect strategy appropriate to that operation. Do not claim this
confirmation persisted a subscription.

## Related references

- [Components catalog](../reference/components-and-modules-catalog.md)
- [HTMX endpoint and swap patterns](../patterns/03-htmx-endpoint-and-swap-patterns.md)
- [Security boundaries](../security/01-security-boundaries-and-safe-rendering.md)
- [Editing an existing resource](build-user-editable-pages.md)
