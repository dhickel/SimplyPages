package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.components.Image;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Resolver helpers and built-in forum tag resolvers.
 */
public final class ForumTagResolvers {

    private ForumTagResolvers() {}

    /**
     * Creates a resolver from a key and batch function.
     */
    public static ForumTagResolver of(
            String key,
            Function<Set<String>, Map<String, Component>> batchResolver
    ) {
        String normalized = ForumTagParser.normalizeKey(key);
        return new ForumTagResolver() {
            @Override
            public String key() {
                return normalized;
            }

            @Override
            public Map<String, Component> resolveBatch(Set<String> values) {
                return batchResolver.apply(values);
            }
        };
    }

    public static ForumTagResolver quote() {
        return of("quote", values -> {
            Map<String, Component> resolved = new LinkedHashMap<>();
            for (String value : values) {
                HtmlTag blockquote = new HtmlTag("blockquote")
                    .withAttribute("class", "forum-tag forum-tag-quote");
                HtmlTag link = new HtmlTag("a")
                    .withAttribute("class", "forum-tag-quote-link")
                    .withAttribute("href", "#" + value)
                    .withInnerText("Quoted reference: " + value);
                blockquote.withChild(link);
                resolved.put(value, blockquote);
            }
            return resolved;
        });
    }

    public static ForumTagResolver image() {
        return of("image", values -> {
            Map<String, Component> resolved = new LinkedHashMap<>();
            for (String value : values) {
                try {
                    resolved.put(value, Image.create(value, "forum-image-tag").withClass("forum-tag forum-tag-image"));
                } catch (IllegalArgumentException ex) {
                    // Keep unresolved value visible in output.
                    resolved.put(value, new HtmlTag("span")
                        .withAttribute("class", "forum-tag forum-tag-image-invalid")
                        .withInnerText("[invalid image: " + value + "]"));
                }
            }
            return resolved;
        });
    }

    public static ForumTagResolver mention() {
        return of("mention", values -> {
            Map<String, Component> resolved = new LinkedHashMap<>();
            for (String value : values) {
                HtmlTag mention = new HtmlTag("span")
                    .withAttribute("class", "forum-tag forum-tag-mention")
                    .withInnerText("@" + value);
                resolved.put(value, mention);
            }
            return resolved;
        });
    }

    public static ForumTagResolver link() {
        return of("link", values -> {
            Map<String, Component> resolved = new LinkedHashMap<>();
            for (String value : values) {
                try {
                    resolved.put(value, Link.create(value, value).withClass("forum-tag forum-tag-link"));
                } catch (IllegalArgumentException ex) {
                    resolved.put(value, new HtmlTag("span")
                        .withAttribute("class", "forum-tag forum-tag-link-invalid")
                        .withInnerText("[invalid link: " + value + "]"));
                }
            }
            return resolved;
        });
    }
}
