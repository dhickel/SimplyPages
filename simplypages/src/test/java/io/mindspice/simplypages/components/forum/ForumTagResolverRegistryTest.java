package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.components.forum.tags.*;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForumTagResolverRegistryTest {

    @Test
    @DisplayName("ForumTagResolverRegistry should fail on duplicate normalized key registration")
    void duplicateKeyFailsFast() {
        ForumTagResolverRegistry registry = ForumTagResolverRegistry.create()
            .register(ForumTagResolvers.of("quote", tags -> Map.of()));

        assertThrows(IllegalArgumentException.class,
            () -> registry.register(ForumTagResolvers.of("Quote", tags -> Map.of())));
    }

    @Test
    @DisplayName("ForumTagResolverRegistry should resolve batches by type")
    void resolvesBatchesByType() {
        AtomicInteger batchCalls = new AtomicInteger();

        ForumTagResolver resolver = ForumTagResolvers.of("custom.tag", tags -> {
            batchCalls.incrementAndGet();
            Map<Tag, Component> resolved = new LinkedHashMap<>();
            for (Tag tag : tags) {
                resolved.put(tag, new HtmlTag("span").withInnerText("resolved:" + tag.value()));
            }
            return resolved;
        });

        ForumTagResolverRegistry registry = ForumTagResolverRegistry.create().register(resolver);
        TagType type = TagType.of("custom.tag");
        Tag a = new Tag(type, "a");
        Tag b = new Tag(type, "b");

        Map<Tag, Component> resolved = registry.resolveAll(Map.of(type, Set.of(a, b)));

        assertEquals(1, batchCalls.get());
        assertEquals(2, resolved.size());
        assertTrue(resolved.containsKey(a));
        assertTrue(resolved.containsKey(b));
    }

    @Test
    @DisplayName("ForumTagResolverRegistry should return empty when type has no resolver")
    void missingResolverIsEmpty() {
        ForumTagResolverRegistry registry = ForumTagResolverRegistry.withBuiltIns();
        TagType missing = TagType.of("missing.key");

        Map<Tag, Component> resolved = registry.resolve(missing, Set.of(new Tag(missing, "x")));

        assertTrue(resolved.isEmpty());
        assertFalse(registry.hasResolver(TagType.of("quote.missing")));
    }

    @Test
    @DisplayName("ForumTagResolverRegistry should return empty when resolver returns null or empty")
    void nullOrEmptyResolverResponsesAreNormalized() {
        TagType nullType = TagType.of("null.tag");
        TagType emptyType = TagType.of("empty.tag");
        Tag nullTag = new Tag(nullType, "x");
        Tag emptyTag = new Tag(emptyType, "y");

        ForumTagResolverRegistry registry = ForumTagResolverRegistry.create()
            .register(ForumTagResolvers.of("null.tag", tags -> null))
            .register(ForumTagResolvers.of("empty.tag", tags -> Map.of()));

        assertTrue(registry.resolve(nullType, Set.of(nullTag)).isEmpty());
        assertTrue(registry.resolve(emptyType, Set.of(emptyTag)).isEmpty());
    }

    @Test
    @DisplayName("ForumTagResolverRegistry should return empty on null or empty bulk requests")
    void emptyBulkRequestsReturnEmptyMap() {
        ForumTagResolverRegistry registry = ForumTagResolverRegistry.withBuiltIns();

        assertTrue(registry.resolveAll(null).isEmpty());
        assertTrue(registry.resolveAll(Map.of()).isEmpty());
    }

    @Test
    @DisplayName("ForumTagResolverRegistry should report resolver presence for registered type")
    void hasResolverTrueForRegisteredType() {
        ForumTagResolverRegistry registry = ForumTagResolverRegistry.withBuiltIns();
        assertTrue(registry.hasResolver(TagType.of("quote")));
    }
}
