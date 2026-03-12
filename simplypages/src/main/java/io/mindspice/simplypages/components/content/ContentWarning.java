package io.mindspice.simplypages.components.content;

import java.nio.file.Path;

/**
 * Non-fatal parse or generation warning captured during content bundle generation.
 */
public record ContentWarning(
    String sectionKey,
    Path sourcePath,
    String message
) {}
