package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.forms.Form;
import io.mindspice.simplypages.components.forms.TextArea;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.Module;

/**
 * Composer module for creating comments in a topic thread.
 */
public class ForumCommentComposerModule extends Module {

    private String submitUrl = "/forum/comments";
    private String bodyFieldName = "comment";
    private String bodyPlaceholder = "Write a comment...";
    private String submitLabel = "Post Comment";
    private String topicId;

    public ForumCommentComposerModule() {
        super("div");
        this.withClass("forum-comment-composer-module");
    }

    public static ForumCommentComposerModule create() {
        return new ForumCommentComposerModule();
    }

    public ForumCommentComposerModule withSubmitUrl(String submitUrl) {
        this.submitUrl = submitUrl;
        return this;
    }

    public ForumCommentComposerModule withBodyFieldName(String bodyFieldName) {
        this.bodyFieldName = bodyFieldName;
        return this;
    }

    public ForumCommentComposerModule withBodyPlaceholder(String bodyPlaceholder) {
        this.bodyPlaceholder = bodyPlaceholder;
        return this;
    }

    public ForumCommentComposerModule withSubmitLabel(String submitLabel) {
        this.submitLabel = submitLabel;
        return this;
    }

    public ForumCommentComposerModule withTopicId(String topicId) {
        this.topicId = topicId;
        return this;
    }

    @Override
    public ForumCommentComposerModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public ForumCommentComposerModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isBlank()) {
            super.withChild(Header.H4(title).withClass("forum-comment-composer-title"));
        }

        Form form = Form.create()
            .withClass("forum-comment-composer-form")
            .withHxPost(submitUrl)
            .withHxSwap("none");

        if (topicId != null && !topicId.isBlank()) {
            form.withChild(new HtmlTag("input", true)
                .withAttribute("type", "hidden")
                .withAttribute("name", "topicId")
                .withAttribute("value", topicId));
        }

        form.addField("Comment", TextArea.create(bodyFieldName)
                .withRows(5)
                .withPlaceholder(bodyPlaceholder)
                .required())
            .withChild(Button.submit(submitLabel).withClass("forum-comment-composer-submit"));

        super.withChild(form);
    }
}
