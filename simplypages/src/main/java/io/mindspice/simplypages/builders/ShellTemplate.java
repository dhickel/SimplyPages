package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.Template;

import java.util.Objects;

/**
 * Wrapper around a compiled shell template with a dedicated content slot.
 *
 * <p>Renders full document HTML including doctype. Thread-safe as long as callers use
 * request-scoped {@link RenderContext} instances and do not mutate shared context concurrently.</p>
 */
public final class ShellTemplate implements Component {
    private static final String DOCTYPE = "<!DOCTYPE html>\n";

    private final Template template;
    private final SlotKey<Component> contentSlot;

    public ShellTemplate(Template template, SlotKey<Component> contentSlot) {
        this.template = Objects.requireNonNull(template, "template cannot be null");
        this.contentSlot = Objects.requireNonNull(contentSlot, "contentSlot cannot be null");
    }

    /**
     * Renders with explicit content bound to the shell content slot.
     */
    public String renderWithContent(Component content) {
        return DOCTYPE + template.render(RenderContext.of(contentSlot, content));
    }

    /**
     * Returns the dedicated shell content slot key.
     */
    public SlotKey<Component> contentSlot() {
        return contentSlot;
    }

    /**
     * Exposes the underlying compiled template for advanced composition.
     */
    public Template asTemplate() {
        return template;
    }

    @Override
    public String render(RenderContext context) {
        return DOCTYPE + template.render(context == null ? RenderContext.empty() : context);
    }
}
