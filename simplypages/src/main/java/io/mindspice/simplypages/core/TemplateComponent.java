package io.mindspice.simplypages.core;

/**
 * {@link Component} adapter that binds a compiled {@link Template} to a fixed {@link RenderContext}.
 *
 * <p>Context contract: {@link #render(RenderContext)} ignores the parent context and always renders
 * with the bound context.</p>
 *
 * <p>Mutability/thread-safety: immutable wrapper. Effective thread-safety depends on whether the
 * bound context is mutated concurrently.</p>
 */
public class TemplateComponent implements Component {
    private final Template template;
    private final RenderContext context;

    /**
     * Creates a template-backed component with a fixed render context.
     *
     * @param template compiled template to render
     * @param context context used for all renders
     */
    public TemplateComponent(Template template, RenderContext context) {
        this.template = template;
        this.context = context;
    }

    /**
     * Factory for {@link TemplateComponent}.
     */
    public static TemplateComponent of(Template template, RenderContext context) {
        return new TemplateComponent(template, context);
    }

    /**
     * Renders using the bound context and ignores {@code parentContext}.
     *
     * @param parentContext ignored
     * @return rendered HTML for the bound template/context pair
     */
    @Override
    public String render(RenderContext parentContext) {
        return template.render(this.context);
    }
}
