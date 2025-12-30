package io.mindspice.simplypages.editing;


/**
 * Interface for authorization checks in the editing system.
 * <p>
 * Implement this interface to integrate with your application's
 * authorization system (e.g., Spring Security).
 * </p>
 * <p>
 * Example implementation with Spring Security:
 * <pre>{@code
 * @Service
 * public class ModuleAuthChecker implements AuthorizationChecker {
 *
 *     @Autowired
 *     private ModuleRepository moduleRepo;
 *
 *     @Autowired
 *     private UserRepository userRepo;
 *
 *     @Override
 *     public boolean canEdit(String moduleId, String userId) {
 *         Module module = moduleRepo.findById(moduleId);
 *         User user = userRepo.findById(userId);
 *
 *         return module.getOwnerId().equals(userId)
 *             || user.hasRole("ADMIN")
 *             || user.hasRole("MODERATOR");
 *     }
 *
 *     @Override
 *     public boolean canDelete(String moduleId, String userId) {
 *         Module module = moduleRepo.findById(moduleId);
 *         User user = userRepo.findById(userId);
 *
 *         // Typically more restrictive than edit permissions
 *         return module.getOwnerId().equals(userId)
 *             || user.hasRole("ADMIN");
 *     }
 *
 *     @Override
 *     public EditMode getEditMode(String moduleId, String userId) {
 *         Module module = moduleRepo.findById(moduleId);
 *         User user = userRepo.findById(userId);
 *
 *         // Owners and admins get immediate updates
 *         if (module.getOwnerId().equals(userId) || user.hasRole("ADMIN")) {
 *             return EditMode.OWNER_EDIT;
 *         }
 *
 *         // Regular users need approval
 *         return EditMode.USER_EDIT;
 *     }
 * }
 * }</pre>
 * </p>
 */
public interface AuthorizationChecker {

    /**
     * Check if the specified user can edit the given module.
     *
     * @param moduleId the unique identifier of the module
     * @param userId the unique identifier of the user
     * @return true if the user has edit permission, false otherwise
     */
    boolean canEdit(String moduleId, String userId);

    /**
     * Check if the specified user can delete the given module.
     * <p>
     * Typically this is more restrictive than edit permissions.
     * </p>
     *
     * @param moduleId the unique identifier of the module
     * @param userId the unique identifier of the user
     * @return true if the user has delete permission, false otherwise
     */
    boolean canDelete(String moduleId, String userId);

    /**
     * Determine the edit mode for the specified user and module.
     * <p>
     * Owners and admins typically get OWNER_EDIT (immediate updates).
     * Regular users typically get USER_EDIT (approval required).
     * </p>
     *
     * @param moduleId the unique identifier of the module
     * @param userId the unique identifier of the user
     * @return the appropriate EditMode for this user
     */
    EditMode getEditMode(String moduleId, String userId);
}
