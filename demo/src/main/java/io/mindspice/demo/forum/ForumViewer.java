package io.mindspice.demo.forum;

/**
 * Temporary session-backed viewer identity for the forum demo.
 */
public record ForumViewer(
    String userId,
    String displayName,
    boolean moderator
) {
    public ForumViewer {
        userId = normalize(userId, "demo-user");
        displayName = normalize(displayName, "Demo User");
    }

    private static String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
