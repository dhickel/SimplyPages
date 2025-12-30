package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Form component for wrapping input elements.
 * Supports HTMX attributes for dynamic form submission.
 *
 * <h2>CSRF Protection</h2>
 * <p>POST forms should include CSRF tokens to prevent Cross-Site Request Forgery attacks.
 * Use {@link #withCsrfToken(String)} to add the token:</p>
 * <pre>{@code
 * Form.create()
 *     .withAction("/submit")
 *     .withMethod(Method.POST)
 *     .withCsrfToken(csrfToken)  // Protects against CSRF
 *     .withChild(...)
 * }</pre>
 *
 * <h2>HTMX Forms</h2>
 * <p>For HTMX submissions, use {@link #withHxPostCsrf(String, String)} which
 * automatically includes the CSRF token in request headers.</p>
 */
public class Form extends HtmlTag {

    public enum Method {
        GET, POST, PUT, DELETE, PATCH
    }

    public Form() {
        super("form");
        this.withAttribute("class", "form");
    }

    public static Form create() {
        return new Form();
    }

    public Form withAction(String action) {
        this.withAttribute("action", action);
        return this;
    }

    public Form withMethod(Method method) {
        if (method == Method.GET || method == Method.POST) {
            this.withAttribute("method", method.name());
        } else {
            // For PUT, DELETE, PATCH, use POST and add hidden _method field
            this.withAttribute("method", "POST");
            withChild(
                new HtmlTag("input", true)
                    .withAttribute("type", "hidden")
                    .withAttribute("name", "_method")
                    .withAttribute("value", method.name())
            );
        }
        return this;
    }

    public Form withId(String id) {
        this.withAttribute("id", id);
        return this;
    }

    // HTMX Integration
    public Form withHxPost(String url) {
        this.withAttribute("hx-post", url);
        return this;
    }

    public Form withHxGet(String url) {
        this.withAttribute("hx-get", url);
        return this;
    }

    public Form withHxPut(String url) {
        this.withAttribute("hx-put", url);
        return this;
    }

    public Form withHxDelete(String url) {
        this.withAttribute("hx-delete", url);
        return this;
    }

    public Form withHxTarget(String target) {
        this.withAttribute("hx-target", target);
        return this;
    }

    public Form withHxSwap(String swap) {
        this.withAttribute("hx-swap", swap);
        return this;
    }

    public Form withHxTrigger(String trigger) {
        this.withAttribute("hx-trigger", trigger);
        return this;
    }

    public Form withEnctype(String enctype) {
        this.withAttribute("enctype", enctype);
        return this;
    }

    public Form multipart() {
        return this.withEnctype("multipart/form-data");
    }

    public Form noValidate() {
        this.withAttribute("novalidate", "");
        return this;
    }

    /**
     * Adds a CSRF token as a hidden input field.
     *
     * <p>When using Spring Security, pass the CSRF token from the controller:</p>
     * <pre>{@code
     * @PostMapping("/submit")
     * public String form(CsrfToken csrf) {
     *     return Form.create()
     *         .withAction("/submit")
     *         .withMethod(Method.POST)
     *         .withCsrfToken(csrf.getToken())  // Spring provides token
     *         .withChild(...)
     *         .render();
     * }
     * }</pre>
     *
     * <p>For other frameworks, generate the token using your security library
     * and pass it to this method.</p>
     *
     * @param token the CSRF token value
     * @return this form for method chaining
     */
    public Form withCsrfToken(String token) {
        withChild(
            new HtmlTag("input", true)
                .withAttribute("type", "hidden")
                .withAttribute("name", "_csrf")
                .withAttribute("value", token)
        );
        return this;
    }

    /**
     * Configures this form for HTMX POST with CSRF token.
     *
     * <p>This is a convenience method that combines {@link #withHxPost(String)}
     * with CSRF token header configuration.</p>
     *
     * @param url the URL to POST to
     * @param csrfToken the CSRF token value
     * @return this form for method chaining
     */
    public Form withHxPostCsrf(String url, String csrfToken) {
        withHxPost(url);
        // Add CSRF token as request header for HTMX
        String headers = String.format("{\"X-CSRF-TOKEN\": \"%s\"}", csrfToken);
        withAttribute("hx-headers", headers);
        return this;
    }

    public Form withClass(String className) {
        String currentClass = "form";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    public Form withChild(Component component) {
        super.withChild(component);
        return this;
    }

    // Form field group helper
    public Form addField(String label, Component input) {
        Div fieldGroup = new Div().withClass("form-field");

        if (label != null && !label.isEmpty()) {
            HtmlTag labelTag = new HtmlTag("label")
                .withAttribute("class", "form-label")
                .withInnerText(label);
            fieldGroup.withChild(labelTag);
        }

        fieldGroup.withChild(input);
        return this.withChild(fieldGroup);
    }

    // Utility inner class for Div (to avoid circular dependency)
    private static class Div extends HtmlTag {
        public Div() {
            super("div");
        }

        public Div withClass(String className) {
            this.withAttribute("class", className);
            return this;
        }

        @Override
        public Div withChild(Component component) {
            super.withChild(component);
            return this;
        }
    }
}
