package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.core.Component;

/**
 * Immutable descriptor for a module child that can be edited independently.
 *
 * <p>Mutability and thread-safety: immutable and thread-safe once created.</p>
 */
public class EditableChild {
    private final String id;
    private final String label;
    private final Component component;

    /**
     * Creates an editable-child descriptor.
     */
    public EditableChild(String id, String label, Component component) {
        this.id = id;
        this.label = label;
        this.component = component;
    }

    /**
     * Static factory for {@link EditableChild}.
     */
    public static EditableChild create(String id, String label, Component component) {
        return new EditableChild(id, label, component);
    }

    /**
     * Returns the child identifier used by edit/delete endpoints.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns human-readable child label for editing UIs.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the underlying child component reference.
     */
    public Component getComponent() {
        return component;
    }
}
