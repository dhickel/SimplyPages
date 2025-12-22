package io.mindspice.jhf.components.forms;

import io.mindspice.jhf.components.Div;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Radio button group component.
 * Creates a group of mutually exclusive radio options.
 */
public class RadioGroup extends Div {

    private final String name;
    private final List<RadioOption> options = new ArrayList<>();
    private String selectedValue;

    public RadioGroup(String name) {
        super();
        this.name = name;
        this.withClass("form-radio-group");
    }

    public static RadioGroup create(String name) {
        return new RadioGroup(name);
    }

    public RadioGroup addOption(String value, String label) {
        options.add(new RadioOption(value, label));
        return this;
    }

    public RadioGroup withSelectedValue(String value) {
        this.selectedValue = value;
        return this;
    }

    public RadioGroup required() {
        // Will be applied to all radio buttons
        options.forEach(opt -> opt.required = true);
        return this;
    }

    public RadioGroup inline() {
        this.withClass("radio-inline");
        return this;
    }

    @Override
    public String render() {
        for (RadioOption option : options) {
            Div optionWrapper = new Div().withClass("radio-option");

            String id = name + "-" + option.value.replaceAll("[^a-zA-Z0-9]", "-");

            HtmlTag input = new HtmlTag("input", true)
                .withAttribute("type", "radio")
                .withAttribute("name", name)
                .withAttribute("id", id)
                .withAttribute("value", option.value)
                .withAttribute("class", "radio-input");

            if (option.value.equals(selectedValue)) {
                input.withAttribute("checked", "");
            }

            if (option.required) {
                input.withAttribute("required", "");
            }

            HtmlTag label = new HtmlTag("label")
                .withAttribute("for", id)
                .withAttribute("class", "radio-label")
                .withInnerText(option.label);

            optionWrapper.withChild(input).withChild(label);
            super.withChild(optionWrapper);
        }

        return super.render();
    }

    private static class RadioOption {
        String value;
        String label;
        boolean required = false;

        RadioOption(String value, String label) {
            this.value = value;
            this.label = label;
        }
    }
}
