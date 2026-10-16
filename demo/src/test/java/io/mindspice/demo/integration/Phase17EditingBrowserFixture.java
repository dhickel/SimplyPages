package io.mindspice.demo.integration;

import tools.jackson.databind.ObjectMapper;
import io.mindspice.demo.DemoApplication;
import io.mindspice.demo.EditingDemoController;
import io.mindspice.simplypages.editing.EditMode;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Test-scope launcher: arranges only the existing seeded LIST before the listener starts. */
public final class Phase17EditingBrowserFixture {
    private Phase17EditingBrowserFixture() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 6) throw new IllegalArgumentException("Expected port, content, edit, delete, mode, token");
        int port = Integer.parseInt(args[0]);
        if (port < 19000 || port > 19999) throw new IllegalArgumentException("Fixture ports must be 190xx-199xx");
        String content = new String(Base64.getDecoder().decode(args[1]), StandardCharsets.UTF_8);
        boolean canEdit = flag(args[2]);
        boolean canDelete = flag(args[3]);
        EditMode mode = EditMode.valueOf(args[4]);
        String token = args[5];
        if (!token.matches("[a-f0-9]{32}")) throw new IllegalArgumentException("Invalid readiness token");
        try (ServerSocket probe = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"))) {
            // Refuse a busy port; never attach to another process.
        }
        AtomicBoolean arranged = new AtomicBoolean();
        SpringApplication application = new SpringApplication(DemoApplication.class);
        application.addInitializers(context -> context.addBeanFactoryPostProcessor(factory ->
                factory.addBeanPostProcessor(new BeanPostProcessor() {
                    @Override
                    public Object postProcessAfterInitialization(Object bean, String name) {
                        if (bean instanceof EditingDemoController controller) {
                            if (!arranged.compareAndSet(false, true)) throw new IllegalStateException("Duplicate controller");
                            arrange(controller, content, canEdit, canDelete, mode);
                        }
                        return bean;
                    }
                })));
        application.addListeners(event -> {
            if (event instanceof WebServerInitializedEvent && !arranged.get()) {
                throw new IllegalStateException("Listener started before seed arrangement");
            }
        });
        var context = application.run("--server.address=127.0.0.1", "--server.port=" + port);
        if (!arranged.get()) {
            context.close();
            throw new IllegalStateException("Production controller not arranged");
        }
        // Parent pipe EOF also closes Spring if the test worker is terminated unexpectedly.
        Thread parent = new Thread(() -> {
            try {
                while (System.in.read() != -1) { }
            } catch (java.io.IOException ignored) {
                // A broken parent pipe has the same ownership meaning as EOF.
            } finally {
                try {
                    System.out.println("PHASE17_FIXTURE_STATE " + new ObjectMapper().writeValueAsString(
                            seedState(context.getBean(EditingDemoController.class))));
                    System.out.flush();
                } catch (Exception failure) {
                    failure.printStackTrace(System.err);
                } finally {
                    context.close();
                }
            }
        }, "phase17-fixture-parent");
        parent.setDaemon(true);
        parent.start();
        System.out.println("PHASE17_FIXTURE_READY " + token + " " + port);
        System.out.flush();
    }

    private static boolean flag(String value) {
        if (!value.equals("true") && !value.equals("false")) throw new IllegalArgumentException("Invalid flag");
        return Boolean.parseBoolean(value);
    }

    private static void arrange(EditingDemoController controller, String content, boolean canEdit,
            boolean canDelete, EditMode mode) {
        try {
            Map<?, ?> pages = (Map<?, ?>) field(controller, "pages").get(controller);
            Object page = pages.get("demo-page");
            int lists = 0;
            Object seed = null;
            for (Object row : (List<?>) field(page, "rows").get(page)) {
                for (Object module : (List<?>) field(row, "modules").get(row)) {
                    String kind = field(module, "kind").get(module).toString();
                    String id = (String) field(module, "id").get(module);
                    if (kind.equals("LIST")) {
                        lists++;
                        if (!id.equals("module-9")) throw new IllegalStateException("Unexpected LIST seed");
                        seed = module;
                    }
                }
            }
            if (lists != 1 || seed == null) throw new IllegalStateException("Expected only production module-9 LIST");
            field(seed, "content").set(seed, content);
            field(seed, "canEdit").setBoolean(seed, canEdit);
            field(seed, "canDelete").setBoolean(seed, canDelete);
            field(seed, "editMode").set(seed, mode);
            if (!field(seed, "title").get(seed).equals("Nested Editing Demo")
                    || field(seed, "width").getInt(seed) != 12 || !field(seed, "useMarkdown").getBoolean(seed)) {
                throw new IllegalStateException("Production unrelated seed fields changed");
            }
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot arrange current production seed", failure);
        }
    }

    private static Map<String, Object> seedState(EditingDemoController controller) throws ReflectiveOperationException {
        Map<?, ?> pages = (Map<?, ?>) field(controller, "pages").get(controller);
        Object page = pages.get("demo-page");
        for (Object row : (List<?>) field(page, "rows").get(page)) {
            for (Object module : (List<?>) field(row, "modules").get(row)) {
                if (field(module, "id").get(module).equals("module-9")) {
                    return Map.of("present", true, "kind", field(module, "kind").get(module).toString(),
                            "title", field(module, "title").get(module), "content", field(module, "content").get(module),
                            "width", field(module, "width").get(module), "useMarkdown", field(module, "useMarkdown").get(module),
                            "canEdit", field(module, "canEdit").get(module), "canDelete", field(module, "canDelete").get(module),
                            "mode", field(module, "editMode").get(module).toString());
                }
            }
        }
        return Map.of("present", false);
    }

    private static Field field(Object owner, String name) throws NoSuchFieldException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
