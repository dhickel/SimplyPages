package io.mindspice.simplypages.core;

/**
 * Adapter class that wraps a {@link Template} and a {@link RenderContext} to make them usable as a standard {@link Component}.
 *
 * <p>This component is crucial for the "Composite Pattern" of rendering, allowing high-performance,
 * pre-compiled templates to be mixed seamlessly into request-scoped component trees.</p>
 *
 * <h2>Use Case</h2>
 * <ul>
 *   <li>Embedding a highly dynamic module (Pattern B) into a conditional page layout (Pattern A).</li>
 *   <li>Creating lists or grids of templates where the layout is built at runtime.</li>
 * </ul>
 */
public class TemplateComponent implements Component {
    private final Template template;
    private final RenderContext context;

    public TemplateComponent(Template template, RenderContext context) {
        this.template = template;
        this.context = context;
    }

    public static TemplateComponent of(Template template, RenderContext context) {
        return new TemplateComponent(template, context);
    }

    @Override
    public String render(RenderContext parentContext) {
        // We use the internal context, ignoring the parent context, because
        // the template is bound to its specific data.
        return template.render(this.context);
    }
}
