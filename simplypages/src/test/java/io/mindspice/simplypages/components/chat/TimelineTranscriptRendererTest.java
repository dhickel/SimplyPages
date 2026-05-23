package io.mindspice.simplypages.components.chat;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TimelineTranscriptRendererTest {

    private record Block(
        String id,
        String label,
        String kind,
        boolean open
    ) implements EmbeddedBlockData {
        @Override
        public HtmlTag content() {
            return new HtmlTag("pre").withInnerText("content");
        }
    }

    private record Entry(
        String id,
        String title,
        String body,
        String actor,
        String timestamp,
        String status,
        List<Block> embeddedBlocks
    ) implements TranscriptEntryData {}

    @Test
    void rendersEntriesWithGenericEmbeddedBlocks() {
        TimelineTranscriptRenderer<Entry, Void> renderer = TimelineTranscriptRenderer.<Entry, Void>builder().build();

        String html = renderer.render(List.of(new Entry(
            "e-1",
            "Planned",
            "Body **markdown**",
            "Assistant",
            "10:00",
            "running",
            List.of(new Block("b-1", "Details", "text", true))
        )), null).render();

        HtmlAssert.assertThat(html)
            .hasElement(".timeline-transcript")
            .hasElement("article.timeline-transcript-entry[data-transcript-entry-id=\"e-1\"]")
            .elementTextEquals(".timeline-transcript-entry-actor", "Assistant")
            .elementTextEquals(".timeline-transcript-entry-title", "Planned")
            .hasElement("details.embedded-block[open]")
            .attributeEquals(".embedded-block", "data-embedded-block-kind", "text")
            .elementTextEquals(".embedded-block-summary", "Details");
    }

    @Test
    void rendersEmptyState() {
        String html = TimelineTranscriptRenderer.<Entry, Void>builder()
            .withEmptyStateText("Nothing")
            .build()
            .render(List.of(), null)
            .render();

        HtmlAssert.assertThat(html)
            .elementTextEquals(".timeline-transcript-empty", "Nothing");
    }

    @Test
    void requiresEntryId() {
        TimelineTranscriptRenderer<Entry, Void> renderer = TimelineTranscriptRenderer.<Entry, Void>builder().build();

        assertThrows(IllegalArgumentException.class, () -> renderer.render(List.of(new Entry(
            "",
            "Title",
            "Body",
            "",
            "",
            "",
            List.of()
        )), null));
    }
}
