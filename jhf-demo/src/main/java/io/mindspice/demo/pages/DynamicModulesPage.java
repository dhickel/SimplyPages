package io.mindspice.demo.pages;

import io.mindspice.jhf.components.Div;
import io.mindspice.jhf.components.Header;
import io.mindspice.jhf.components.Markdown;
import io.mindspice.jhf.components.Paragraph;
import io.mindspice.jhf.layout.Page;
import io.mindspice.jhf.modules.DynamicContent;
import io.mindspice.jhf.modules.DynamicModule;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Demonstrates the DynamicModule system for caching static content.
 */
@Component
public class DynamicModulesPage implements DemoPage {

    // Define a static instance of the module to simulate global caching
    // In a real app, this could be a singleton bean or stored in a cache manager
    private static final ServerStatusModule SERVER_STATUS_MODULE = new ServerStatusModule();

    @Override
    public String render() {
        return Page.builder()
                .addComponents(Header.H1("Dynamic Modules"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        ## Cached Static Content

                        This page demonstrates the `DynamicModule` system. The module below has its
                        static structure (title, layout, labels) built and cached once.

                        On each request, only the dynamic values (Time, Request Count, Active Users)
                        are injected into the cached template string.

                        ### How it works
                        1. `ServerStatusModule` extends `DynamicModule`
                        2. `buildContent()` runs once, creating the structure with placeholders
                        3. `renderWithDynamic()` injects fresh data into the cached template
                        """)))

                .addComponents(Header.H2("Live Example"))
                .addRow(row -> {
                    // Render the cached module with fresh dynamic data
                    String renderedHtml = SERVER_STATUS_MODULE.renderWithDynamic(
                            DynamicContent.of("time", new Paragraph()
                                    .withClass("text-xl font-bold")
                                    .withInnerText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))),

                            DynamicContent.of("requests", new Div()
                                    .withClass("stat-value text-xl font-bold")
                                    .withInnerText(String.valueOf((int) (Math.random() * 1000) + 500))),

                            DynamicContent.of("users", new Div()
                                    .withClass("stat-value text-xl font-bold")
                                    .withInnerText(String.valueOf((int) (Math.random() * 50) + 10)))
                    );

                    // We need to wrap the string result in a component to add it to the row
                    // Using a Div with unsafe HTML (trusted because it comes from our module)
                    row.withChild(new Div().withUnsafeHtml(renderedHtml));
                })
                .build()
                .render();
    }

    /**
     * Example Dynamic Module that displays server stats.
     * The structure is static, but values are dynamic.
     */
    public static class ServerStatusModule extends DynamicModule {

        public ServerStatusModule() {
            super("div");
            this.withClass("server-status-module p-6 border rounded shadow-md bg-white");
            this.withTitle("Server Status Monitor");
        }

        @Override
        protected void buildContent() {
            // Static Title
            if (title != null) {
                // Reordered: withClass first, then wrapped in withChild
                withChild(Header.H2(title).withClass("mb-4 border-b pb-2"));
            }

            // Static Layout with Dynamic Placeholders
            withChild(new Div().withClass("grid grid-cols-3 gap-4")
                    .withChild(createStatBox("Current Time", "time"))
                    .withChild(createStatBox("Total Requests", "requests"))
                    .withChild(createStatBox("Active Users", "users"))
            );

            withChild(new Paragraph()
                    .withClass("mt-4 text-sm text-gray-500 italic")
                    .withInnerText("This footer is static and cached."));
        }

        private io.mindspice.jhf.core.Component createStatBox(String label, String dynamicTag) {
            Div box = new Div().withClass("stat-box bg-gray-50 p-4 rounded text-center");

            // Static Label
            box.withChild(new Div()
                    .withClass("text-gray-600 text-sm uppercase mb-2")
                    .withInnerText(label));

            // Dynamic Placeholder
            box.withChild(createDynamicPlaceholder(dynamicTag));

            return box;
        }
    }
}
