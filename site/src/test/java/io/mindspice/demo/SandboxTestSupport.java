package io.mindspice.demo;

import io.mindspice.demo.forum.ForumDemoService;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Credentials and visitor state for retained behavior tests; security rejection tests use raw requests. */
public final class SandboxTestSupport implements AutoCloseable {
    public final SandboxRegistry registry = new SandboxRegistry(Clock.systemUTC(), false);
    public final MockHttpSession session = new MockHttpSession();

    public SandboxTestSupport() { prepareSession(session); }

    public ForumDemoService forum() {
        SandboxRegistry.Entry entry = registry.acquire(session, true, false);
        try { return entry.forum; } finally { registry.release(entry, session, false); }
    }

    public EditingDemoController editing() {
        SandboxRegistry.Entry entry = registry.acquire(session, true, false);
        try { return entry.editing; } finally { registry.release(entry, session, false); }
    }

    private void prepareSession(MockHttpSession visitor) {
        if (visitor.getAttribute(SandboxSecurityFilter.TOKEN_ATTRIBUTE) == null) {
            visitor.setAttribute(SandboxSecurityFilter.TOKEN_ATTRIBUTE, UUID.randomUUID().toString());
        }
        SandboxRegistry.Entry entry = registry.acquire(visitor, true, false);
        registry.release(entry, visitor, true);
    }

    private RequestPostProcessor credentials(boolean initialize) {
        return request -> {
            if (request.getSession(false) instanceof MockHttpSession visitor) {
                if (initialize) prepareSession(visitor);
                else if (visitor.getAttribute(SandboxSecurityFilter.TOKEN_ATTRIBUTE) == null) {
                    visitor.setAttribute(SandboxSecurityFilter.TOKEN_ATTRIBUTE, UUID.randomUUID().toString());
                }
                request.addHeader(SandboxSecurityFilter.TOKEN_HEADER, visitor.getAttribute(SandboxSecurityFilter.TOKEN_ATTRIBUTE));
            }
            request.addHeader("Origin", "http://localhost");
            return request;
        };
    }

    public DefaultMockMvcBuilder web(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context)
            .defaultRequest(get("/").session(session).with(credentials(true)))
            .addFilters(new SandboxSecurityFilter(registry));
    }

    public StandaloneMockMvcBuilder standalone(Object controller) {
        if (controller instanceof EditingDemoController) {
            SandboxRegistry.Entry entry = registry.acquire(session, true, false);
            try {
                var field = SandboxRegistry.Entry.class.getDeclaredField("editing");
                field.setAccessible(true);
                field.set(entry, controller);
            } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
            finally { registry.release(entry, session, false); }
        }
        return MockMvcBuilders.standaloneSetup(controller)
            .defaultRequest(get("/").session(session).with(credentials(true)))
            .addFilters(new SandboxSecurityFilter(registry));
    }

    public StandaloneMockMvcBuilder chat(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
            .defaultRequest(get("/").with(credentials(false)))
            .addFilters(new SandboxSecurityFilter(registry));
    }

    @Override public void close() { registry.close(); }
}
