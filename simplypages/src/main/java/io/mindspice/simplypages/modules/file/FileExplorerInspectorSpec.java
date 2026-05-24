package io.mindspice.simplypages.modules.file;

public record FileExplorerInspectorSpec(String title, String emptyMessage) {
    public static FileExplorerInspectorSpec defaults() {
        return new FileExplorerInspectorSpec("Inspector", "Select an entry to inspect details.");
    }
}
