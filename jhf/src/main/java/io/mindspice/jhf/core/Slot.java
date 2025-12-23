package io.mindspice.jhf.core;

/**
 * A marker component representing a dynamic slot in a Template.
 * It renders a placeholder that is replaced by Template.render().
 */
public class Slot implements Component {
    private final SlotKey<?> key;
    private final String placeholder;

    private Slot(SlotKey<?> key) {
        this.key = key;
        // We use a specific format that Template can easily find and replace.
        // Format: {{SLOT:name}}
        this.placeholder = "{{SLOT:" + key.getName() + "}}";
    }

    /**
     * Creates a new Slot for the given key.
     *
     * @param key The slot key
     * @return A new Slot component
     */
    public static Slot of(SlotKey<?> key) {
        return new Slot(key);
    }

    @Override
    public String render() {
        return placeholder;
    }

    public String getPlaceholder() {
        return placeholder;
    }
}
