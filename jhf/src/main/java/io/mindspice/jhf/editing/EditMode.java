package io.mindspice.jhf.editing;


/**
 * Defines editing behavior modes for editable modules.
 * <p>
 * USER_EDIT mode requires changes to go through an approval workflow before
 * becoming visible to other users. OWNER_EDIT mode allows immediate updates
 * that go live without approval.
 * </p>
 */
public enum EditMode {
    /**
     * User edits require approval before going live.
     * Typically used for community contributions, wiki-style editing,
     * or when users are editing content they don't own.
     */
    USER_EDIT,

    /**
     * Owner/admin edits go live immediately.
     * Typically used when the user owns the content or has
     * administrative/moderator privileges.
     */
    OWNER_EDIT
}
