package io.mindspice.simplypages.core;

/**
 * Immutable value wrapper with an explicit runtime type token.
 *
 * <p>Used by {@link SlotKeyMap} to preserve type intent across string-keyed storage.</p>
 *
 * <p>Mutability/thread-safety: immutable and thread-safe if contained value is safely shared.</p>
 */
public record TypedValue(Class<?> type, Object value) {

    /**
     * Validates that {@code type} is non-null and, when {@code value} is non-null, that
     * {@code value} is an instance of {@code type}.
     */
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

    /**
     * Creates a typed wrapper from the provided type token and value.
     */
    public static <T> TypedValue of(Class<T> type, T value) {
        return new TypedValue(type, value);
    }

    /**
     * Convenience factory for {@link String} values.
     */
    public static TypedValue string(String value) {
        return of(String.class, value);
    }

    /**
     * Convenience factory for {@link Boolean} values.
     */
    public static TypedValue bool(Boolean value) {
        return of(Boolean.class, value);
    }

    /**
     * Convenience factory for {@link Integer} values.
     */
    public static TypedValue integer(Integer value) {
        return of(Integer.class, value);
    }

    /**
     * Returns the raw wrapped value without additional type checks.
     *
     * @param <T> caller-asserted type
     * @return wrapped value cast to {@code T}
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) value;
    }

    /**
     * Returns the wrapped value after validating assignability to {@code expectedType}.
     *
     * @param expectedType type required by caller
     * @param <T> expected type
     * @return wrapped value cast to {@code expectedType}
     * @throws IllegalArgumentException when {@code expectedType} is not assignable from stored type
     */
    public <T> T getValueAs(Class<T> expectedType) {
        if (!expectedType.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Cannot cast " + type + " to " + expectedType);
        }
        return expectedType.cast(value);
    }
}
