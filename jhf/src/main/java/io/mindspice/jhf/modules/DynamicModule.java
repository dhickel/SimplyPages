package io.mindspice.jhf.modules;

import io.mindspice.jhf.components.TextNode;
import io.mindspice.jhf.core.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collections;

/**
 * Abstract base class for modules that support cached static content with dynamic injection.
 *
 * <p>DynamicModule optimizes rendering performance by separating static structure from
 * dynamic content. The static structure (including placeholders) is rendered once and cached
 * as a template string. Subsequent renders with dynamic content simply inject the new
 * components into the existing template string, avoiding full component tree reconstruction.</p>
 *
 * <h2>Usage</h2>
 * <ol>
 *   <li>Extend {@code DynamicModule}</li>
 *   <li>Implement {@link #buildContent()} normally</li>
 *   <li>Use {@link #createDynamicPlaceholder(String)} to create placeholders</li>
 *   <li>Place these placeholders anywhere in your component tree</li>
 *   <li>Use {@link #renderWithDynamic(DynamicContent...)} to render with updates</li>
 * </ol>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public class UserStatsModule extends DynamicModule {
 *     @Override
 *     protected void buildContent() {
 *         withChild(new Header.H2("User Statistics")); // Static
 *
 *         // Nested placeholder
 *         Div container = new Div().withClass("stats-container");
 *         container.withChild(createDynamicPlaceholder("stats-grid"));
 *         withChild(container);
 *     }
 * }
 *
 * // Usage
 * // First render builds and caches the template
 * String html = module.renderWithDynamic(
 *     DynamicContent.of("stats-grid", new StatsGrid(userData))
 * );
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>The template is cached in the instance. If the module instance is reused across
 * threads (e.g., as a singleton), initialization is thread-safe via double-checked locking.
 * {@code renderWithDynamic} is thread-safe as it operates on the immutable template string.</p>
 */
public abstract class DynamicModule extends Module {

    private static final String PLACEHOLDER_PREFIX = "{{__JHF_DYN__";
    private static final String PLACEHOLDER_SUFFIX = "__}}";

    private volatile String cachedTemplate;
    private final Set<String> registeredTags = Collections.synchronizedSet(new HashSet<>());
    private final Object lock = new Object();

    protected DynamicModule(String tagName) {
        super(tagName);
    }

    /**
     * Creates a placeholder component for dynamic content.
     *
     * <p>Use this method within {@link #buildContent()} to generate a component
     * that marks a location for dynamic injection. This component can be nested
     * anywhere within the module's structure.</p>
     *
     * @param tag the unique identifier for this dynamic section
     * @return a Component representing the placeholder
     */
    protected Component createDynamicPlaceholder(String tag) {
        registeredTags.add(tag);
        return new TextNode(getPlaceholder(tag));
    }

    /**
     * Adds a placeholder for dynamic content as a direct child.
     * Convenience method for when nesting is not required.
     *
     * @param tag the unique identifier for this dynamic section
     */
    protected void addDynamic(String tag) {
        super.withChild(createDynamicPlaceholder(tag));
    }

    /**
     * Renders the module with the provided dynamic content updates.
     *
     * <p>If the static template hasn't been built yet, this triggers {@link #render()}
     * to build and cache it. Then it substitutes the placeholders with the rendered
     * HTML of the provided dynamic components.</p>
     *
     * @param updates the dynamic content to inject
     * @return the fully rendered HTML string
     * @throws IllegalArgumentException if an update references a non-existent tag
     */
    public String renderWithDynamic(List<DynamicContent> updates) {
        // Ensure template is built and cached
        if (cachedTemplate == null) {
            render();
        }

        String result = cachedTemplate;
        for (DynamicContent update : updates) {
            if (!registeredTags.contains(update.tag())) {
                throw new IllegalArgumentException("Unknown dynamic tag: " + update.tag());
            }
            String replacementHtml = update.component().render();
            result = result.replace(getPlaceholder(update.tag()), replacementHtml);
        }
        return result;
    }

    /**
     * Renders the module with the provided dynamic content updates.
     * Varargs convenience method.
     *
     * @param updates the dynamic content to inject
     * @return the fully rendered HTML string
     */
    public String renderWithDynamic(DynamicContent... updates) {
        return renderWithDynamic(Arrays.asList(updates));
    }

    /**
     * Renders the static template of this module.
     *
     * <p>This method builds the component tree (if not already built) and generates
     * the HTML string containing placeholders. The result is cached for future calls.</p>
     *
     * <p><strong>Note:</strong> Calling this directly returns the template WITH
     * placeholders (e.g. {@code {{__JHF_DYN__tag__}}}). Use {@link #renderWithDynamic}
     * for the final output.</p>
     *
     * @return the cached static template string
     */
    @Override
    public String render() {
        if (cachedTemplate != null) {
            return cachedTemplate;
        }

        synchronized (lock) {
            if (cachedTemplate != null) {
                return cachedTemplate;
            }

            // Module.render() logic duplication to control lifecycle:
            // We want to persist the 'children' list implicitly by relying on the fact
            // that we only call buildContent() once.
            // However, super.render() (Module.render) clears children!
            // We must rely on Module.render() calling buildContent() internally.

            // Let Module.render() do its work (clearing children, calling buildContent, rendering)
            // We just capture the result.
            cachedTemplate = super.render();
            return cachedTemplate;
        }
    }

    private String getPlaceholder(String tag) {
        return PLACEHOLDER_PREFIX + tag + PLACEHOLDER_SUFFIX;
    }
}
