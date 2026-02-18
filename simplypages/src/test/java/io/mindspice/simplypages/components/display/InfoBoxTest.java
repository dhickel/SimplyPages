package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InfoBoxTest {

    @Test
    @DisplayName("InfoBox should render title, value, and icon")
    void testInfoBoxRendering() {
        InfoBox box = InfoBox.create()
            .withTitle("Users")
            .withValue("42")
            .withIcon("👤");

        String html = box.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.info-box")
            .elementTextEquals(".info-box-title", "Users")
            .elementTextEquals(".info-box-value", "42")
            .elementTextEquals(".info-box-icon", "👤");
    }
}
