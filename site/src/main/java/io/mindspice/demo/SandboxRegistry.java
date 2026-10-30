package io.mindspice.demo;

import io.mindspice.demo.forum.ForumDemoService;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Bounded temporary editing/forum state. Session ownership is not persona authentication. */
@Service
public class SandboxRegistry implements AutoCloseable {
    static final String SESSION_STARTED = "site.sandbox.started";
    static final String SESSION_DESTROYED = "site.sandbox.destroyed";
    static final String REQUEST_ENTRY = SandboxRegistry.class.getName();
    static final int MAX_VISITORS = 32;
    static final long IDLE_MILLIS = Duration.ofMinutes(30).toMillis();
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final Clock clock;
    private final ScheduledExecutorService cleanup;
    private boolean closed;

    public SandboxRegistry() { this(Clock.systemUTC(), true); }

    SandboxRegistry(Clock clock, boolean schedule) {
        this.clock = clock;
        cleanup = schedule ? Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "site-sandbox-cleanup");
            thread.setDaemon(true);
            return thread;
        }) : null;
        if (cleanup != null) cleanup.scheduleWithFixedDelay(this::sweep, 1, 1, TimeUnit.MINUTES);
    }

    synchronized Entry acquire(HttpSession session, boolean initialize, boolean reset) {
        sweep();
        if (closed) throw failure(503, "Sandboxes are stopping.");
        if (session == null) throw failure(404, "Open the sandbox first.");
        try {
            String owner = session.getId();
            Object started = session.getAttribute(SESSION_STARTED);
            if (Boolean.TRUE.equals(session.getAttribute(SESSION_DESTROYED))) {
                throw failure(404, "Open the sandbox first.");
            }
            Entry entry = entries.get(owner);
            if (entry != null && (entry.resetting || (reset && entry.users != 0))) {
                throw failure(409, "Sandbox is busy. Try again.");
            }
            if (entry == null || reset) {
                if (!reset && started != null) {
                    throw failure(410, "This sandbox expired. Start a new sandbox explicitly.");
                }
                if (!initialize && !reset) throw failure(404, "Open the sandbox first.");
                if (entry == null && entries.size() >= MAX_VISITORS) throw failure(503, "Sandbox capacity reached. Try later.");
                entry = new Entry(owner, clock.millis(), reset, entry);
                entries.put(owner, entry);
            }
            entry.users++;
            return entry;
        } catch (IllegalStateException invalidSession) {
            throw failure(404, "Open the sandbox first.");
        }
    }

    synchronized void release(Entry entry, HttpSession session, boolean successful) {
        entry.users--;
        // Destruction, shutdown or replacement must never be undone by a late dispatch.
        if (entries.get(entry.owner) != entry) return;
        try {
            if (Boolean.TRUE.equals(session.getAttribute(SESSION_DESTROYED))) {
                entries.remove(entry.owner);
                return;
            }
            if (successful) {
                if (!entry.committed) session.setAttribute(SESSION_STARTED, Boolean.TRUE);
                entry.committed = true;
                entry.resetting = false;
                entry.previous = null;
                entry.activity = clock.millis();
            } else if (!entry.committed && entry.users == 0) {
                if (entry.previous == null) entries.remove(entry.owner);
                else entries.put(entry.owner, entry.previous);
            }
        } catch (IllegalStateException invalidSession) {
            entries.remove(entry.owner);
        }
    }

    public synchronized void sessionDestroyed(String owner) { entries.remove(owner); }

    synchronized void sweep() {
        long now = clock.millis();
        entries.values().removeIf(entry -> entry.users == 0 && now - entry.activity >= IDLE_MILLIS);
    }

    static Entry current(HttpServletRequest request) {
        Entry entry = (Entry) request.getAttribute(REQUEST_ENTRY);
        if (entry == null) throw failure(404, "Open the sandbox first.");
        return entry;
    }

    static ResponseStatusException failure(int status, String message) {
        return new ResponseStatusException(HttpStatus.valueOf(status), message);
    }

    @Override @PreDestroy
    public synchronized void close() {
        closed = true;
        if (cleanup != null) cleanup.shutdownNow();
        entries.clear();
    }

    static final class Entry {
        final EditingDemoController editing = new EditingDemoController();
        final ForumDemoService forum = new ForumDemoService();
        final String owner;
        long activity;
        int users;
        boolean committed;
        boolean resetting;
        Entry previous;
        Entry(String owner, long now, boolean resetting, Entry previous) {
            this.owner = owner;
            this.activity = now;
            this.resetting = resetting;
            this.previous = previous;
        }
    }
}
