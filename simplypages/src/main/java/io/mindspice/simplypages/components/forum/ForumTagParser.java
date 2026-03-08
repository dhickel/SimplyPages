package io.mindspice.simplypages.components.forum;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Fault-tolerant parser for forum inline tag tokens.
 *
 * <p>Syntax: {@code [[key::value]]} where {@code key} is normalized to lower-case and may only
 * contain {@code a-z} and {@code .}. To render a literal token without parsing, prefix with a
 * backslash: {@code \[[key::value]]}.</p>
 */
public final class ForumTagParser {

    private static final Pattern KEY_PATTERN = Pattern.compile("[a-z]+(?:\\.[a-z]+)*");

    public sealed interface Segment permits TextSegment, TagSegment {}

    public record TextSegment(String text) implements Segment {
        public TextSegment {
            text = text == null ? "" : text;
        }
    }

    public record TagSegment(String rawToken, String key, String value) implements Segment {
        public TagSegment {
            rawToken = Objects.requireNonNull(rawToken, "rawToken");
            key = Objects.requireNonNull(key, "key");
            value = value == null ? "" : value;
        }
    }

    public static ForumTagParser create() {
        return new ForumTagParser();
    }

    /**
     * Normalizes and validates a tag key.
     *
     * @param rawKey raw key input
     * @return normalized key
     * @throws IllegalArgumentException when key format is invalid
     */
    public static String normalizeKey(String rawKey) {
        if (rawKey == null) {
            throw new IllegalArgumentException("Tag key cannot be null");
        }
        String normalized = rawKey.trim().toLowerCase(Locale.ROOT);
        if (!KEY_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                "Invalid tag key: '" + rawKey + "'. Allowed format: lower-case alpha segments separated by '.'"
            );
        }
        return normalized;
    }

    /**
     * Parses content into literal text and tag token segments.
     *
     * <p>Malformed tokens are preserved as literal text and never throw parse errors.</p>
     */
    public List<Segment> parse(String input) {
        if (input == null || input.isEmpty()) {
            return List.of(new TextSegment(""));
        }

        List<Segment> segments = new ArrayList<>();
        StringBuilder textBuffer = new StringBuilder();

        int i = 0;
        while (i < input.length()) {
            if (isEscapedTokenStart(input, i)) {
                int tokenEnd = input.indexOf("]]", i + 3);
                if (tokenEnd >= 0) {
                    // Drop only the escape slash and keep token literal output.
                    textBuffer.append(input, i + 1, tokenEnd + 2);
                    i = tokenEnd + 2;
                    continue;
                }
            }

            if (isTokenStart(input, i)) {
                int tokenEnd = input.indexOf("]]", i + 2);
                if (tokenEnd < 0) {
                    // Unterminated token; preserve remaining text as-is.
                    textBuffer.append(input.substring(i));
                    break;
                }

                String rawToken = input.substring(i, tokenEnd + 2);
                String body = input.substring(i + 2, tokenEnd);
                int delimiterIdx = body.indexOf("::");

                if (delimiterIdx <= 0) {
                    // Not a valid token body; preserve literal token text.
                    textBuffer.append(rawToken);
                    i = tokenEnd + 2;
                    continue;
                }

                String rawKey = body.substring(0, delimiterIdx);
                String value = body.substring(delimiterIdx + 2);
                String normalizedKey;
                try {
                    normalizedKey = normalizeKey(rawKey);
                } catch (IllegalArgumentException ex) {
                    textBuffer.append(rawToken);
                    i = tokenEnd + 2;
                    continue;
                }

                flushText(segments, textBuffer);
                segments.add(new TagSegment(rawToken, normalizedKey, value));
                i = tokenEnd + 2;
                continue;
            }

            textBuffer.append(input.charAt(i));
            i++;
        }

        flushText(segments, textBuffer);
        if (segments.isEmpty()) {
            return List.of(new TextSegment(""));
        }
        return List.copyOf(segments);
    }

    private boolean isTokenStart(String input, int index) {
        return index + 1 < input.length()
            && input.charAt(index) == '['
            && input.charAt(index + 1) == '[';
    }

    private boolean isEscapedTokenStart(String input, int index) {
        return index + 2 < input.length()
            && input.charAt(index) == '\\'
            && input.charAt(index + 1) == '['
            && input.charAt(index + 2) == '[';
    }

    private void flushText(List<Segment> segments, StringBuilder buffer) {
        if (buffer.length() > 0) {
            segments.add(new TextSegment(buffer.toString()));
            buffer.setLength(0);
        }
    }
}
