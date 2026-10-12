package io.mindspice.simplypages.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotKeyTest {

    @Test
    @DisplayName("SlotKey canonical constructor rejects null names immediately")
    void canonicalConstructorRejectsNullName() {
        assertThrows(NullPointerException.class, () -> new SlotKey<String>(null, null));
    }

    @Test
    @DisplayName("Every SlotKey factory rejects null names at its construction boundary")
    void factoriesRejectNullNamesAndDoNotInvokeProviders() {
        AtomicInteger providerCalls = new AtomicInteger();
        Function<RenderContext, String> provider = context -> {
            providerCalls.incrementAndGet();
            return "default";
        };

        SlotKey<String> noDefault = SlotKey.of("no-default");
        SlotKey<String> constant = SlotKey.of("constant", "value");
        SlotKey<String> function = SlotKey.of("function", provider);
        SlotKey<String> nullProvider = SlotKey.of("null-provider", (Function<RenderContext, String>) null);
        SlotKey<String> nullConstant = SlotKey.of("null-constant", (String) null);

        assertNull(noDefault.defaultProvider());
        assertEquals("value", constant.getDefault(RenderContext.empty()));
        assertSame(provider, function.defaultProvider());
        assertNull(nullProvider.defaultProvider());
        assertNotNull(nullConstant.defaultProvider());
        assertNull(nullConstant.getDefault(RenderContext.empty()));
        assertEquals(0, providerCalls.get());

        assertThrows(NullPointerException.class, () -> new SlotKey<>(null, provider));
        assertThrows(NullPointerException.class, () -> SlotKey.<String>of(null));
        assertThrows(NullPointerException.class, () -> SlotKey.<String>of(null, "value"));
        assertThrows(NullPointerException.class, () -> SlotKey.<String>of(null, provider));
        assertThrows(NullPointerException.class,
                () -> SlotKey.<String>of(null, (Function<RenderContext, String>) null));
        assertThrows(NullPointerException.class, () -> SlotKey.<String>of(null, (String) null));
        assertEquals(0, providerCalls.get());

        assertEquals("default", function.getDefault(RenderContext.empty()));
        assertEquals(1, providerCalls.get());
    }

    @Test
    @DisplayName("SlotKey keeps its public record components and callable canonical constructor")
    void publicRecordShapeAndConstructorRemainAvailable() throws Exception {
        RecordComponent[] components = SlotKey.class.getRecordComponents();
        assertArrayEquals(new String[] {"name", "defaultProvider"},
                new String[] {components[0].getName(), components[1].getName()});

        Constructor<?> constructor = SlotKey.class.getConstructor(String.class, Function.class);
        assertTrue(Modifier.isPublic(constructor.getModifiers()));
        Function<RenderContext, String> provider = context -> "value";
        SlotKey<?> key = (SlotKey<?>) constructor.newInstance("constructor-name", provider);

        assertEquals("constructor-name", key.name());
        assertSame(provider, key.defaultProvider());
    }

    @Test
    @DisplayName("Non-null names remain literal, usable, and free of normalization policy")
    void unusualNonNullNamesRemainUnchanged() {
        String[] names = {
            "", " ", "\t", "\n", "\u0000", " leading ", "trailing ",
            "BMP-é", "non-BMP-😀", "composed-é", "decomposed-e\u0301",
            "isolated-high-\uD800", "isolated-low-\uDC00", "MiXeD"
        };

        for (String name : names) {
            String sourceName = new String(name);
            String defaultValue = "default-" + name;
            String liveValue = "<&>";
            Function<RenderContext, String> provider = context -> "provider-default";
            SlotKey<String> noDefault = SlotKey.of(sourceName);
            SlotKey<String> key = SlotKey.of(sourceName, defaultValue);
            SlotKey<String> functionKey = SlotKey.of(sourceName, provider);
            SlotKey<String> sameName = new SlotKey<>(new String(name), context -> "other-default");

            assertSame(sourceName, noDefault.name());
            assertSame(sourceName, key.name());
            assertSame(sourceName, functionKey.name());
            assertSame(provider, functionKey.defaultProvider());
            assertEquals(name, key.name());
            assertEquals("SlotKey{" + name + "}", key.toString());
            assertNull(noDefault.defaultProvider());
            assertSame(defaultValue, key.getDefault(RenderContext.empty()));
            assertEquals(key, sameName);
            assertEquals(key.hashCode(), sameName.hashCode());
            assertNotEquals(key, SlotKey.of(name + "x", defaultValue));

            Map<SlotKey<?>, Object> hashValues = new HashMap<>();
            hashValues.put(key, liveValue);
            assertSame(liveValue, hashValues.get(sameName));

            RenderContext defaults = RenderContext.empty();
            assertSame(defaultValue, defaults.get(key).orElseThrow());

            RenderContext context = RenderContext.of(key, liveValue);
            assertSame(liveValue, context.get(sameName).orElseThrow());
            assertEquals("&lt;&amp;&gt;", Slot.of(sameName).render(context));

            HtmlTag root = new HtmlTag("div")
                    .withInnerText(sameName)
                    .withChild(Slot.of(sameName));
            Template template = Template.of(root);
            String expected = "<div>&lt;&amp;&gt;&lt;&amp;&gt;</div>";
            assertEquals(expected, template.render(context));
            assertEquals(expected, root.render(context));

            SlotKeyMap map = SlotKeyMap.fromRenderContext(context);
            assertEquals(liveValue, map.getValue(name, String.class).orElseThrow());
            RenderContext restored = map.toRenderContext(Map.of(name, sameName));
            assertSame(liveValue, restored.get(sameName).orElseThrow());
            assertEquals("&lt;&amp;&gt;", Slot.of(sameName).render(restored));
        }

        assertNotEquals(SlotKey.of("case"), SlotKey.of("Case"));
        assertNotEquals(SlotKey.of("e\u0301"), SlotKey.of("é"));
        assertNotEquals(SlotKey.of("left "), SlotKey.of(" left"));
    }

    @Test
    @DisplayName("SlotKey equality remains name-only across types, defaults, and providers")
    void equalityAndHashCodeIgnoreTypeAndDefaultProvider() {
        SlotKey<String> stringKey = SlotKey.of("shared", "text");
        SlotKey<Integer> integerKey = SlotKey.of("shared", 42);
        SlotKey<Boolean> providerKey = SlotKey.of("shared", context -> true);

        assertEquals(stringKey, integerKey);
        assertEquals(integerKey, providerKey);
        assertEquals(stringKey.hashCode(), integerKey.hashCode());
        assertEquals("text", stringKey.getDefault(RenderContext.empty()));
        assertEquals(42, integerKey.getDefault(RenderContext.empty()));
        assertTrue(providerKey.getDefault(RenderContext.empty()));
    }

    @Test
    @DisplayName("SlotKey aliases work through direct slots, text slots, and fresh templates")
    void aliasesWorkAcrossDirectAndCompiledRendering() {
        SlotKey<String> source = SlotKey.of("render-value");
        SlotKey<String> alias = SlotKey.of(new String("render-value"));
        RenderContext context = RenderContext.of(source, "<&>");

        HtmlTag root = new HtmlTag("div")
                .withInnerText(alias)
                .withChild(Slot.of(alias));
        Template template = Template.of(root);

        assertEquals("<div>&lt;&amp;&gt;&lt;&amp;&gt;</div>", template.render(context));
        assertEquals("&lt;&amp;&gt;", Slot.of(alias).render(context));
        assertEquals(template.render(context), root.render(context));

        context.withPolicy(RenderContext.RenderPolicy.NEVER_COMPILE).put(source, "<next>");
        assertEquals("<div>&lt;next&gt;&lt;next&gt;</div>", template.render(context));
        assertEquals("<div>&lt;next&gt;&lt;next&gt;</div>", root.render(context));
    }

    @Test
    @DisplayName("Defaults stay live, retain identity, and remain separate from compiled entries")
    void defaultsAndCompiledEntriesKeepExistingContextBehavior() {
        AtomicInteger defaultCalls = new AtomicInteger();
        Object defaultValue = new Object();
        SlotKey<Object> key = SlotKey.of("default-value", context -> {
            defaultCalls.incrementAndGet();
            return defaultValue;
        });
        RenderContext context = RenderContext.empty()
                .withPolicy(RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT);

        assertSame(defaultValue, context.get(key).orElseThrow());
        assertFalse(context.isCompiled(key));
        assertEquals(1, defaultCalls.get());

        assertEquals(defaultValue.toString(), Slot.of(key).render(context));
        assertFalse(context.isCompiled(key));
        assertEquals(2, defaultCalls.get());

        context.put(key, "<live>");
        assertEquals("&lt;live&gt;", Slot.of(key).render(context));
        assertTrue(context.isCompiled(key));
        assertEquals("&lt;live&gt;", context.getCompiled(key).orElseThrow());
        assertSame(defaultValue, context.get(key).orElseThrow());

        context.put(key, null);
        assertFalse(context.isCompiled(key));
        assertSame(defaultValue, context.get(key).orElseThrow());

        context.putCompiled(key, "<b>trusted</b>");
        assertEquals("<b>trusted</b>", Slot.of(key).render(context));
        assertSame(defaultValue, context.get(key).orElseThrow());
        assertTrue(defaultCalls.get() >= 3);
    }

    @Test
    @DisplayName("SlotKey defaults and null values preserve direct and template composition")
    void nullValuesAndTemplateCompositionRemainSeparateFromNameValidation() {
        SlotKey<String> valueKey = SlotKey.of("value", "fallback");
        HtmlTag root = new HtmlTag("section")
                .withChild(Slot.of(valueKey))
                .withInnerText(valueKey);
        Template template = Template.of(root);
        RenderContext context = RenderContext.of(SlotKey.of("value"), "provided");

        assertEquals("<section>providedprovided</section>", template.render(context));
        context.put(valueKey, null);
        assertEquals("<section>fallbackfallback</section>", template.render(context));

        context.putCompiled(valueKey, "<i>cached</i>");
        assertEquals("<section><i>cached</i><i>cached</i></section>", template.render(context));
        context.remove(valueKey);
        assertEquals("fallback", Slot.of(valueKey).render(context));
    }

    @Test
    @DisplayName("SlotKey aliases retain the legacy string bridge behavior")
    void legacyStringBridgeUsesLiteralNames() {
        SlotKey<String> source = SlotKey.of("legacy-name", "fallback");
        RenderContext original = RenderContext.of(source, "value");
        SlotKeyMap map = SlotKeyMap.fromRenderContext(original);

        assertEquals("value", map.getValue("legacy-name", String.class).orElseThrow());

        SlotKey<String> alias = SlotKey.of(new String("legacy-name"));
        RenderContext restored = map.toRenderContext(Map.of("legacy-name", alias));
        assertEquals("value", restored.get(alias).orElseThrow());
        assertEquals("value", Slot.of(alias).render(restored));
    }

    @Test
    @DisplayName("SlotKey equality should be based on name")
    void testSlotKeyEquality() {
        SlotKey<String> keyA1 = SlotKey.of("alpha");
        SlotKey<String> keyA2 = SlotKey.of("alpha");
        SlotKey<String> keyB = SlotKey.of("beta");

        assertEquals(keyA1, keyA2);
        assertNotEquals(keyA1, keyB);
        assertEquals(keyA1.hashCode(), keyA2.hashCode());
    }

    @Test
    @DisplayName("SlotKey should expose name and defaults")
    void testSlotKeyNameAndDefaults() {
        SlotKey<String> key = SlotKey.of("title", "fallback");

        assertEquals("title", key.name());
        assertEquals("fallback", key.getDefault(RenderContext.empty()));
        assertTrue(key.toString().contains("title"));
    }

    @Test
    @DisplayName("SlotKey should support dynamic defaults")
    void testSlotKeyDynamicDefault() {
        SlotKey<String> source = SlotKey.of("source");
        SlotKey<String> derived = SlotKey.of("derived", ctx -> ctx.get(source).orElse("none"));
        RenderContext context = RenderContext.builder().with(source, "value").build();

        assertEquals("value", derived.getDefault(context));
    }
}
