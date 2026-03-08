package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.forms.Form;
import io.mindspice.simplypages.components.forms.TextArea;
import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.core.Module;

/**
 * Composer module for creating new forum topics.
 */
public class ForumTopicComposerModule extends Module {

    private String submitUrl = "/forum/topics";
    private String titleFieldName = "title";
    private String bodyFieldName = "body";
    private String titlePlaceholder = "Topic title";
    private String bodyPlaceholder = "Write your topic...";
    private String submitLabel = "Create Topic";

    public ForumTopicComposerModule() {
        super("div");
        this.withClass("forum-topic-composer-module");
    }

    public static ForumTopicComposerModule create() {
        return new ForumTopicComposerModule();
    }

    public ForumTopicComposerModule withSubmitUrl(String submitUrl) {
        this.submitUrl = submitUrl;
        return this;
    }

    public ForumTopicComposerModule withTitleFieldName(String titleFieldName) {
        this.titleFieldName = titleFieldName;
        return this;
    }

    public ForumTopicComposerModule withBodyFieldName(String bodyFieldName) {
        this.bodyFieldName = bodyFieldName;
        return this;
    }

    public ForumTopicComposerModule withTitlePlaceholder(String titlePlaceholder) {
        this.titlePlaceholder = titlePlaceholder;
        return this;
    }

    public ForumTopicComposerModule withBodyPlaceholder(String bodyPlaceholder) {
        this.bodyPlaceholder = bodyPlaceholder;
        return this;
    }

    public ForumTopicComposerModule withSubmitLabel(String submitLabel) {
        this.submitLabel = submitLabel;
        return this;
    }

    @Override
    public ForumTopicComposerModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public ForumTopicComposerModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isBlank()) {
            super.withChild(Header.H3(title).withClass("forum-topic-composer-title"));
        }

        Form form = Form.create()
            .withClass("forum-topic-composer-form")
            .withHxPost(submitUrl)
            .withHxSwap("none")
            .addField("Title", TextInput.create(titleFieldName)
                .withPlaceholder(titlePlaceholder)
                .required())
            .addField("Body", TextArea.create(bodyFieldName)
                .withPlaceholder(bodyPlaceholder)
                .withRows(8)
                .required())
            .withChild(Button.submit(submitLabel).withClass("forum-topic-composer-submit"));

        super.withChild(form);
    }
}
