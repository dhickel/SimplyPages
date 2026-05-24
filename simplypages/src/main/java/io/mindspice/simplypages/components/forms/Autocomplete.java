package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reusable HTMX-first autocomplete/combobox field and related option/status fragments.
 *
 * <p>Mutable and not thread-safe while being configured. Mutate within a request-scoped flow.
 * For reuse, stop mutating and render a stable structure with per-request context data.</p>
 */
public class Autocomplete extends HtmlTag {

    private final String name;
    private final String rootId;
    private final String statusId;
    private final String resultsId;
    private final TextInput input;
    private final HtmlTag label;
    private final HtmlTag labelText;
    private final HtmlTag status;
    private final HtmlTag results;

    /**
     * Creates an autocomplete field with default IDs and classes.
     *
     * @param name input name attribute
     */
    public Autocomplete(String name) {
        super("div");
        this.name = normalizeName(name);
        this.rootId = "sp-autocomplete-" + sanitize(this.name);
        this.statusId = rootId + "-status";
        this.resultsId = rootId + "-results";

        withId(rootId);
        withClass("sp-autocomplete");
        withAttribute("data-sp-autocomplete-name", this.name);

        this.input = TextInput.search(this.name)
            .withId(rootId + "-input")
            .withClass("sp-autocomplete-input")
            .withAutocomplete("off");
        this.labelText = new HtmlTag("span").withClass("sp-autocomplete-label-text").withInnerText("Search");
        this.label = new HtmlTag("label")
            .withClass("sp-autocomplete-label")
            .withAttribute("for", rootId + "-input")
            .withChild(labelText)
            .withChild(input);

        this.status = new HtmlTag("div")
            .withId(statusId)
            .withClass("sp-autocomplete-status")
            .withAttribute("aria-live", "polite");
        this.results = new HtmlTag("div")
            .withId(resultsId)
            .withClass("sp-autocomplete-results")
            .withAttribute("role", "listbox");

        withChild(label);
        withChild(status);
        withChild(results);
    }

    /**
     * Creates a new autocomplete field.
     *
     * @param name input name attribute
     * @return field instance
     */
    public static Autocomplete create(String name) {
        return new Autocomplete(name);
    }

    /**
     * Sets visible label text.
     */
    public Autocomplete withLabel(String labelText) {
        this.labelText.withInnerText(labelText);
        return this;
    }

    /**
     * Sets search input placeholder.
     */
    public Autocomplete withPlaceholder(String placeholder) {
        input.withPlaceholder(placeholder);
        return this;
    }

    /**
     * Sets initial input value.
     */
    public Autocomplete withValue(String value) {
        input.withValue(value);
        return this;
    }

    /**
     * Marks field required and defaults status to "Required".
     */
    public Autocomplete required() {
        input.required();
        status.withInnerText("Required");
        return this;
    }

    /**
     * Sets request endpoint for live option search and applies default HTMX wiring.
     */
    public Autocomplete withOptionsEndpoint(String endpoint) {
        input.hxGet(endpoint)
            .hxTrigger("keyup changed delay:250ms, focus")
            .hxTarget("#" + resultsId)
            .hxInclude("closest .sp-autocomplete")
            .hxSwap("innerHTML");
        return this;
    }

    /**
     * Overrides HTMX trigger expression used for option search requests.
     */
    public Autocomplete withOptionsTrigger(String trigger) {
        input.hxTrigger(trigger);
        return this;
    }

    /**
     * Overrides HTMX include selector used on option search requests.
     */
    public Autocomplete withOptionsInclude(String selector) {
        input.hxInclude(selector);
        return this;
    }

    /**
     * Sets request endpoint for value validation and configures status target refresh.
     */
    public Autocomplete withValidationEndpoint(String endpoint) {
        input.withAttribute("hx-on::after-request",
            "htmx.ajax('GET', '" + endpoint + "', {target: '#" + statusId + "', source: this})");
        return this;
    }

    /**
     * Renders a hidden context input under the root so HTMX include can send extra parameters.
     */
    public Autocomplete withContextParam(String key, String value) {
        if (isBlank(key) || isBlank(value)) {
            return this;
        }
        withChild(new HtmlTag("input", true)
            .withAttribute("type", "hidden")
            .withAttribute("name", key)
            .withAttribute("value", value));
        return this;
    }

    /**
     * Adds many hidden context parameters.
     */
    public Autocomplete withContextParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return this;
        }
        params.forEach(this::withContextParam);
        return this;
    }

    /**
     * Replaces status region content.
     */
    public Autocomplete withStatus(StatusMessage message) {
        StatusMessage normalized = message == null ? new StatusMessage("", State.DEFAULT) : message;
        String cssClass = switch (normalized.state()) {
            case SELECTED -> "sp-autocomplete-status sp-autocomplete-selected";
            case INVALID -> "sp-autocomplete-status sp-autocomplete-invalid";
            case DEFAULT -> "sp-autocomplete-status";
        };
        status.withAttribute("class", cssClass);
        status.withInnerText(normalized.text());
        return this;
    }

    /**
     * Builds a status fragment for an HTMX validation target.
     */
    public static Component status(StatusMessage message) {
        StatusMessage normalized = message == null ? new StatusMessage("", State.DEFAULT) : message;
        String cssClass = switch (normalized.state()) {
            case SELECTED -> "sp-autocomplete-status sp-autocomplete-selected";
            case INVALID -> "sp-autocomplete-status sp-autocomplete-invalid";
            case DEFAULT -> "sp-autocomplete-status";
        };
        return new HtmlTag("div").withClass(cssClass).withInnerText(normalized.text());
    }

    /**
     * Builds an options list fragment for an HTMX search target.
     */
    public static Component options(OptionsConfig config, List<OptionRow> rows) {
        HtmlTag list = new HtmlTag("div").withClass("sp-autocomplete-options");
        if (rows == null || rows.isEmpty()) {
            return list.withChild(new HtmlTag("div")
                .withClass("sp-autocomplete-empty")
                .withInnerText(config.emptyText()));
        }

        for (OptionRow row : rows) {
            HtmlTag option = new HtmlTag("button")
                .withAttribute("type", "button")
                .withClass("sp-autocomplete-option" + (row.available() ? "" : " sp-autocomplete-invalid"))
                .withAttribute("role", "option")
                .hxGet(row.selectUrl())
                .hxTarget(config.selectionTarget())
                .hxSwap(config.selectionSwap());

            option.withChild(new HtmlTag("span")
                .withClass("sp-autocomplete-option-label")
                .withInnerText(row.label()));
            option.withChild(new HtmlTag("code").withInnerText(row.value()));
            if (!isBlank(row.detail())) {
                option.withChild(new HtmlTag("span")
                    .withClass("sp-autocomplete-option-detail")
                    .withInnerText(row.detail()));
            }
            if (!isBlank(row.status())) {
                option.withChild(new HtmlTag("span")
                    .withClass("sp-autocomplete-option-status")
                    .withInnerText(row.status()));
            }
            list.withChild(option);
        }
        return list;
    }

    /**
     * Creates a selection URL by appending named query parameters.
     */
    public static String appendQuery(String baseUrl, Map<String, String> params) {
        Objects.requireNonNull(baseUrl, "baseUrl");
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }
        StringBuilder builder = new StringBuilder(baseUrl);
        builder.append(baseUrl.contains("?") ? "&" : "?");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (isBlank(entry.getKey())) {
                continue;
            }
            if (!first) {
                builder.append('&');
            }
            builder.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
            first = false;
        }
        return builder.toString();
    }

    private static String normalizeName(String value) {
        if (isBlank(value)) {
            return "value";
        }
        return value.trim();
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9_-]", "-");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /**
     * Configuration for rendering option list HTMX behavior.
     */
    public record OptionsConfig(String selectionTarget, String selectionSwap, String emptyText) {
        public OptionsConfig {
            selectionTarget = isBlank(selectionTarget) ? "this" : selectionTarget;
            selectionSwap = isBlank(selectionSwap) ? "outerHTML" : selectionSwap;
            emptyText = isBlank(emptyText) ? "No matches" : emptyText;
        }
    }

    /**
     * Data for one selectable option row.
     */
    public record OptionRow(String value, String label, String detail, String status, boolean available, String selectUrl) {
        public OptionRow {
            value = value == null ? "" : value;
            label = isBlank(label) ? value : label;
            detail = detail == null ? "" : detail;
            status = status == null ? "" : status;
            available = available;
            selectUrl = selectUrl == null ? "#" : selectUrl;
        }
    }

    /**
     * Validation/status message payload.
     */
    public record StatusMessage(String text, State state) {
        public StatusMessage {
            text = text == null ? "" : text;
            state = state == null ? State.DEFAULT : state;
        }
    }

    /**
     * Status state variants.
     */
    public enum State {
        DEFAULT,
        SELECTED,
        INVALID
    }
}
