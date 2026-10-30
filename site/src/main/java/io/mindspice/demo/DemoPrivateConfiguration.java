package io.mindspice.demo;

import io.mindspice.demo.chat.ChatDemoService;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
public class DemoPrivateConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(DemoPrivateConfiguration.class);
    private final Environment environment;

    public DemoPrivateConfiguration(Environment environment) { this.environment = environment; }

    @Bean
    ServletListenerRegistrationBean<HttpSessionListener> privateChatSessionCleanup(ChatDemoService chat, SandboxRegistry sandboxes) {
        return new ServletListenerRegistrationBean<>(new HttpSessionListener() {
            @Override
            public void sessionDestroyed(HttpSessionEvent event) {
                String owner = event.getSession().getId();
                // Servlet destruction listeners run while the session can still accept attributes.
                event.getSession().setAttribute(SandboxRegistry.SESSION_DESTROYED, Boolean.TRUE);
                chat.sessionDestroyed(owner);
                sandboxes.sessionDestroyed(owner);
            }
        });
    }

    @Bean
    FilterRegistrationBean<Filter> privateChatNoStore() {
        FilterRegistrationBean<Filter> filter = new FilterRegistrationBean<>((request, response, chain) -> {
            ((HttpServletResponse) response).setHeader("Cache-Control", "no-store");
            chain.doFilter(request, response);
        });
        // Include negotiation/method/mapping failures, before MVC chooses a handler.
        filter.addUrlPatterns("/chat", "/chat/*");
        return filter;
    }

    @Bean
    FilterRegistrationBean<SandboxSecurityFilter> sandboxSecurity(SandboxRegistry sandboxes) {
        FilterRegistrationBean<SandboxSecurityFilter> filter = new FilterRegistrationBean<>(new SandboxSecurityFilter(sandboxes));
        filter.setOrder(-100);
        filter.addUrlPatterns("/*");
        return filter;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warnPrivateDemo() {
        LOG.warn("TEMPORARY SITE SANDBOXES: session ownership is NOT authentication. "
            + "Flags/modes/approval are simulations. Site-local origin/CSRF checks are not rate protection or production security guarantees.");
        String address = environment.getProperty("server.address");
        if (!loopback(address)) LOG.warn("NON-LOOPBACK PRIVATE DEMO BIND OVERRIDE: {}. "
            + "Network exposure is operator-owned; do not treat this demo as a secure deployment.", address);
    }

    private boolean loopback(String address) {
        if (address == null || address.isBlank()) return false;
        try { return InetAddress.getByName(address).isLoopbackAddress(); }
        catch (UnknownHostException invalid) { return false; }
    }
}
