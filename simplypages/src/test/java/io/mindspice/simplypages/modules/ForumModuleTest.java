package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.forum.ForumPost;
import io.mindspice.simplypages.testutil.HtmlAssert;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ForumModuleTest {

    @Test
    @DisplayName("ForumModule should render titled post list with ordered posts")
    void testForumModuleRendering() {
        ForumModule module = ForumModule.create()
            .withTitle("Forum")
            .addPost(ForumPost.create().withTitle("First").withContent("Body 1").disableMarkdown())
            .addPost(ForumPost.create().withTitle("Second").withContent("Body 2").disableMarkdown());

        String html = module.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.forum-module")
            .hasElement("div.forum-module > h2.module-title")
            .hasElement("div.forum-module > div.post-list")
            .hasElementCount("div.post-list > div.forum-post", 2)
            .elementTextEquals("h2.module-title", "Forum")
            .elementTextEquals("div.post-list > div.forum-post:nth-child(1) h3.post-title", "First")
            .elementTextEquals("div.post-list > div.forum-post:nth-child(2) h3.post-title", "Second")
            .childOrder("div.forum-module", "h2.module-title", "div.post-list");

        SnapshotAssert.assertMatches("modules/forum-module/two-posts", html);
    }
}
