package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.forum.ForumPost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForumModuleTest {

    @Test
    @DisplayName("ForumModule should render post list")
    void testForumModuleRendering() {
        ForumModule module = ForumModule.create()
            .withTitle("Forum")
            .addPost(ForumPost.create().withTitle("Post").withContent("Body").disableMarkdown());

        String html = module.render();

        assertTrue(html.contains("forum-module"));
        assertTrue(html.contains("Forum"));
        assertTrue(html.contains("Post"));
    }
}
