package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.components.display.Modal;

import java.util.function.Supplier;

/**
 * Optional authentication/authorization wrapper for edit operations.
 * <p>
 * Provides a standardized pattern for protecting edit endpoints with permission checks.
 * Framework users can use this utility to wrap their edit actions with auth checks,
 * or implement their own authorization patterns.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @GetMapping("/api/modules/{id}/edit")
 * @ResponseBody
 * public String editModule(@PathVariable String id, Principal principal) {
 *     return AuthWrapper.requireForEdit(
 *         () -> canUserEdit(id, principal.name()),
 *         () -> {
 *             Module module = findModule(id);
 *             Editable<?> adapter = (Editable<?>) module;
 *             return EditModalBuilder.create()
 *                 .withTitle("Edit Module")
 *                 .withEditView(adapter.buildEditView())
 *                 .withSaveUrl("/api/modules/" + id + "/update")
 *                 .build()
 *                 .render();
 *         }
 *     );
 * }
 * }</pre>
 *
 * <h2>Custom Unauthorized Handler:</h2>
 * <pre>{@code
 * return AuthWrapper.require(
 *     () -> hasPermission(user, resource),
 *     () -> performAction(),
 *     () -> customUnauthorizedResponse()
 * );
 * }</pre>
 */
public class AuthWrapper {

    /**
     * Generic authorization wrapper with custom unauthorized handler.
     *
     * @param authCheck           Supplier that returns true if action is authorized
     * @param action              Action to perform if authorized
     * @param unauthorizedHandler Response to return if not authorized
     * @param <T>                 Return type of the action
     * @return Result of action if authorized, otherwise result of unauthorizedHandler
     */
    public static <T> T require(
            Supplier<Boolean> authCheck,
            Supplier<T> action,
            Supplier<T> unauthorizedHandler
    ) {
        return authCheck.get() ? action.get() : unauthorizedHandler.get();
    }

    /**
     * Authorization wrapper for edit operations with default unauthorized modal.
     * <p>
     * Returns a Modal with "Unauthorized" title and "Permission denied" error message
     * if the auth check fails.
     * </p>
     *
     * @param authCheck Supplier that returns true if edit is authorized
     * @param action    Action to perform if authorized (typically returns Modal HTML)
     * @return Result of action if authorized, otherwise unauthorized modal
     */
    public static String requireForEdit(
            Supplier<Boolean> authCheck,
            Supplier<String> action
    ) {
        return require(
                authCheck,
                action,
                () -> Modal.create()
                        .withTitle("Unauthorized")
                        .withBody(Alert.danger("Permission denied"))
                        .render()
        );
    }

    /**
     * Authorization wrapper for edit operations with custom error message.
     *
     * @param authCheck    Supplier that returns true if edit is authorized
     * @param action       Action to perform if authorized
     * @param errorMessage Custom error message to display if unauthorized
     * @return Result of action if authorized, otherwise modal with custom error
     */
    public static String requireForEdit(
            Supplier<Boolean> authCheck,
            Supplier<String> action,
            String errorMessage
    ) {
        return require(
                authCheck,
                action,
                () -> Modal.create()
                        .withTitle("Unauthorized")
                        .withBody(Alert.danger(errorMessage))
                        .render()
        );
    }

    /**
     * Authorization wrapper for delete operations with default unauthorized modal.
     *
     * @param authCheck Supplier that returns true if delete is authorized
     * @param action    Action to perform if authorized
     * @return Result of action if authorized, otherwise unauthorized modal
     */
    public static String requireForDelete(
            Supplier<Boolean> authCheck,
            Supplier<String> action
    ) {
        return require(
                authCheck,
                action,
                () -> Modal.create()
                        .withTitle("Unauthorized")
                        .withBody(Alert.danger("You do not have permission to delete this content"))
                        .render()
        );
    }

    /**
     * Authorization wrapper for create operations with default unauthorized modal.
     *
     * @param authCheck Supplier that returns true if create is authorized
     * @param action    Action to perform if authorized
     * @return Result of action if authorized, otherwise unauthorized modal
     */
    public static String requireForCreate(
            Supplier<Boolean> authCheck,
            Supplier<String> action
    ) {
        return require(
                authCheck,
                action,
                () -> Modal.create()
                        .withTitle("Unauthorized")
                        .withBody(Alert.danger("You do not have permission to create content"))
                        .render()
        );
    }
}
