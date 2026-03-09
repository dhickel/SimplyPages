package io.mindspice.demo.forum;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic seed factory for the demo in-memory forum data.
 */
final class ForumSeedFactory {

    private static final long SEED = 20260309L;

    private ForumSeedFactory() {}

    static SeedData buildDefaultSeed() {
        Random random = new Random(SEED);
        LocalDateTime baseTime = LocalDateTime.of(2026, 3, 1, 10, 0);

        List<GroupSeed> groups = List.of(
            new GroupSeed("grp-platform", "Platform", List.of("cat-announcements", "cat-guides")),
            new GroupSeed("grp-product", "Product", List.of("cat-feedback", "cat-feature-requests")),
            new GroupSeed("grp-community", "Community", List.of("cat-off-topic"))
        );

        List<CategorySeed> categories = List.of(
            new CategorySeed("cat-announcements", "grp-platform", "Announcements", "Release notes and important updates."),
            new CategorySeed("cat-guides", "grp-platform", "How-To Guides", "Implementation tips and walkthroughs."),
            new CategorySeed("cat-feedback", "grp-product", "Feedback", "General product feedback and usage reports."),
            new CategorySeed("cat-feature-requests", "grp-product", "Feature Requests", "Proposals for new capabilities."),
            new CategorySeed("cat-off-topic", "grp-community", "Off Topic", "Community discussion outside product scope.")
        );

        List<String> authors = List.of(
            "Avery", "Jordan", "Sam", "Riley", "Parker", "Kai", "Morgan", "Casey", "Jamie", "Devon"
        );

        List<String> topicPrefixes = List.of(
            "Status update:", "Question:", "Proposal:", "Troubleshooting:", "Showcase:", "RFC:"
        );

        List<String> topicBodies = List.of(
            "We are validating forum pagination behavior across scoped categories and HTMX fragments.",
            "Current test run includes inline tags like [[mention::qa-team]] and [[link::https://example.com/release-notes]].",
            "This thread tracks implementation notes and expected rendering outcomes for contributors.",
            "We are checking edit/delete ownership rules with temporary session identity.",
            "Please share reproducible flows and annotate with [[mention::docs]] when documentation should be updated."
        );

        List<String> commentBodies = List.of(
            "Confirmed on latest branch. Pagination controls look stable.",
            "I can reproduce this with the default dataset and forum actions.",
            "The new renderer classes are much cleaner than the legacy output.",
            "Can we verify this with one more category scope before merging?",
            "I tested quote/edit/delete and the flow is working in HTMX fragments."
        );

        List<TopicSeed> topics = new ArrayList<>();
        List<CommentSeed> comments = new ArrayList<>();

        int topicSeq = 1;
        int commentSeq = 1;
        int topicTimeOffset = 0;

        for (CategorySeed category : categories) {
            int topicCount = 7 + random.nextInt(3);
            for (int i = 0; i < topicCount; i++) {
                String topicId = String.format("topic-%04d", topicSeq++);
                String author = authors.get(random.nextInt(authors.size()));
                String title = topicPrefixes.get(random.nextInt(topicPrefixes.size())) + " "
                    + category.title() + " #" + (i + 1);
                String body = topicBodies.get(random.nextInt(topicBodies.size()));

                LocalDateTime topicTime = baseTime.plusHours(topicTimeOffset++);
                topics.add(new TopicSeed(
                    topicId,
                    category.id(),
                    "user-" + toSlug(author),
                    title,
                    body,
                    author,
                    topicTime,
                    random.nextInt(42)
                ));

                int commentCount = (i == 0)
                    ? 16 + random.nextInt(7)
                    : 6 + random.nextInt(6);
                List<String> topicCommentIds = new ArrayList<>();

                for (int c = 0; c < commentCount; c++) {
                    String commentId = String.format("comment-%05d", commentSeq++);
                    String commentAuthor = authors.get(random.nextInt(authors.size()));
                    String ownerId = "user-" + toSlug(commentAuthor);
                    String parentId = null;
                    int depth = 0;

                    if (c > 0 && random.nextDouble() < 0.35) {
                        parentId = topicCommentIds.get(random.nextInt(topicCommentIds.size()));
                        depth = 1;
                    }

                    String bodyText = commentBodies.get(random.nextInt(commentBodies.size()));
                    if (c > 0 && (c == 1 || random.nextDouble() < 0.4)) {
                        String quotedId = topicCommentIds.get(random.nextInt(topicCommentIds.size()));
                        bodyText = "[[quote::" + quotedId + "]]\n" + bodyText;
                    }

                    comments.add(new CommentSeed(
                        commentId,
                        topicId,
                        parentId,
                        depth,
                        ownerId,
                        bodyText,
                        commentAuthor,
                        null,
                        topicTime.plusMinutes((c + 1) * 3L),
                        random.nextInt(18)
                    ));
                    topicCommentIds.add(commentId);
                }
            }
        }

        return new SeedData(groups, categories, topics, comments);
    }

    private static String toSlug(String input) {
        StringBuilder slug = new StringBuilder();
        for (char c : input.toLowerCase().toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                slug.append(c);
            } else if (slug.length() > 0 && slug.charAt(slug.length() - 1) != '-') {
                slug.append('-');
            }
        }
        while (slug.length() > 0 && slug.charAt(slug.length() - 1) == '-') {
            slug.deleteCharAt(slug.length() - 1);
        }
        return slug.isEmpty() ? "user" : slug.toString();
    }

    record SeedData(
        List<GroupSeed> groups,
        List<CategorySeed> categories,
        List<TopicSeed> topics,
        List<CommentSeed> comments
    ) {}

    record GroupSeed(String id, String title, List<String> categoryIds) {}

    record CategorySeed(String id, String groupId, String title, String description) {}

    record TopicSeed(
        String id,
        String categoryId,
        String ownerId,
        String title,
        String body,
        String author,
        LocalDateTime createdAt,
        int likes
    ) {}

    record CommentSeed(
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
    ) {}
}
