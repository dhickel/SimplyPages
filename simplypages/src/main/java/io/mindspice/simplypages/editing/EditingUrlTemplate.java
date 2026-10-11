package io.mindspice.simplypages.editing;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Framework-owned editing URL interpolation; literal URL portions remain caller-owned. */
final class EditingUrlTemplate {
    private static final Pattern SCHEME = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:");
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private EditingUrlTemplate() {}

    static String validate(String template, String... tokens) {
        if (template == null) {
            throw new IllegalArgumentException("Editing URL template must not be null");
        }
        Set<String> allowed = Set.of(tokens);
        // Fixed URLs remain caller-owned, including non-hierarchical URLs.
        if (template.indexOf('{') < 0 && template.indexOf('}') < 0) {
            return template;
        }
        validateTokens(template, allowed, false);
        // Browsers trim leading/trailing C0/space, ignore TAB/LF/CR, and treat path
        // backslashes as slashes for HTTP(S) and relative URLs. Inspect that structure
        // too; expansion must still preserve every original literal byte.
        validateTokens(browserStructure(template), allowed, true);
        return template;
    }

    private static void validateTokens(String template, Set<String> allowed, boolean browser) {
        int fragment = template.indexOf('#');
        int end = fragment < 0 ? template.length() : fragment;
        int question = template.indexOf('?');
        int pathEnd = question < 0 || question >= end ? end : question;
        var scheme = SCHEME.matcher(template);
        boolean absolute = scheme.find();
        int pathStart = absolute ? scheme.end() : 0;
        if (browser && absolute && !template.startsWith("//", pathStart)) {
            throw new IllegalArgumentException("Templated absolute URLs require scheme://literal-authority");
        }
        if (template.startsWith("//", pathStart)) {
            int authorityStart = pathStart + 2;
            if (browser && (authorityStart >= pathEnd || template.charAt(authorityStart) == '/')) {
                throw new IllegalArgumentException("Templated URLs require a nonempty literal authority");
            }
            int slash = template.indexOf('/', authorityStart);
            pathStart = slash < 0 || slash >= pathEnd ? pathEnd : slash;
        }

        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == '}') {
                throw new IllegalArgumentException("Unmatched editing URL token brace");
            }
            if (c != '{') {
                continue;
            }
            int close = template.indexOf('}', i + 1);
            if (close < 0 || !allowed.contains(template.substring(i + 1, close))) {
                throw new IllegalArgumentException("Unsupported editing URL token");
            }
            boolean segment = i >= pathStart && close < pathEnd
                    && (i == pathStart || template.charAt(i - 1) == '/')
                    && (close + 1 == pathEnd || template.charAt(close + 1) == '/');
            int parameterStart = Math.max(question, template.lastIndexOf('&', i));
            int equals = template.indexOf('=', parameterStart + 1);
            boolean value = question >= 0 && i > question && close < end
                    && equals == i - 1
                    && (close + 1 == end || template.charAt(close + 1) == '&');
            if (!segment && !value) {
                throw new IllegalArgumentException("Editing URL tokens require whole path segments or query values");
            }
            i = close;
        }
    }

    private static String browserStructure(String template) {
        int start = 0;
        int end = template.length();
        while (start < end && template.charAt(start) <= ' ') {
            start++;
        }
        while (end > start && template.charAt(end - 1) <= ' ') {
            end--;
        }
        StringBuilder structure = new StringBuilder();
        boolean path = true;
        for (int i = start; i < end; i++) {
            char c = template.charAt(i);
            if (c == '\t' || c == '\n' || c == '\r') {
                continue;
            }
            if (c == '?' || c == '#') {
                path = false;
            }
            structure.append(path && c == '\\' ? '/' : c);
        }
        return structure.toString();
    }

    static String expand(String template, Function<String, String> values) {
        StringBuilder result = new StringBuilder();
        int from = 0;
        int open;
        while ((open = template.indexOf('{', from)) >= 0) {
            int close = template.indexOf('}', open + 1);
            result.append(template, from, open);
            result.append(encodeId(values.apply(template.substring(open + 1, close))));
            from = close + 1;
        }
        return result.append(template, from, template.length()).toString();
    }

    private static String encodeId(String id) {
        if (id == null || id.isEmpty() || id.equals(".") || id.equals("..")) {
            throw new IllegalArgumentException("Invalid interpolated editing identifier");
        }
        boolean blank = true;
        for (int i = 0; i < id.length();) {
            char c = id.charAt(i);
            if (Character.isSurrogate(c) && (!Character.isHighSurrogate(c)
                    || i + 1 == id.length() || !Character.isLowSurrogate(id.charAt(i + 1)))) {
                throw new IllegalArgumentException("Malformed Unicode editing identifier");
            }
            int codePoint = id.codePointAt(i);
            blank &= Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint) || codePoint == 0x85;
            i += Character.charCount(codePoint);
        }
        if (blank) {
            throw new IllegalArgumentException("Blank interpolated editing identifier");
        }
        StringBuilder encoded = new StringBuilder();
        for (byte b : id.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xff;
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '-' || c == '.' || c == '_' || c == '~') {
                encoded.append((char) c);
            } else {
                encoded.append('%').append(HEX[c >>> 4]).append(HEX[c & 15]);
            }
        }
        return encoded.toString();
    }
}
