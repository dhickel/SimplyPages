package io.mindspice.simplypages.components.chat;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

/**
 * Default generic disclosure block for transcript entries.
 */
public class DefaultEmbeddedBlockComponent implements EmbeddedBlockComponent {

    private String blockId;
    private String kind;
    private String label;
    private Component content;
    private boolean open;

    public static DefaultEmbeddedBlockComponent create() {
        return new DefaultEmbeddedBlockComponent();
    }

    @Override
    public EmbeddedBlockComponent withBlockId(String blockId) {
        this.blockId = blockId;
        return this;
    }

    @Override
    public EmbeddedBlockComponent withKind(String kind) {
        this.kind = kind;
        return this;
    }

    @Override
    public EmbeddedBlockComponent withLabel(String label) {
        this.label = label;
        return this;
    }

    @Override
    public EmbeddedBlockComponent withContent(Component content) {
        this.content = content;
        return this;
    }

    @Override
    public EmbeddedBlockComponent open(boolean open) {
        this.open = open;
        return this;
    }

    @Override
    public String render(RenderContext context) {
        HtmlTag root = new HtmlTag("details")
            .withAttribute("class", "embedded-block")
            .withAttribute("data-embedded-block-id", safe(blockId))
            .withAttribute("data-embedded-block-kind", safe(kind));
        if (open) {
            root.withAttribute("open", "open");
        }

        root.withChild(new HtmlTag("summary")
            .withAttribute("class", "embedded-block-summary")
            .withInnerText(safe(label)));

        root.withChild(new HtmlTag("div")
            .withAttribute("class", "embedded-block-body")
            .withChild(content == null ? new HtmlTag("span") : content));

        return root.render(context);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
