package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.core.Component;

/**
 * Wrapper for a child component that can be edited within a module.
 */
public class EditableChild {
    private final String id;
    private final String label;
    private final Component component;

    public EditableChild(String id, String label, Component component) {
        this.id = id;
        this.label = label;
        this.component = component;
    }

    public static EditableChild create(String id, String label, Component component) {
        return new EditableChild(id, label, component);
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public Component getComponent() {
        return component;
    }
}
