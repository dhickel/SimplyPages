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
    private volatile boolean built = false;
    /** Lock guarding first-build and explicit rebuild transitions. */
    private final Object buildLock = new Object();

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
     * Replaces built structure immediately, restoring entry-state children, attributes, cached id
     * and build status if composition throws a {@link RuntimeException} or {@link Error}.
     *
     * <p>The snapshot is shallow and includes caller changes made before this call. It does not
     * restore module id, title, text/slots, subclass fields, descendants or external effects.
     * A previously built module remains built after failure; a never-built module remains retryable.</p>
     */
    protected void rebuildContent() {
        synchronized (buildLock) {
            doBuildLocked(true);
        }
    }

    /**
     * Ensures module structure is built once. Failed initial builds restore shallow child and
     * attribute collections and cached id so a later call can retry. Subclasses own rollback of
     * all other state and side effects; see {@link #rebuildContent()} for the explicit rebuild boundary.
     */
    public Module build() {
        if (!built) {
            synchronized (buildLock) {
                if (!built) {
                    doBuildLocked(false);
                }
            }
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
     * Build implementation that must run while holding {@link #buildLock}.
     */
    private void doBuildLocked(boolean rebuild) {
        var originalChildren = new java.util.ArrayList<>(children);
        var originalAttributes = new java.util.ArrayList<>(attributes);
        String originalId = id;
        boolean originallyBuilt = built;
        try {
            if (rebuild) {
                children.clear();
                built = false;
            }
            ensureModuleClass();
            buildContent();
            built = true;
        } catch (RuntimeException | Error failure) {
            children.clear();
            children.addAll(originalChildren);
            attributes.clear();
            attributes.addAll(originalAttributes);
            id = originalId;
            built = originallyBuilt;
            throw failure;
        }
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
