package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.core.Module;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.forms.Form;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Module wrapper for composing and rendering a {@link Form}.
 *
 * <p>Contract: when {@link #withSubmitUrl(String)} is set, it is applied as {@code hx-post} on
 * the wrapped form during build.</p>
 *
 * <p>Mutability and thread-safety: mutable and not thread-safe. Form fields are accumulated on
 * this instance; mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class FormModule extends Module {

    private Form form;
    private String submitUrl;
    private String description;

    /** Creates a module with an empty form. */
    public FormModule() {
        super("div");
        this.withClass("form-module");
        this.form = Form.create();
    }

    /** Creates a new module instance. */
    public static FormModule create() {
        return new FormModule();
    }

    /** Sets HTMX submit endpoint for the wrapped form. */
    public FormModule withSubmitUrl(String url) {
        this.submitUrl = url;
        return this;
    }

    /** Sets optional descriptive text rendered above the form. */
    public FormModule withDescription(String description) {
        this.description = description;
        return this;
    }

    /** Appends a labeled field to the wrapped form. */
    public FormModule addField(String label, Component input) {
        this.form.addField(label, input);
        return this;
    }

    /** Replaces the wrapped form component. */
    public FormModule withForm(Form form) {
        this.form = form;
        return this;
    }

    @Override
    public FormModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public FormModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }

        if (description != null && !description.isEmpty()) {
            HtmlTag descDiv = new HtmlTag("p")
                .withAttribute("class", "module-description")
                .withInnerText(description);
            super.withChild(descDiv);
        }

        if (submitUrl != null) {
            form.withHxPost(submitUrl);
        }

        super.withChild(form);
    }
}
