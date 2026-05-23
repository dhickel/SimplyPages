package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.Test;

class MasterDetailBrowserModuleTest {

    @Test
    void rendersMasterDetailBrowserWithHtmxItems() {
        String html = MasterDetailBrowserModule.create()
            .withModuleId("browser")
            .withTitle("Projects")
            .withTarget("#project-detail")
            .addItem("p1", "Project 1", "active", "/projects/p1")
            .addItem("p2", "Project 2", "/projects/p2")
            .withActiveKey("p1")
            .withDetail(new HtmlTag("div").withAttribute("class", "detail-body").withInnerText("Detail"))
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("section#browser.master-detail-browser-module")
            .hasElement(".master-detail-browser-shell")
            .hasElementCount(".master-detail-item", 2)
            .attributeEquals("button[data-item-key=\"p1\"]", "aria-current", "true")
            .attributeEquals("button[data-item-key=\"p1\"]", "hx-get", "/projects/p1")
            .attributeEquals("button[data-item-key=\"p1\"]", "hx-target", "#project-detail")
            .hasElement("#project-detail.master-detail-detail")
            .hasElement(".detail-body");
    }
}
