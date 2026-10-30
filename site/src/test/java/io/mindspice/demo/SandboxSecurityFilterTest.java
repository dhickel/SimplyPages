package io.mindspice.demo;

import io.mindspice.demo.chat.ChatDemoService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SandboxSecurityFilterTest {
    private final SandboxRegistry registry = new SandboxRegistry(Clock.systemUTC(), false);
    private final SandboxSecurityFilter filter = new SandboxSecurityFilter(registry);

    @AfterEach void close() { registry.close(); }

    private MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }

    private MockHttpServletResponse dispatch(MockHttpServletRequest request, FilterChain chain) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    private void assertNoSandbox(MockHttpSession session) {
        assertNull(session.getAttribute(SandboxRegistry.SESSION_STARTED));
        assertNull(session.getAttribute(ChatDemoService.SESSION_CONVERSATION));
        assertEquals(404, assertThrows(ResponseStatusException.class,
            () -> registry.acquire(session, false, false)).getStatusCode().value());
    }

    @Test void canonicalPresentationProvidesStableSsrTokenWithoutStateAdmission() throws Exception {
        for (String path : List.of("/demos", "/demos/components/text-structure", "/demos/components/forms",
            "/demos/components/display-data", "/demos/components/navigation", "/demos/components/media",
            "/demos/layout", "/demos/modules", "/demos/modules/contact-form", "/demos/builders",
            "/demos/builders/shell", "/demos/features/templates", "/demos/features/htmx",
            "/demos/features/static-content", "/demos/features/safe-rendering")) {
            MockHttpServletRequest request = request("GET", path);
            MockHttpServletResponse response = dispatch(request, (req, res) -> {
                assertNotNull(SandboxSecurityFilter.requestToken(request));
                assertNull(request.getAttribute(SandboxRegistry.REQUEST_ENTRY));
            });
            MockHttpSession session = (MockHttpSession) request.getSession(false);
            assertNotNull(session, path);
            String token = SandboxSecurityFilter.requestToken(request);
            assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
            assertEquals(token, response.getHeader(SandboxSecurityFilter.TOKEN_HEADER));
            assertEquals("no-store", response.getHeader("Cache-Control"));
            assertNoSandbox(session);

            MockHttpServletRequest hx = request("GET", path);
            hx.setSession(session);
            hx.addHeader("HX-Request", "TrUe");
            assertEquals(token, dispatch(hx, (req, res) ->
                assertEquals(token, SandboxSecurityFilter.requestToken(hx)))
                .getHeader(SandboxSecurityFilter.TOKEN_HEADER));
            assertNoSandbox(session);
        }
    }

    @Test void tokenAccessorNeverAllocatesAndPublicReadsDoNotCreateSessions() throws Exception {
        for (String path : List.of("/", "/docs", "/docs/reference/build-and-assets.md", "/javadocs/index.html",
            "/css/site.css", "/js/site.js", "/media/sample.svg", "/webjars/htmx.org/dist/htmx.min.js")) {
            MockHttpServletRequest request = request("GET", path);
            dispatch(request, (req, res) -> assertNull(request.getSession(false)));
            assertThrows(IllegalStateException.class, () -> SandboxSecurityFilter.requestToken(request));
            assertNull(request.getSession(false), path);
        }
        MockHttpServletRequest request = request("GET", "/docs");
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        assertThrows(IllegalStateException.class, () -> SandboxSecurityFilter.requestToken(request));
        assertFalse(session.getAttributeNames().hasMoreElements());
        session.setAttribute(SandboxSecurityFilter.TOKEN_ATTRIBUTE, "existing-private-token");
        dispatch(request, (req, res) -> assertThrows(IllegalStateException.class,
            () -> SandboxSecurityFilter.requestToken(request)));
        assertEquals("existing-private-token", session.getAttribute(SandboxSecurityFilter.TOKEN_ATTRIBUTE));
    }

    @Test void nonDocumentApiReadsAndChatHxNavigationDoNotInitializeTokens() throws Exception {
        for (String path : List.of("/demos/api", "/demos/api/form-preview", "/demos/api/template-card",
            "/chat", "/demos/features/chat")) {
            MockHttpServletRequest request = request("GET", path);
            request.addHeader("HX-Request", "true");
            MockHttpServletResponse response = dispatch(request, (req, res) -> assertNull(request.getSession(false)));
            assertNull(request.getSession(false), path);
            assertNull(response.getHeader(SandboxSecurityFilter.TOKEN_HEADER));
            assertEquals("no-store", response.getHeader("Cache-Control"));
        }
        for (String method : List.of("HEAD", "OPTIONS")) {
            MockHttpServletRequest request = request(method, "/demos/components/forms");
            dispatch(request, (req, res) -> assertNull(request.getSession(false)));
            assertNull(request.getSession(false));
        }
    }

    @Test void presentationAndAliasesRejectForeignReadsAndUnverifiedWritesBeforeAllocation() throws Exception {
        for (String path : List.of("/demos/components/forms", "/demos/features/templates",
            "/demos/features/editing", "/demos/features/forum", "/demos/features/chat",
            "/demos/features/editing/save/module-1", "/demos/features/forum/topics/create")) {
            MockHttpServletRequest foreign = request("GET", path);
            foreign.addHeader("Origin", "https://foreign.test");
            MockHttpServletResponse response = dispatch(foreign, (req, res) -> fail("Foreign read reached MVC"));
            assertEquals(403, response.getStatus(), path);
            assertEquals("no-store", response.getHeader("Cache-Control"));
            assertNull(response.getHeader(SandboxSecurityFilter.TOKEN_HEADER));
            assertNull(foreign.getSession(false));
            MockHttpSession existing = new MockHttpSession();
            existing.setAttribute(SandboxSecurityFilter.TOKEN_ATTRIBUTE, "existing-private-token");
            MockHttpServletRequest foreignWithSession = request("GET", path);
            foreignWithSession.setSession(existing);
            foreignWithSession.addHeader("Origin", "https://foreign.test");
            MockHttpServletResponse rejected = dispatch(foreignWithSession, (req, res) -> fail("Foreign read reached MVC"));
            assertEquals(403, rejected.getStatus(), path);
            assertNull(rejected.getHeader(SandboxSecurityFilter.TOKEN_HEADER), path);
            assertThrows(IllegalStateException.class, () -> SandboxSecurityFilter.requestToken(foreignWithSession));
            MockHttpServletRequest foreignWrite = request("POST", path);
            foreignWrite.setSession(existing);
            foreignWrite.addHeader("Origin", "https://foreign.test");
            foreignWrite.addHeader(SandboxSecurityFilter.TOKEN_HEADER, "existing-private-token");
            MockHttpServletResponse rejectedWrite = dispatch(foreignWrite, (req, res) -> fail("Foreign write reached MVC"));
            assertEquals(403, rejectedWrite.getStatus(), path);
            assertNull(rejectedWrite.getHeader(SandboxSecurityFilter.TOKEN_HEADER), path);
            MockHttpServletRequest write = request("POST", path);
            write.addHeader("Origin", "http://localhost");
            assertEquals(403, dispatch(write, (req, res) -> fail("Unverified write reached MVC")).getStatus());
            assertNull(write.getSession(false));
        }
        MockHttpServletRequest fetch = request("GET", "/demos/components/forms");
        fetch.addHeader("Sec-Fetch-Site", "cross-site");
        assertEquals(403, dispatch(fetch, (req, res) -> fail("Cross-site read reached MVC")).getStatus());
        assertNull(fetch.getSession(false));
    }

    @Test void canonicalEditingAndForumEntryUsesProtectedRegistryLifecycle() throws Exception {
        for (String path : List.of("/demos/features/editing", "/demos/features/forum")) {
            MockHttpServletRequest request = request("GET", path);
            SandboxRegistry.Entry[] captured = new SandboxRegistry.Entry[1];
            dispatch(request, (req, res) -> {
                captured[0] = SandboxRegistry.current(request);
                assertFalse(captured[0].committed);
                assertNotNull(SandboxSecurityFilter.requestToken(request));
                assertEquals(path.endsWith("forum"), Thread.holdsLock(captured[0].forum));
            });
            assertNotNull(captured[0]);
            assertTrue(captured[0].committed);
            assertEquals(0, captured[0].users);
            assertNull(request.getAttribute(SandboxRegistry.REQUEST_ENTRY));
            assertEquals(Boolean.TRUE, request.getSession(false).getAttribute(SandboxRegistry.SESSION_STARTED));

            MockHttpServletRequest failed = request("GET", path);
            dispatch(failed, (req, res) -> ((MockHttpServletResponse) res).setStatus(406));
            assertNoSandbox((MockHttpSession) failed.getSession(false));
            assertNull(failed.getAttribute(SandboxRegistry.REQUEST_ENTRY));

            MockHttpServletRequest descendant = request("GET", path + "/unknown");
            assertEquals(404, dispatch(descendant, (req, res) -> fail("Uninitialized descendant reached MVC")).getStatus());
            assertNull(descendant.getSession(false));
        }
    }

    @Test void presentationDoesNotConsumeCapacityAndStillWorksWhenRegistryIsFull() throws Exception {
        for (int i = 0; i < SandboxRegistry.MAX_VISITORS + 1; i++) {
            MockHttpServletRequest request = request("GET", "/demos/components/forms");
            assertEquals(200, dispatch(request, (req, res) -> {}).getStatus());
            assertNoSandbox((MockHttpSession) request.getSession(false));
        }
        for (int i = 0; i < SandboxRegistry.MAX_VISITORS; i++) {
            assertEquals(200, dispatch(request("GET", "/demos/features/editing"), (req, res) -> {}).getStatus());
        }
        assertEquals(503, dispatch(request("GET", "/demos/features/forum"),
            (req, res) -> fail("Capacity failure reached MVC")).getStatus());
        MockHttpServletRequest presentation = request("GET", "/demos/components/forms");
        assertEquals(200, dispatch(presentation, (req, res) -> {}).getStatus());
        assertNoSandbox((MockHttpSession) presentation.getSession(false));
    }
}
