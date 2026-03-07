package io.mindspice.simplypages.core;

import java.util.Optional;

/**
 * Base class for high-level composed components with build-once lifecycle semantics.
 *
 * <p>Lifecycle contract:</p>
 * <p>- callers configure mutable fields through fluent methods</p>
 * <p>- {@link #build()} invokes {@link #buildContent()} once per instance</p>
 * <p>- {@link #render(RenderContext)} guarantees build before rendering</p>
 *
 * <p>Dynamic-data boundary: {@link #buildContent()} should compose structure only. Per-request
 * dynamic values must flow through {@link SlotKey} and {@link Slot}.</p>
 *
 * <p>Mutability/thread-safety: mutable and not thread-safe while being configured. Mutate module
 * state in a request-scoped flow; for reuse, stop mutating after composition/build and render via
 * a stable structure (typically {@link Template}) with per-request {@link RenderContext} values.</p>
 */
public abstract class Module extends HtmlTag {

    /** Optional module id mirrored to HTML {@code id} attribute. */
    protected String moduleId;

    /** Optional title consumed by subclass build logic. */
    protected String title;

    /** Build guard enforcing idempotent {@link #build()} behavior. */
    private boolean built = false;

    /**
     * Creates a module backed by the provided container tag.
     */
    protected Module(String tagName) {
        super(tagName);
    }

    /**
     * Sets module id and synchronizes the underlying {@code id} attribute.
     */
    public Module withModuleId(String moduleId) {
        this.moduleId = moduleId;
        this.withAttribute("id", moduleId);
        return this;
    }

    /**
     * Returns the configured module id, or {@code null}.
     */
    public String getModuleId() {
        return this.moduleId;
    }

    /**
     * Returns the configured module title, or {@code null}.
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Sets optional title consumed by {@link #buildContent()} implementations.
     */
    public Module withTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Adds a class token; {@code module} base class is still enforced by build lifecycle.
     */
    @Override
    public Module withClass(String className) {
        super.addClass(className);
        return this;
    }

    /**
     * Disallowed on modules; width must be controlled by layout containers.
     *
     * @deprecated use layout containers to size modules
     * @throws UnsupportedOperationException always
     */
    @Override
    @Deprecated
    public Module withWidth(String width) {
        throw new UnsupportedOperationException("Modules should not set width directly. Use a container or layout to control module width.");
    }

    /**
     * Disallowed on modules; max-width must be controlled by layout containers.
     *
     * @deprecated use layout containers to size modules
     * @throws UnsupportedOperationException always
     */
    @Override
    @Deprecated
    public Module withMaxWidth(String maxWidth) {
        throw new UnsupportedOperationException("Modules should not set width directly. Use a container or layout to control module width.");
    }

    /**
     * Disallowed on modules; min-width must be controlled by layout containers.
     *
     * @deprecated use layout containers to size modules
     * @throws UnsupportedOperationException always
     */
    @Override
    @Deprecated
    public Module withMinWidth(String minWidth) {
        throw new UnsupportedOperationException("Modules should not set width directly. Use a container or layout to control module width.");
    }

    /**
     * Composes module structure.
     *
     * <p>Called at most once by {@link #build()} unless {@link #rebuildContent()} is invoked. Do
     * not rely on this method for per-request mutation.</p>
     */
    protected abstract void buildContent();

    /**
     * Clears children, resets build guard, and immediately rebuilds structure.
     */
    protected void rebuildContent() {
        children.clear();
        built = false;
        build();
    }

    /**
     * Ensures module structure is built once.
     */
    public Module build() {
        if (!built) {
            ensureModuleClass();
            buildContent();
            built = true;
        }
        return this;
    }

    /**
     * Builds if needed, then delegates rendering to {@link HtmlTag}.
     */
    @Override
    public String render(RenderContext context) {
        build();
        return super.render(context);
    }

    /**
     * Renders with {@link RenderContext#empty()}.
     */
    @Override
    public String render() {
        return render(RenderContext.empty());
    }

    /**
     * Ensures the mandatory {@code module} class token exists.
     */
    private void ensureModuleClass() {
        addClass("module");
    }

    /**
     * Returns first matching attribute value for maintenance diagnostics.
     */
    private Optional<String> getAttributeValue(String name) {
        return attributes.stream()
                .filter(attr -> name.equals(attr.name()))
                .map(Attribute::value)
                .findFirst();
    }
}
