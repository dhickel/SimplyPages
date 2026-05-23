package io.mindspice.simplypages.components.chat;

import io.mindspice.simplypages.components.Markdown;
import io.mindspice.simplypages.components.display.StatusBadge;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Renderer for chronological assistant/workspace transcript entries.
 */
public final class TimelineTranscriptRenderer<ENTRY extends TranscriptEntryData, CTX> {

    private final Supplier<? extends EmbeddedBlockComponent> embeddedBlockSupplier;
    private final BiFunction<ENTRY, CTX, String> bodyTextResolver;
    private final String emptyStateText;

    private TimelineTranscriptRenderer(Builder<ENTRY, CTX> builder) {
        this.embeddedBlockSupplier = builder.embeddedBlockSupplier;
        this.bodyTextResolver = builder.bodyTextResolver;
        this.emptyStateText = builder.emptyStateText;
    }

    public static <ENTRY extends TranscriptEntryData, CTX> Builder<ENTRY, CTX> builder() {
        return new Builder<>();
    }

    public Component render(Collection<ENTRY> entries, CTX context) {
        HtmlTag root = new HtmlTag("div").withAttribute("class", "timeline-transcript");

        if (entries == null || entries.isEmpty()) {
            root.withChild(new HtmlTag("div")
                .withAttribute("class", "timeline-transcript-empty")
                .withInnerText(emptyStateText));
            return root;
        }

        for (ENTRY entry : List.copyOf(entries)) {
            String entryId = requireNonBlank(entry.id(), "transcript entry id");
            HtmlTag item = new HtmlTag("article")
                .withAttribute("class", "timeline-transcript-entry")
                .withAttribute("data-transcript-entry-id", entryId);

            HtmlTag header = new HtmlTag("header")
                .withAttribute("class", "timeline-transcript-entry-header");
            if (!safe(entry.actor()).isBlank()) {
                header.withChild(new HtmlTag("span")
                    .withAttribute("class", "timeline-transcript-entry-actor")
                    .withInnerText(entry.actor()));
            }
            if (!safe(entry.timestamp()).isBlank()) {
                header.withChild(new HtmlTag("time")
                    .withAttribute("class", "timeline-transcript-entry-timestamp")
                    .withInnerText(entry.timestamp()));
            }
            if (!safe(entry.status()).isBlank()) {
                header.withChild(StatusBadge.info(entry.status()));
            }
            item.withChild(header);

            if (!safe(entry.title()).isBlank()) {
                item.withChild(new HtmlTag("h3")
                    .withAttribute("class", "timeline-transcript-entry-title")
                    .withInnerText(entry.title()));
            }

            item.withChild(new HtmlTag("div")
                .withAttribute("class", "timeline-transcript-entry-body")
                .withChild(new Markdown(safe(bodyTextResolver.apply(entry, context)))));

            Collection<? extends EmbeddedBlockData> blocks = entry.embeddedBlocks();
            if (blocks != null && !blocks.isEmpty()) {
                HtmlTag embedded = new HtmlTag("div")
                    .withAttribute("class", "timeline-transcript-embedded-blocks");
                for (EmbeddedBlockData block : blocks) {
                    EmbeddedBlockComponent component = Objects.requireNonNull(
                        embeddedBlockSupplier.get(),
                        "embedded block supplier returned null"
                    );
                    component.withBlockId(requireNonBlank(block.id(), "embedded block id"))
                        .withKind(safe(block.kind()))
                        .withLabel(requireNonBlank(block.label(), "embedded block label"))
                        .withContent(block.content())
                        .open(block.open());
                    embedded.withChild(component);
                }
                item.withChild(embedded);
            }

            root.withChild(item);
        }

        return root;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required " + fieldName);
        }
        return value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Builder<ENTRY extends TranscriptEntryData, CTX> {
        private Supplier<? extends EmbeddedBlockComponent> embeddedBlockSupplier = DefaultEmbeddedBlockComponent::create;
        private BiFunction<ENTRY, CTX, String> bodyTextResolver = (entry, ignored) -> entry.body();
        private String emptyStateText = "No transcript entries yet.";

        private Builder() {}

        public Builder<ENTRY, CTX> withEmbeddedBlockSupplier(
            Supplier<? extends EmbeddedBlockComponent> embeddedBlockSupplier
        ) {
            this.embeddedBlockSupplier = Objects.requireNonNull(embeddedBlockSupplier, "embeddedBlockSupplier");
            return this;
        }

        public Builder<ENTRY, CTX> withBodyTextResolver(BiFunction<ENTRY, CTX, String> bodyTextResolver) {
            this.bodyTextResolver = Objects.requireNonNull(bodyTextResolver, "bodyTextResolver");
            return this;
        }

        public Builder<ENTRY, CTX> withEmptyStateText(String emptyStateText) {
            if (emptyStateText == null || emptyStateText.isBlank()) {
                throw new IllegalArgumentException("emptyStateText cannot be null or blank");
            }
            this.emptyStateText = emptyStateText;
            return this;
        }

        public TimelineTranscriptRenderer<ENTRY, CTX> build() {
            return new TimelineTranscriptRenderer<>(this);
        }
    }
}
