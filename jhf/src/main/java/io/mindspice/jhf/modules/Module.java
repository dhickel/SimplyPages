package io.mindspice.jhf.modules;

import io.mindspice.jhf.core.Attribute;
import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.Optional;

/**
 * Abstract base class for high-level reusable modules in the framework.
 *
 * <p>Modules are sophisticated components that combine multiple primitive components
 * to create complete functional units. While basic components (like {@code Div}, {@code Button})
 * represent individual HTML elements, modules represent entire page sections with
 * cohesive functionality.</p>
 *
 * <h2>What are Modules?</h2>
 * <p>Modules serve as building blocks for complex page layouts. They:</p>
 * <ul>
 *   <li><strong>Combine Primitives:</strong> Compose multiple basic components into functional units</li>
 *   <li><strong>Provide Structure:</strong> Enforce consistent layout and styling patterns</li>
 *   <li><strong>Encapsulate Behavior:</strong> Bundle related functionality together</li>
 *   <li><strong>Promote Reusability:</strong> Create once, use throughout your application</li>
 * </ul>
 *
 * <h2>Built-in Modules</h2>
 * <ul>
 *   <li><strong>ContentModule:</strong> Display formatted text and Markdown content</li>
 *   <li><strong>FormModule:</strong> Complete forms with structure and styling</li>
 *   <li><strong>GalleryModule:</strong> Image galleries with captions and layouts</li>
 *   <li><strong>DataModule:</strong> Type-safe data table displays</li>
 *   <li><strong>ForumModule:</strong> Discussion threads and post lists</li>
 * </ul>
 *
 * <h2>Module vs Component</h2>
 * <pre>
 * Component (Low-level):
 *   - Single HTML element
 *   - Minimal structure
 *   - Example: Button, TextInput, Paragraph
 *
 * Module (High-level):
 *   - Multiple components combined
 *   - Complex structure with layout
 *   - Example: ContentModule (title + markdown + styling)
 * </pre>
 *
 * <h2>Module Lifecycle</h2>
 * <p>Modules use lazy initialization:</p>
 * <ol>
 *   <li><strong>Construction:</strong> Module instance created, properties set</li>
 *   <li><strong>Configuration:</strong> Fluent methods configure module behavior</li>
 *   <li><strong>Render Call:</strong> {@link #render()} is invoked</li>
 *   <li><strong>Content Building:</strong> {@link #buildContent()} assembles components</li>
 *   <li><strong>HTML Generation:</strong> Final HTML string is generated</li>
 * </ol>
 *
 * <h2>Creating Custom Modules</h2>
 *
 * <h3>Example: Simple Custom Module</h3>
 * <pre>{@code
 * public class UserProfileModule extends Module {
 *     private User user;
 *
 *     public UserProfileModule() {
 *         super("div");
 *         this.withClass("user-profile-module");
 *     }
 *
 *     public static UserProfileModule create() {
 *         return new UserProfileModule();
 *     }
 *
 *     public UserProfileModule withUser(User user) {
 *         this.user = user;
 *         return this;
 *     }
 *
 *     @Override
 *     protected void buildContent() {
 *         // Add title if present
 *         if (title != null) {
 *             super.withChild(Header.H2(title).withClass("module-title"));
 *         }
 *
 *         // Build profile card
 *         Card card = Card.create()
 *             .withHeader(user.getName())
 *             .withBody(new Paragraph().withInnerText(user.getEmail()));
 *
 *         super.withChild(card);
 *     }
 * }
 *
 * // Usage:
 * UserProfileModule profile = UserProfileModule.create()
 *     .withTitle("User Profile")
 *     .withUser(currentUser);
 *
 * String html = profile.render();
 * }</pre>
 *
 * <h3>Example: Data-Driven Module</h3>
 * <pre>{@code
 * public class ResearchListModule extends Module {
 *     private List<ResearchPaper> papers;
 *     private boolean showAbstracts;
 *
 *     public ResearchListModule() {
 *         super("div");
 *         this.withClass("research-list-module");
 *     }
 *
 *     public static ResearchListModule create() {
 *         return new ResearchListModule();
 *     }
 *
 *     public ResearchListModule withPapers(List<ResearchPaper> papers) {
 *         this.papers = papers;
 *         return this;
 *     }
 *
 *     public ResearchListModule showAbstracts(boolean show) {
 *         this.showAbstracts = show;
 *         return this;
 *     }
 *
 *     @Override
 *     protected void buildContent() {
 *         if (title != null) {
 *             super.withChild(Header.H2(title));
 *         }
 *
 *         for (ResearchPaper paper : papers) {
 *             Card paperCard = Card.create()
 *                 .withHeader(paper.getTitle())
 *                 .withBody(paper.getAuthor() + " - " + paper.getYear());
 *
 *             if (showAbstracts) {
 *                 paperCard.withFooter(paper.getAbstract());
 *             }
 *
 *             super.withChild(paperCard);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Module Properties</h2>
 * <p>All modules inherit these common properties:</p>
 * <ul>
 *   <li><strong>moduleId:</strong> Unique identifier for the module (becomes HTML id attribute)</li>
 *   <li><strong>title:</strong> Optional title displayed at the top of the module</li>
 *   <li><strong>class:</strong> CSS classes (always includes "module" plus custom classes)</li>
 * </ul>
 *
 * <h2>HTMX Integration</h2>
 * <p>Modules work seamlessly with HTMX for dynamic updates:</p>
 * <pre>{@code
 * // Module that refreshes every 30 seconds
 * DataModule<Stats> liveStats = DataModule.create(Stats.class)
 *     .withModuleId("live-stats")
 *     .withAttribute("hx-get", "/api/stats")
 *     .withAttribute("hx-trigger", "every 30s")
 *     .withAttribute("hx-swap", "outerHTML");
 *
 * // Lazy-loaded module
 * ContentModule lazyContent = ContentModule.create()
 *     .withModuleId("lazy-content")
 *     .withAttribute("hx-get", "/api/content")
 *     .withAttribute("hx-trigger", "revealed")  // Load when scrolled into view
 *     .withAttribute("hx-swap", "outerHTML");
 * }</pre>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li><strong>Single Responsibility:</strong> Each module should have one clear purpose</li>
 *   <li><strong>Composition Over Inheritance:</strong> Use existing components rather than extending</li>
 *   <li><strong>Fluent API:</strong> Return {@code this} from configuration methods</li>
 *   <li><strong>Static Factory:</strong> Provide a {@code create()} factory method</li>
 *   <li><strong>Lazy Building:</strong> Build content in {@link #buildContent()}, not the constructor</li>
 *   <li><strong>Default Values:</strong> Provide sensible defaults for all optional properties</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Modules are <strong>not thread-safe</strong>. Create new module instances for each request.
 * Do not share module instances across multiple threads or requests.</p>
 *
 * @see ContentModule
 * @see FormModule
 * @see DataModule
 * @see GalleryModule
 * @see ForumModule
 */
public abstract class Module extends HtmlTag {

    /** Unique identifier for this module instance (becomes HTML id attribute) */
    protected String moduleId;

    /** Optional title displayed at the top of the module */
    protected String title;

    /**
     * Creates a new module with the specified HTML tag name.
     *
     * <p>All modules are automatically assigned the "module" CSS class for consistent styling.</p>
     *
     * @param tagName the HTML element to use as the module container (typically "div")
     */
    protected Module(String tagName) {
        super(tagName);
        // Don't set class here - let withClass() handle it
    }

    /**
     * Sets the unique identifier for this module.
     *
     * <p>The module ID is rendered as the HTML {@code id} attribute, which:</p>
     * <ul>
     *   <li>Provides a unique anchor for linking</li>
     *   <li>Enables targeted HTMX updates</li>
     *   <li>Allows CSS and JavaScript selection</li>
     * </ul>
     *
     * <p>Example:</p>
     * <pre>{@code
     * module.withModuleId("user-profile-123");
     * // Renders: <div id="user-profile-123" class="module">...</div>
     * }</pre>
     *
     * @param moduleId unique identifier for this module
     * @return this Module instance for method chaining
     */
    public Module withModuleId(String moduleId) {
        this.moduleId = moduleId;
        this.withAttribute("id", moduleId);
        return this;
    }

    /**
     * Sets the title for this module.
     *
     * <p>The title is typically rendered as a header (H2) at the top of the module.
     * Subclasses control exactly how the title is displayed in their {@link #buildContent()}
     * implementation.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * module.withTitle("User Statistics");
     * // Typically renders: <h2 class="module-title">User Statistics</h2>
     * }</pre>
     *
     * @param title the module title (can be null for no title)
     * @return this Module instance for method chaining
     */
    public Module withTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Adds a custom CSS class to this module.
     *
     * <p>The "module" class is always present. This method appends additional
     * classes for custom styling.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * module.withClass("featured");
     * // Renders: <div class="module featured">...</div>
     * }</pre>
     *
     * @param className additional CSS class name(s) to add
     * @return this Module instance for method chaining
     */
    public Module withClass(String className) {
        String currentClass = "module";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    /**
     * Builds the content of this module by composing child components.
     *
     * <p>This method is called automatically during {@link #render()} and is where
     * subclasses assemble their component structure. Use {@link #withChild(Component)}
     * to add components to the module.</p>
     *
     * <p><strong>Note:</strong> This method may be called multiple times if the module is
     * rendered multiple times. The children are automatically cleared before each call to
     * ensure consistent output. Do not rely on state from previous calls.</p>
     *
     * <p><strong>Implementation Guidelines:</strong></p>
     * <ul>
     *   <li>Check if {@link #title} is present and add a header if so</li>
     *   <li>Compose child components based on module properties</li>
     *   <li>Add components using {@code super.withChild()}</li>
     *   <li>Don't call {@code render()} - that happens automatically</li>
     *   <li>Don't accumulate state - children are cleared before each call</li>
     * </ul>
     *
     * <p>Example Implementation:</p>
     * <pre>{@code
     * @Override
     * protected void buildContent() {
     *     // Add title if present
     *     if (title != null && !title.isEmpty()) {
     *         super.withChild(Header.H2(title).withClass("module-title"));
     *     }
     *
     *     // Build module-specific content
     *     Card card = Card.create()
     *         .withBody("Module content here");
     *
     *     super.withChild(card);
     * }
     * }</pre>
     */
    protected abstract void buildContent();

    /**
     * Renders this module to an HTML string.
     *
     * <p>This method orchestrates the rendering process:</p>
     * <ol>
     *   <li>Clears any previously built content</li>
     *   <li>Calls {@link #buildContent()} to assemble components (lazy initialization)</li>
     *   <li>Delegates to {@link HtmlTag#render()} for actual HTML generation</li>
     * </ol>
     *
     * <p>This method clears any previously built content and rebuilds it fresh on each call.
     * This ensures that calling {@code render()} multiple times produces consistent output
     * and prevents child element accumulation. Modules can safely be rendered multiple times
     * or reused across multiple pages.</p>
     *
     * <p><strong>Note:</strong> The {@link #buildContent()} method is called each time this module
     * is rendered. Implementations should use {@link #withChild(Component)} and related methods
     * to add content. The children are automatically cleared before each call to ensure consistent
     * output.</p>
     *
     * @return complete HTML string representation of this module
     */
    @Override
    protected void build() {
        ensureModuleClass();
        buildContent();
    }

    /**
     * Ensures the "module" class is present on this component.
     * This is called automatically before rendering.
     *
     * <p>This method works correctly even if classes were added via
     * {@link #withClass(String)} or {@link #withAttribute(String, String)}.</p>
     */
    private void ensureModuleClass() {
        // Use existing withClass() method which handles class attribute correctly
        String currentClass = getAttributeValue("class").orElse("");
        if (!currentClass.contains("module")) {
            withClass("module");
        }
    }

    /**
     * Gets the value of an attribute by name.
     *
     * @param name the attribute name
     * @return Optional containing the attribute value, or empty if not found
     */
    private Optional<String> getAttributeValue(String name) {
        return attributes.stream()
                .filter(attr -> name.equals(attr.getName()))
                .map(Attribute::getValue)
                .findFirst();
    }
}
