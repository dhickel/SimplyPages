package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.core.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Registry for forum tag resolvers, keyed by normalized tag key.
 */
public final class ForumTagResolverRegistry {

    private final Map<String, ForumTagResolver> resolvers = new LinkedHashMap<>();

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
        String key = ForumTagParser.normalizeKey(resolver.key());
        if (resolvers.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate forum tag resolver registration for key: " + key);
        }
        resolvers.put(key, resolver);
        return this;
    }

    public boolean hasResolver(String key) {
        String normalized = ForumTagParser.normalizeKey(key);
        return resolvers.containsKey(normalized);
    }

    public Map<String, Component> resolve(String key, Set<String> values) {
        String normalized = ForumTagParser.normalizeKey(key);
        ForumTagResolver resolver = resolvers.get(normalized);
        if (resolver == null || values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Component> resolved = resolver.resolveBatch(Set.copyOf(values));
        if (resolved == null || resolved.isEmpty()) {
            return Collections.emptyMap();
        }
        return Map.copyOf(resolved);
    }

    public Map<String, Map<String, Component>> resolveAll(Map<String, Set<String>> valuesByKey) {
        if (valuesByKey == null || valuesByKey.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Map<String, Component>> allResolved = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : valuesByKey.entrySet()) {
            String key = ForumTagParser.normalizeKey(entry.getKey());
            Map<String, Component> resolved = resolve(key, entry.getValue());
            if (!resolved.isEmpty()) {
                allResolved.put(key, resolved);
            }
        }
        return Map.copyOf(allResolved);
    }
}
