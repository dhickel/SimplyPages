package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.SlotKeyMap;

import java.util.Map;

/**
 * Utility for generating basic edit form controls from slot metadata.
 *
 * <p>Contract: field type is inferred from {@code slotTypes}; unknown types fall back to a text
 * input rendered from {@code toString()}.</p>
 *
 * <p>Mutability and thread-safety: stateless utility; thread-safe.</p>
 */
public class EditFormBuilder {

    /**
     * Builds a form-like component tree from slot names, types, and current values.
     */
    public static Component fromSlots(
        Map<String, SlotKey<?>> slotMapping,
        Map<String, Class<?>> slotTypes,
        SlotKeyMap currentValues
    ) {
        Div form = new Div();

        slotMapping.forEach((name, key) -> {
            Class<?> type = slotTypes.get(name);
            Object value = currentValues.getValue(name, Object.class).orElse(null);

            Component field = createField(name, type, value);
            form.withChild(field);
        });

        return form;
    }

    /**
     * Creates the field component used for a single slot entry.
     */
    private static Component createField(String name, Class<?> type, Object value) {
        String label = formatLabel(name);

        if (Boolean.class.equals(type) || boolean.class.equals(type)) {
            return FormFieldHelper.checkboxField(label, name, Boolean.TRUE.equals(value));
        }

        if (String.class.equals(type)) {
            String stringValue = value != null ? value.toString() : "";
            if (name.toLowerCase().contains("content")
                || name.toLowerCase().contains("description")
                || name.toLowerCase().contains("bio")) {
                return FormFieldHelper.textAreaField(label, name, stringValue, 10);
            }
            return FormFieldHelper.textField(label, name, stringValue);
        }

        String fallback = value != null ? value.toString() : "";
        return FormFieldHelper.textField(label, name, fallback);
    }

    /**
     * Converts a camelCase slot name into a spaced label.
     */
    private static String formatLabel(String name) {
        String result = name.substring(0, 1).toUpperCase() + name.substring(1);
        return result.replaceAll("([A-Z])", " $1").trim();
    }
}
