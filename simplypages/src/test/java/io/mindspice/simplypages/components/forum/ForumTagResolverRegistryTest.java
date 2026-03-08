package io.mindspice.simplypages.components.forum;

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
            .register(ForumTagResolvers.of("quote", values -> Map.of()));

        assertThrows(IllegalArgumentException.class,
            () -> registry.register(ForumTagResolvers.of("Quote", values -> Map.of())));
    }

    @Test
    @DisplayName("ForumTagResolverRegistry should resolve batches by key")
    void resolvesBatchesByKey() {
        AtomicInteger batchCalls = new AtomicInteger();

        ForumTagResolver resolver = ForumTagResolvers.of("custom.tag", values -> {
            batchCalls.incrementAndGet();
            Map<String, Component> resolved = new LinkedHashMap<>();
            for (String value : values) {
                resolved.put(value, new HtmlTag("span").withInnerText("resolved:" + value));
            }
            return resolved;
        });

        ForumTagResolverRegistry registry = ForumTagResolverRegistry.create().register(resolver);
        Map<String, Map<String, Component>> resolved = registry.resolveAll(Map.of("custom.tag", Set.of("a", "b")));

        assertEquals(1, batchCalls.get());
        assertTrue(resolved.containsKey("custom.tag"));
        assertEquals(2, resolved.get("custom.tag").size());
    }

    @Test
    @DisplayName("ForumTagResolverRegistry should return empty when key has no resolver")
    void missingResolverIsEmpty() {
        ForumTagResolverRegistry registry = ForumTagResolverRegistry.withBuiltIns();
        Map<String, Component> resolved = registry.resolve("missing.key", Set.of("x"));

        assertTrue(resolved.isEmpty());
        assertFalse(registry.hasResolver("quote.missing"));
    }
}
