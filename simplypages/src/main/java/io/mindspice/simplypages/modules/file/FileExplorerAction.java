package io.mindspice.simplypages.modules.file;

public record FileExplorerAction(
    String key,
    String label,
    String method,
    String endpoint,
    String hxTarget,
    String hxSwap,
    String hxConfirm,
    String cssClass
) {
    public FileExplorerAction {
        method = method == null || method.isBlank() ? "get" : method.toLowerCase();
        hxSwap = hxSwap == null || hxSwap.isBlank() ? "innerHTML" : hxSwap;
    }
}
