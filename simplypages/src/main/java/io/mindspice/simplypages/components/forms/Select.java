package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Select/dropdown control with mutable option list.
 *
 * <p>Mutable and not thread-safe. Options and attributes are accumulated on this instance. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class Select extends HtmlTag {

    private final List<Option> options = new ArrayList<>();

    /**
     * Creates a select element.
     *
     * @param name form field name
     */
    public Select(String name) {
        super("select");
        this.withAttribute("name", name);
        this.withAttribute("class", "form-select");
    }

    /**
     * Creates a select element.
     *
     * @param name form field name
     * @return new select
     */
    public static Select create(String name) {
        return new Select(name);
    }

    /**
     * Sets id attribute.
     *
     * @param id element id
     * @return this select
     */
    @Override
    public Select withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Enables multi-select behavior.
     *
     * @return this select
     */
    public Select multiple() {
        this.withAttribute("multiple", "");
        return this;
    }

    /**
     * Marks select required.
     *
     * @return this select
     */
    public Select required() {
        this.withAttribute("required", "");
        return this;
    }

    /**
     * Marks select disabled.
     *
     * @return this select
     */
    public Select disabled() {
        this.withAttribute("disabled", "");
        return this;
    }

    /**
     * Sets number of visible options.
     *
     * @param size visible row count
     * @return this select
     */
    public Select withSize(int size) {
        this.withAttribute("size", String.valueOf(size));
        return this;
    }

    /**
     * Appends one unselected option.
     *
     * @param value option value
     * @param label option label
     * @return this select
     */
    public Select addOption(String value, String label) {
        options.add(new Option(value, label, false));
        return this;
    }

    /**
     * Appends one option.
     *
     * @param value option value
     * @param label option label
     * @param selected initial selected flag
     * @return this select
     */
    public Select addOption(String value, String label, boolean selected) {
        options.add(new Option(value, label, selected));
        return this;
    }

    /**
     * Appends options where value and label are identical.
     *
     * @param values option value/label list
     * @return this select
     */
    public Select addOptions(List<String> values) {
        values.forEach(v -> options.add(new Option(v, v, false)));
        return this;
    }

    /**
     * Appends class token(s).
     *
     * @param className class token(s)
     * @return this select
     */
    @Override
    public Select withClass(String className) {
        super.addClass(className);
        return this;
    }

    /**
     * Renders children plus options appended in insertion order.
     *
     * @return children stream
     */
    @Override
    protected Stream<Component> getChildrenStream() {
        return Stream.concat(
            super.getChildrenStream(),
            options.stream().map(opt -> (Component) opt)
        );
    }

    /**
     * Sets inline width style.
     *
     * @param width CSS width value
     * @return this select
     */
    @Override
    public Select withWidth(String width) {
        super.withWidth(width);
        return this;
    }

    /**
     * Sets inline max-width style.
     *
     * @param maxWidth CSS max-width value
     * @return this select
     */
    @Override
    public Select withMaxWidth(String maxWidth) {
        super.withMaxWidth(maxWidth);
        return this;
    }

    /**
     * Sets inline min-width style.
     *
     * @param minWidth CSS min-width value
     * @return this select
     */
    @Override
    public Select withMinWidth(String minWidth) {
        super.withMinWidth(minWidth);
        return this;
    }

    /**
     * Renderable select option model.
     *
     * <p>Effectively immutable except {@link #disabled()}.</p>
     */
    public static class Option implements Component {
        private final String value;
        private final String label;
        private final boolean selected;
        private boolean disabled;

        /**
         * Creates an option.
         *
         * @param value option value
         * @param label option label
         * @param selected initial selected flag
         */
        public Option(String value, String label, boolean selected) {
            this.value = value;
            this.label = label;
            this.selected = selected;
        }

        /**
         * Marks option disabled.
         *
         * @return this option
         */
        public Option disabled() {
            this.disabled = true;
            return this;
        }

        /**
         * Renders escaped option HTML.
         *
         * @param context render context (unused)
         * @return option HTML
         */
        @Override
        public String render(RenderContext context) {
            String safeValue = value == null ? "" : Encode.forHtmlAttribute(value);
            String safeLabel = label == null ? "" : Encode.forHtml(label);
            StringBuilder sb = new StringBuilder("<option value=\"").append(safeValue).append("\"");
            if (selected) {
                sb.append(" selected");
            }
            if (disabled) {
                sb.append(" disabled");
            }
            sb.append(">").append(safeLabel).append("</option>");
            return sb.toString();
        }

        /**
         * Renders with empty context.
         *
         * @return option HTML
         */
        @Override
        public String render() {
            return render(RenderContext.empty());
        }
    }
}
