package io.mindspice.simplypages.components.navigation;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.Test;

class HtmxTabNavTest {

    @Test
    void rendersHtmxTabButtons() {
        String html = HtmxTabNav.create("agent-tabs", "#agent-panel")
            .addTab("overview", "Overview", "/agents/1/overview")
            .addTab("runs", "Runs", "/agents/1/runs")
            .withActiveKey("runs")
            .withPushUrl(true)
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("nav#agent-tabs.htmx-tab-nav")
            .hasElementCount("button.htmx-tab", 2)
            .attributeEquals("button[data-tab-key=\"runs\"]", "aria-selected", "true")
            .attributeEquals("button[data-tab-key=\"runs\"]", "hx-get", "/agents/1/runs")
            .attributeEquals("button[data-tab-key=\"runs\"]", "hx-target", "#agent-panel")
            .attributeEquals("button[data-tab-key=\"runs\"]", "hx-push-url", "true");
    }
}
