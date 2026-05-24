package io.mindspice.simplypages.modules.file;

import io.mindspice.simplypages.core.Component;

import java.util.List;

public record FileExplorerState(
    String rootLabel,
    String currentPath,
    List<FileBreadcrumbItem> breadcrumbs,
    List<FileEntryView> entries,
    FileEntryView selectedEntry,
    List<FileExplorerAction> toolbarActions,
    Component inspectorContent,
    Component viewerContent,
    boolean pickerMode,
    String pickerValue
) {}
