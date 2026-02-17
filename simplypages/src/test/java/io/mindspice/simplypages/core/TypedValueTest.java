package io.mindspice.simplypages.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TypedValueTest {

    @Test
    @DisplayName("TypedValue should preserve type and value")
    void testTypeChecking() {
        TypedValue value = TypedValue.string("Hello");
        assertEquals(String.class, value.type());
        assertEquals("Hello", value.getValue());
    }

    @Test
    @DisplayName("TypedValue should reject mismatched types")
    void testTypeMismatch() {
        assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("rawtypes")
            Class raw = String.class;
            TypedValue.of(raw, 123);
        });
    }

    @Test
    @DisplayName("TypedValue convenience factories should work")
    void testConvenienceFactories() {
        TypedValue boolValue = TypedValue.bool(true);
        TypedValue intValue = TypedValue.integer(42);

        assertEquals(Boolean.class, boolValue.type());
        assertEquals(Boolean.TRUE, boolValue.getValue());
        assertEquals(Integer.class, intValue.type());
        assertEquals(Integer.valueOf(42), intValue.getValue());
    }
}
