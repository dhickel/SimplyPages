package io.mindspice.simplypages.components.forum.tags;

import io.mindspice.simplypages.core.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Resolver registry keyed by {@link TagType}.
 */
public final class ForumTagResolverRegistry {

    private final Map<TagType, ForumTagResolver> resolvers = new LinkedHashMap<>();

    public static ForumTagResolverRegistry create() {
        return new ForumTagResolverRegistry();
    }

    public static ForumTagResolverRegistry withBuiltIns() {
        return create().registerBuiltIns();
    }

    public ForumTagResolverRegistry registerBuiltIns() {
        return register(ForumTagResolvers.quote())
            .register(ForumTagResolvers.image())
            .register(ForumTagResolvers.mention())
            .register(ForumTagResolvers.link());
    }

    public ForumTagResolverRegistry register(ForumTagResolver resolver) {
        Objects.requireNonNull(resolver, "resolver");
        TagType type = resolver.tagType();
        if (resolvers.containsKey(type)) {
            throw new IllegalArgumentException("Duplicate forum tag resolver registration for key: " + type.key());
        }
        resolvers.put(type, resolver);
        return this;
    }

    public boolean hasResolver(TagType type) {
        return resolvers.containsKey(Objects.requireNonNull(type, "type"));
    }

    public Map<Tag, Component> resolve(TagType type, Set<Tag> tags) {
        Objects.requireNonNull(type, "type");
        ForumTagResolver resolver = resolvers.get(type);
        if (resolver == null || tags == null || tags.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Tag, Component> resolved = resolver.resolveBatch(Set.copyOf(tags));
        if (resolved == null || resolved.isEmpty()) {
            return Collections.emptyMap();
        }
        return Map.copyOf(resolved);
    }

    public Map<Tag, Component> resolveAll(Map<TagType, Set<Tag>> tagsByType) {
        if (tagsByType == null || tagsByType.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Tag, Component> allResolved = new LinkedHashMap<>();
        for (Map.Entry<TagType, Set<Tag>> entry : tagsByType.entrySet()) {
            Map<Tag, Component> resolved = resolve(entry.getKey(), entry.getValue());
            if (!resolved.isEmpty()) {
                allResolved.putAll(resolved);
            }
        }

        return Map.copyOf(allResolved);
    }
}
