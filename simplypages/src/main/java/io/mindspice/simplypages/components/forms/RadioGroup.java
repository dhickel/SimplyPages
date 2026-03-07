package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Radio button group with shared input name.
 *
 * <p>Mutable and not thread-safe. Options are accumulated in-memory and rendered in insertion order. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class RadioGroup extends Div {

    private final String name;
    private final List<RadioOption> options = new ArrayList<>();
    private String selectedValue;

    /**
     * Creates a radio group.
     *
     * @param name shared radio input name
     */
    public RadioGroup(String name) {
        super();
        this.name = name;
        this.withClass("form-radio-group");
    }

    /**
     * Creates a radio group.
     *
     * @param name shared radio input name
     * @return new radio group
     */
    public static RadioGroup create(String name) {
        return new RadioGroup(name);
    }

    /**
     * Appends one option.
     *
     * @param value option value
     * @param label option label
     * @return this group
     */
    public RadioGroup addOption(String value, String label) {
        options.add(new RadioOption(value, label));
        return this;
    }

    /**
     * Sets selected option value.
     *
     * @param value value matched during render
     * @return this group
     */
    public RadioGroup withSelectedValue(String value) {
        this.selectedValue = value;
        return this;
    }

    /**
     * Marks all current options as required.
     *
     * <p>Options added after this call are not marked required unless this method is invoked again.</p>
     *
     * @return this group
     */
    public RadioGroup required() {
        // Will be applied to all radio buttons
        options.forEach(opt -> opt.required = true);
        return this;
    }

    /**
     * Adds {@code radio-inline} class.
     *
     * @return this group
     */
    public RadioGroup inline() {
        this.withClass("radio-inline");
        return this;
    }

    /**
     * Builds radio-option wrappers and appends inherited children.
     *
     * @return child stream
     */
    @Override
    protected Stream<Component> getChildrenStream() {
        Stream<Component> optionsStream = options.stream().map(option -> {
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
            return optionWrapper;
        });

        return Stream.concat(optionsStream, super.getChildrenStream());
    }

    /**
     * Internal mutable option model used at render time.
     */
    private static class RadioOption {
        String value;
        String label;
        boolean required = false;

        /**
         * Creates an option with label/value.
         *
         * @param value option value
         * @param label option label
         */
        RadioOption(String value, String label) {
            this.value = value;
            this.label = label;
        }
    }
}
