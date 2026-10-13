package io.mindspice.demo.integration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.mindspice.demo.DemoPrivateConfiguration;
import io.mindspice.demo.chat.ChatDemoService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionListener;
import org.apache.catalina.Context;
import org.apache.catalina.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Real Boot precedence and real Tomcat destruction, with no non-loopback listener or test endpoint. */
class DemoPrivateConfigurationTest {
    private static final String PRIVATE_WARNING = "PRIVATE NON-DURABLE DEMO";
    private static final String EXPOSURE_WARNING = "NON-LOOPBACK PRIVATE DEMO BIND OVERRIDE";

    @Test
    void packagedDefaultWinsOverSpringDefaultsAndDoesNotChangeDefaultPort() {
        try (Warnings warnings = new Warnings()) {
            SpringApplication application = application(WebApplicationType.NONE, warnings, Map.of());
            application.setDefaultProperties(Map.of("server.address", "0.0.0.0"));
            try (ConfigurableApplicationContext context = application.run()) {
                assertEquals("127.0.0.1", context.getEnvironment().getProperty("server.address"));
                ServerProperties server = Binder.get(context.getEnvironment()).bind("server", ServerProperties.class).get();
                assertTrue(server.getAddress().isLoopbackAddress());
                assertNull(server.getPort(), "An absent port property must remain unspecified for Boot's factory default");
                assertEquals(8080, new TomcatServletWebServerFactory().getPort());
                assertNull(context.getEnvironment().getProperty("server.port"), "The demo must not impose a port property");
                warnings.assertPrivateOnly();
                assertTrue(warnings.messages().getFirst().contains("NOT authentication"));
                assertTrue(warnings.messages().getFirst().contains("NO CSRF"));
                assertTrue(warnings.messages().getFirst().contains("rate protection"));
                assertTrue(warnings.messages().getFirst().contains("shared editing sandbox"));
                assertTrue(warnings.messages().getFirst().contains("Flags/modes/approval are simulations"));
            }
        }
    }

    @Test
    void environmentAddressAndPortOverrideRemainOrdinarySpringProperties() {
        try (Warnings warnings = new Warnings()) {
            SpringApplication application = application(WebApplicationType.NONE, warnings,
                Map.of("SERVER_ADDRESS", "0.0.0.0", "SERVER_PORT", "9137"));
            try (ConfigurableApplicationContext context = application.run()) {
                assertEquals("0.0.0.0", context.getEnvironment().getProperty("server.address"));
                assertEquals("9137", context.getEnvironment().getProperty("server.port"));
                ServerProperties server = Binder.get(context.getEnvironment()).bind("server", ServerProperties.class).get();
                assertEquals(InetAddress.getByName("0.0.0.0"), server.getAddress());
                assertEquals(9137, server.getPort());
                assertFalse(context instanceof ServletWebServerApplicationContext, "Warning tests must never bind a non-loopback server");
                warnings.assertExposed();
            }
        } catch (java.net.UnknownHostException impossibleNumericAddress) {
            throw new AssertionError(impossibleNumericAddress);
        }
    }

    @Test
    void commandLineAddressAndPortWinOverEnvironmentWithoutOpeningListener() throws Exception {
        try (Warnings warnings = new Warnings()) {
            SpringApplication application = application(WebApplicationType.NONE, warnings,
                Map.of("SERVER_ADDRESS", "127.0.0.1", "SERVER_PORT", "9137"));
            try (ConfigurableApplicationContext context = application.run("--server.address=192.0.2.17", "--server.port=9248")) {
                assertEquals("192.0.2.17", context.getEnvironment().getProperty("server.address"));
                assertEquals("9248", context.getEnvironment().getProperty("server.port"));
                ServerProperties server = Binder.get(context.getEnvironment()).bind("server", ServerProperties.class).get();
                assertEquals(InetAddress.getByName("192.0.2.17"), server.getAddress());
                assertEquals(9248, server.getPort());
                assertFalse(context instanceof ServletWebServerApplicationContext);
                warnings.assertExposed();
            }
        }
    }

    @Test
    void explicitLoopbackAndPortOverrideAreHonoredWithoutExposureWarning() {
        try (Warnings warnings = new Warnings()) {
            SpringApplication application = application(WebApplicationType.NONE, warnings, Map.of());
            try (ConfigurableApplicationContext context = application.run("--server.address=127.0.0.2", "--server.port=9359")) {
                assertEquals("127.0.0.2", context.getEnvironment().getProperty("server.address"));
                assertEquals("9359", context.getEnvironment().getProperty("server.port"));
                ServerProperties server = Binder.get(context.getEnvironment()).bind("server", ServerProperties.class).get();
                assertTrue(server.getAddress().isLoopbackAddress());
                assertEquals(9359, server.getPort());
                warnings.assertPrivateOnly();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "127.0.0.2", "::1", "localhost"})
    void recognizedLoopbackAddressesStillAlwaysWarnPrivate(String address) {
        try (Warnings warnings = new Warnings()) {
            warnings.attach();
            new DemoPrivateConfiguration(new MockEnvironment().withProperty("server.address", address)).warnPrivateDemo();
            warnings.assertPrivateOnly();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0", "::", "192.0.2.17", "", " ", "300.300.300.300"})
    void nonLoopbackBlankAndInvalidAddressesAreConspicuouslyWarnedInMemory(String address) {
        try (Warnings warnings = new Warnings()) {
            warnings.attach();
            new DemoPrivateConfiguration(new MockEnvironment().withProperty("server.address", address)).warnPrivateDemo();
            warnings.assertExposed();
        }
    }

    @Test
    void missingAddressDoesNotSilentlyClaimLoopback() {
        try (Warnings warnings = new Warnings()) {
            warnings.attach();
            new DemoPrivateConfiguration(new MockEnvironment()).warnPrivateDemo();
            warnings.assertExposed();
        }
    }

    @Test
    void realEmbeddedTomcatInvalidationCallsRegisteredListenerAndReleasesOnlyDestroyedSession() throws Exception {
        ChatDemoService service;
        List<AtomicInteger> completions = new ArrayList<>();
        try (Warnings warnings = new Warnings()) {
            SpringApplication application = application(WebApplicationType.SERVLET, warnings, Map.of());
            // The only actual listener in this suite binds the packaged loopback default on an owned ephemeral port.
            try (ConfigurableApplicationContext context = application.run("--server.port=0")) {
                assertInstanceOf(ServletWebServerApplicationContext.class, context);
                ServletWebServerApplicationContext web = (ServletWebServerApplicationContext) context;
                TomcatWebServer server = assertInstanceOf(TomcatWebServer.class, web.getWebServer());
                InetAddress bound = (InetAddress) server.getTomcat().getConnector().getProtocolHandler().getClass()
                    .getMethod("getAddress").invoke(server.getTomcat().getConnector().getProtocolHandler());
                assertEquals(InetAddress.getByName("127.0.0.1"), bound);
                assertTrue(server.getPort() > 0);
                assertEquals("127.0.0.1", context.getEnvironment().getProperty("server.address"));
                warnings.assertPrivateOnly();
                service = context.getBean(ChatDemoService.class);
                try (var client = java.net.http.HttpClient.newHttpClient()) {
                for (var endpoint : Map.of("/chat/new", 405, "/chat/history?conversationId=unknown", 406,
                        "/chat/missing", 404).entrySet()) {
                    var http = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:" + server.getPort() + endpoint.getKey()))
                        .header("Accept", endpoint.getValue() == 406 ? "application/json" : "text/html").GET().build();
                    var response = client.send(http, java.net.http.HttpResponse.BodyHandlers.ofString());
                    assertEquals(endpoint.getValue(), response.statusCode());
                    assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
                }
                }
                assertEquals(0, count(service, "conversationCount"));
                ServletListenerRegistrationBean<?> bean = context.getBean(ServletListenerRegistrationBean.class);
                assertInstanceOf(HttpSessionListener.class, bean.getListener());
                Context servlet = (Context) server.getTomcat().getHost().findChildren()[0];
                assertTrue(List.of(servlet.getApplicationLifecycleListeners()).contains(bean.getListener()),
                    "Boot must actually register the production listener in the embedded servlet container");
                Session firstManaged = servlet.getManager().createSession(null);
                Session secondManaged = servlet.getManager().createSession(null);
                HttpSession first = firstManaged.getSession();
                HttpSession second = secondManaged.getSession();
                assertFalse(first instanceof org.springframework.mock.web.MockHttpSession);
                String firstId = service.initialize(first, false).conversationId();
                String secondId = service.initialize(second, false).conversationId();
                completions.add(installTransport(service.stream(first, firstId)));
                completions.add(installTransport(service.stream(first, firstId)));
                completions.add(installTransport(service.stream(second, secondId)));
                assertEquals(2, count(service, "conversationCount"));
                assertEquals(3, count(service, "streamCount"));
                // Genuine StandardSession.invalidate(), not a direct service/listener invocation or logout endpoint.
                first.invalidate();
                assertFalse(firstManaged.isValid());
                assertEquals(1, count(service, "conversationCount"));
                assertEquals(1, count(service, "streamCount"));
                assertEquals(1, completions.get(0).get());
                assertEquals(1, completions.get(1).get());
                assertEquals(0, completions.get(2).get());
                assertEquals(secondId, service.history(second, secondId).conversationId());
                assertEquals(1, service.history(second, secondId).messages().size());
                second.invalidate();
                assertFalse(secondManaged.isValid());
                assertEquals(0, count(service, "conversationCount"));
                assertEquals(0, count(service, "streamCount"));
                assertEquals(1, completions.get(2).get());
                assertEquals(0, servlet.getManager().getActiveSessions());
            }
            assertEquals(0, count(service, "conversationCount"));
            assertEquals(0, count(service, "streamCount"));
            Method stopped = ChatDemoService.class.getDeclaredMethod("cleanupStopped");
            stopped.setAccessible(true);
            assertEquals(true, stopped.invoke(service));
            for (AtomicInteger completion : completions) assertEquals(1, completion.get());
        }
    }

    private static SpringApplication application(WebApplicationType type, Warnings warnings, Map<String, Object> environmentValues) {
        SpringApplication application = new SpringApplication(PrivateTestApplication.class);
        application.setWebApplicationType(type);
        application.setRegisterShutdownHook(false);
        application.setLogStartupInfo(false);
        application.setBannerMode(org.springframework.boot.Banner.Mode.OFF);
        StandardEnvironment environment = new StandardEnvironment();
        // Isolate host environment; retain Spring's real SERVER_* relaxed environment mapping/precedence.
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("testEnvironment", environmentValues));
        application.setEnvironment(environment);
        // Logging has initialized by this event; warnings are emitted at the subsequent ApplicationReadyEvent.
        application.addListeners((org.springframework.context.ApplicationListener<ApplicationStartedEvent>) event -> warnings.attach());
        return application;
    }

    private static int count(ChatDemoService service, String name) throws Exception {
        Method method = ChatDemoService.class.getDeclaredMethod(name);
        method.setAccessible(true);
        return (int) method.invoke(service);
    }

    private static AtomicInteger installTransport(SseEmitter emitter) throws Exception {
        AtomicInteger completions = new AtomicInteger();
        Class<?> handler = Class.forName(ResponseBodyEmitter.class.getName() + "$Handler");
        Object transport = Proxy.newProxyInstance(handler.getClassLoader(), new Class<?>[]{handler}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "complete" -> completions.incrementAndGet();
                case "completeWithError" -> fail("Unexpected SSE transport failure");
                case "send", "onTimeout", "onError", "onCompletion" -> { }
                case "toString" -> { return "InMemoryServletTransport"; }
                case "hashCode" -> { return System.identityHashCode(proxy); }
                case "equals" -> { return proxy == args[0]; }
                default -> throw new AssertionError("Unexpected transport method " + method.getName());
            }
            return null;
        });
        Method initialize = ResponseBodyEmitter.class.getDeclaredMethod("initialize", handler);
        initialize.setAccessible(true);
        initialize.invoke(emitter, transport);
        return completions;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({DemoPrivateConfiguration.class, io.mindspice.demo.ChatDemoController.class,
        io.mindspice.demo.pages.ChatDemoPage.class})
    static class PrivateTestApplication {
        @Bean ChatDemoService chatDemoService() { return new ChatDemoService(); }
    }

    private static final class Warnings implements AutoCloseable {
        private final Logger logger = (Logger) LoggerFactory.getLogger(DemoPrivateConfiguration.class);
        private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        private boolean attached;

        void attach() {
            if (attached) return;
            appender.start();
            logger.addAppender(appender);
            attached = true;
        }
        List<String> messages() { return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList(); }
        void assertPrivateOnly() {
            assertEquals(1, appender.list.size());
            assertEquals(Level.WARN, appender.list.getFirst().getLevel());
            assertTrue(messages().getFirst().contains(PRIVATE_WARNING));
            assertFalse(messages().getFirst().contains(EXPOSURE_WARNING));
        }
        void assertExposed() {
            assertEquals(2, appender.list.size());
            assertTrue(appender.list.stream().allMatch(e -> e.getLevel() == Level.WARN));
            assertTrue(messages().get(0).contains(PRIVATE_WARNING));
            assertTrue(messages().get(1).contains(EXPOSURE_WARNING));
            assertTrue(messages().get(1).contains("Network exposure is operator-owned"));
            assertTrue(messages().get(1).contains("not treat this demo as a secure deployment"));
        }
        @Override public void close() {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
