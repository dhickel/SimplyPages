package io.mindspice.demo.integration;

import io.mindspice.demo.DemoApplication;
import io.mindspice.demo.forum.ForumDemoService;
import io.mindspice.demo.forum.ForumViewer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
class ForumDemoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ForumDemoService forumService;

    @Test
    @DisplayName("Forum page should render in full shell with demo sections")
    void forumPageRenders() throws Exception {
        mockMvc.perform(get("/forum"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"forum-main\"")))
            .andExpect(content().string(containsString("Viewer Session")))
            .andExpect(content().string(containsString("Categories")))
            .andExpect(content().string(not(containsString(">Topics<"))))
            .andExpect(content().string(containsString("Forum")));
    }

    @Test
    @DisplayName("Viewer role dropdown should persist moderator selection across requests")
    void viewerRoleDropdownPersistsModeratorSelection() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/forum/viewer")
                .session(session)
                .header("HX-Request", "true")
                .param("displayName", "Moderator User")
                .param("userId", "user-moderator")
                .param("role", "moderator")
                .param("scope", "")
                .param("topic", "")
                .param("view", "categories")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Viewer set to Moderator User (moderator)")))
            .andExpect(content().string(containsString("Current viewer: Moderator User [user-moderator] · moderator")))
            .andExpect(content().string(containsString("<option value=\"moderator\" selected>Moderator</option>")));

        mockMvc.perform(get("/forum")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Current viewer: Moderator User [user-moderator] · moderator")))
            .andExpect(content().string(containsString("<option value=\"moderator\" selected>Moderator</option>")));
    }

    @Test
    @DisplayName("Forum should drill down from category topics into topic comments")
    void drillDownFlowRendersSingleStageAtATime() throws Exception {
        String parentBody = "Drilldown parent body that must render in thread comments view.";
        ForumDemoService.TopicView topic = forumService.createTopic(
            "cat-feedback",
            "Drilldown Parent Topic",
            parentBody,
            new ForumViewer("user-drill", "Drill User", false)
        ).orElseThrow();

        mockMvc.perform(get("/forum/topics")
                .header("HX-Request", "true")
                .param("scope", "cat-feedback")
                .param("view", "topics")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(">Topics<")))
            .andExpect(content().string(containsString("forum-topic-title-link")))
            .andExpect(content().string(not(containsString("Open Thread"))))
            .andExpect(content().string(not(containsString("Thread:"))));

        mockMvc.perform(get("/forum/topics/{topicId}/comments", topic.id())
                .header("HX-Request", "true")
                .param("scope", "cat-feedback")
                .param("topic", topic.id())
                .param("view", "comments")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Thread: " + topic.title())))
            .andExpect(content().string(containsString("forum-demo-parent-topic-toggle\" open")))
            .andExpect(content().string(containsString("Parent Post")))
            .andExpect(content().string(containsString(parentBody)))
            .andExpect(content().string(not(containsString(">Topics<"))));
    }

    @Test
    @DisplayName("Topic page should render preview text while thread page renders full parent body")
    void previewInTopicsAndFullBodyInThread() throws Exception {
        String longBody = "Preview verification body " + "x".repeat(260) + " ENDMARKER";
        ForumDemoService.TopicView topic = forumService.createTopic(
            "cat-feedback",
            "Preview Body Topic",
            longBody,
            new ForumViewer("user-preview", "Preview User", false)
        ).orElseThrow();

        mockMvc.perform(get("/forum/topics")
                .header("HX-Request", "true")
                .param("scope", "cat-feedback")
                .param("view", "topics")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Preview verification body")))
            .andExpect(content().string(containsString("...")))
            .andExpect(content().string(not(containsString("ENDMARKER"))));

        mockMvc.perform(get("/forum/topics/{topicId}/comments", topic.id())
                .header("HX-Request", "true")
                .param("scope", "cat-feedback")
                .param("topic", topic.id())
                .param("view", "comments")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("ENDMARKER")));
    }

    @Test
    @DisplayName("Topics view should render collapsed New Topic composer above topic list panel")
    void topicsComposerCollapsedAndAboveList() throws Exception {
        mockMvc.perform(get("/forum/topics")
                .header("HX-Request", "true")
                .param("scope", "cat-feedback")
                .param("view", "topics")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("forum-demo-composer-toggle")))
            .andExpect(content().string(containsString(">New Topic<")))
            .andExpect(content().string(not(containsString("forum-demo-composer-toggle\" open"))))
            .andExpect(result -> {
                String html = result.getResponse().getContentAsString();
                int composerIndex = html.indexOf("forum-demo-composer-toggle");
                int topicsHeaderIndex = html.indexOf(">Topics<");
                assertTrue(
                    composerIndex >= 0 && topicsHeaderIndex >= 0 && composerIndex < topicsHeaderIndex,
                    "Expected New Topic composer to render above Topics panel"
                );
            });
    }

    @Test
    @DisplayName("Comments view should render collapsed New Comment composer below comments list")
    void commentsComposerCollapsedAndBelowList() throws Exception {
        ForumDemoService.TopicView topic = forumService.listAllTopicsNewestFirst().getFirst();

        mockMvc.perform(get("/forum/topics/{topicId}/comments", topic.id())
                .header("HX-Request", "true")
                .param("scope", topic.categoryId())
                .param("topic", topic.id())
                .param("view", "comments")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("forum-demo-composer-toggle")))
            .andExpect(content().string(containsString(">New Comment<")))
            .andExpect(content().string(not(containsString("forum-demo-composer-toggle\" open"))))
            .andExpect(result -> {
                String html = result.getResponse().getContentAsString();
                int commentsIndex = html.indexOf("forum-comments-view");
                int composerIndex = html.lastIndexOf("forum-demo-composer-toggle");
                assertTrue(
                    commentsIndex >= 0 && composerIndex > commentsIndex,
                    "Expected New Comment composer to render below comment list"
                );
            });
    }

    @Test
    @DisplayName("Topic pagination endpoint should return HTMX fragment content")
    void topicPaginationFragment() throws Exception {
        mockMvc.perform(get("/forum/topics")
                .header("HX-Request", "true")
                .param("scope", "cat-feedback")
                .param("view", "topics")
                .param("topicPage", "2")
                .param("topicSize", "5")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"forum-main\"")))
            .andExpect(content().string(containsString("Page 2 of")))
            .andExpect(content().string(containsString("data-sp-scroll-top=\"target\"")))
            .andExpect(content().string(not(containsString("<!DOCTYPE html>"))));
    }

    @Test
    @DisplayName("Quote action should prefill comment composer with quote token")
    void quoteActionPrefillsComposer() throws Exception {
        ForumDemoService.CommentView comment = forumService.listAllCommentsOldestFirst().getFirst();
        ForumDemoService.TopicView topic = forumService.findTopic(comment.topicId()).orElseThrow();

        mockMvc.perform(post("/forum/comments/{commentId}/quote", comment.id())
                .header("HX-Request", "true")
                .param("scope", topic.categoryId())
                .param("topic", topic.id())
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("forum-comment-meta")))
            .andExpect(content().string(containsString("forum-comment-actions")))
            .andExpect(content().string(containsString("forum-demo-composer-toggle\" open")))
            .andExpect(content().string(containsString("[[quote::" + comment.id() + "]]")));
    }

    @Test
    @DisplayName("Topic quote action should keep comment composer expanded")
    void topicQuoteActionKeepsComposerExpanded() throws Exception {
        ForumDemoService.TopicView topic = forumService.listAllTopicsNewestFirst().getFirst();

        mockMvc.perform(post("/forum/topics/{topicId}/quote", topic.id())
                .header("HX-Request", "true")
                .param("scope", topic.categoryId())
                .param("topic", topic.id())
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("forum-demo-composer-toggle\" open")))
            .andExpect(content().string(containsString("[[quote::" + topic.id() + "]]")));
    }

    @Test
    @DisplayName("Quote action should append quote token to existing comment draft text")
    void quoteActionAppendsToExistingDraftText() throws Exception {
        ForumDemoService.CommentView comment = forumService.listAllCommentsOldestFirst().getFirst();
        ForumDemoService.TopicView topic = forumService.findTopic(comment.topicId()).orElseThrow();
        String existingDraft = "Already typing this comment.";

        mockMvc.perform(post("/forum/comments/{commentId}/quote", comment.id())
                .header("HX-Request", "true")
                .param("scope", topic.categoryId())
                .param("topic", topic.id())
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8")
                .param("body", existingDraft))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(existingDraft)))
            .andExpect(content().string(containsString("forum-demo-composer-toggle\" open")))
            .andExpect(content().string(containsString("[[quote::" + comment.id() + "]]")))
            .andExpect(result -> {
                String html = result.getResponse().getContentAsString();
                int existingIndex = html.indexOf(existingDraft);
                int quoteIndex = html.indexOf("[[quote::" + comment.id() + "]]");
                assertTrue(
                    existingIndex >= 0 && quoteIndex > existingIndex,
                    "Expected quote token to be appended after existing draft text"
                );
            });
    }

    @Test
    @DisplayName("Comments view should resolve quote tags into demo quote scaffolding")
    void quoteTagsResolveToDemoScaffold() throws Exception {
        ForumDemoService.CommentView quotedComment = forumService.listAllCommentsOldestFirst().stream()
            .filter(comment -> comment.body().contains("[[quote::"))
            .findFirst()
            .orElseThrow();
        ForumDemoService.TopicView topic = forumService.findTopic(quotedComment.topicId()).orElseThrow();

        mockMvc.perform(get("/forum/topics/{topicId}/comments", topic.id())
                .header("HX-Request", "true")
                .param("scope", topic.categoryId())
                .param("topic", topic.id())
                .param("view", "comments")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("forum-demo-quote")))
            .andExpect(content().string(containsString("Quoted comment by")));
    }

    @Test
    @DisplayName("Quote scaffold should render full normalized quoted comment text without truncation")
    void quoteScaffoldRendersFullQuotedCommentText() throws Exception {
        ForumViewer viewer = new ForumViewer("user-quote-full", "Quote Full", false);
        ForumDemoService.TopicView topic = forumService.createTopic(
            "cat-feedback",
            "Quote Full Text Topic",
            "Topic body",
            viewer
        ).orElseThrow();

        String quoteTailMarker = "QUOTE_FULL_TEXT_TAIL_MARKER";
        String quotedBody = "Long quoted body " + "word ".repeat(90) + quoteTailMarker;
        ForumDemoService.CommentView quoted = forumService.createComment(topic.id(), null, quotedBody, viewer).orElseThrow();
        forumService.createComment(topic.id(), null, "[[quote::" + quoted.id() + "]]", viewer).orElseThrow();

        mockMvc.perform(get("/forum/topics/{topicId}/comments", topic.id())
                .header("HX-Request", "true")
                .param("scope", topic.categoryId())
                .param("topic", topic.id())
                .param("view", "comments")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "100"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("forum-demo-quote")))
            .andExpect(content().string(containsString(quoteTailMarker)));
    }

    @Test
    @DisplayName("Owner can edit; non-owner delete should be forbidden")
    void ownerEditAndNonOwnerDeleteAuthorization() throws Exception {
        MockHttpSession ownerSession = new MockHttpSession();
        MockHttpSession otherSession = new MockHttpSession();

        mockMvc.perform(post("/forum/viewer")
                .session(ownerSession)
                .param("displayName", "Owner")
                .param("userId", "user-owner")
                .param("role", "user")
                .param("scope", "cat-feedback")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/forum/viewer")
                .session(otherSession)
                .param("displayName", "Other")
                .param("userId", "user-other")
                .param("role", "user")
                .param("scope", "cat-feedback")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk());

        String title = "Owner Edit Flow Topic";

        mockMvc.perform(post("/forum/topics/create")
                .session(ownerSession)
                .header("HX-Request", "true")
                .param("scope", "cat-feedback")
                .param("title", title)
                .param("body", "Original body")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk());

        ForumDemoService.TopicView createdTopic = forumService.listTopicsForCategory("cat-feedback").stream()
            .filter(topic -> title.equals(topic.title()))
            .findFirst()
            .orElseThrow();

        mockMvc.perform(post("/forum/topics/{topicId}/delete", createdTopic.id())
                .session(otherSession)
                .header("HX-Request", "true")
                .param("scope", "cat-feedback")
                .param("topic", createdTopic.id())
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isForbidden())
            .andExpect(content().string(containsString("not allowed")));

        mockMvc.perform(post("/forum/topics/{topicId}/update", createdTopic.id())
                .session(ownerSession)
                .header("HX-Request", "true")
                .param("scope", "cat-feedback")
                .param("topic", createdTopic.id())
                .param("title", "Updated Title")
                .param("body", "Updated body")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Updated Title")));
    }

    @Test
    @DisplayName("Moderator can delete topic owned by another viewer")
    void moderatorCanDeleteTopic() throws Exception {
        MockHttpSession ownerSession = new MockHttpSession();
        MockHttpSession modSession = new MockHttpSession();

        mockMvc.perform(post("/forum/viewer")
                .session(ownerSession)
                .param("displayName", "Author")
                .param("userId", "user-author")
                .param("role", "user")
                .param("scope", "cat-guides")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/forum/viewer")
                .session(modSession)
                .param("displayName", "Moderator")
                .param("userId", "user-mod")
                .param("role", "moderator")
                .param("scope", "cat-guides")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk());

        String title = "Moderator Delete Flow Topic";
        mockMvc.perform(post("/forum/topics/create")
                .session(ownerSession)
                .header("HX-Request", "true")
                .param("scope", "cat-guides")
                .param("title", title)
                .param("body", "To be deleted")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk());

        ForumDemoService.TopicView createdTopic = forumService.listTopicsForCategory("cat-guides").stream()
            .filter(topic -> title.equals(topic.title()))
            .findFirst()
            .orElseThrow();

        mockMvc.perform(post("/forum/topics/{topicId}/delete", createdTopic.id())
                .session(modSession)
                .header("HX-Request", "true")
                .param("scope", "cat-guides")
                .param("topic", createdTopic.id())
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Topic deleted")));
    }

    @Test
    @DisplayName("Comment pagination endpoint should support second-page thread rendering")
    void commentPaginationFragment() throws Exception {
        ForumDemoService.TopicView pagedTopic = forumService.listAllTopicsNewestFirst().stream()
            .filter(topic -> forumService.countCommentsForTopic(topic.id()) >= 6)
            .findFirst()
            .orElseThrow();

        mockMvc.perform(get("/forum/topics/{topicId}/comments", pagedTopic.id())
                .header("HX-Request", "true")
                .param("scope", pagedTopic.categoryId())
                .param("topic", pagedTopic.id())
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "2")
                .param("commentSize", "3"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Page 2 of")))
            .andExpect(content().string(containsString("data-sp-scroll-top=\"target\"")))
            .andExpect(content().string(containsString("Parent Post")))
            .andExpect(content().string(not(containsString("forum-demo-parent-topic-toggle\" open"))))
            .andExpect(content().string(not(containsString("<!DOCTYPE html>"))));
    }
}
