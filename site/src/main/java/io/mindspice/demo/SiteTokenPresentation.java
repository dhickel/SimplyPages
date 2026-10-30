package io.mindspice.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.owasp.encoder.Encode;

import java.util.regex.Pattern;

/** Adds the already prepared request token without reparsing native textarea content. */
@ControllerAdvice(basePackages = "io.mindspice.demo")
public class SiteTokenPresentation implements ResponseBodyAdvice<String> {
    private static final Pattern OPEN_TAG = Pattern.compile("<(body|form|[a-z][a-z0-9-]*)(\\s[^<>]*?)?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern POST = Pattern.compile("hx-post=\"([^\"]+)\"");

    @Override public boolean supports(MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) {
        return method.getParameterType() == String.class;
    }

    @Override public String beforeBodyWrite(String body, MethodParameter method, MediaType type,
        Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest serverRequest, ServerHttpResponse response) {
        if (body == null || !type.isCompatibleWith(MediaType.TEXT_HTML)
            || !(serverRequest instanceof ServletServerHttpRequest servlet)) return body;
        HttpServletRequest request = servlet.getServletRequest();
        // Only the security filter may prepare tokens. Non-sandbox pages never allocate one here.
        if (request.getSession(false) == null
            || request.getSession(false).getAttribute(SandboxSecurityFilter.TOKEN_ATTRIBUTE) == null) return body;
        String path = request.getRequestURI();
        if (!(path.startsWith("/demos") || path.startsWith("/editing-demo") || path.startsWith("/forum") || path.startsWith("/chat"))) return body;
        if ("true".equalsIgnoreCase(request.getHeader("HX-Request")) && body.isEmpty()) return body;
        String token = SandboxSecurityFilter.requestToken(request);
        return decorate(body, token);
    }

    static String decorate(String html, String token) {
        String header = " hx-headers=\"" + Encode.forHtmlAttribute("{\"X-Sandbox-CSRF\":\"" + token + "\"}") + "\"";
        var tags = OPEN_TAG.matcher(html);
        StringBuilder result = new StringBuilder();
        while (tags.find()) {
            String tag = tags.group();
            boolean form = tags.group(1).equalsIgnoreCase("form");
            if ((form || tags.group(1).equalsIgnoreCase("body") || tag.contains("hx-")) && !tag.contains("hx-headers="))
                tag = tag.substring(0, tag.length() - 1) + header + ">";
            if (form) {
                var post = POST.matcher(tag);
                boolean hxPost = post.find();
                if (!tag.contains(" action=") && hxPost) {
                    String method = tag.toLowerCase(java.util.Locale.ROOT).contains(" method=") ? "" : " method=\"post\"";
                    tag = tag.substring(0, tag.length() - 1) + " action=\"" + post.group(1) + "\"" + method + ">";
                }
                // GET forms must not leak a token into the query string.
                if (hxPost || tag.matches("(?is).*\\bmethod=\"post\".*"))
                    tag += "<input type=\"hidden\" name=\"_csrf\" value=\"" + Encode.forHtmlAttribute(token) + "\">";
            }
            tags.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(tag));
        }
        tags.appendTail(result);
        return result.toString();
    }
}
