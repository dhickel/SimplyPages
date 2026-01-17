package io.mindspice.simplypages.core;

/**
 * Wraps a slot value with runtime type information for safety.
 */
public final class TypedValue {
    private final Class<?> type;
    private final Object value;

    private TypedValue(Class<?> type, Object value) {
        this.type = type;
        this.value = value;
    }

    public static <T> TypedValue of(Class<T> type, T value) {
        if (value != null && !type.isInstance(value)) {
            throw new IllegalArgumentException(
                String.format("Type mismatch: expected %s, got %s",
                    type.getName(), value.getClass().getName())
            );
        }
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

    public Class<?> getType() {
        return type;
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
