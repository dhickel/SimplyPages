package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutocompleteTest {

    @Test
    @DisplayName("Autocomplete should render HTMX-wired field with status and context params")
    void testFieldRendering() {
        String html = Autocomplete.create("planId")
            .withLabel("Plan")
            .withPlaceholder("Search plans")
            .withValue("abc123")
            .required()
            .withOptionsEndpoint("/selectors/plans/options?name=planId")
            .withValidationEndpoint("/selectors/plans/validate?name=planId")
            .withContextParams(Map.of("workspaceId", "w-1", "limit", "15"))
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("div#sp-autocomplete-planId.sp-autocomplete")
            .hasElement("label.sp-autocomplete-label > input#sp-autocomplete-planId-input.form-input.sp-autocomplete-input")
            .elementTextEquals("label.sp-autocomplete-label > span.sp-autocomplete-label-text", "Plan")
            .attributeEquals("input#sp-autocomplete-planId-input", "name", "planId")
            .attributeEquals("input#sp-autocomplete-planId-input", "type", "search")
            .attributeEquals("input#sp-autocomplete-planId-input", "placeholder", "Search plans")
            .attributeEquals("input#sp-autocomplete-planId-input", "value", "abc123")
            .attributeEquals("input#sp-autocomplete-planId-input", "required", "")
            .attributeEquals("input#sp-autocomplete-planId-input", "hx-get", "/selectors/plans/options?name=planId")
            .attributeEquals("input#sp-autocomplete-planId-input", "hx-trigger", "keyup changed delay:250ms, focus")
            .attributeEquals("input#sp-autocomplete-planId-input", "hx-target", "#sp-autocomplete-planId-results")
            .attributeEquals("input#sp-autocomplete-planId-input", "hx-include", "closest .sp-autocomplete")
            .attributeEquals("input#sp-autocomplete-planId-input", "hx-swap", "innerHTML")
            .attributeEquals("input#sp-autocomplete-planId-input", "hx-on::after-request",
                "htmx.ajax('GET', '/selectors/plans/validate?name=planId', {target: '#sp-autocomplete-planId-status', source: this})")
            .hasElementCount("div#sp-autocomplete-planId > input[type=hidden]", 2)
            .attributeEquals("div#sp-autocomplete-planId > input[name=workspaceId]", "value", "w-1")
            .attributeEquals("div#sp-autocomplete-planId > input[name=limit]", "value", "15")
            .elementTextEquals("div#sp-autocomplete-planId-status", "Required")
            .hasElement("div#sp-autocomplete-planId-results[role=listbox]");
    }

    @Test
    @DisplayName("Autocomplete options should render rows and empty state")
    void testOptionsRendering() {
        Autocomplete.OptionsConfig config = new Autocomplete.OptionsConfig("#selector-root", "outerHTML", "No plans");
        String optionsHtml = Autocomplete.options(config, List.of(
            new Autocomplete.OptionRow("p-1", "Plan One", "active", "ready", true, "/selectors/plans/selected?value=p-1"),
            new Autocomplete.OptionRow("p-2", "Plan Two", "", "archived", false, "/selectors/plans/selected?value=p-2")
        )).render();

        HtmlAssert.assertThat(optionsHtml)
            .hasElement("div.sp-autocomplete-options")
            .hasElementCount("button.sp-autocomplete-option", 2)
            .hasElementCount("button.sp-autocomplete-option.sp-autocomplete-invalid", 1)
            .attributeEquals("button.sp-autocomplete-option", "hx-target", "#selector-root")
            .attributeEquals("button.sp-autocomplete-option", "hx-swap", "outerHTML")
            .hasElement("button.sp-autocomplete-option > span.sp-autocomplete-option-label")
            .hasElement("button.sp-autocomplete-option > code")
            .hasElement("button.sp-autocomplete-option > span.sp-autocomplete-option-detail")
            .hasElement("button.sp-autocomplete-option > span.sp-autocomplete-option-status");

        String emptyHtml = Autocomplete.options(config, List.of()).render();
        HtmlAssert.assertThat(emptyHtml)
            .hasElement("div.sp-autocomplete-options > div.sp-autocomplete-empty")
            .elementTextEquals("div.sp-autocomplete-empty", "No plans");
    }

    @Test
    @DisplayName("Autocomplete status and query append should normalize defaults")
    void testStatusAndQueryAppend() {
        String selectedStatus = Autocomplete.status(new Autocomplete.StatusMessage("Selected: Plan One", Autocomplete.State.SELECTED))
            .render();
        HtmlAssert.assertThat(selectedStatus)
            .hasElement("div.sp-autocomplete-status.sp-autocomplete-selected")
            .elementTextEquals("div.sp-autocomplete-status", "Selected: Plan One");

        String defaultStatus = Autocomplete.status(new Autocomplete.StatusMessage("", null)).render();
        HtmlAssert.assertThat(defaultStatus)
            .hasElement("div.sp-autocomplete-status")
            .doesNotHaveElement("div.sp-autocomplete-invalid");

        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", "planId");
        params.put("value", "plan 1");
        params.put("required", "true");
        String query = Autocomplete.appendQuery("/selectors/plans/selected", params);
        assertEquals("/selectors/plans/selected?name=planId&value=plan+1&required=true", query);
    }
}
