package io.mindspice.jhf.core;

/**
 * Base interface for all renderable UI components in the Java HTML Framework.
 *
 * <p>The Component interface defines the contract for any element that can be rendered to HTML.
 * All components must implement the {@link #render()} method which generates the HTML string
 * representation of the component.</p>
 *
 * <h2>Component Hierarchy</h2>
 * <pre>
 * Component (interface)
 * └── HtmlTag (abstract class) - Base implementation
 *     ├── Basic Components (Div, Paragraph, Header, etc.)
 *     ├── Form Components (TextInput, Select, Button, etc.)
 *     ├── Display Components (Table, Card, Alert, etc.)
 *     ├── Media Components (Gallery, Video, Audio)
 *     ├── Forum Components (ForumPost, Comment, etc.)
 *     ├── Navigation Components (Link, NavBar, SideNav)
 *     ├── Layout Components (Row, Column, Grid)
 *     └── Module (abstract class) - High-level compositions
 *         ├── ContentModule
 *         ├── FormModule
 *         ├── DataModule
 *         └── GalleryModule
 * </pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Simple component usage
 * Component header = Header.H1("Welcome");
 * String html = header.render();  // Returns: <h1>Welcome</h1>
 *
 * // Nested components
 * Component div = new Div()
 *     .withChild(Header.H2("Title"))
 *     .withChild(new Paragraph().withInnerText("Content"));
 * String html = div.render();  // Returns nested HTML structure
 *
 * // In a Spring controller
 * @GetMapping("/page")
 * @ResponseBody
 * public String myPage() {
 *     Component page = Page.builder()
 *         .addComponents(Header.H1("My Page"))
 *         .build();
 *     return page.render();
 * }
 * }</pre>
 *
 * <h2>Design Philosophy</h2>
 * <ul>
 *   <li><strong>Server-Side Rendering:</strong> All HTML generation happens on the server</li>
 *   <li><strong>Type Safety:</strong> Compile-time checks for component structure</li>
 *   <li><strong>Composability:</strong> Components can contain other components</li>
 *   <li><strong>Fluent API:</strong> Method chaining for readable component construction</li>
 * </ul>
 *
 * @see HtmlTag
 * @see Module
 */
public interface Component {
    /**
     * Renders this component to an HTML string using the provided context.
     *
     * <p>This method is responsible for generating the complete HTML representation
     * of the component, including all nested children and attributes, resolving any
     * dynamic slots from the context.</p>
     *
     * @param context the context containing values for dynamic slots
     * @return HTML string representation of this component
     */
    default String render(RenderContext context) {
        return render();
    }

    /**
     * Renders this component to an HTML string with an empty context.
     *
     * <p>This is a convenience method that delegates to {@link #render(RenderContext)}
     * with {@link RenderContext#empty()}.</p>
     *
     * @return HTML string representation of this component
     */
    default String render() {
        return render(RenderContext.empty());
    }
}
