package io.mindspice.demo.forum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ForumDemoServiceTest {

    @Test
    @DisplayName("Default seed should initialize grouped categories and a few dozen topics")
    void seedShapeIsLoaded() {
        ForumDemoService service = new ForumDemoService();

        assertEquals(3, service.listCategoryGroups().size());
        assertEquals(5, service.categoryIdsInDisplayOrder().size());
        assertTrue(service.listAllTopicsNewestFirst().size() >= 30);
        assertTrue(service.listAllCommentsOldestFirst().size() >= 90);
    }

    @Test
    @DisplayName("Deleting a parent comment should remove nested reply comments")
    void deleteCommentRemovesSubtree() {
        ForumDemoService service = new ForumDemoService();
        ForumViewer viewer = new ForumViewer("user-tree", "Tree User", false);

        ForumDemoService.TopicView topic = service.createTopic(
            "cat-feedback",
            "Tree delete topic",
            "Root body",
            viewer
        ).orElseThrow();

        ForumDemoService.CommentView parent = service.createComment(topic.id(), null, "Parent", viewer).orElseThrow();
        ForumDemoService.CommentView child = service.createComment(topic.id(), parent.id(), "Child", viewer).orElseThrow();

        assertTrue(service.findComment(parent.id()).isPresent());
        assertTrue(service.findComment(child.id()).isPresent());

        assertTrue(service.deleteComment(parent.id()));
        assertTrue(service.findComment(parent.id()).isEmpty());
        assertTrue(service.findComment(child.id()).isEmpty());
    }

    @Test
    @DisplayName("Ownership and moderator checks should gate topic modification")
    void ownershipAndModeratorChecks() {
        ForumDemoService service = new ForumDemoService();
        ForumViewer owner = new ForumViewer("user-owner", "Owner", false);
        ForumViewer other = new ForumViewer("user-other", "Other", false);
        ForumViewer moderator = new ForumViewer("user-mod", "Moderator", true);

        ForumDemoService.TopicView topic = service.createTopic(
            "cat-guides",
            "Auth test topic",
            "Body",
            owner
        ).orElseThrow();

        assertTrue(service.canEditTopic(topic.id(), owner));
        assertFalse(service.canEditTopic(topic.id(), other));
        assertTrue(service.canEditTopic(topic.id(), moderator));
    }
}
