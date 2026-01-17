package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.SlotKeyMap;

import java.util.Map;

/**
 * Utility to auto-generate edit forms from slot mappings.
 */
public class EditFormBuilder {

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

    private static String formatLabel(String name) {
        String result = name.substring(0, 1).toUpperCase() + name.substring(1);
        return result.replaceAll("([A-Z])", " $1").trim();
    }
}
