package io.mindspice.demo;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;

/** Runs before MVC, including mapping/negotiation errors. Never allocates on rejected writes. */
public final class SandboxSecurityFilter implements Filter {
    public static final String TOKEN_ATTRIBUTE = "site.sandbox.csrf";
    public static final String TOKEN_HEADER = "X-Sandbox-CSRF";
    public static final String TOKEN_PARAMETER = "_csrf";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String[] VIEWER_ATTRIBUTES = {
        ForumDemoController.SESSION_VIEWER_ID, ForumDemoController.SESSION_VIEWER_NAME,
        ForumDemoController.SESSION_VIEWER_MODERATOR
    };
    private final SandboxRegistry sandboxes;

    public SandboxSecurityFilter(SandboxRegistry sandboxes) { this.sandboxes = sandboxes; }

    @Override
    public void doFilter(ServletRequest rawRequest, ServletResponse rawResponse, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) rawRequest;
        HttpServletResponse response = (HttpServletResponse) rawResponse;
        final String path;
        try { path = routePath(request); }
        catch (IllegalArgumentException invalidPath) {
            response.setHeader("Cache-Control", "no-store");
            reject(response, 400, "Invalid request path.");
            return;
        }
        if (!protectedPath(path)) { chain.doFilter(request, response); return; }
        response.setHeader("Cache-Control", "no-store");
        String method = request.getMethod();
        boolean safe = "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
        if ((!safe && (!sameOrigin(request, true) || !validToken(request)))
            || (safe && !sameOrigin(request, false))) {
            reject(response, 403, "Sandbox origin or CSRF verification failed.");
            return;
        }
        if ("/sandbox/token".equals(path)) {
            if (!"GET".equals(method)) { reject(response, 405, "Use GET to obtain a sandbox token."); return; }
            String token = token(request.getSession());
            response.setHeader(TOKEN_HEADER, token);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"token\":\"" + token + "\",\"header\":\"" + TOKEN_HEADER
                + "\",\"parameter\":\"" + TOKEN_PARAMETER + "\"}");
            return;
        }
        boolean reset = "/sandbox/new".equals(path) && "POST".equals(method);
        boolean stateRoute = path.equals("/editing-demo") || path.startsWith("/editing-demo/")
            || path.equals("/forum") || path.startsWith("/forum/");
        boolean initial = "GET".equals(method) && (path.equals("/editing-demo") || path.equals("/forum"));
        // Chat HX navigation must remain allocation-free. Chat APIs retain their own ownership checks.
        boolean chatPage = "GET".equals(method) && path.equals("/chat")
            && !"true".equalsIgnoreCase(request.getHeader("HX-Request"));
        if (initial || chatPage) {
            response.setHeader(TOKEN_HEADER, token(request.getSession()));
        } else if (request.getSession(false) != null) {
            Object existing = request.getSession(false).getAttribute(TOKEN_ATTRIBUTE);
            if (existing instanceof String value) response.setHeader(TOKEN_HEADER, value);
        }
        if (!stateRoute && !reset) { chain.doFilter(request, response); return; }
        HttpSession session = request.getSession(false);
        SandboxRegistry.Entry entry;
        try {
            entry = sandboxes.acquire(session, initial, reset);
        } catch (ResponseStatusException failure) {
            reject(response, failure.getStatusCode().value(), failure.getReason());
            return;
        }
        boolean completed = false;
        Object[] previousViewer = null;
        try {
            request.setAttribute(SandboxRegistry.REQUEST_ENTRY, entry);
            if (reset) {
                previousViewer = Arrays.stream(VIEWER_ATTRIBUTES).map(session::getAttribute).toArray();
                for (String attribute : VIEWER_ATTRIBUTES) session.removeAttribute(attribute);
                response.setStatus(204);
            } else if (path.equals("/forum") || path.startsWith("/forum/")) {
                synchronized (entry.forum) { chain.doFilter(request, response); }
            } else {
                // Editing already serializes lookup, mutation and rendered snapshots on its own monitor.
                chain.doFilter(request, response);
            }
            completed = true;
        } finally {
            request.removeAttribute(SandboxRegistry.REQUEST_ENTRY);
            boolean successful = completed && response.getStatus() < 400;
            try {
                if (reset && !successful && previousViewer != null) {
                    try {
                        for (int i = 0; i < VIEWER_ATTRIBUTES.length; i++) {
                            session.setAttribute(VIEWER_ATTRIBUTES[i], previousViewer[i]);
                        }
                    } catch (IllegalStateException invalidSession) { /* Destruction discards the persona too. */ }
                }
            } finally {
                sandboxes.release(entry, session, successful);
            }
        }
    }

    private static String routePath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = org.springframework.web.util.UriUtils.decode(
                request.getRequestURI().substring(request.getContextPath().length()), StandardCharsets.UTF_8);
        }
        // Match MVC's decoded path segments, not a raw URI that can hide a protected route.
        var segments = new java.util.ArrayDeque<String>();
        for (String segment : path.split("/")) {
            int matrix = segment.indexOf(';');
            if (matrix >= 0) segment = segment.substring(0, matrix);
            if (segment.equals("..")) { if (!segments.isEmpty()) segments.removeLast(); }
            else if (!segment.isEmpty() && !segment.equals(".")) segments.addLast(segment);
        }
        return "/" + String.join("/", segments);
    }

    private static boolean protectedPath(String path) {
        return path.equals("/editing-demo") || path.startsWith("/editing-demo/")
            || path.equals("/forum") || path.startsWith("/forum/")
            || path.equals("/chat") || path.startsWith("/chat/")
            || path.equals("/sandbox") || path.startsWith("/sandbox/")
            || path.startsWith("/demos/api/");
    }

    private static String token(HttpSession session) {
        synchronized (session) {
            Object existing = session.getAttribute(TOKEN_ATTRIBUTE);
            if (existing instanceof String value) return value;
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(TOKEN_ATTRIBUTE, token);
            return token;
        }
    }

    private static boolean validToken(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null || !(session.getAttribute(TOKEN_ATTRIBUTE) instanceof String expected)) return false;
            var headers = Collections.list(request.getHeaders(TOKEN_HEADER));
            String[] parameters = request.getParameterValues(TOKEN_PARAMETER);
            if (headers.size() > 1 || (parameters != null && parameters.length != 1)) return false;
            String supplied = headers.isEmpty() ? (parameters == null ? null : parameters[0]) : headers.getFirst();
            if (supplied == null || !equalToken(expected, supplied)) return false;
            return parameters == null || equalToken(expected, parameters[0]);
        } catch (IllegalStateException invalidSession) { return false; }
    }

    private static boolean equalToken(String expected, String supplied) {
        return supplied.length() == expected.length() && MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.US_ASCII), supplied.getBytes(StandardCharsets.US_ASCII));
    }

    private static boolean sameOrigin(HttpServletRequest request, boolean required) {
        String fetchSite = request.getHeader("Sec-Fetch-Site");
        if (fetchSite != null && !fetchSite.equals("same-origin") && !fetchSite.equals("none")) return false;
        var origins = Collections.list(request.getHeaders("Origin"));
        if (origins.size() > 1) return false;
        String source = origins.isEmpty() ? request.getHeader("Referer") : origins.getFirst();
        if (source == null) return !required;
        try {
            URI uri = URI.create(source);
            if (uri.getUserInfo() != null || uri.getHost() == null || uri.getFragment() != null
                || (!origins.isEmpty() && (uri.getRawQuery() != null
                    || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())))) return false;
            int port = uri.getPort() == -1 ? defaultPort(uri.getScheme()) : uri.getPort();
            return request.getScheme().equalsIgnoreCase(uri.getScheme())
                && request.getServerName().equalsIgnoreCase(uri.getHost()) && request.getServerPort() == port;
        } catch (IllegalArgumentException invalid) { return false; }
    }

    private static int defaultPort(String scheme) {
        if ("https".equalsIgnoreCase(scheme)) return 443;
        if ("http".equalsIgnoreCase(scheme)) return 80;
        return -1;
    }

    private static void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message == null ? "Sandbox request failed." : message);
    }
}
