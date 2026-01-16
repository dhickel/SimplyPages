package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.core.Component;

/**
 * Wrapper for a child component that can be edited within a parent module.
 */
public class EditableChild {
    private final String id;
    private final Component component;
    private final String title;

    public EditableChild(String id, Component component, String title) {
        this.id = id;
        this.component = component;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public Component getComponent() {
        return component;
    }

    public String getTitle() {
        return title;
    }
}
