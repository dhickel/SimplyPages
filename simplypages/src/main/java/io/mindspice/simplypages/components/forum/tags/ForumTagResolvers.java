package io.mindspice.simplypages.components.forum.tags;

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

    public static ForumTagResolver of(
            String key,
            Function<Set<Tag>, Map<Tag, Component>> batchResolver
    ) {
        TagType type = TagType.of(key);
        return new ForumTagResolver() {
            @Override
            public TagType tagType() {
                return type;
            }

            @Override
            public Map<Tag, Component> resolveBatch(Set<Tag> tags) {
                return batchResolver.apply(tags);
            }
        };
    }

    public static ForumTagResolver quote() {
        return of("quote", tags -> {
            Map<Tag, Component> resolved = new LinkedHashMap<>();
            for (Tag tag : tags) {
                HtmlTag blockquote = new HtmlTag("blockquote")
                    .withAttribute("class", "forum-tag forum-tag-quote");
                HtmlTag link = new HtmlTag("a")
                    .withAttribute("class", "forum-tag-quote-link")
                    .withAttribute("href", "#" + tag.value())
                    .withInnerText("Quoted reference: " + tag.value());
                blockquote.withChild(link);
                resolved.put(tag, blockquote);
            }
            return resolved;
        });
    }

    public static ForumTagResolver image() {
        return of("image", tags -> {
            Map<Tag, Component> resolved = new LinkedHashMap<>();
            for (Tag tag : tags) {
                try {
                    resolved.put(tag, Image.create(tag.value(), "forum-image-tag").withClass("forum-tag forum-tag-image"));
                } catch (IllegalArgumentException ex) {
                    resolved.put(tag, new HtmlTag("span")
                        .withAttribute("class", "forum-tag forum-tag-image-invalid")
                        .withInnerText("[invalid image: " + tag.value() + "]"));
                }
            }
            return resolved;
        });
    }

    public static ForumTagResolver mention() {
        return of("mention", tags -> {
            Map<Tag, Component> resolved = new LinkedHashMap<>();
            for (Tag tag : tags) {
                HtmlTag mention = new HtmlTag("span")
                    .withAttribute("class", "forum-tag forum-tag-mention")
                    .withInnerText("@" + tag.value());
                resolved.put(tag, mention);
            }
            return resolved;
        });
    }

    public static ForumTagResolver link() {
        return of("link", tags -> {
            Map<Tag, Component> resolved = new LinkedHashMap<>();
            for (Tag tag : tags) {
                try {
                    resolved.put(tag, Link.create(tag.value(), tag.value()).withClass("forum-tag forum-tag-link"));
                } catch (IllegalArgumentException ex) {
                    resolved.put(tag, new HtmlTag("span")
                        .withAttribute("class", "forum-tag forum-tag-link-invalid")
                        .withInnerText("[invalid link: " + tag.value() + "]"));
                }
            }
            return resolved;
        });
    }
}
