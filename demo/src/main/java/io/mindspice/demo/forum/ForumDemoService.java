package io.mindspice.demo.forum;

import io.mindspice.simplypages.components.forum.categories.ForumCategoryData;
import io.mindspice.simplypages.components.forum.comments.ForumCommentData;
import io.mindspice.simplypages.components.forum.topics.ForumTopicData;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory forum backend used by the demo module.
 */
@Service
public class ForumDemoService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.US);

    private final Object lock = new Object();
    private final Map<String, CategoryGroupEntity> groupsById = new LinkedHashMap<>();
    private final Map<String, CategoryEntity> categoriesById = new LinkedHashMap<>();
    private final Map<String, TopicEntity> topicsById = new LinkedHashMap<>();
    private final Map<String, CommentEntity> commentsById = new LinkedHashMap<>();

    private int topicSequence = 1;
    private int commentSequence = 1;

    public ForumDemoService() {
        seedDefaultData();
    }

    public List<CategoryGroupView> listCategoryGroups() {
        synchronized (lock) {
            Map<String, Integer> topicCounts = computeTopicCountsByCategory();
            List<CategoryGroupView> groups = new ArrayList<>();
            for (CategoryGroupEntity group : groupsById.values()) {
                List<CategoryView> categories = new ArrayList<>();
                for (String categoryId : group.categoryIds) {
                    CategoryEntity category = categoriesById.get(categoryId);
                    if (category != null) {
                        categories.add(new CategoryView(
                            category.id,
                            category.groupId,
                            category.title,
                            category.description,
                            topicCounts.getOrDefault(category.id, 0)
                        ));
                    }
                }
                groups.add(new CategoryGroupView(group.id, group.title, List.copyOf(categories)));
            }
            return List.copyOf(groups);
        }
    }

    public List<String> categoryIdsInDisplayOrder() {
        synchronized (lock) {
            List<String> ids = new ArrayList<>();
            for (CategoryGroupEntity group : groupsById.values()) {
                ids.addAll(group.categoryIds);
            }
            return List.copyOf(ids);
        }
    }

    public boolean categoryExists(String categoryId) {
        synchronized (lock) {
            return categoriesById.containsKey(categoryId);
        }
    }

    public List<TopicView> listAllTopicsNewestFirst() {
        synchronized (lock) {
            Map<String, Integer> repliesByTopic = computeRepliesByTopic();
            return topicsById.values().stream()
                .sorted(Comparator.comparing((TopicEntity t) -> t.createdAt).reversed().thenComparing(t -> t.id))
                .map(topic -> toTopicView(topic, repliesByTopic.getOrDefault(topic.id, 0)))
                .toList();
        }
    }

    public List<TopicView> listTopicsForCategory(String categoryId) {
        synchronized (lock) {
            if (categoryId == null || categoryId.isBlank()) {
                return List.of();
            }
            Map<String, Integer> repliesByTopic = computeRepliesByTopic();
            return topicsById.values().stream()
                .filter(topic -> categoryId.equals(topic.categoryId))
                .sorted(Comparator.comparing((TopicEntity t) -> t.createdAt).reversed().thenComparing(t -> t.id))
                .map(topic -> toTopicView(topic, repliesByTopic.getOrDefault(topic.id, 0)))
                .toList();
        }
    }

    public int countTopicsForCategory(String categoryId) {
        synchronized (lock) {
            if (categoryId == null || categoryId.isBlank()) {
                return 0;
            }
            int count = 0;
            for (TopicEntity topic : topicsById.values()) {
                if (categoryId.equals(topic.categoryId)) {
                    count++;
                }
            }
            return count;
        }
    }

    public List<CommentView> listAllCommentsOldestFirst() {
        synchronized (lock) {
            Map<String, Integer> repliesByComment = computeRepliesByComment();
            return commentsById.values().stream()
                .sorted(Comparator.comparing((CommentEntity c) -> c.createdAt).thenComparing(c -> c.id))
                .map(comment -> toCommentView(comment, repliesByComment.getOrDefault(comment.id, 0)))
                .toList();
        }
    }

    public int countCommentsForTopic(String topicId) {
        synchronized (lock) {
            if (topicId == null || topicId.isBlank()) {
                return 0;
            }
            int count = 0;
            for (CommentEntity comment : commentsById.values()) {
                if (topicId.equals(comment.topicId)) {
                    count++;
                }
            }
            return count;
        }
    }

    public Optional<TopicView> findTopic(String topicId) {
        synchronized (lock) {
            TopicEntity topic = topicsById.get(topicId);
            if (topic == null) {
                return Optional.empty();
            }
            int replies = computeRepliesByTopic().getOrDefault(topicId, 0);
            return Optional.of(toTopicView(topic, replies));
        }
    }

    public Optional<CommentView> findComment(String commentId) {
        synchronized (lock) {
            CommentEntity comment = commentsById.get(commentId);
            if (comment == null) {
                return Optional.empty();
            }
            int replies = computeRepliesByComment().getOrDefault(commentId, 0);
            return Optional.of(toCommentView(comment, replies));
        }
    }

    public Optional<TopicView> createTopic(String categoryId, String title, String body, ForumViewer viewer) {
        Objects.requireNonNull(viewer, "viewer");
        synchronized (lock) {
            if (!categoriesById.containsKey(categoryId)) {
                return Optional.empty();
            }

            String normalizedTitle = normalizeRequired(title);
            String normalizedBody = normalizeRequired(body);
            if (normalizedTitle == null || normalizedBody == null) {
                return Optional.empty();
            }

            String topicId = String.format("topic-%04d", topicSequence++);
            TopicEntity entity = new TopicEntity(
                topicId,
                categoryId,
                viewer.userId(),
                normalizedTitle,
                normalizedBody,
                viewer.displayName(),
                LocalDateTime.now(),
                0
            );
            topicsById.put(topicId, entity);
            return Optional.of(toTopicView(entity, 0));
        }
    }

    public Optional<CommentView> createComment(String topicId, String parentId, String body, ForumViewer viewer) {
        Objects.requireNonNull(viewer, "viewer");
        synchronized (lock) {
            TopicEntity topic = topicsById.get(topicId);
            if (topic == null) {
                return Optional.empty();
            }

            String normalizedBody = normalizeRequired(body);
            if (normalizedBody == null) {
                return Optional.empty();
            }

            int depth = 0;
            String normalizedParent = normalizeOptional(parentId);
            if (normalizedParent != null) {
                CommentEntity parent = commentsById.get(normalizedParent);
                if (parent == null || !topicId.equals(parent.topicId)) {
                    normalizedParent = null;
                } else {
                    depth = Math.min(5, parent.depth + 1);
                }
            }

            String commentId = String.format("comment-%05d", commentSequence++);
            CommentEntity entity = new CommentEntity(
                commentId,
                topicId,
                normalizedParent,
                depth,
                viewer.userId(),
                normalizedBody,
                viewer.displayName(),
                null,
                LocalDateTime.now(),
                0
            );
            commentsById.put(commentId, entity);
            return Optional.of(toCommentView(entity, 0));
        }
    }

    public boolean canEditTopic(String topicId, ForumViewer viewer) {
        Objects.requireNonNull(viewer, "viewer");
        synchronized (lock) {
            TopicEntity topic = topicsById.get(topicId);
            return topic != null && isOwnerOrModerator(topic.ownerId, viewer);
        }
    }

    public boolean canDeleteTopic(String topicId, ForumViewer viewer) {
        return canEditTopic(topicId, viewer);
    }

    public boolean canEditComment(String commentId, ForumViewer viewer) {
        Objects.requireNonNull(viewer, "viewer");
        synchronized (lock) {
            CommentEntity comment = commentsById.get(commentId);
            return comment != null && isOwnerOrModerator(comment.ownerId, viewer);
        }
    }

    public boolean canDeleteComment(String commentId, ForumViewer viewer) {
        return canEditComment(commentId, viewer);
    }

    public Optional<TopicView> updateTopic(String topicId, String title, String body) {
        synchronized (lock) {
            TopicEntity topic = topicsById.get(topicId);
            if (topic == null) {
                return Optional.empty();
            }

            String normalizedTitle = normalizeRequired(title);
            String normalizedBody = normalizeRequired(body);
            if (normalizedTitle == null || normalizedBody == null) {
                return Optional.empty();
            }

            topic.title = normalizedTitle;
            topic.body = normalizedBody;
            int replies = computeRepliesByTopic().getOrDefault(topic.id, 0);
            return Optional.of(toTopicView(topic, replies));
        }
    }

    public Optional<CommentView> updateComment(String commentId, String body) {
        synchronized (lock) {
            CommentEntity comment = commentsById.get(commentId);
            if (comment == null) {
                return Optional.empty();
            }

            String normalizedBody = normalizeRequired(body);
            if (normalizedBody == null) {
                return Optional.empty();
            }

            comment.body = normalizedBody;
            int replies = computeRepliesByComment().getOrDefault(comment.id, 0);
            return Optional.of(toCommentView(comment, replies));
        }
    }

    public boolean deleteTopic(String topicId) {
        synchronized (lock) {
            TopicEntity removed = topicsById.remove(topicId);
            if (removed == null) {
                return false;
            }

            commentsById.values().removeIf(comment -> topicId.equals(comment.topicId));
            return true;
        }
    }

    public boolean deleteComment(String commentId) {
        synchronized (lock) {
            if (!commentsById.containsKey(commentId)) {
                return false;
            }

            Deque<String> stack = new ArrayDeque<>();
            stack.push(commentId);
            while (!stack.isEmpty()) {
                String currentId = stack.pop();
                for (CommentEntity comment : commentsById.values()) {
                    if (currentId.equals(comment.parentId)) {
                        stack.push(comment.id);
                    }
                }
                commentsById.remove(currentId);
            }
            return true;
        }
    }

    private void seedDefaultData() {
        synchronized (lock) {
            ForumSeedFactory.SeedData seed = ForumSeedFactory.buildDefaultSeed();

            for (ForumSeedFactory.GroupSeed group : seed.groups()) {
                groupsById.put(group.id(), new CategoryGroupEntity(
                    group.id(),
                    group.title(),
                    List.copyOf(group.categoryIds())
                ));
            }

            for (ForumSeedFactory.CategorySeed category : seed.categories()) {
                categoriesById.put(category.id(), new CategoryEntity(
                    category.id(),
                    category.groupId(),
                    category.title(),
                    category.description()
                ));
            }

            int maxTopic = 0;
            for (ForumSeedFactory.TopicSeed topic : seed.topics()) {
                topicsById.put(topic.id(), new TopicEntity(
                    topic.id(),
                    topic.categoryId(),
                    topic.ownerId(),
                    topic.title(),
                    topic.body(),
                    topic.author(),
                    topic.createdAt(),
                    topic.likes()
                ));
                maxTopic = Math.max(maxTopic, parseNumericSuffix(topic.id()));
            }

            int maxComment = 0;
            for (ForumSeedFactory.CommentSeed comment : seed.comments()) {
                commentsById.put(comment.id(), new CommentEntity(
                    comment.id(),
                    comment.topicId(),
                    comment.parentId(),
                    comment.depth(),
                    comment.ownerId(),
                    comment.body(),
                    comment.author(),
                    comment.avatarUrl(),
                    comment.createdAt(),
                    comment.likes()
                ));
                maxComment = Math.max(maxComment, parseNumericSuffix(comment.id()));
            }

            topicSequence = Math.max(1, maxTopic + 1);
            commentSequence = Math.max(1, maxComment + 1);
        }
    }

    private Map<String, Integer> computeTopicCountsByCategory() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TopicEntity topic : topicsById.values()) {
            counts.merge(topic.categoryId, 1, Integer::sum);
        }
        return counts;
    }

    private Map<String, Integer> computeRepliesByTopic() {
        Map<String, Integer> replies = new LinkedHashMap<>();
        for (CommentEntity comment : commentsById.values()) {
            replies.merge(comment.topicId, 1, Integer::sum);
        }
        return replies;
    }

    private Map<String, Integer> computeRepliesByComment() {
        Map<String, Integer> replies = new LinkedHashMap<>();
        for (CommentEntity comment : commentsById.values()) {
            if (comment.parentId != null && !comment.parentId.isBlank()) {
                replies.merge(comment.parentId, 1, Integer::sum);
            }
        }
        return replies;
    }

    private TopicView toTopicView(TopicEntity topic, int replies) {
        return new TopicView(
            topic.id,
            topic.categoryId,
            topic.ownerId,
            topic.title,
            topic.body,
            topic.author,
            topic.createdAt.format(TIME_FORMAT),
            topic.likes,
            replies
        );
    }

    private CommentView toCommentView(CommentEntity comment, int replies) {
        return new CommentView(
            comment.id,
            comment.topicId,
            comment.parentId,
            comment.depth,
            comment.ownerId,
            comment.body,
            comment.author,
            comment.avatarUrl,
            comment.createdAt.format(TIME_FORMAT),
            comment.likes,
            replies
        );
    }

    private boolean isOwnerOrModerator(String ownerId, ForumViewer viewer) {
        return viewer.moderator() || ownerId.equals(viewer.userId());
    }

    private String normalizeRequired(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int parseNumericSuffix(String id) {
        int idx = id.lastIndexOf('-');
        if (idx < 0 || idx + 1 >= id.length()) {
            return 0;
        }
        try {
            return Integer.parseInt(id.substring(idx + 1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static final class CategoryGroupEntity {
        private final String id;
        private final String title;
        private final List<String> categoryIds;

        private CategoryGroupEntity(String id, String title, List<String> categoryIds) {
            this.id = id;
            this.title = title;
            this.categoryIds = categoryIds;
        }
    }

    private static final class CategoryEntity {
        private final String id;
        private final String groupId;
        private final String title;
        private final String description;

        private CategoryEntity(String id, String groupId, String title, String description) {
            this.id = id;
            this.groupId = groupId;
            this.title = title;
            this.description = description;
        }
    }

    private static final class TopicEntity {
        private final String id;
        private final String categoryId;
        private final String ownerId;
        private String title;
        private String body;
        private final String author;
        private final LocalDateTime createdAt;
        private final int likes;

        private TopicEntity(
            String id,
            String categoryId,
            String ownerId,
            String title,
            String body,
            String author,
            LocalDateTime createdAt,
            int likes
        ) {
            this.id = id;
            this.categoryId = categoryId;
            this.ownerId = ownerId;
            this.title = title;
            this.body = body;
            this.author = author;
            this.createdAt = createdAt;
            this.likes = likes;
        }
    }

    private static final class CommentEntity {
        private final String id;
        private final String topicId;
        private final String parentId;
        private final int depth;
        private final String ownerId;
        private String body;
        private final String author;
        private final String avatarUrl;
        private final LocalDateTime createdAt;
        private final int likes;

        private CommentEntity(
            String id,
            String topicId,
            String parentId,
            int depth,
            String ownerId,
            String body,
            String author,
            String avatarUrl,
            LocalDateTime createdAt,
            int likes
        ) {
            this.id = id;
            this.topicId = topicId;
            this.parentId = parentId;
            this.depth = depth;
            this.ownerId = ownerId;
            this.body = body;
            this.author = author;
            this.avatarUrl = avatarUrl;
            this.createdAt = createdAt;
            this.likes = likes;
        }
    }

    public record CategoryGroupView(
        String id,
        String title,
        List<CategoryView> categories
    ) {}

    public record CategoryView(
        String id,
        String groupId,
        String title,
        String description,
        Integer topicCount
    ) implements ForumCategoryData {}

    public record TopicView(
        String id,
        String categoryId,
        String ownerId,
        String title,
        String body,
        String author,
        String timestamp,
        Integer likes,
        Integer replies
    ) implements ForumTopicData {}

    public record CommentView(
        String id,
        String topicId,
        String parentId,
        int depth,
        String ownerId,
        String body,
        String author,
        String avatarUrl,
        String timestamp,
        Integer likes,
        Integer replies
    ) implements ForumCommentData {}
}
