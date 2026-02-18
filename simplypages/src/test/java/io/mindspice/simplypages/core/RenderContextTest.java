package io.mindspice.simplypages.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderContextTest {

    @Test
    @DisplayName("RenderContext should return provided values")
    void testContextValueLookup() {
        SlotKey<String> key = SlotKey.of("username");
        RenderContext context = RenderContext.builder()
            .with(key, "alice")
            .build();

        assertTrue(context.get(key).isPresent());
        assertTrue(context.get(key).get().equals("alice"));
    }

    @Test
    @DisplayName("RenderContext should return default values")
    void testContextDefaultValue() {
        SlotKey<String> key = SlotKey.of("default", "fallback");
        RenderContext context = RenderContext.empty();

        assertTrue(context.get(key).isPresent());
        assertTrue(context.get(key).get().equals("fallback"));
    }

    @Test
    @DisplayName("RenderContext should return empty when no value or default")
    void testContextEmptyValue() {
        SlotKey<String> key = SlotKey.of("missing");
        RenderContext context = RenderContext.empty();

        assertFalse(context.get(key).isPresent());
    }

    @Test
    @DisplayName("RenderContext should support dynamic default providers")
    void testContextDynamicDefault() {
        SlotKey<String> base = SlotKey.of("base");
        SlotKey<String> derived = SlotKey.of("derived", ctx -> ctx.get(base).orElse("none") + "-suffix");
        RenderContext context = RenderContext.builder().with(base, "seed").build();

        assertEquals("seed-suffix", context.get(derived).orElse(""));
    }

    @Test
    @DisplayName("RenderContext should copy provided maps")
    void testContextMapCopy() {
        SlotKey<String> key = SlotKey.of("user");
        Map<SlotKey<?>, Object> values = new HashMap<>();
        values.put(key, "alice");

        RenderContext context = RenderContext.of(values);
        values.put(key, "bob");

        assertEquals("alice", context.get(key).orElse(""));
    }

    @Test
    @DisplayName("RenderContext should support compile policy")
    void testPolicySupport() {
        RenderContext context = RenderContext.empty()
            .withPolicy(RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT);

        assertEquals(RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT, context.getPolicy());
    }

    @Test
    @DisplayName("RenderContext should support compiled entries")
    void testCompiledEntries() {
        SlotKey<String> key = SlotKey.of("title");
        RenderContext context = RenderContext.empty()
            .putCompiled(key, "<b>cached</b>");

        assertTrue(context.isCompiled(key));
        assertEquals("<b>cached</b>", context.getCompiled(key).orElse(""));
        assertFalse(context.get(key).isPresent());
    }

    @Test
    @DisplayName("RenderContext put should invalidate compiled entry")
    void testPutInvalidatesCompiledEntry() {
        SlotKey<String> key = SlotKey.of("title");
        RenderContext context = RenderContext.empty()
            .putCompiled(key, "<b>cached</b>")
            .put(key, "live");

        assertFalse(context.isCompiled(key));
        assertEquals("live", context.get(key).orElse(""));
    }

    @Test
    @DisplayName("RenderContext should support override and restore behavior for nested rendering scopes")
    void testOverrideAndRestoreBehavior() {
        SlotKey<String> key = SlotKey.of("name", "fallback");
        RenderContext context = RenderContext.empty()
            .put(key, "outer");

        assertEquals("outer", context.get(key).orElse(""));

        context.put(key, "inner");
        assertEquals("inner", context.get(key).orElse(""));

        context.put(key, "outer");
        assertEquals("outer", context.get(key).orElse(""));

        context.remove(key);
        assertEquals("fallback", context.get(key).orElse(""));
    }

    @Test
    @DisplayName("RenderContext should evaluate dynamic defaults against latest overridden values")
    void testDynamicDefaultTracksOverrides() {
        SlotKey<String> base = SlotKey.of("base", "root");
        SlotKey<String> derived = SlotKey.of("derived", ctx -> "prefix-" + ctx.get(base).orElse("none"));
        RenderContext context = RenderContext.empty();

        assertEquals("prefix-root", context.get(derived).orElse(""));

        context.put(base, "branch");
        assertEquals("prefix-branch", context.get(derived).orElse(""));

        context.remove(base);
        assertEquals("prefix-root", context.get(derived).orElse(""));
    }

    @Test
    @DisplayName("RenderContext should invalidate compiled entries when live values replace them repeatedly")
    void testCompiledEntryInvalidationAcrossReplacements() {
        SlotKey<String> key = SlotKey.of("title");
        RenderContext context = RenderContext.empty();

        context.putCompiled(key, "<b>cached</b>");
        assertTrue(context.isCompiled(key));
        assertEquals("<b>cached</b>", context.getCompiled(key).orElse(""));

        context.put(key, "live-a");
        assertFalse(context.isCompiled(key));
        assertEquals("live-a", context.get(key).orElse(""));

        context.putCompiled(key, "<i>cached-b</i>");
        assertTrue(context.isCompiled(key));
        assertEquals("<i>cached-b</i>", context.getCompiled(key).orElse(""));

        context.put(key, "live-b");
        assertFalse(context.isCompiled(key));
        assertEquals("live-b", context.get(key).orElse(""));
    }

    @Test
    @DisplayName("RenderContext should reject null policy assignment")
    void testNullPolicyRejected() {
        RenderContext context = RenderContext.empty();
        assertThrows(NullPointerException.class, () -> context.withPolicy(null));
    }
}
