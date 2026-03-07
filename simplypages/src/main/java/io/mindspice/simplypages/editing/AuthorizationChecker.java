package io.mindspice.simplypages.editing;


/**
 * Application-facing authorization contract for editing operations.
 *
 * <p>Framework boundary: SimplyPages does not implement policy decisions. Implementations must
 * enforce authorization based on application identity, ownership, and role rules.</p>
 */
public interface AuthorizationChecker {

    /**
     * Returns whether a user can edit the specified module.
     *
     * @param moduleId the unique identifier of the module
     * @param userId the unique identifier of the user
     * @return true if the user has edit permission, false otherwise
     */
    boolean canEdit(String moduleId, String userId);

    /**
     * Returns whether a user can delete the specified module.
     *
     * @param moduleId the unique identifier of the module
     * @param userId the unique identifier of the user
     * @return true if the user has delete permission, false otherwise
     */
    boolean canDelete(String moduleId, String userId);

    /**
     * Resolves edit mode for the user/module pair.
     *
     * @param moduleId the unique identifier of the module
     * @param userId the unique identifier of the user
     * @return the appropriate EditMode for this user
     */
    EditMode getEditMode(String moduleId, String userId);
}
