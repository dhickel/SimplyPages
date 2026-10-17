package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.components.forum.comments.DefaultForumCommentComponent;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.SlotKey;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultForumCommentComponentTest {
    private static final int[] DEPTHS = {0, 1, 2, 4, 5, 9, Integer.MIN_VALUE, Integer.MAX_VALUE};

    @Test
    void emitsRawDepthAndOnlyFiniteVisualBucket() {
        for (int depth : DEPTHS) {
            Element card = card(depth, null, null);
            int bucket = Math.max(0, Math.min(depth, 4));
            assertEquals(String.valueOf(depth), card.attr("data-depth"));
            assertEquals("forum-comment forum-comment-depth-" + bucket, card.attr("class"));
            assertFalse(card.hasAttr("style"), "depth must not introduce inline styling");
        }
    }

    @Test
    void parentMetadataIsIndependentAndBlankParentsRemainOmitted() {
        Element root = card(4, null, null);
        Element empty = card(4, "", null);
        Element blank = card(4, " \t\n", null);
        Element escaped = card(Integer.MIN_VALUE, "parent<&\"", null);

        assertFalse(root.hasAttr("data-parent-id"));
        assertFalse(empty.hasAttr("data-parent-id"));
        assertFalse(blank.hasAttr("data-parent-id"));
        assertEquals("parent<&\"", escaped.attr("data-parent-id"));
        assertEquals(String.valueOf(Integer.MIN_VALUE), escaped.attr("data-depth"));
    }

    @Test
    void preservesBodyActionsContextAndEscapingExactlyOnce() {
        RenderContext context = RenderContext.of(Map.of());
        Counting body = new Counting("body<&", context);
        Counting action = new Counting("action<&", context);
        var component = DefaultForumCommentComponent.create()
            .withCommentId("comment<&")
            .withTopicId("topic<&")
            .withParentId("parent<&")
            .withDepth(2)
            .withAuthor("author<&")
            .withTimestamp("time<&")
            .withBody(body)
            .withActions(List.of(action))
            .withLikes(3)
            .withReplies(4);

        String html = component.render(context);
        Element card = Jsoup.parseBodyFragment(html).selectFirst(".forum-comment");
        assertEquals(1, body.calls);
        assertEquals(1, action.calls);
        assertSame(context, body.context);
        assertSame(context, action.context);
        assertEquals("comment<&", card.attr("data-comment-id"));
        assertEquals("parent<&", card.attr("data-parent-id"));
        assertEquals("author<&", card.selectFirst(".forum-comment-author").text());
        assertEquals("time<&", card.selectFirst(".forum-comment-timestamp").text());
        assertEquals(1, card.select(".body").size());
        assertEquals(1, card.select(".action").size());
        assertTrue(html.contains("&lt;"));
    }

    @Test
    void directAndCompiledFreshRendersRemainEquivalentAndConfiguredStateStaysLive() {
        var component = DefaultForumCommentComponent.create()
            .withCommentId("c")
            .withTopicId("t")
            .withDepth(1)
            .withBody(new HtmlTag("p").withInnerText("body"));

        var template = component.compile();
        String compiledBefore = template.render(RenderContext.empty());
        String directBefore = component.render();
        assertEquals(directBefore, compiledBefore);
        assertEquals("forum-comment-depth-1", Jsoup.parseBodyFragment(directBefore)
            .selectFirst(".forum-comment").classNames().stream()
            .filter(name -> name.startsWith("forum-comment-depth-"))
            .findFirst().orElseThrow());

        component.withDepth(Integer.MAX_VALUE).withParentId("p");
        String compiledAfter = template.render(RenderContext.empty());
        String directAfter = component.render();
        assertEquals(directAfter, compiledAfter);
        Element after = Jsoup.parseBodyFragment(compiledAfter).selectFirst(".forum-comment");
        assertEquals(String.valueOf(Integer.MAX_VALUE), after.attr("data-depth"));
        assertTrue(after.hasClass("forum-comment-depth-4"));
        assertEquals("p", after.attr("data-parent-id"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 4, 5, 9, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void freshFirstBarrierRendersPreserveRawStateAndIndependentContexts(int depth) throws Exception {
        for (boolean compiled : List.of(false, true)) {
            SlotKey<String> value = SlotKey.of("request");
            var calls = new ConcurrentHashMap<RenderContext, List<String>>();
            Component body = context -> {
                calls.get(context).add("body");
                return new HtmlTag("p").withClass("body").withInnerText(value).render(context);
            };
            Component first = context -> {
                calls.computeIfAbsent(context, ignored -> new ArrayList<>()).add("first");
                return new HtmlTag("button").withInnerText(value).render(context);
            };
            Component second = context -> {
                calls.get(context).add("second");
                return new HtmlTag("button").withInnerText(value).render(context);
            };
            var suppliedActions = new ArrayList<>(List.of(first, second));
            var component = DefaultForumCommentComponent.create();
            component.withCommentId("c<&\"").withTopicId("t<&\"").withParentId("p<&\"")
                .withDepth(depth).withAuthor("author<&").withTimestamp("time<&")
                .withAvatarUrl("javascript:invalid").withBody(body).withActions(suppliedActions)
                .withLikes(-1).withReplies(0);
            suppliedActions.clear(); // The configured action list is an independent snapshot.
            Map<String, Object> before = rawState(component);
            assertEquals(depth, before.get("depth"));
            var template = compiled ? component.compile() : null;
            assertTrue(calls.isEmpty(), "compilation must not warm up the live delegate");
            assertEquals(before, rawState(component));
            int workers = 8;
            var barrier = new CyclicBarrier(workers);
            try (var pool = Executors.newFixedThreadPool(workers)) {
                var futures = new ArrayList<java.util.concurrent.Future<String>>();
                for (int index = 0; index < workers; index++) {
                    String text = "request-" + index + "<&\"";
                    futures.add(pool.submit(() -> {
                        RenderContext context = RenderContext.of(value, text);
                        barrier.await(10, TimeUnit.SECONDS);
                        String output = compiled ? template.render(context) : component.render(context);
                        Element card = Jsoup.parseBodyFragment(output).selectFirst(".forum-comment");
                        assertEquals(String.valueOf(depth), card.attr("data-depth"));
                        assertEquals(text, card.selectFirst(".body").text());
                        assertEquals(List.of(text, text), card.select("button").eachText());
                        assertEquals(List.of("first", "second", "body"), calls.get(context));
                        assertEquals(text, context.get(value).orElseThrow());
                        return output;
                    }));
                }
                for (var future : futures) assertTrue(future.get(15, TimeUnit.SECONDS).contains("&lt;"));
            }
            assertEquals(workers, calls.size());
            assertEquals(before, rawState(component));
        }
    }

    @Test
    void avatarAndNullableFieldsAndCountersRemainIndependent() throws Exception {
        for (String avatar : new String[]{null, "", " \t\n", "javascript:alert(1)", "https://example.com/avatar.png"}) {
            for (Integer likes : new Integer[]{null, 0, -1, Integer.MAX_VALUE}) {
                for (Integer replies : new Integer[]{null, 0, -2, Integer.MAX_VALUE}) {
                    var component = DefaultForumCommentComponent.create();
                    component.withDepth(Integer.MIN_VALUE).withAvatarUrl(avatar).withLikes(likes).withReplies(replies)
                        .withActions(null);
                    var before = rawState(component);
                    var template = component.compile();
                    String output = template.render(RenderContext.empty());
                    assertEquals(output, component.render());
                    Element card = Jsoup.parseBodyFragment(output).selectFirst(".forum-comment");
                    assertEquals("", card.attr("data-comment-id"));
                    assertEquals("", card.attr("data-topic-id"));
                    assertEquals("", card.selectFirst(".forum-comment-author").text());
                    assertEquals("", card.selectFirst(".forum-comment-timestamp").text());
                    assertEquals(0, card.select(".forum-comment-actions").size());
                    assertEquals("width: 150px; height: 150px;", card.selectFirst(".forum-comment-avatar-slot").attr("style"));
                    assertEquals(avatar != null && avatar.startsWith("https:") ? 1 : 0,
                        card.select("img.forum-comment-avatar-image[width=150][height=150]").size());
                    assertEquals(likes == null && replies == null ? 0 : 1, card.select(".forum-comment-footer").size());
                    assertEquals(likes == null ? List.of() : List.of(likes + " likes"), card.select(".forum-comment-likes").eachText());
                    assertEquals(replies == null ? List.of() : List.of(replies + " replies"), card.select(".forum-comment-replies").eachText());
                    assertEquals(before, rawState(component));
                }
            }
        }
    }

    @Test
    void customSubclassRenderingRemainsLiveWithSameContextAfterCompilation() {
        AtomicInteger calls = new AtomicInteger();
        SlotKey<String> key = SlotKey.of("custom");
        var custom = new DefaultForumCommentComponent() {
            @Override
            public String render(RenderContext context) {
                calls.incrementAndGet();
                return new HtmlTag("aside").withInnerText(key).render(context);
            }
        };
        var template = custom.compile();
        assertEquals(0, calls.get());
        for (String text : List.of("first<&", "second<&")) {
            var context = RenderContext.of(key, text);
            assertEquals(custom.render(context), template.render(context));
        }
        assertEquals(4, calls.get());
    }

    private static Map<String, Object> rawState(DefaultForumCommentComponent component) throws Exception {
        var state = new LinkedHashMap<String, Object>();
        for (var field : DefaultForumCommentComponent.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            Object value = field.get(component);
            state.put(field.getName(), value instanceof List<?> list ? new ArrayList<>(list) : value);
        }
        return state;
    }

    private Element card(int depth, String parent, Component body) {
        var component = DefaultForumCommentComponent.create()
            .withCommentId("c")
            .withTopicId("t")
            .withParentId(parent)
            .withDepth(depth)
            .withBody(body);
        return Jsoup.parseBodyFragment(component.render()).selectFirst(".forum-comment");
    }

    private static final class Counting implements Component {
        private final String text;
        private final RenderContext expected;
        private int calls;
        private RenderContext context;

        private Counting(String text, RenderContext expected) {
            this.text = text;
            this.expected = expected;
        }

        @Override
        public String render(RenderContext context) {
            calls++;
            this.context = context;
            assertSame(expected, context);
            String css = text.startsWith("body") ? "body" : "action";
            return new HtmlTag("span").withClass(css).withInnerText(text).render(context);
        }
    }
}
