package io.mindspice.simplypages.modules.file;

import java.util.List;

public record FileEntryView(
    String id,
    String name,
    String path,
    String type,
    String sizeLabel,
    String summary,
    List<String> tags,
    boolean directory,
    boolean selected,
    List<FileExplorerAction> actions
) {}
