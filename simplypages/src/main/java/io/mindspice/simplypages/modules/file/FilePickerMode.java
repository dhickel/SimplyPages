package io.mindspice.simplypages.modules.file;

public enum FilePickerMode {
    FILES,
    DIRECTORIES,
    FILES_OR_DIRECTORIES;

    public boolean allows(FileEntryView entry) {
        if (entry == null) { return false; }
        return switch (this) {
            case FILES -> !entry.directory();
            case DIRECTORIES -> entry.directory();
            case FILES_OR_DIRECTORIES -> true;
        };
    }
}
