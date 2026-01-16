package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.forms.Checkbox;
import io.mindspice.simplypages.components.forms.TextArea;
import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.core.Component;

/**
 * Helper for building consistent form fields in Editable implementations.
 */
public final class FormFieldHelper {

    private FormFieldHelper() {
    }

    /**
     * Create a labeled text input field.
     *
     * @param label The field label
     * @param name The input name attribute
     * @param value The current value (may be null)
     * @return A form field component
     */
    public static Component textField(String label, String name, String value) {
        Div group = new Div().withClass("form-field");
        group.withChild(new Paragraph(label + ":").withClass("form-label"));
        group.withChild(TextInput.create(name)
            .withValue(value != null ? value : "")
            .withMaxWidth("100%"));
        return group;
    }

    /**
     * Create a labeled textarea field.
     *
     * @param label The field label
     * @param name The textarea name attribute
     * @param value The current value (may be null)
     * @param rows Number of rows
     * @return A form field component
     */
    public static Component textAreaField(String label, String name, String value, int rows) {
        Div group = new Div().withClass("form-field");
        group.withChild(new Paragraph(label + ":").withClass("form-label"));
        group.withChild(TextArea.create(name)
            .withValue(value != null ? value : "")
            .withRows(rows)
            .withMaxWidth("100%"));
        return group;
    }

    /**
     * Create a labeled checkbox field.
     *
     * @param label The field label
     * @param name The checkbox name attribute
     * @param checked Whether the checkbox is checked
     * @return A form field component
     */
    public static Component checkboxField(String label, String name, boolean checked) {
        Div group = new Div().withClass("form-field");
        Checkbox checkbox = Checkbox.create(name, "true").withLabel(label);
        if (checked) {
            checkbox.checked();
        }
        group.withChild(checkbox);
        return group;
    }
}
