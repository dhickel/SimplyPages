package io.mindspice.demo;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SandboxRegistryTest {
    private static final class MutableClock extends Clock {
        long now;
        public Instant instant() { return Instant.ofEpochMilli(now); }
        public long millis() { return now; }
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
    }

    private static void failure(int status, Runnable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action::run).getStatusCode().value());
    }

    @Test void missingExpiredExplicitResetAndSessionDestructionNeverImplicitlyRecreateState() {
        MutableClock clock = new MutableClock();
        try (SandboxRegistry registry = new SandboxRegistry(clock, false)) {
            MockHttpSession session = new MockHttpSession();
            failure(404, () -> registry.acquire(null, false, false));
            failure(404, () -> registry.acquire(session, false, false));
            MockHttpSession invalidated = new MockHttpSession();
            invalidated.invalidate();
            failure(404, () -> registry.acquire(invalidated, true, false));
            SandboxRegistry.Entry first = registry.acquire(session, true, false);
            registry.release(first, session, true);
            clock.now = SandboxRegistry.IDLE_MILLIS;
            registry.sweep();
            failure(410, () -> registry.acquire(session, true, false));
            failure(410, () -> registry.acquire(session, false, false));
            SandboxRegistry.Entry replacement = registry.acquire(session, false, true);
            assertNotSame(first, replacement);
            registry.release(replacement, session, true);
            registry.sessionDestroyed(session.getId());
            failure(410, () -> registry.acquire(session, false, false));
            MockHttpSession newSession = new MockHttpSession();
            SandboxRegistry.Entry newEntry = registry.acquire(newSession, true, false);
            registry.release(newEntry, newSession, true);
            assertNotSame(replacement, newEntry);
        }
    }

    @Test void actualCapacityReclaimsExpiredButNeverEvictsValidVisitorsAndCloseStopsAdmission() {
        MutableClock clock = new MutableClock();
        SandboxRegistry registry = new SandboxRegistry(clock, false);
        List<MockHttpSession> sessions = new ArrayList<>();
        List<SandboxRegistry.Entry> entries = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            MockHttpSession session = new MockHttpSession();
            sessions.add(session);
            SandboxRegistry.Entry entry = registry.acquire(session, true, false);
            registry.release(entry, session, true);
            entries.add(entry);
        }
        MockHttpSession rejected = new MockHttpSession();
        failure(503, () -> registry.acquire(rejected, true, false));
        assertNull(rejected.getAttribute(SandboxRegistry.SESSION_STARTED));
        SandboxRegistry.Entry pendingReset = registry.acquire(sessions.getFirst(), false, true);
        failure(503, () -> registry.acquire(rejected, true, false));
        registry.release(pendingReset, sessions.getFirst(), false);
        assertSame(entries.getFirst(), registry.acquire(sessions.getFirst(), false, false));
        registry.release(entries.getFirst(), sessions.getFirst(), false);
        clock.now = SandboxRegistry.IDLE_MILLIS - 1;
        SandboxRegistry.Entry live = registry.acquire(sessions.getFirst(), false, false);
        registry.release(live, sessions.getFirst(), true);
        clock.now++;
        SandboxRegistry.Entry admitted = registry.acquire(rejected, true, false);
        registry.release(admitted, rejected, true);
        assertSame(live, registry.acquire(sessions.getFirst(), false, false));
        registry.release(live, sessions.getFirst(), false);
        failure(410, () -> registry.acquire(sessions.get(1), true, false));
        registry.close();
        failure(503, () -> registry.acquire(rejected, false, true));
    }

    @Test void activeRequestsAreNotSweptOrResetAndFailedRequestsDoNotRefreshIdleTime() {
        MutableClock clock = new MutableClock();
        try (SandboxRegistry registry = new SandboxRegistry(clock, false)) {
            MockHttpSession session = new MockHttpSession();
            SandboxRegistry.Entry entry = registry.acquire(session, true, false);
            registry.release(entry, session, true);
            assertSame(entry, registry.acquire(session, false, false));
            clock.now = SandboxRegistry.IDLE_MILLIS;
            registry.sweep();
            failure(409, () -> registry.acquire(session, false, true));
            assertSame(entry, registry.acquire(session, false, false));
            registry.release(entry, session, false);
            registry.release(entry, session, false);
            failure(410, () -> registry.acquire(session, false, false));
        }
    }

    @Test void filterMissingExpiredCapacityAndCsrfRejectionsHaveNoControllerDispatchOrAllocation() throws Exception {
        MutableClock clock = new MutableClock();
        try (SandboxRegistry registry = new SandboxRegistry(clock, false)) {
            SandboxSecurityFilter filter = new SandboxSecurityFilter(registry);
            MockHttpSession session = new MockHttpSession();
            session.setAttribute(SandboxSecurityFilter.TOKEN_ATTRIBUTE, "test-token");
            AtomicBoolean dispatched = new AtomicBoolean();
            var request = request("GET", "/editing-demo/edit/module-1", session);
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> dispatched.set(true));
            assertEquals(404, response.getStatus());
            assertFalse(dispatched.get());
            assertNull(session.getAttribute(SandboxRegistry.SESSION_STARTED));
            SandboxRegistry.Entry entry = registry.acquire(session, true, false);
            registry.release(entry, session, true);
            clock.now = SandboxRegistry.IDLE_MILLIS;
            for (String path : List.of("/editing-demo", "/forum", "/editing-demo/edit/module-1")) {
                response = new MockHttpServletResponse();
                filter.doFilter(request("GET", path, session), response, (req, res) -> dispatched.set(true));
                assertEquals(410, response.getStatus());
                assertFalse(dispatched.get());
            }
            response = new MockHttpServletResponse();
            filter.doFilter(request("POST", "/sandbox/new", session), response, (req, res) -> dispatched.set(true));
            assertEquals(403, response.getStatus());
            failure(410, () -> registry.acquire(session, false, false));
            var reset = request("POST", "/sandbox/new", session);
            reset.addHeader("Origin", "http://localhost");
            reset.addHeader(SandboxSecurityFilter.TOKEN_HEADER, "test-token");
            response = new MockHttpServletResponse();
            filter.doFilter(reset, response, (req, res) -> dispatched.set(true));
            assertEquals(204, response.getStatus());
            assertFalse(dispatched.get());
            for (int i = 1; i < 32; i++) {
                MockHttpSession nextSession = new MockHttpSession();
                SandboxRegistry.Entry next = registry.acquire(nextSession, true, false);
                registry.release(next, nextSession, true);
            }
            MockHttpSession overflow = new MockHttpSession();
            response = new MockHttpServletResponse();
            filter.doFilter(request("GET", "/forum", overflow), response, (req, res) -> dispatched.set(true));
            assertEquals(503, response.getStatus());
            assertNull(overflow.getAttribute(SandboxRegistry.SESSION_STARTED));
            assertFalse(dispatched.get());
        }
    }

    @Test void thrownControllerFailureReleasesLeaseWithoutRefreshingActivity() throws Exception {
        MutableClock clock = new MutableClock();
        try (SandboxRegistry registry = new SandboxRegistry(clock, false)) {
            SandboxSecurityFilter filter = new SandboxSecurityFilter(registry);
            MockHttpSession session = new MockHttpSession();
            SandboxRegistry.Entry entry = registry.acquire(session, true, false);
            registry.release(entry, session, true);
            clock.now = SandboxRegistry.IDLE_MILLIS - 1;
            assertThrows(jakarta.servlet.ServletException.class, () -> filter.doFilter(
                request("GET", "/editing-demo/edit/module-1", session), new MockHttpServletResponse(),
                (req, res) -> { throw new jakarta.servlet.ServletException("failed before response status"); }));
            assertEquals(0, entry.users);
            assertEquals(0, entry.activity);
            clock.now++;
            failure(410, () -> registry.acquire(session, false, false));
        }
    }

    @Test void failedInitialDispatchesDoNotConsumeStartedMarkersOrCapacity() throws Exception {
        try (SandboxRegistry registry = new SandboxRegistry(new MutableClock(), false)) {
            SandboxSecurityFilter filter = new SandboxSecurityFilter(registry);
            for (int i = 0; i < SandboxRegistry.MAX_VISITORS * 2; i++) {
                MockHttpSession session = new MockHttpSession();
                var request = request("GET", i % 2 == 0 ? "/editing-demo" : "/forum", session);
                var response = new MockHttpServletResponse();
                filter.doFilter(request, response, (req, res) -> ((HttpServletResponse) res).setStatus(406));
                assertEquals(406, response.getStatus());
                assertNull(request.getAttribute(SandboxRegistry.REQUEST_ENTRY));
                assertNull(session.getAttribute(SandboxRegistry.SESSION_STARTED));
                failure(404, () -> registry.acquire(session, false, false));
            }
            fillCapacity(registry);
        }
    }

    @Test void initialDispatchExceptionsRollBackAdmissionAndPermitSameSessionRetry() throws Exception {
        try (SandboxRegistry registry = new SandboxRegistry(new MutableClock(), false)) {
            SandboxSecurityFilter filter = new SandboxSecurityFilter(registry);
            MockHttpSession session = new MockHttpSession();
            var request = request("GET", "/editing-demo", session);
            assertThrows(ServletException.class, () -> filter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> { throw new ServletException("initial dispatch failed"); }));
            assertNull(request.getAttribute(SandboxRegistry.REQUEST_ENTRY));
            assertNull(session.getAttribute(SandboxRegistry.SESSION_STARTED));
            failure(404, () -> registry.acquire(session, false, false));
            assertThrows(AssertionError.class, () -> filter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> { throw new AssertionError("initial dispatch error"); }));
            assertNull(session.getAttribute(SandboxRegistry.SESSION_STARTED));
            failure(404, () -> registry.acquire(session, false, false));
            filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
            assertEquals(Boolean.TRUE, session.getAttribute(SandboxRegistry.SESSION_STARTED));
            SandboxRegistry.Entry committed = registry.acquire(session, false, false);
            registry.release(committed, session, false);
        }
    }

    @Test void overlappingInitialRequestsKeepAnySuccessfulCommitAndRollBackOnlyWhenAllFail() throws Exception {
        for (boolean successful : List.of(false, true)) {
            for (boolean firstFinishesFirst : List.of(false, true)) {
                try (SandboxRegistry registry = new SandboxRegistry(new MutableClock(), false);
                     var workers = Executors.newFixedThreadPool(2)) {
                    SandboxSecurityFilter filter = new SandboxSecurityFilter(registry);
                    MockHttpSession session = new MockHttpSession();
                    CountDownLatch firstEntered = new CountDownLatch(1), secondEntered = new CountDownLatch(1);
                    CountDownLatch finishFirst = new CountDownLatch(1), finishSecond = new CountDownLatch(1);
                    AtomicReference<SandboxRegistry.Entry> firstEntry = new AtomicReference<>(), secondEntry = new AtomicReference<>();
                    var first = workers.submit(() -> {
                        filter.doFilter(request("GET", "/editing-demo", session), new MockHttpServletResponse(), (req, res) -> {
                            firstEntry.set(SandboxRegistry.current((MockHttpServletRequest) req));
                            firstEntered.countDown();
                            await(finishFirst);
                            if (!successful) ((HttpServletResponse) res).setStatus(500);
                        });
                        return null;
                    });
                    try {
                        assertTrue(firstEntered.await(10, TimeUnit.SECONDS));
                        var second = workers.submit(() -> {
                            filter.doFilter(request("GET", "/forum", session), new MockHttpServletResponse(), (req, res) -> {
                                secondEntry.set(SandboxRegistry.current((MockHttpServletRequest) req));
                                secondEntered.countDown();
                                await(finishSecond);
                                ((HttpServletResponse) res).setStatus(406);
                            });
                            return null;
                        });
                        try {
                            assertTrue(secondEntered.await(10, TimeUnit.SECONDS));
                            assertSame(firstEntry.get(), secondEntry.get());
                            assertNull(session.getAttribute(SandboxRegistry.SESSION_STARTED));
                            if (firstFinishesFirst) {
                                finishFirst.countDown();
                                first.get(10, TimeUnit.SECONDS);
                                assertEquals(1, firstEntry.get().users);
                                assertEquals(successful ? Boolean.TRUE : null, session.getAttribute(SandboxRegistry.SESSION_STARTED));
                                finishSecond.countDown();
                            } else {
                                finishSecond.countDown();
                                second.get(10, TimeUnit.SECONDS);
                                assertEquals(1, firstEntry.get().users);
                                assertNull(session.getAttribute(SandboxRegistry.SESSION_STARTED));
                                finishFirst.countDown();
                            }
                            first.get(10, TimeUnit.SECONDS);
                            second.get(10, TimeUnit.SECONDS);
                            assertEquals(0, firstEntry.get().users);
                            if (successful) {
                                assertEquals(Boolean.TRUE, session.getAttribute(SandboxRegistry.SESSION_STARTED));
                                SandboxRegistry.Entry committed = registry.acquire(session, false, false);
                                assertSame(firstEntry.get(), committed);
                                registry.release(committed, session, false);
                            } else {
                                assertNull(session.getAttribute(SandboxRegistry.SESSION_STARTED));
                                failure(404, () -> registry.acquire(session, false, false));
                                fillCapacity(registry);
                            }
                        } finally { finishSecond.countDown(); }
                    } finally { finishFirst.countDown(); }
                }
            }
        }
    }

    @Test void destructionDuringDispatchDiscardsPendingAdmissionEvenWhenDispatchLaterSucceeds() throws Exception {
        try (SandboxRegistry registry = new SandboxRegistry(new MutableClock(), false);
             var workers = Executors.newSingleThreadExecutor()) {
            SandboxSecurityFilter filter = new SandboxSecurityFilter(registry);
            MockHttpSession session = new MockHttpSession();
            String owner = session.getId();
            CountDownLatch entered = new CountDownLatch(1), finish = new CountDownLatch(1);
            AtomicReference<SandboxRegistry.Entry> pending = new AtomicReference<>();
            var dispatch = workers.submit(() -> {
                filter.doFilter(request("GET", "/editing-demo", session), new MockHttpServletResponse(), (req, res) -> {
                    pending.set(SandboxRegistry.current((MockHttpServletRequest) req));
                    entered.countDown();
                    await(finish);
                });
                return null;
            });
            try {
                assertTrue(entered.await(10, TimeUnit.SECONDS));
                registry.sessionDestroyed(owner);
                session.invalidate();
                finish.countDown();
                dispatch.get(10, TimeUnit.SECONDS);
                assertEquals(0, pending.get().users);
                assertFalse(pending.get().committed);
                fillCapacity(registry);
            } finally { finish.countDown(); }
        }
    }

    @Test void lateReleasesNeverRestoreDestroyedEntriesOrRemoveTheirReplacement() {
        for (boolean successful : List.of(false, true)) {
            try (SandboxRegistry registry = new SandboxRegistry(new MutableClock(), false)) {
                MockHttpSession session = new MockHttpSession();
                SandboxRegistry.Entry pending = registry.acquire(session, true, false);
                registry.sessionDestroyed(session.getId());
                registry.release(pending, session, successful);
                assertNull(session.getAttribute(SandboxRegistry.SESSION_STARTED));
                failure(404, () -> registry.acquire(session, false, false));

                SandboxRegistry.Entry previous = registry.acquire(session, true, false);
                registry.release(previous, session, true);
                SandboxRegistry.Entry reset = registry.acquire(session, false, true);
                registry.sessionDestroyed(session.getId());
                SandboxRegistry.Entry replacement = registry.acquire(session, false, true);
                registry.release(replacement, session, true);
                registry.release(reset, session, successful);
                assertSame(replacement, registry.acquire(session, false, false));
                registry.release(replacement, session, false);
                registry.sessionDestroyed(session.getId());
                fillCapacity(registry);
            }
        }
    }

    @Test void failedResetRestoresPriorEntryAndDoesNotRefreshOrReviveExpiredState() {
        MutableClock clock = new MutableClock();
        try (SandboxRegistry registry = new SandboxRegistry(clock, false)) {
            MockHttpSession session = new MockHttpSession();
            SandboxRegistry.Entry previous = registry.acquire(session, true, false);
            registry.release(previous, session, true);
            clock.now = SandboxRegistry.IDLE_MILLIS - 1;
            SandboxRegistry.Entry reset = registry.acquire(session, false, true);
            failure(409, () -> registry.acquire(session, false, false));
            failure(409, () -> registry.acquire(session, true, false));
            failure(409, () -> registry.acquire(session, false, true));
            registry.release(reset, session, false);
            assertSame(previous, registry.acquire(session, false, false));
            registry.release(previous, session, false);
            assertEquals(0, previous.activity);
            clock.now++;
            failure(410, () -> registry.acquire(session, true, false));
            SandboxRegistry.Entry expiredReset = registry.acquire(session, false, true);
            registry.release(expiredReset, session, false);
            assertEquals(Boolean.TRUE, session.getAttribute(SandboxRegistry.SESSION_STARTED));
            failure(410, () -> registry.acquire(session, true, false));

            MockHttpSession fresh = new MockHttpSession();
            SandboxRegistry.Entry freshReset = registry.acquire(fresh, false, true);
            registry.release(freshReset, fresh, false);
            assertNull(fresh.getAttribute(SandboxRegistry.SESSION_STARTED));
            failure(404, () -> registry.acquire(fresh, false, false));
            fillCapacity(registry);
        }
    }

    @Test void failedResetResponseRestoresEntryAndForumPersonaOnErrorStatusAndException() throws Exception {
        for (boolean throwsFailure : List.of(false, true)) {
            MutableClock clock = new MutableClock();
            try (SandboxRegistry registry = new SandboxRegistry(clock, false)) {
                SandboxSecurityFilter filter = new SandboxSecurityFilter(registry);
                MockHttpSession session = new MockHttpSession();
                session.setAttribute(SandboxSecurityFilter.TOKEN_ATTRIBUTE, "test-token");
                session.setAttribute(ForumDemoController.SESSION_VIEWER_ID, "owner");
                session.setAttribute(ForumDemoController.SESSION_VIEWER_NAME, "Owner Name");
                session.setAttribute(ForumDemoController.SESSION_VIEWER_MODERATOR, true);
                SandboxRegistry.Entry previous = registry.acquire(session, true, false);
                registry.release(previous, session, true);
                clock.now = 100;
                var reset = request("POST", "/sandbox/new", session);
                reset.addHeader("Origin", "http://localhost");
                reset.addHeader(SandboxSecurityFilter.TOKEN_HEADER, "test-token");
                var failedResponse = new MockHttpServletResponse() {
                    @Override public void setStatus(int status) {
                        if (throwsFailure) throw new IllegalStateException("failed reset response");
                        super.setStatus(503);
                    }
                };
                if (throwsFailure) {
                    assertThrows(IllegalStateException.class, () -> filter.doFilter(reset, failedResponse,
                        (req, res) -> fail("reset must not dispatch")));
                } else {
                    filter.doFilter(reset, failedResponse, (req, res) -> fail("reset must not dispatch"));
                    assertEquals(503, failedResponse.getStatus());
                }
                assertNull(reset.getAttribute(SandboxRegistry.REQUEST_ENTRY));
                assertSame(previous, registry.acquire(session, false, false));
                registry.release(previous, session, false);
                assertEquals(0, previous.activity);
                assertEquals(Boolean.TRUE, session.getAttribute(SandboxRegistry.SESSION_STARTED));
                assertEquals("owner", session.getAttribute(ForumDemoController.SESSION_VIEWER_ID));
                assertEquals("Owner Name", session.getAttribute(ForumDemoController.SESSION_VIEWER_NAME));
                assertEquals(true, session.getAttribute(ForumDemoController.SESSION_VIEWER_MODERATOR));
            }
        }
    }

    @Test void destroyedOrClosedPendingResetCannotRestorePreviousState() {
        for (boolean close : List.of(false, true)) {
            try (SandboxRegistry registry = new SandboxRegistry(new MutableClock(), false)) {
                MockHttpSession session = new MockHttpSession();
                SandboxRegistry.Entry previous = registry.acquire(session, true, false);
                registry.release(previous, session, true);
                SandboxRegistry.Entry reset = registry.acquire(session, false, true);
                if (close) registry.close();
                else { registry.sessionDestroyed(session.getId()); session.invalidate(); }
                registry.release(reset, session, false);
                assertEquals(0, reset.users);
                if (close) failure(503, () -> registry.acquire(session, false, true));
                else fillCapacity(registry);
            }
        }
    }

    private static void fillCapacity(SandboxRegistry registry) {
        for (int i = 0; i < SandboxRegistry.MAX_VISITORS; i++) {
            MockHttpSession session = new MockHttpSession();
            SandboxRegistry.Entry entry = registry.acquire(session, true, false);
            registry.release(entry, session, true);
        }
        MockHttpSession overflow = new MockHttpSession();
        failure(503, () -> registry.acquire(overflow, true, false));
        assertNull(overflow.getAttribute(SandboxRegistry.SESSION_STARTED));
    }

    private static void await(CountDownLatch latch) throws ServletException {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new ServletException("timed out awaiting test dispatch");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ServletException(interrupted);
        }
    }

    private static MockHttpServletRequest request(String method, String path, MockHttpSession session) {
        var request = new MockHttpServletRequest(method, path);
        request.setSession(session);
        return request;
    }
}
