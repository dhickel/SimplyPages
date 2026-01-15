package io.mindspice.simplypages.editing;

/**
 * Metadata for an editable child component.
 *
 * <p>Used by {@link Editable#getEditableChildren()} to provide information
 * about children that can be edited independently.</p>
 */
public class EditableChild {
    private final String id;
    private final String label;
    private final String summary;

    public EditableChild(String id, String label, String summary) {
        this.id = id;
        this.label = label;
        this.summary = summary;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getSummary() {
        return summary;
    }
}
