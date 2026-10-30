package io.mindspice.demo.forum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
    void inputBoundsRejectWithoutMutationOrIdConsumption() {
        ForumDemoService service = new ForumDemoService();
        ForumDemoService control = new ForumDemoService();
        ForumViewer viewer = new ForumViewer("u".repeat(128), "n".repeat(128), false);
        ForumViewer longId = new ForumViewer("u".repeat(129), "name", false);
        ForumViewer longName = new ForumViewer("user", "n".repeat(129), false);
        String title = "t".repeat(256);
        String body = "b".repeat(16384);

        assertTrue(service.createTopic("cat-guides", title + "t", body, viewer).isEmpty());
        assertTrue(service.createTopic("cat-guides", title, body + "b", viewer).isEmpty());
        assertTrue(service.createTopic("cat-guides", title, body, longId).isEmpty());
        assertTrue(service.createTopic("cat-guides", title, body, longName).isEmpty());
        var topic = service.createTopic("cat-guides", title, body, viewer).orElseThrow();
        assertEquals(control.createTopic("cat-guides", title, body, viewer).orElseThrow().id(), topic.id());
        assertTrue(service.updateTopic(topic.id(), title + "t", "changed").isEmpty());
        assertTrue(service.updateTopic(topic.id(), "changed", body + "b").isEmpty());
        assertEquals(topic, service.findTopic(topic.id()).orElseThrow());

        assertTrue(service.createComment(topic.id(), null, body + "b", viewer).isEmpty());
        assertTrue(service.createComment(topic.id(), null, body, longId).isEmpty());
        assertTrue(service.createComment(topic.id(), null, body, longName).isEmpty());
        var comment = service.createComment(topic.id(), null, body, viewer).orElseThrow();
        assertEquals(control.createComment(topic.id(), null, body, viewer).orElseThrow().id(), comment.id());
        assertTrue(service.updateComment(comment.id(), body + "b").isEmpty());
        assertEquals(comment, service.findComment(comment.id()).orElseThrow());
        assertTrue(service.updateTopic(topic.id(), title, body).isPresent());
        assertTrue(service.updateComment(comment.id(), body).isPresent());
    }

    @Test
    void capacityIncludesSeedsAndInvalidCreatesDoNotConsumeIds() {
        ForumDemoService service = new ForumDemoService();
        ForumViewer viewer = new ForumViewer("user", "User", false);
        String topicId = service.listAllTopicsNewestFirst().getFirst().id();
        while (service.listAllTopicsNewestFirst().size() < 128) {
            service.createTopic("cat-guides", "title", "body", viewer).orElseThrow();
        }
        assertTrue(service.createTopic("missing", "title", "body", viewer).isEmpty());
        assertTrue(service.createTopic("cat-guides", " ", "body", viewer).isEmpty());
        assertTrue(service.createTopic("cat-guides", "title", "body", new ForumViewer("x".repeat(129), "n", false)).isEmpty());
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
            () -> service.createTopic("cat-guides", "title", "body", viewer)).getStatusCode());
        var lastTopic = service.listAllTopicsNewestFirst().stream()
            .mapToInt(t -> Integer.parseInt(t.id().substring(6))).max().orElseThrow();
        assertTrue(service.deleteTopic(topicId));
        assertEquals("topic-%04d".formatted(lastTopic + 1),
            service.createTopic("cat-guides", "title", "body", viewer).orElseThrow().id());

        String target = service.listAllTopicsNewestFirst().getFirst().id();
        while (service.listAllCommentsOldestFirst().size() < 512) {
            service.createComment(target, null, "body", viewer).orElseThrow();
        }
        assertTrue(service.createComment("missing", null, "body", viewer).isEmpty());
        assertTrue(service.createComment(target, null, " ", viewer).isEmpty());
        assertTrue(service.createComment(target, null, "body", new ForumViewer("u", "x".repeat(129), false)).isEmpty());
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
            () -> service.createComment(target, null, "body", viewer)).getStatusCode());
        var comments = service.listAllCommentsOldestFirst();
        int lastComment = comments.stream().mapToInt(c -> Integer.parseInt(c.id().substring(8))).max().orElseThrow();
        assertTrue(service.deleteComment(comments.getLast().id()));
        assertEquals("comment-%05d".formatted(lastComment + 1),
            service.createComment(target, null, "body", viewer).orElseThrow().id());
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
