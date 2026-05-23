package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.Test;

class OobFragmentsTest {

    @Test
    void rendersOutOfBandFragmentResponse() {
        String html = OobFragments.response(
            new HtmlTag("div").withId("primary").withInnerText("Primary"),
            OobFragments.swap("status", new HtmlTag("span").withInnerText("Done")),
            OobFragments.beforeEnd("log", new HtmlTag("span").withInnerText("Line"))
        ).render();

        HtmlAssert.assertThat(html)
            .hasElement(".oob-fragment-response")
            .hasElement("#primary")
            .attributeEquals("#status", "hx-swap-oob", "true")
            .attributeEquals("#log", "hx-swap-oob", "beforeend");
    }
}
