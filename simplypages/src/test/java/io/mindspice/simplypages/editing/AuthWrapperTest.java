package io.mindspice.simplypages.editing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthWrapperTest {

    @Test
    @DisplayName("AuthWrapper should return action result when authorized")
    void testAuthorized() {
        String result = AuthWrapper.requireForEdit(() -> true, () -> "OK");
        assertTrue(result.contains("OK"));
    }

    @Test
    @DisplayName("AuthWrapper should return default unauthorized modal")
    void testUnauthorized() {
        String result = AuthWrapper.requireForEdit(() -> false, () -> "OK");
        assertTrue(result.contains("Unauthorized"));
        assertTrue(result.contains("Permission denied"));
    }

    @Test
    @DisplayName("AuthWrapper should support custom unauthorized message")
    void testUnauthorizedCustomMessage() {
        String result = AuthWrapper.requireForEdit(() -> false, () -> "OK", "No access");
        assertTrue(result.contains("Unauthorized"));
        assertTrue(result.contains("No access"));
    }

    @Test
    @DisplayName("AuthWrapper should return default delete unauthorized modal")
    void testDeleteUnauthorized() {
        String result = AuthWrapper.requireForDelete(() -> false, () -> "OK");
        assertTrue(result.contains("Unauthorized"));
        assertTrue(result.contains("permission to delete"));
    }

    @Test
    @DisplayName("AuthWrapper should return default create unauthorized modal")
    void testCreateUnauthorized() {
        String result = AuthWrapper.requireForCreate(() -> false, () -> "OK");
        assertTrue(result.contains("Unauthorized"));
        assertTrue(result.contains("permission to create content"));
    }
}
