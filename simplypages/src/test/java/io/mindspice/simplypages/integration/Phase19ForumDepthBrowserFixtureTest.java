package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.forum.comments.DefaultForumCommentComponent;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.components.forum.comments.ForumCommentComponent;
import io.mindspice.simplypages.components.forum.comments.ForumCommentData;
import io.mindspice.simplypages.components.forum.comments.ForumCommentRenderer;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.SlotKey;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Actual current-Java direct and fresh-compiled forum markup for the browser depth proof. */
public class Phase19ForumDepthBrowserFixtureTest {
    private static final int[] DEPTHS = {0, 1, 2, 4, 5, 9, Integer.MIN_VALUE, Integer.MAX_VALUE};
    private static final String ESCAPED = "<depth> & \"body\"";
    private static final SlotKey<String> BODY = SlotKey.of("phase19-body");

    @Test
    void writesFreshDirectAndCompiledForumDepthMarkup() throws Exception {
        String html = generate();
        Document document = Jsoup.parse(html);
        assertEquals(16, document.select(".phase19-forum-comment").size());
        assertEquals(8, document.select(".phase19-forum-comment[data-mode=direct]").size());
        assertEquals(8, document.select(".phase19-forum-comment[data-mode=compiled]").size());
        assertEquals(16, document.select(".phase19-forum-comment > .forum-comment[data-depth]").size());
        assertEquals(1, document.select("#phase19-custom[data-depth='2147483647']").size());
        assertEquals(1, document.select("#phase19-legacy.forum-comment").size());
        assertEquals(16, document.select(".forum-comment .forum-comment-avatar-slot").size());
        assertEquals(16, document.select(".forum-comment .phase19-action").size());
        assertEquals(16, document.select(".forum-comment .phase19-body").size());
        assertEquals(16, document.select(".phase19-unowned-layout").size());
        assertEquals(0, document.select("script:not([src]), [onload]").size());
        assertTrue(html.contains("&lt;depth&gt;"));
        assertFalse(html.contains(ESCAPED));
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/phase19-forum-depth-browser-fixture.html"), html);
    }

    /** Standalone generation against the extracted current packaged library. */
    public static void main(String[] args) throws Exception {
        Files.writeString(Path.of(args[0]), generate(), StandardOpenOption.CREATE_NEW);
    }

    private static String generate() {
        HtmlTag fixture = new HtmlTag("main")
            .withAttribute("id", "phase19-forum-depth-fixture")
            .withAttribute("class", "phase19-forum-fixture");
        for (String mode : List.of("direct", "compiled")) {
            HtmlTag view = new HtmlTag("section")
                .withAttribute("class", "forum-comments-view phase19-forum-view")
                .withAttribute("data-mode", mode);
            for (int index = 0; index < DEPTHS.length; index++) {
                int depth = DEPTHS[index];
                String id = "phase19-" + mode + "-" + index;
                var comment = configured(id, mode, depth);
                var context = RenderContext.of(BODY, ESCAPED + " " + mode + " " + depth
                    + " Discussion participants independently describe practical implementation details in normal wrapping words.");
                String output = mode.equals("compiled")
                    ? comment.compile().render(context)
                    : comment.render(context);
                view.withChild(new HtmlTag("div")
                    .withAttribute("class", "phase19-forum-comment")
                    .withAttribute("data-mode", mode)
                    .withAttribute("data-depth-input", String.valueOf(depth))
                    .withUnsafeHtml(output));
            }
            fixture.withChild(view);
        }
        var custom = ForumCommentRenderer.<Metadata, Void>builder()
            .withCommentComponentSupplier(MetadataOnlyComment::new).build()
            .render(List.of(new Metadata("custom", "topic", "missing", Integer.MAX_VALUE, "custom")), null);
        fixture.withChild(custom);
        fixture.withChild(new HtmlTag("div").withId("phase19-legacy").withClass("forum-comment")
            .withChild(new HtmlTag("div").withClass("forum-comment-layout")));
        return fixture.render();
    }

    private static DefaultForumCommentComponent configured(String id, String mode, int depth) {
        String parent = depth == 0 ? "parent<&\"" : depth == 1 ? "" : depth == 2 ? " \t\n" : null;
        HtmlTag body = new HtmlTag("div")
            .withAttribute("class", "phase19-body")
            .withChild(new HtmlTag("p").withInnerText(BODY))
            .withChild(new HtmlTag("div").withClass("forum-comment-layout phase19-unowned-layout")
                .withChild(new HtmlTag("span")).withChild(new HtmlTag("span")));
        Button action = Button.create("Reply")
            .withId(id + "-action")
            .withClass("phase19-action");
        return (DefaultForumCommentComponent) DefaultForumCommentComponent.create()
            .withCommentId(id)
            .withTopicId("phase19-topic")
            .withParentId(parent)
            .withDepth(depth)
            .withAuthor("Author " + mode)
            .withTimestamp("now")
            .withBody(body)
            .withActions(List.of(action))
            .withLikes(2)
            .withReplies(3);
    }

    private record Metadata(String id, String topicId, String parentId, int depth, String body)
        implements ForumCommentData {}

    private static final class MetadataOnlyComment extends DefaultForumCommentComponent {
        private int rawDepth;
        private String rawParent;

        @Override
        public ForumCommentComponent withDepth(int depth) {
            rawDepth = depth;
            return this;
        }

        @Override
        public ForumCommentComponent withParentId(String parent) {
            rawParent = parent;
            return this;
        }

        @Override
        public String render(RenderContext context) {
            return new HtmlTag("div").withId("phase19-custom").withClass("forum-comment")
                .withAttribute("data-depth", String.valueOf(rawDepth)).withAttribute("data-parent-id", rawParent)
                .withChild(new HtmlTag("div").withClass("forum-comment-layout"))
                .render(context);
        }
    }
}
