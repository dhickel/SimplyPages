package io.mindspice.jhf.core;

import java.util.Objects;

/**
 * A typed key for identifying slots in a Template.
 *
 * @param <T> The type of content this slot accepts (typically String or Component)
 */
public class SlotKey<T> {
    private final String name;

    private SlotKey(String name) {
        this.name = name;
    }

    /**
     * Creates a new SlotKey with the given name.
     *
     * @param name The unique name of the slot
     * @param <T> The type of content
     * @return A new SlotKey
     */
    public static <T> SlotKey<T> of(String name) {
        return new SlotKey<>(name);
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SlotKey<?> slotKey = (SlotKey<?>) o;
        return Objects.equals(name, slotKey.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "SlotKey{" + "name='" + name + '\'' + '}';
    }
}
