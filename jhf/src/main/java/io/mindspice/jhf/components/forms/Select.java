package io.mindspice.jhf.components.forms;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Select dropdown component.
 * Supports single and multiple selection.
 */
public class Select extends HtmlTag {

    private final List<Option> options = new ArrayList<>();

    public Select(String name) {
        super("select");
        this.withAttribute("name", name);
        this.withAttribute("class", "form-select");
    }

    public static Select create(String name) {
        return new Select(name);
    }

    public Select withId(String id) {
        this.withAttribute("id", id);
        return this;
    }

    public Select multiple() {
        this.withAttribute("multiple", "");
        return this;
    }

    public Select required() {
        this.withAttribute("required", "");
        return this;
    }

    public Select disabled() {
        this.withAttribute("disabled", "");
        return this;
    }

    public Select withSize(int size) {
        this.withAttribute("size", String.valueOf(size));
        return this;
    }

    public Select addOption(String value, String label) {
        options.add(new Option(value, label, false));
        return this;
    }

    public Select addOption(String value, String label, boolean selected) {
        options.add(new Option(value, label, selected));
        return this;
    }

    public Select addOptions(List<String> values) {
        values.forEach(v -> options.add(new Option(v, v, false)));
        return this;
    }

    public Select withClass(String className) {
        String currentClass = "form-select";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    public String render() {
        // Add all options as children before rendering
        options.forEach(option -> super.withChild(option));
        return super.render();
    }

    @Override
    public Select withWidth(String width) {
        super.withWidth(width);
        return this;
    }

    @Override
    public Select withMaxWidth(String maxWidth) {
        super.withMaxWidth(maxWidth);
        return this;
    }

    @Override
    public Select withMinWidth(String minWidth) {
        super.withMinWidth(minWidth);
        return this;
    }

    public static class Option implements Component {
        private final String value;
        private final String label;
        private final boolean selected;
        private boolean disabled;

        public Option(String value, String label, boolean selected) {
            this.value = value;
            this.label = label;
            this.selected = selected;
        }

        public Option disabled() {
            this.disabled = true;
            return this;
        }

        @Override
        public String render() {
            StringBuilder sb = new StringBuilder("<option value=\"").append(value).append("\"");
            if (selected) {
                sb.append(" selected");
            }
            if (disabled) {
                sb.append(" disabled");
            }
            sb.append(">").append(label).append("</option>");
            return sb.toString();
        }
    }
}
