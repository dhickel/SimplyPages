package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ForumComposerModulesTest {

    @Test
    @DisplayName("ForumTopicComposerModule should render topic form fields and submit action")
    void topicComposerRendersForm() {
        String html = ForumTopicComposerModule.create()
            .withTitle("Start Topic")
            .withSubmitUrl("/api/topics")
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("div.forum-topic-composer-module.module")
            .hasElement("div.forum-topic-composer-module > h3.forum-topic-composer-title")
            .hasElement("div.forum-topic-composer-module form.forum-topic-composer-form")
            .attributeEquals("form.forum-topic-composer-form", "hx-post", "/api/topics")
            .hasElement("form.forum-topic-composer-form input[name=title]")
            .hasElement("form.forum-topic-composer-form textarea[name=body]")
            .hasElement("form.forum-topic-composer-form button.forum-topic-composer-submit");
    }

    @Test
    @DisplayName("ForumCommentComposerModule should render hidden topic id when configured")
    void commentComposerRendersHiddenTopicId() {
        String html = ForumCommentComposerModule.create()
            .withTopicId("topic-44")
            .withSubmitUrl("/api/comments")
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("div.forum-comment-composer-module.module")
            .hasElement("form.forum-comment-composer-form")
            .attributeEquals("form.forum-comment-composer-form", "hx-post", "/api/comments")
            .hasElement("form.forum-comment-composer-form input[type=hidden][name=topicId][value=topic-44]")
            .hasElement("form.forum-comment-composer-form textarea[name=comment]");
    }
}
