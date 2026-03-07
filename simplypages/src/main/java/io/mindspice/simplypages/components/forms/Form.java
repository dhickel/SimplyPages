package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * HTML form wrapper with fluent helpers for method, enctype, and HTMX attributes.
 *
 * <p>Mutable and not thread-safe. Configure per request; avoid reusing one instance across threads. For reuse, stop mutating shared instances and render stable structures with per-request context data.</p>
 *
 * <p>Security boundary: this component can attach CSRF tokens but does not generate or validate them.
 * Token issuance/validation remains an application responsibility.</p>
 */
public class Form extends HtmlTag {

    /**
     * Supported logical HTTP methods for form submission.
     */
    public enum Method {
        GET, POST, PUT, DELETE, PATCH
    }

    /**
     * Creates a form with base class {@code form}.
     */
    public Form() {
        super("form");
        this.withAttribute("class", "form");
    }

    /**
     * Creates a new form.
     *
     * @return new form
     */
    public static Form create() {
        return new Form();
    }

    /**
     * Sets form action URL.
     *
     * @param action action URL
     * @return this form
     */
    public Form withAction(String action) {
        this.withAttribute("action", action);
        return this;
    }

    /**
     * Sets logical method.
     *
     * <p>For {@code PUT}/{@code DELETE}/{@code PATCH}, emits HTML {@code method="POST"} and appends
     * a hidden {@code _method} override field.</p>
     *
     * @param method logical method
     * @return this form
     */
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

    /**
     * Sets id attribute.
     *
     * @param id element id
     * @return this form
     */
    @Override
    public Form withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Sets {@code hx-post}.
     *
     * @param url endpoint URL
     * @return this form
     */
    public Form withHxPost(String url) {
        this.withAttribute("hx-post", url);
        return this;
    }

    /**
     * Sets {@code hx-get}.
     *
     * @param url endpoint URL
     * @return this form
     */
    public Form withHxGet(String url) {
        this.withAttribute("hx-get", url);
        return this;
    }

    /**
     * Sets {@code hx-put}.
     *
     * @param url endpoint URL
     * @return this form
     */
    public Form withHxPut(String url) {
        this.withAttribute("hx-put", url);
        return this;
    }

    /**
     * Sets {@code hx-delete}.
     *
     * @param url endpoint URL
     * @return this form
     */
    public Form withHxDelete(String url) {
        this.withAttribute("hx-delete", url);
        return this;
    }

    /**
     * Sets {@code hx-target}.
     *
     * @param target target selector
     * @return this form
     */
    public Form withHxTarget(String target) {
        this.withAttribute("hx-target", target);
        return this;
    }

    /**
     * Sets {@code hx-swap}.
     *
     * @param swap swap strategy
     * @return this form
     */
    public Form withHxSwap(String swap) {
        this.withAttribute("hx-swap", swap);
        return this;
    }

    /**
     * Sets {@code hx-trigger}.
     *
     * @param trigger trigger definition
     * @return this form
     */
    public Form withHxTrigger(String trigger) {
        this.withAttribute("hx-trigger", trigger);
        return this;
    }

    /**
     * Sets {@code enctype}.
     *
     * @param enctype encoding type
     * @return this form
     */
    public Form withEnctype(String enctype) {
        this.withAttribute("enctype", enctype);
        return this;
    }

    /**
     * Convenience for multipart form encoding.
     *
     * @return this form
     */
    public Form multipart() {
        return this.withEnctype("multipart/form-data");
    }

    /**
     * Sets {@code novalidate}.
     *
     * @return this form
     */
    public Form noValidate() {
        this.withAttribute("novalidate", "");
        return this;
    }

    /**
     * Appends hidden {@code _csrf} input.
     *
     * @param token CSRF token value
     * @return this form
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
     * Convenience helper for HTMX POST with CSRF header.
     *
     * @param url the URL to POST to
     * @param csrfToken the CSRF token value
     * @return this form
     */
    public Form withHxPostCsrf(String url, String csrfToken) {
        withHxPost(url);
        // Add CSRF token as request header for HTMX
        String headers = String.format("{\"X-CSRF-TOKEN\": \"%s\"}", csrfToken);
        withAttribute("hx-headers", headers);
        return this;
    }

    /**
     * Replaces class attribute with {@code form <className>}.
     *
     * @param className additional classes
     * @return this form
     */
    public Form withClass(String className) {
        String currentClass = "form";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    /**
     * Appends child component.
     *
     * @param component child component
     * @return this form
     */
    @Override
    public Form withChild(Component component) {
        super.withChild(component);
        return this;
    }

    /**
     * Appends a field wrapper containing optional label and input component.
     *
     * @param label label text; omitted when null/empty
     * @param input input component
     * @return this form
     */
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

    /**
     * Internal lightweight div helper for field grouping.
     *
     * <p>Mutable and not thread-safe; scoped to enclosing form assembly. For reuse, stop mutating shared instances and render stable structures with per-request context data.</p>
     */
    private static class Div extends HtmlTag {
        /**
         * Creates an empty div.
         */
        public Div() {
            super("div");
        }

        /**
         * Sets class attribute.
         *
         * @param className class token(s)
         * @return this div
         */
        public Div withClass(String className) {
            this.withAttribute("class", className);
            return this;
        }

        /**
         * Appends child component.
         *
         * @param component child component
         * @return this div
         */
        @Override
        public Div withChild(Component component) {
            super.withChild(component);
            return this;
        }
    }
}
