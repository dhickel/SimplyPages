package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProgressBarTest {

    @Test
    @DisplayName("ProgressBar should render value and classes")
    void testProgressBarRendering() {
        ProgressBar bar = ProgressBar.create(50)
            .success()
            .striped()
            .withLabel("Half");

        String html = bar.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.progress[role=\"progressbar\"]")
            .hasElement("div.progress > div.progress-bar.progress-success.progress-bar-striped[style*=\"width: 50%\"]")
            .attributeEquals("div.progress", "aria-valuenow", "50")
            .attributeEquals("div.progress", "aria-valuemin", "0")
            .attributeEquals("div.progress", "aria-valuemax", "100")
            .attributeEquals("div.progress", "aria-label", "Half")
            .doesNotHaveElement(".progress-bar[aria-valuenow]")
            .elementTextEquals("div.progress > div.progress-bar", "Half");
    }

    @Test void maximumControlsClampingAndPercentage() {
        var doc = org.jsoup.Jsoup.parse(ProgressBar.create(150).withMax(200).render());
        org.junit.jupiter.api.Assertions.assertEquals("150", doc.selectFirst(".progress").attr("aria-valuenow"));
        org.junit.jupiter.api.Assertions.assertEquals("width: 75%;", doc.selectFirst(".progress-bar").attr("style"));
        doc = org.jsoup.Jsoup.parse(ProgressBar.create(90).withMax(50).render());
        org.junit.jupiter.api.Assertions.assertEquals("50", doc.selectFirst(".progress").attr("aria-valuenow"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> ProgressBar.create(1).withMax(0));
    }

    @Test void renderingIsPureAndCompilationPreservesNamesAndMaximum() {
        var bar = ProgressBar.create(250).withMax(200).animated().withHeight("32px");
        bar.withAttribute("aria-label", "Upload");
        bar.withMax(300);
        String compiled = bar.compile().render(io.mindspice.simplypages.core.RenderContext.empty());
        org.junit.jupiter.api.Assertions.assertEquals(compiled, bar.render());
        org.junit.jupiter.api.Assertions.assertEquals(compiled, bar.render());
        HtmlAssert.assertThat(compiled)
            .attributeEquals(".progress", "aria-label", "Upload")
            .attributeEquals(".progress", "aria-valuenow", "250")
            .attributeEquals(".progress", "aria-valuemax", "300")
            .attributeEquals(".progress", "style", "height: 32px;")
            .hasElement(".progress-bar.progress-bar-striped.progress-bar-animated[style='width: 83%;']");
        HtmlAssert.assertThat(ProgressBar.create(-1).render())
            .attributeEquals(".progress", "aria-valuenow", "0")
            .attributeEquals(".progress", "aria-label", "Progress");
    }

}
