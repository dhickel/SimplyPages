package io.mindspice.simplypages.core;

/**
 * Wraps a slot value with runtime type information for safety.
 */
public record TypedValue(Class<?> type, Object value) {

    public TypedValue {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (value != null && !type.isInstance(value)) {
            throw new IllegalArgumentException(
                String.format("Type mismatch: expected %s, got %s",
                    type.getName(), value.getClass().getName())
            );
        }
    }

    public static <T> TypedValue of(Class<T> type, T value) {
        return new TypedValue(type, value);
    }

    public static TypedValue string(String value) {
        return of(String.class, value);
    }

    public static TypedValue bool(Boolean value) {
        return of(Boolean.class, value);
    }

    public static TypedValue integer(Integer value) {
        return of(Integer.class, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) value;
    }

    public <T> T getValueAs(Class<T> expectedType) {
        if (!expectedType.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Cannot cast " + type + " to " + expectedType);
        }
        return expectedType.cast(value);
    }
}
