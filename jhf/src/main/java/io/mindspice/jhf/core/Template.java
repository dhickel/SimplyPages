package io.mindspice.jhf.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A pre-compiled template that can be efficiently rendered with dynamic data.
 *
 * <p>Templates are created from a Component structure once. The structure
 * should contain {@link Slot} components where dynamic content will be injected.</p>
 *
 * <p>At render time, a {@link RenderContext} is provided to fill the slots.</p>
 */
public class Template {
    private final String templateString;
    // Pattern to match {{SLOT:name}}
    private static final Pattern SLOT_PATTERN = Pattern.compile("\\{\\{SLOT:(.*?)\\}\\}");

    private Template(String templateString) {
        this.templateString = templateString;
    }

    /**
     * Creates a Template from a Component.
     * The component is rendered once to generate the static structure.
     *
     * @param component The component structure
     * @return A new Template
     */
    public static Template of(Component component) {
        return new Template(component.render());
    }

    /**
     * Renders the template with the given context.
     * Replaces all slots with values from the context.
     *
     * @param context The data to fill slots
     * @return The rendered HTML string
     */
    public String render(RenderContext context) {
        Matcher matcher = SLOT_PATTERN.matcher(templateString);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String slotName = matcher.group(1);
            // We reconstruct the key to look it up.
            // Since SlotKey.equals relies on name, this works.
            SlotKey<Object> keyLookup = SlotKey.of(slotName);

            Object value = context.get(keyLookup);
            String replacement = "";

            if (value != null) {
                if (value instanceof Component) {
                    replacement = ((Component) value).render();
                } else {
                    replacement = value.toString();
                }
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
