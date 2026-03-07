package io.mindspice.simplypages.editing;


/**
 * Defines persistence/approval behavior expected by edit handlers.
 */
public enum EditMode {
    /**
     * User edits should enter an approval workflow before publication.
     */
    USER_EDIT,

    /**
     * Owner/admin edits may be applied immediately.
     */
    OWNER_EDIT
}
