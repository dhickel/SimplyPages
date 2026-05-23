package io.mindspice.simplypages.components.chat;

import io.mindspice.simplypages.core.Component;

/**
 * Component contract populated by {@link TimelineTranscriptRenderer}.
 */
public interface EmbeddedBlockComponent extends Component {
    EmbeddedBlockComponent withBlockId(String blockId);
    EmbeddedBlockComponent withKind(String kind);
    EmbeddedBlockComponent withLabel(String label);
    EmbeddedBlockComponent withContent(Component content);
    EmbeddedBlockComponent open(boolean open);
}
