package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.core.Module;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.forms.Form;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Module for creating forms.
 */
public class FormModule extends Module {

    private Form form;
    private String submitUrl;
    private String description;

    public FormModule() {
        super("div");
        this.withClass("form-module");
        this.form = Form.create();
    }

    public static FormModule create() {
        return new FormModule();
    }

    public FormModule withSubmitUrl(String url) {
        this.submitUrl = url;
        return this;
    }

    public FormModule withDescription(String description) {
        this.description = description;
        return this;
    }

    public FormModule addField(String label, Component input) {
        this.form.addField(label, input);
        return this;
    }

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
