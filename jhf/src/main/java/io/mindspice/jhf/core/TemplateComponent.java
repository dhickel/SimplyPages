package io.mindspice.jhf.core;

/**
 * A helper component that adapts a Template and RenderContext into a standard Component.
 * This allows pre-compiled Templates to be used within standard Page layouts.
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
    public String render() {
        return template.render(this.context);
    }
}
