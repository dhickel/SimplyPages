package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.Test;

class TimelineTranscriptModuleTest {

    @Test
    void rendersTranscriptModuleShell() {
        String html = TimelineTranscriptModule.create()
            .withModuleId("timeline")
            .withTitle("Timeline")
            .withDescription("Execution log")
            .withTranscript(new HtmlTag("div").withAttribute("class", "timeline-transcript").withInnerText("entry"))
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("section#timeline.timeline-transcript-module")
            .elementTextEquals(".module-title", "Timeline")
            .elementTextEquals(".module-description", "Execution log")
            .hasElement(".timeline-transcript");
    }
}
