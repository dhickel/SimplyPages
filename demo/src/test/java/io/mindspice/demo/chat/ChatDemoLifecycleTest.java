package io.mindspice.demo.chat;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/** Tests the real fixed production profile; reflection is confined to Spring's transport boundary. */
class ChatDemoLifecycleTest {
    private static final Duration IDLE = Duration.ofMinutes(30);
    private static final Duration STREAM = Duration.ofMinutes(5);

    @Test
    void productionProfileAndFiniteEmitterAreExact() {
        assertEquals(32, ChatDemoService.MAX_CONVERSATIONS);
        assertEquals(2, ChatDemoService.MAX_SESSION_STREAMS);
        assertEquals(64, ChatDemoService.MAX_STREAMS);
        assertEquals(4096, ChatDemoService.MAX_MESSAGE_UNITS);
        assertEquals(IDLE.toMillis(), ChatDemoService.IDLE_MILLIS);
        assertEquals(STREAM.toMillis(), ChatDemoService.STREAM_MILLIS);
        try (ChatDemoService service = service(new MutableClock())) {
            MockHttpSession session = new MockHttpSession();
            String id = service.initialize(session, false).conversationId();
            assertEquals(STREAM.toMillis(), service.stream(session, id).getTimeout());
            assertEquals(1, service.conversationCount());
            assertEquals(1, service.streamCount());
        }
    }

    @Test
    void lookupOnlyOperationsNeverAllocateAndRequireExactOwnership() {
        try (ChatDemoService service = service(new MutableClock())) {
            MockHttpSession owner = new MockHttpSession();
            MockHttpSession stranger = new MockHttpSession();
            String id = service.initialize(owner, false).conversationId();
            ChatDemoService.Snapshot original = service.history(owner, id);
            for (HttpSession session : new HttpSession[]{null, stranger, owner}) {
                for (String invalid : new String[]{null, "", " \t\n"}) {
                    failure(400, () -> service.history(session, invalid));
                    failure(400, () -> service.appendExchange(session, invalid, "text", "Name"));
                    failure(400, () -> service.stream(session, invalid));
                }
            }
            for (String foreign : new String[]{id, "unknown", " " + id, id + " ", id.toUpperCase()}) {
                failure(404, () -> service.history(stranger, foreign));
                failure(404, () -> service.appendExchange(stranger, foreign, "text", "Name"));
                failure(404, () -> service.stream(stranger, foreign));
            }
            for (String altered : new String[]{"unknown", " " + id, id + " ", id.toUpperCase()}) {
                failure(404, () -> service.history(owner, altered));
                failure(404, () -> service.appendExchange(owner, altered, "text", "Name"));
                failure(404, () -> service.stream(owner, altered));
            }
            failure(404, () -> service.history(null, id));
            failure(404, () -> service.appendExchange(null, id, "text", "Name"));
            failure(404, () -> service.stream(null, id));
            assertNull(stranger.getAttribute(ChatDemoService.SESSION_CONVERSATION));
            assertEquals(original, service.history(owner, id));
            assertEquals(1, service.conversationCount());
            assertEquals(0, service.streamCount());
        }
    }

    @Test
    void invalidatedSessionCannotInitializeOrAccessAndDoesNotAllocate() {
        try (ChatDemoService service = service(new MutableClock())) {
            MockHttpSession session = new MockHttpSession();
            session.invalidate();
            failure(404, () -> service.initialize(session, false));
            failure(404, () -> service.initialize(session, true));
            failure(404, () -> service.history(session, "unknown"));
            failure(404, () -> service.appendExchange(session, "unknown", "text", "Name"));
            failure(404, () -> service.stream(session, "unknown"));
            assertEquals(0, service.conversationCount());
            assertEquals(0, service.streamCount());
        }
    }

    @Test
    void concurrentFirstInitializationPublishesExactlyOneConversation() throws Exception {
        try (ChatDemoService service = service(new MutableClock())) {
            MockHttpSession session = new MockHttpSession();
            List<ChatDemoService.Snapshot> snapshots = race(64, i -> service.initialize(session, false));
            assertEquals(1, snapshots.stream().map(ChatDemoService.Snapshot::conversationId).distinct().count());
            assertEquals(snapshots.getFirst().conversationId(), session.getAttribute(ChatDemoService.SESSION_CONVERSATION));
            for (ChatDemoService.Snapshot snapshot : snapshots) {
                assertEquals(1, snapshot.messages().size());
                assertEquals(1, snapshot.cursor());
                assertEquals("assistant", snapshot.messages().getFirst().role());
            }
            assertEquals(1, service.conversationCount());
        }
    }

    @Test
    void snapshotIsImmutableWelcomeAndLatestTwentyFiveWholePairsRetainMonotonicCursor() {
        try (ChatDemoService service = service(new MutableClock())) {
            MockHttpSession session = new MockHttpSession();
            ChatDemoService.Snapshot first = service.initialize(session, false);
            String id = first.conversationId();
            String welcomeId = first.messages().getFirst().id();
            assertThrows(UnsupportedOperationException.class, () -> first.messages().clear());
            ChatDemoService.Snapshot early = service.appendExchange(session, id, "exchange-1", "Name");
            for (int i = 2; i <= 40; i++) service.appendExchange(session, id, "exchange-" + i, "Name");
            ChatDemoService.Snapshot last = service.history(session, id);
            assertEquals(1, first.messages().size());
            assertEquals(3, early.messages().size());
            assertEquals("exchange-1", early.messages().get(1).body());
            assertEquals(51, last.messages().size());
            assertEquals(81, last.cursor());
            assertEquals(welcomeId, last.messages().getFirst().id());
            assertEquals(51, new HashSet<>(last.messages().stream().map(ChatDemoService.ChatMessageView::id).toList()).size());
            for (int pair = 0; pair < 25; pair++) {
                var user = last.messages().get(1 + pair * 2);
                var assistant = last.messages().get(2 + pair * 2);
                assertEquals("user", user.role());
                assertEquals("assistant", assistant.role());
                assertEquals("exchange-" + (16 + pair), user.body());
                assertEquals("Received: " + user.body(), assistant.body());
            }
            assertThrows(UnsupportedOperationException.class, () -> last.messages().add(first.messages().getFirst()));
        }
    }

    @Test
    void concurrentExchangesRemainAtomicWholePairsWithNoLostCursor() throws Exception {
        try (ChatDemoService service = service(new MutableClock())) {
            MockHttpSession session = new MockHttpSession();
            String id = service.initialize(session, false).conversationId();
            List<ChatDemoService.Snapshot> results = race(40,
                i -> service.appendExchange(session, id, "concurrent-" + i, "Name"));
            assertEquals(40, results.stream().map(ChatDemoService.Snapshot::cursor).distinct().count());
            assertEquals(81, service.history(session, id).cursor());
            for (ChatDemoService.Snapshot snapshot : results) {
                assertTrue(snapshot.messages().size() <= 51);
                assertEquals(1, snapshot.messages().size() % 2);
                for (int i = 1; i < snapshot.messages().size(); i += 2) {
                    assertEquals("user", snapshot.messages().get(i).role());
                    assertEquals("assistant", snapshot.messages().get(i + 1).role());
                    assertEquals("Received: " + snapshot.messages().get(i).body(), snapshot.messages().get(i + 1).body());
                }
            }
        }
    }

    @Test
    void utf16BoundPrecedesTrimAndNamesHaveExactFallbackBoundary() {
        try (ChatDemoService service = service(new MutableClock())) {
            MockHttpSession session = new MockHttpSession();
            String id = service.initialize(session, false).conversationId();
            String astral = "\uD83D\uDE00".repeat(2048);
            var accepted = service.appendExchange(session, id, astral, "n".repeat(64));
            assertEquals(4096, accepted.messages().get(1).body().length());
            assertEquals("n".repeat(64), accepted.messages().get(1).author());
            for (String invalid : new String[]{null, "", " \t\n", astral + "x", " " + "x".repeat(4096), " ".repeat(4097)}) {
                failure(400, () -> service.appendExchange(session, id, invalid, "Name"));
                assertEquals(accepted, service.history(session, id));
            }
            for (String name : new String[]{null, "", " \t", "n".repeat(65)}) {
                var result = service.appendExchange(session, id, "  trimmed  ", name);
                var user = result.messages().get(result.messages().size() - 2);
                assertEquals("trimmed", user.body());
                assertEquals("Demo User", user.author());
            }
            var result = service.appendExchange(session, id, "x", " Name ");
            assertEquals("Name", result.messages().get(result.messages().size() - 2).author());
        }
    }

    enum Touch { PAGE, HISTORY, MESSAGE }

    @ParameterizedTest
    @EnumSource(Touch.class)
    void successfulOwnedActivityRefreshesIdleLifetime(Touch touch) {
        MutableClock clock = new MutableClock();
        try (ChatDemoService service = service(clock)) {
            MockHttpSession session = new MockHttpSession();
            String id = service.initialize(session, false).conversationId();
            clock.advance(IDLE.minusMillis(1));
            switch (touch) {
                case PAGE -> assertEquals(id, service.initialize(session, false).conversationId());
                case HISTORY -> service.history(session, id);
                case MESSAGE -> service.appendExchange(session, id, "text", "Name");
            }
            clock.advance(IDLE.minusMillis(1));
            service.sweep();
            assertEquals(1, service.conversationCount());
            clock.advance(Duration.ofMillis(1));
            service.sweep();
            assertEquals(0, service.conversationCount());
            failure(410, () -> service.history(session, id));
        }
    }

    @Test
    void failuresAndStreamReconnectDoNotRefreshIdleLifetime() {
        MutableClock clock = new MutableClock();
        try (ChatDemoService service = service(clock)) {
            MockHttpSession owner = new MockHttpSession();
            MockHttpSession stranger = new MockHttpSession();
            String id = service.initialize(owner, false).conversationId();
            clock.advance(IDLE.minusMillis(1));
            failure(400, () -> service.appendExchange(owner, id, " ", "Name"));
            failure(404, () -> service.history(stranger, id));
            failure(404, () -> service.appendExchange(stranger, id, "text", "Name"));
            failure(404, () -> service.initialize(strangerWithForeignAttribute(stranger, id), false));
            service.stream(owner, id);
            service.stream(owner, id);
            failure(429, () -> service.stream(owner, id));
            assertEquals(2, service.streamCount());
            clock.advance(Duration.ofMillis(1));
            failure(410, () -> service.history(owner, id));
            assertEquals(0, service.conversationCount());
            assertEquals(0, service.streamCount());
        }
    }

    @Test
    void expiryIsInclusiveAndNormalPageDoesNotSilentlyAllocateReplacement() {
        MutableClock clock = new MutableClock();
        try (ChatDemoService service = service(clock)) {
            MockHttpSession session = new MockHttpSession();
            String id = service.initialize(session, false).conversationId();
            clock.advance(IDLE.minusMillis(1));
            service.sweep();
            assertEquals(1, service.conversationCount());
            clock.advance(Duration.ofMillis(1));
            failure(410, () -> service.initialize(session, false));
            failure(410, () -> service.history(session, id));
            failure(410, () -> service.appendExchange(session, id, "text", "Name"));
            failure(410, () -> service.stream(session, id));
            assertEquals(id, session.getAttribute(ChatDemoService.SESSION_CONVERSATION));
            assertEquals(0, service.conversationCount());
            String replacement = service.initialize(session, true).conversationId();
            assertNotEquals(id, replacement);
            assertEquals(1, service.conversationCount());
            failure(404, () -> service.history(session, id));
        }
    }

    @Test
    void ownedExpiryHasTruthfulHttp410WithoutImplicitAllocationAndExplicitNewUsesGeneratedMarkup() throws Exception {
        MutableClock clock = new MutableClock();
        try (ChatDemoService service = service(clock)) {
            var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new io.mindspice.demo.ChatDemoController(service, new io.mindspice.demo.pages.ChatDemoPage())).build();
            MockHttpSession session = new MockHttpSession();
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/chat").session(session))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
            String id = (String) session.getAttribute(ChatDemoService.SESSION_CONVERSATION);
            clock.advance(IDLE);
            for (String endpoint : List.of("history", "stream", "messages")) {
                var request = endpoint.equals("messages")
                    ? org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/chat/messages").param("message", "expired draft")
                    : org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/chat/" + endpoint);
                mvc.perform(request.session(session).param("conversationId", id))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isGone())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control", "no-store"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncNotStarted());
            }
            var page = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/chat").session(session))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isGone()).andReturn();
            assertTrue(page.getResponse().getContentAsString().contains("data-chat-expired=\"true\""));
            assertTrue(page.getResponse().getContentAsString().contains("id=\"chat-new\""));
            assertEquals(id, session.getAttribute(ChatDemoService.SESSION_CONVERSATION));
            assertEquals(0, service.conversationCount());
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/chat/new").session(session))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
            assertNotEquals(id, session.getAttribute(ChatDemoService.SESSION_CONVERSATION));
            assertEquals(1, service.conversationCount());
        }
    }

    @Test
    void capacityRejectsWithoutEvictionThenReclaimsOnlyExpiredOwnersBeforeAdmission() {
        MutableClock clock = new MutableClock();
        try (ChatDemoService service = service(clock)) {
            List<MockHttpSession> sessions = sessions(32);
            List<String> ids = sessions.stream().map(s -> service.initialize(s, false).conversationId()).toList();
            service.stream(sessions.getFirst(), ids.getFirst());
            MockHttpSession rejected = new MockHttpSession();
            failure(503, () -> service.initialize(rejected, false));
            assertNull(rejected.getAttribute(ChatDemoService.SESSION_CONVERSATION));
            for (int i = 0; i < 32; i++) assertEquals(ids.get(i), service.history(sessions.get(i), ids.get(i)).conversationId());
            clock.advance(Duration.ofMinutes(1));
            for (int i = 1; i < 32; i++) service.history(sessions.get(i), ids.get(i));
            clock.advance(Duration.ofMinutes(29));
            service.initialize(rejected, false);
            assertEquals(32, service.conversationCount());
            assertEquals(0, service.streamCount());
            failure(410, () -> service.history(sessions.getFirst(), ids.getFirst()));
            MockHttpSession stillRejected = new MockHttpSession();
            failure(503, () -> service.initialize(stillRejected, true));
            assertNull(stillRejected.getAttribute(ChatDemoService.SESSION_CONVERSATION));
            for (int i = 1; i < 32; i++) assertEquals(ids.get(i), service.history(sessions.get(i), ids.get(i)).conversationId());
        }
    }

    @Test
    void explicitNewCannotEvictAnotherOwnerEvenWithForeignConversationAttribute() {
        try (ChatDemoService service = service(new MutableClock())) {
            List<MockHttpSession> sessions = sessions(32);
            List<String> ids = sessions.stream().map(s -> service.initialize(s, false).conversationId()).toList();
            MockHttpSession stranger = new MockHttpSession();
            stranger.setAttribute(ChatDemoService.SESSION_CONVERSATION, ids.getFirst());
            failure(404, () -> service.initialize(stranger, false));
            failure(404, () -> service.initialize(stranger, true));
            assertEquals(32, service.conversationCount());
            for (int i = 0; i < 32; i++) assertEquals(ids.get(i), service.history(sessions.get(i), ids.get(i)).conversationId());
            assertEquals(0, service.streamCount());
        }
    }

    @Test
    void simultaneousAdmissionNeverExceedsThirtyTwoAndRejectedSessionsHaveNoState() throws Exception {
        try (ChatDemoService service = service(new MutableClock())) {
            List<MockHttpSession> sessions = sessions(96);
            List<Integer> statuses = race(96, i -> {
                try { service.initialize(sessions.get(i), false); return 200; }
                catch (ChatDemoService.ChatFailure failure) { return failure.status(); }
            });
            assertEquals(32, statuses.stream().filter(s -> s == 200).count());
            assertEquals(64, statuses.stream().filter(s -> s == 503).count());
            assertEquals(32, service.conversationCount());
            for (int i = 0; i < sessions.size(); i++) {
                Object id = sessions.get(i).getAttribute(ChatDemoService.SESSION_CONVERSATION);
                if (statuses.get(i) == 503) assertNull(id);
                else assertEquals(id, service.history(sessions.get(i), (String) id).conversationId());
            }
            assertEquals(0, service.streamCount());
        }
    }

    @Test
    void simultaneousStreamAdmissionHonorsTwoPerOwnerAndSixtyFourTotal() throws Exception {
        try (ChatDemoService service = service(new MutableClock())) {
            List<MockHttpSession> sessions = sessions(32);
            List<String> ids = sessions.stream().map(s -> service.initialize(s, false).conversationId()).toList();
            List<Integer> statuses = race(128, i -> {
                int owner = i % 32;
                try { service.stream(sessions.get(owner), ids.get(owner)); return 200; }
                catch (ChatDemoService.ChatFailure failure) { return failure.status(); }
            });
            assertEquals(64, statuses.stream().filter(s -> s == 200).count());
            assertEquals(64, statuses.stream().filter(s -> s == 429 || s == 503).count());
            assertEquals(64, service.streamCount());
            for (int owner = 0; owner < 32; owner++) {
                int successful = 0;
                for (int i = owner; i < 128; i += 32) if (statuses.get(i) == 200) successful++;
                assertEquals(2, successful);
                int index = owner;
                failure(503, () -> service.stream(sessions.get(index), ids.get(index)));
            }
            failure(503, () -> service.initialize(new MockHttpSession(), false));
            assertEquals(64, service.streamCount());
        }
    }

    @Test
    void actualFullRegistryReturnsHttp503BeforeAllocationAndOwnershipStillWins() throws Exception {
        try (ChatDemoService service = service(new MutableClock())) {
            List<MockHttpSession> sessions = sessions(32);
            List<String> ids = sessions.stream().map(session -> service.initialize(session, false).conversationId()).toList();
            var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new io.mindspice.demo.ChatDemoController(service, new io.mindspice.demo.pages.ChatDemoPage())).build();
            Transport first = new Transport(service, service.stream(sessions.getFirst(), ids.getFirst()), false);
            for (int i = 0; i < 32; i++) {
                if (i != 0) service.stream(sessions.get(i), ids.get(i));
                service.stream(sessions.get(i), ids.get(i));
            }
            assertEquals(64, service.streamCount());
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/chat/stream")
                    .session(sessions.getFirst()).param("conversationId", ids.getFirst()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isServiceUnavailable())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncNotStarted())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control", "no-store"));
            assertEquals(64, service.streamCount());
            failure(404, () -> service.stream(sessions.get(1), ids.getFirst()));
            first.callback(Callback.TIMEOUT);
            assertEquals(63, service.streamCount());
            failure(429, () -> service.stream(sessions.get(1), ids.get(1)));
            service.stream(sessions.getFirst(), ids.getFirst());
            assertEquals(64, service.streamCount());
        }
    }

    @Test
    void finiteStreamDeadlineReleasesSlotsWithoutExpiringOrTouchingConversation() throws Exception {
        MutableClock clock = new MutableClock();
        try (ChatDemoService service = service(clock)) {
            MockHttpSession session = new MockHttpSession();
            String id = service.initialize(session, false).conversationId();
            Transport transport = new Transport(service, service.stream(session, id), false);
            assertTrue(transport.events().contains("chat-updated"));
            assertTrue(transport.events().contains("id:1"));
            clock.advance(STREAM.minusMillis(1));
            service.sweep();
            assertEquals(1, service.streamCount());
            clock.advance(Duration.ofMillis(1));
            service.sweep();
            assertEquals(0, service.streamCount());
            assertEquals(1, service.conversationCount());
            assertEquals(1, transport.completions.get());
            assertTrue(transport.events().contains("chat-ended"));
            assertTrue(transport.events().contains("ended"));
            Transport replacement = new Transport(service, service.stream(session, id), false);
            transport.callbacksRepeatedly();
            assertEquals(1, service.streamCount());
            clock.advance(IDLE.minus(STREAM));
            service.sweep();
            assertEquals(0, service.conversationCount());
            assertEquals(0, service.streamCount());
            assertEquals(1, replacement.completions.get());
            assertTrue(replacement.events().contains("expired"));
            failure(410, () -> service.history(session, id));
        }
    }

    enum Callback { COMPLETION, TIMEOUT, ERROR }

    @ParameterizedTest
    @EnumSource(Callback.class)
    void everyCallbackDetachesIdempotentlyAndOldCallbacksCannotRemoveReplacement(Callback callback) throws Exception {
        try (ChatDemoService service = service(new MutableClock())) {
            MockHttpSession session = new MockHttpSession();
            String id = service.initialize(session, false).conversationId();
            Transport old = new Transport(service, service.stream(session, id), false);
            old.callback(callback);
            assertEquals(0, service.streamCount());
            Transport current = new Transport(service, service.stream(session, id), false);
            old.callbacksRepeatedly();
            assertEquals(1, service.streamCount());
            service.appendExchange(session, id, "after replacement", "Name");
            assertTrue(current.events().contains("id:3"));
            assertFalse(old.events().contains("id:3"));
            current.callbacksRepeatedly();
            assertEquals(0, service.streamCount());
            assertEquals(1, service.conversationCount());
        }
    }

    @Test
    void initialTransportFailureInvokesPreconfiguredErrorCallbackAndReleasesSlot() throws Exception {
        try (ChatDemoService service = service(new MutableClock())) {
            MockHttpSession session = new MockHttpSession();
            String id = service.initialize(session, false).conversationId();
            SseEmitter emitter = service.stream(session, id);
            assertEquals(1, service.streamCount());
            Transport failed = new Transport(service, emitter, true);
            assertEquals(0, service.streamCount());
            failed.callbacksRepeatedly();
            service.stream(session, id);
            service.stream(session, id);
            assertEquals(2, service.streamCount());
        }
    }

    @Test
    void failedUpdateReleasesExactlyOnceAndDoesNotFailCommittedExchangeOrOtherRecipient() throws Exception {
        try (ChatDemoService service = service(new MutableClock())) {
            MockHttpSession session = new MockHttpSession();
            String id = service.initialize(session, false).conversationId();
            Transport failed = new Transport(service, service.stream(session, id), false);
            Transport healthy = new Transport(service, service.stream(session, id), false);
            failed.failSends = true;
            ChatDemoService.Snapshot result = service.appendExchange(session, id, "committed", "Name");
            assertEquals(3, result.cursor());
            assertEquals(result, service.history(session, id));
            assertEquals(1, failed.errors.get());
            assertEquals(1, service.streamCount());
            assertTrue(healthy.events().contains("id:3"));
            failed.callbacksRepeatedly();
            assertEquals(1, service.streamCount());
            service.close();
            assertEquals(1, healthy.completions.get());
            assertEquals(1, failed.errors.get());
        }
    }

    @Test
    void terminalSendFailureStillCompletesAndReleasesAllDetachedRecipients() throws Exception {
        MutableClock clock = new MutableClock();
        try (ChatDemoService service = service(clock)) {
            MockHttpSession session = new MockHttpSession();
            String id = service.initialize(session, false).conversationId();
            Transport disconnected = new Transport(service, service.stream(session, id), false);
            Transport healthy = new Transport(service, service.stream(session, id), false);
            disconnected.failSends = true;
            clock.advance(IDLE);
            service.sweep();
            assertEquals(0, service.conversationCount());
            assertEquals(0, service.streamCount());
            assertEquals(1, disconnected.completions.get());
            assertEquals(1, healthy.completions.get());
            assertEquals(0, disconnected.errors.get());
            assertTrue(healthy.events().contains("expired"));
            disconnected.callbacksRepeatedly();
            healthy.callbacksRepeatedly();
            service.close();
            assertEquals(1, disconnected.completions.get());
            assertEquals(1, healthy.completions.get());
        }
    }

    @Test
    void streamAdmissionCapturesItsInitialCursorAtomicallyWithConcurrentExchange() throws Exception {
        try (ChatDemoService service = service(new MutableClock())) {
            MockHttpSession session = new MockHttpSession();
            String id = service.initialize(session, false).conversationId();
            List<Object> results = race(2, i -> i == 0
                ? service.stream(session, id)
                : service.appendExchange(session, id, "racing exchange", "Name"));
            SseEmitter emitter = (SseEmitter) results.getFirst();
            Object registration = ((Map<?, ?>) field(service, "streams")).get(emitter);
            long captured = (long) field(registration, "initialCursor");
            assertTrue(captured == 1 || captured == 3, "Admission must observe a complete before/after exchange cursor");
            Transport transport = new Transport(service, emitter, false);
            assertTrue(transport.events().contains("id:" + captured + "\n"));
            assertEquals(3, service.history(session, id).cursor());
            assertEquals(1, service.streamCount());
        }
    }

    @Test
    void explicitReplacementAtFullCapacityUsesOwnSlotAndEndsOnlyItsStreams() throws Exception {
        try (ChatDemoService service = service(new MutableClock())) {
            List<MockHttpSession> sessions = sessions(32);
            List<String> ids = sessions.stream().map(s -> service.initialize(s, false).conversationId()).toList();
            Transport old = new Transport(service, service.stream(sessions.getFirst(), ids.getFirst()), false);
            Transport other = new Transport(service, service.stream(sessions.get(1), ids.get(1)), false);
            service.appendExchange(sessions.getFirst(), ids.getFirst(), "old content", "Name");
            ChatDemoService.Snapshot replacement = service.initialize(sessions.getFirst(), true);
            assertNotEquals(ids.getFirst(), replacement.conversationId());
            assertEquals(1, replacement.cursor());
            assertEquals(1, replacement.messages().size());
            assertEquals(32, service.conversationCount());
            assertEquals(1, service.streamCount());
            assertEquals(1, old.completions.get());
            assertEquals(0, other.completions.get());
            failure(404, () -> service.history(sessions.getFirst(), ids.getFirst()));
            failure(404, () -> service.appendExchange(sessions.getFirst(), ids.getFirst(), "stale", "Name"));
            failure(404, () -> service.stream(sessions.getFirst(), ids.getFirst()));
            service.stream(sessions.getFirst(), replacement.conversationId());
            old.callbacksRepeatedly();
            assertEquals(2, service.streamCount());
            assertEquals(ids.get(1), service.history(sessions.get(1), ids.get(1)).conversationId());
        }
    }

    @Test
    void sessionDestructionUsesExactOwnerAndIsIdempotentWithLateCallbacks() throws Exception {
        try (ChatDemoService service = service(new MutableClock())) {
            MockHttpSession first = new MockHttpSession();
            MockHttpSession second = new MockHttpSession();
            String firstId = service.initialize(first, false).conversationId();
            String secondId = service.initialize(second, false).conversationId();
            Transport firstStream = new Transport(service, service.stream(first, firstId), false);
            Transport secondStream = new Transport(service, service.stream(second, secondId), false);
            service.sessionDestroyed("not-an-owner");
            assertEquals(2, service.conversationCount());
            service.sessionDestroyed(first.getId());
            service.sessionDestroyed(first.getId());
            firstStream.callbacksRepeatedly();
            assertEquals(1, service.conversationCount());
            assertEquals(1, service.streamCount());
            assertEquals(1, firstStream.completions.get());
            assertEquals(0, secondStream.completions.get());
            assertTrue(firstStream.events().contains("expired"));
            failure(410, () -> service.history(first, firstId));
            assertEquals(secondId, service.history(second, secondId).conversationId());
        }
    }

    @Test
    void concurrentCallbacksExpirySessionDestructionAndShutdownReleaseAtMostOnce() throws Exception {
        MutableClock clock = new MutableClock();
        try (ChatDemoService service = service(clock)) {
            MockHttpSession session = new MockHttpSession();
            String id = service.initialize(session, false).conversationId();
            Transport first = new Transport(service, service.stream(session, id), false);
            Transport second = new Transport(service, service.stream(session, id), false);
            clock.advance(IDLE);
            race(24, i -> {
                switch (i % 6) {
                    case 0 -> first.callbacksRepeatedly();
                    case 1 -> second.callbacksRepeatedly();
                    case 2 -> service.sweep();
                    case 3 -> service.sessionDestroyed(session.getId());
                    default -> service.close();
                }
                return true;
            });
            assertEquals(0, service.streamCount());
            assertEquals(0, service.conversationCount());
            assertTrue(first.completions.get() <= 1);
            assertTrue(second.completions.get() <= 1);
            assertEquals(0, first.errors.get() + second.errors.get());
            failure(503, () -> service.initialize(new MockHttpSession(), false));
        }
    }

    @Test
    void shutdownClosesAllSixtyFourStreamsStopsAdmissionAndIsIdempotent() throws Exception {
        ChatDemoService service = new ChatDemoService(new MutableClock(), true);
        try {
            List<MockHttpSession> sessions = sessions(32);
            List<Transport> transports = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            for (MockHttpSession session : sessions) {
                String id = service.initialize(session, false).conversationId();
                ids.add(id);
                transports.add(new Transport(service, service.stream(session, id), false));
                transports.add(new Transport(service, service.stream(session, id), false));
            }
            assertEquals(32, service.conversationCount());
            assertEquals(64, service.streamCount());
            service.close();
            service.close();
            service.sweep();
            assertEquals(0, service.conversationCount());
            assertEquals(0, service.streamCount());
            assertTrue(service.cleanupStopped());
            ScheduledExecutorService executor = (ScheduledExecutorService) field(service, "cleanup");
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            assertTrue(executor.isTerminated());
            for (Transport transport : transports) {
                assertEquals(1, transport.completions.get());
                assertTrue(transport.events().contains("chat-ended"));
                transport.callbacksRepeatedly();
                assertEquals(1, transport.completions.get());
            }
            failure(503, () -> service.initialize(sessions.getFirst(), false));
            failure(503, () -> service.initialize(new MockHttpSession(), true));
            failure(503, () -> service.history(sessions.getFirst(), ids.getFirst()));
            failure(503, () -> service.appendExchange(sessions.getFirst(), ids.getFirst(), "text", "Name"));
            failure(503, () -> service.stream(sessions.getFirst(), ids.getFirst()));
            assertEquals(0, service.conversationCount());
            assertEquals(0, service.streamCount());
        } finally { service.close(); }
    }

    @Test
    void scheduledSweepRunsWithoutAccessUsingAdvancedDeterministicClock() throws Exception {
        MutableClock clock = new MutableClock();
        ChatDemoService service = new ChatDemoService(clock, true);
        try {
            MockHttpSession session = new MockHttpSession();
            String id = service.initialize(session, false).conversationId();
            Transport transport = new Transport(service, service.stream(session, id), false);
            clock.advance(STREAM);
            await(() -> service.streamCount() == 0);
            assertEquals(1, service.conversationCount());
            await(() -> transport.completions.get() == 1);
            clock.advance(IDLE.minus(STREAM));
            await(() -> service.conversationCount() == 0);
            failure(410, () -> service.history(session, id));
        } finally {
            service.close();
            assertTrue(service.cleanupStopped());
            assertTrue(((ScheduledExecutorService) field(service, "cleanup")).awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void serviceRetainsNoHttpSessionObjectsOrUnboundedExpiredOwnerRegistry() throws Exception {
        try (ChatDemoService service = service(new MutableClock())) {
            MockHttpSession session = new MockHttpSession();
            String id = service.initialize(session, false).conversationId();
            service.stream(session, id);
            for (Field declared : ChatDemoService.class.getDeclaredFields()) {
                declared.setAccessible(true);
                Object value = declared.get(service);
                assertFalse(value instanceof HttpSession);
                if (value instanceof Map<?, ?> map) {
                    assertTrue(map.size() <= 64);
                    for (Object state : map.values()) {
                        for (Field retained : state.getClass().getDeclaredFields()) {
                            retained.setAccessible(true);
                            assertFalse(retained.get(state) instanceof HttpSession);
                        }
                    }
                }
            }
            service.sessionDestroyed(session.getId());
            assertEquals(0, ((Map<?, ?>) field(service, "conversations")).size());
            assertEquals(0, ((Map<?, ?>) field(service, "streams")).size());
            assertEquals(1, java.util.Collections.list(session.getAttributeNames()).size());
        }
    }

    private static ChatDemoService service(MutableClock clock) { return new ChatDemoService(clock, false); }

    private static MockHttpSession strangerWithForeignAttribute(MockHttpSession session, String id) {
        session.setAttribute(ChatDemoService.SESSION_CONVERSATION, id);
        return session;
    }

    private static void failure(int status, Runnable action) {
        assertEquals(status, assertThrows(ChatDemoService.ChatFailure.class, action::run).status());
    }

    private static List<MockHttpSession> sessions(int count) {
        List<MockHttpSession> sessions = new ArrayList<>();
        for (int i = 0; i < count; i++) sessions.add(new MockHttpSession());
        return sessions;
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static <T> List<T> race(int count, IndexedOperation<T> operation) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int index = i;
                futures.add(executor.submit((Callable<T>) () -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Admission gate timed out");
                    return operation.run(index);
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS), "All contenders must reach the gate");
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) results.add(future.get(15, TimeUnit.SECONDS));
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @FunctionalInterface
    private interface IndexedOperation<T> { T run(int index) throws Exception; }

    private static void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(10);
        assertTrue(condition.getAsBoolean(), "Scheduled production sweep did not finish before deadline");
    }

    private static final class MutableClock extends Clock {
        private final AtomicLong millis = new AtomicLong(Instant.parse("2026-10-11T12:00:00Z").toEpochMilli());
        void advance(Duration duration) { millis.addAndGet(duration.toMillis()); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.offset(Clock.fixed(instant(), zone), Duration.ZERO); }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis()); }
        @Override public long millis() { return millis.get(); }
    }

    /** Real ResponseBodyEmitter initialization/callbacks with a bounded in-memory HTTP sink. */
    private static final class Transport {
        final AtomicInteger completions = new AtomicInteger();
        final AtomicInteger errors = new AtomicInteger();
        private final StringBuilder sent = new StringBuilder();
        private final Object serviceLock;
        private Runnable completion;
        private Runnable timeout;
        private Consumer<Throwable> error;
        volatile boolean failSends;

        @SuppressWarnings("unchecked")
        Transport(ChatDemoService service, SseEmitter emitter, boolean failInitial) throws Exception {
            serviceLock = field(service, "lock");
            completion = (Runnable) fieldFromEmitter(emitter, "completionCallback");
            timeout = (Runnable) fieldFromEmitter(emitter, "timeoutCallback");
            error = (Consumer<Throwable>) fieldFromEmitter(emitter, "errorCallback");
            Class<?> handlerType = Class.forName(ResponseBodyEmitter.class.getName() + "$Handler");
            Object handler = Proxy.newProxyInstance(handlerType.getClassLoader(), new Class<?>[]{handlerType}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "send" -> {
                        assertFalse(Thread.holdsLock(serviceLock), "Network sends must be outside the service monitor");
                        if (failInitial || failSends) throw new IOException("Test transport disconnected");
                        if (args.length == 1 && args[0] instanceof Iterable<?> values) {
                            for (Object value : values) {
                                Method getData = value.getClass().getMethod("getData");
                                getData.setAccessible(true);
                                record(getData.invoke(value));
                            }
                        } else record(args[0]);
                    }
                    case "complete" -> {
                        assertFalse(Thread.holdsLock(serviceLock), "Network completion must be outside the service monitor");
                        completions.incrementAndGet();
                    }
                    case "completeWithError" -> {
                        assertFalse(Thread.holdsLock(serviceLock));
                        errors.incrementAndGet();
                    }
                    case "onCompletion" -> completion = (Runnable) args[0];
                    case "onTimeout" -> timeout = (Runnable) args[0];
                    case "onError" -> error = (Consumer<Throwable>) args[0];
                    case "toString" -> { return "InMemorySseTransport"; }
                    case "hashCode" -> { return System.identityHashCode(proxy); }
                    case "equals" -> { return proxy == args[0]; }
                    default -> throw new AssertionError("Unexpected Spring transport method: " + method.getName());
                }
                return null;
            });
            Method initialize = ResponseBodyEmitter.class.getDeclaredMethod("initialize", handlerType);
            initialize.setAccessible(true);
            try { initialize.invoke(emitter, handler); }
            catch (InvocationTargetException failed) {
                if (!failInitial || !(failed.getCause() instanceof IOException)) throw failed;
                // This is Spring's own initialization-error path, not a direct service detach call.
                Method initializeWithError = ResponseBodyEmitter.class.getDeclaredMethod("initializeWithError", Throwable.class);
                initializeWithError.setAccessible(true);
                initializeWithError.invoke(emitter, failed.getCause());
            }
        }

        private static Object fieldFromEmitter(SseEmitter emitter, String name) throws ReflectiveOperationException {
            Field field = ResponseBodyEmitter.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(emitter);
        }

        private synchronized void record(Object data) { sent.append(data); }
        synchronized String events() { return sent.toString(); }
        void callback(Callback callback) {
            switch (callback) {
                case COMPLETION -> completion.run();
                case TIMEOUT -> timeout.run();
                case ERROR -> error.accept(new IOException("Test callback"));
            }
        }
        void callbacksRepeatedly() {
            for (int i = 0; i < 3; i++) for (Callback callback : Callback.values()) callback(callback);
        }
    }
}
