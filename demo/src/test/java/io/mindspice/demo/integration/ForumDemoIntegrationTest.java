package io.mindspice.demo.integration;

import io.mindspice.demo.DemoApplication;
import io.mindspice.demo.forum.ForumDemoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
            .andExpect(content().string(containsString("Forum Demo")))
            .andExpect(content().string(containsString("id=\"forum-main\"")))
            .andExpect(content().string(containsString("Viewer Session")))
            .andExpect(content().string(containsString("Categories")))
            .andExpect(content().string(not(containsString(">Topics<"))))
            .andExpect(content().string(containsString("Forum")));
    }

    @Test
    @DisplayName("Forum should drill down from category topics into topic comments")
    void drillDownFlowRendersSingleStageAtATime() throws Exception {
        ForumDemoService.TopicView topic = forumService.listTopicsForCategory("cat-feedback").getFirst();

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
            .andExpect(content().string(not(containsString(">Topics<"))));
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
            .andExpect(content().string(containsString("[[quote::" + comment.id() + "]]")));
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
            .andExpect(content().string(not(containsString("<!DOCTYPE html>"))));
    }
}
