package io.mindspice.simplypages.components.forum.topics;

/**
 * Optional topic title-link metadata populated by {@link ForumTopicRenderer}.
 */
public record ForumTopicTitleLink(
    String href,
    String hxGet,
    String hxTarget,
    String hxSwap,
    String hxPushUrl
) {
    public ForumTopicTitleLink {
        if (href == null || href.isBlank()) {
            throw new IllegalArgumentException("Topic title link requires a non-blank href");
        }

        href = href.trim();
        hxGet = normalize(hxGet);
        hxTarget = normalize(hxTarget);
        hxSwap = normalize(hxSwap);
        hxPushUrl = normalize(hxPushUrl);
    }

    public static ForumTopicTitleLink href(String href) {
        return new ForumTopicTitleLink(href, null, null, null, null);
    }

    public static ForumTopicTitleLink htmx(
        String href,
        String hxGet,
        String hxTarget,
        String hxSwap,
        String hxPushUrl
    ) {
        return new ForumTopicTitleLink(href, hxGet, hxTarget, hxSwap, hxPushUrl);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
