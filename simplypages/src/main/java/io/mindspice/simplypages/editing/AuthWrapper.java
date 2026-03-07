package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.components.display.Modal;

import java.util.function.Supplier;

/**
 * Small utility for wrapping edit actions with authorization checks.
 *
 * <p>Framework boundary: this class only gates execution and returns fallback UI content. It does
 * not authenticate users or enforce transport-layer protections.</p>
 *
 * <p>Mutability and thread-safety: stateless utility methods; thread-safe.</p>
 */
public class AuthWrapper {

    /**
     * Executes {@code action} when authorized, otherwise executes {@code unauthorizedHandler}.
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
     * Authorization wrapper for edit operations with custom error text.
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
