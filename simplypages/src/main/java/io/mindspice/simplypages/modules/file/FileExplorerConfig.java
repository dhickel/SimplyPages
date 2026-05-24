package io.mindspice.simplypages.modules.file;

public record FileExplorerConfig(
    FileExplorerEndpoints endpoints,
    FileExplorerMode explorerMode,
    FilePickerMode pickerMode,
    String rootId,
    String listTargetId,
    String inspectorTargetId,
    String viewerTargetId,
    String modalContainerId,
    String pickerCallbackTargetId,
    boolean allowCreateFolder,
    boolean allowCreateText,
    boolean allowCreateMarkdown,
    boolean allowRename,
    boolean allowDelete,
    boolean allowCopyMove,
    boolean allowTags
) {
    public static FileExplorerConfig defaults(FileExplorerEndpoints endpoints) {
        return new FileExplorerConfig(
            endpoints,
            FileExplorerMode.CARDS,
            FilePickerMode.FILES_OR_DIRECTORIES,
            "file-explorer-root",
            "file-explorer-list",
            "file-explorer-inspector",
            "file-explorer-viewer",
            "file-explorer-modal",
            "file-picker-value",
            false,
            false,
            false,
            false,
            false,
            false,
            false
        );
    }
}
