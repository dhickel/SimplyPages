package io.mindspice.simplypages.components.content;

import java.nio.file.Path;

/**
 * Non-fatal parse or generation warning captured during content bundle generation.
 * For generated detail/index collisions, {@code sectionKey} and {@code sourcePath}
 * identify the detail's section and file. The message also identifies the index's
 * section and directory, states detail precedence and pagination masking, and suggests
 * choosing non-overlapping base paths or published slugs. Source paths are diagnostic
 * context, not content intended for automatic public display.
 */
public record ContentWarning(
    String sectionKey,
    Path sourcePath,
    String message
) {}
