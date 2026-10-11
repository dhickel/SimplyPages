package io.mindspice.simplypages.editing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationResultTest {

    @Test
    @DisplayName("ValidationResult.valid should be valid and immutable")
    void testValid() {
        ValidationResult result = ValidationResult.valid();

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.getErrors().add("Error"));
        assertThrows(UnsupportedOperationException.class, () -> result.getErrors().clear());
    }

    @Test
    @DisplayName("ValidationResult.invalid should be invalid")
    void testInvalid() {
        ValidationResult result = ValidationResult.invalid("Error");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Error"));
    }

    @Test
    @DisplayName("ValidationResult.invalid(List) should be invalid")
    void testInvalidList() {
        ValidationResult result = ValidationResult.invalid(List.of("A", "B"));
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("A"));
    }

    @Test
    @DisplayName("ValidationResult should snapshot caller-owned arrays and lists")
    void testFactoryInputsAreSnapshotted() {
        String[] sourceArray = {"array-a", "array-b"};
        ValidationResult arrayResult = ValidationResult.invalid(sourceArray);
        sourceArray[0] = "changed";

        List<String> sourceList = new ArrayList<>(Arrays.asList("list-a", "list-b"));
        ValidationResult listResult = ValidationResult.invalid(sourceList);
        sourceList.set(0, "changed");
        sourceList.add("caller-only");

        assertEquals(Arrays.asList("array-a", "array-b"), arrayResult.getErrors());
        assertEquals(Arrays.asList("list-a", "list-b"), listResult.getErrors());
    }

    @Test
    @DisplayName("ValidationResult errors reject every exposed mutation path")
    void testInvalidErrorsAreImmutable() {
        List<String> errors = ValidationResult.invalid("B", "A").getErrors();

        assertThrows(UnsupportedOperationException.class, () -> errors.add("C"));
        assertThrows(UnsupportedOperationException.class, () -> errors.remove("B"));
        assertThrows(UnsupportedOperationException.class, () -> errors.set(0, "C"));
        assertThrows(UnsupportedOperationException.class, errors::clear);

        Iterator<String> iterator = errors.iterator();
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove);

        ListIterator<String> listIterator = errors.listIterator();
        listIterator.next();
        assertThrows(UnsupportedOperationException.class, () -> listIterator.set("C"));
        assertThrows(UnsupportedOperationException.class, () -> listIterator.add("C"));
        assertThrows(UnsupportedOperationException.class, listIterator::remove);

        List<String> subList = errors.subList(0, 1);
        assertThrows(UnsupportedOperationException.class, () -> subList.add("C"));
        assertThrows(UnsupportedOperationException.class, subList::clear);

        assertThrows(UnsupportedOperationException.class, () -> errors.sort(String::compareTo));
        assertThrows(UnsupportedOperationException.class, () -> errors.replaceAll(error -> error.toLowerCase()));
        assertThrows(UnsupportedOperationException.class, () -> errors.removeIf(error -> true));
        assertEquals(Arrays.asList("B", "A"), errors);
    }

    @Test
    @DisplayName("ValidationResult should preserve order, duplicates, null elements, and formatting")
    void testErrorsPreserveCompatibility() {
        ValidationResult result = ValidationResult.invalid("first", null, "first");

        assertEquals(Arrays.asList("first", null, "first"), result.getErrors());
        assertEquals("first|null|first", result.getErrorsAsString("|"));
        assertEquals("ValidationResult{valid=false, errors=[first, null, first]}", result.toString());
    }

    @Test
    @DisplayName("ValidationResult.invalid should reject null and empty error containers")
    void testInvalidRequiresErrors() {
        assertThrows(IllegalArgumentException.class, () -> ValidationResult.invalid());
        assertThrows(IllegalArgumentException.class, () -> ValidationResult.invalid(new String[0]));
        assertThrows(IllegalArgumentException.class, () -> ValidationResult.invalid(List.of()));
        assertThrows(IllegalArgumentException.class, () -> ValidationResult.invalid((List<String>) null));
        assertThrows(IllegalArgumentException.class, () -> ValidationResult.invalid((String[]) null));
    }

    @Test
    @DisplayName("ValidationResult should format errors as strings")
    void testErrorsAsString() {
        ValidationResult result = ValidationResult.invalid("A", "B");

        assertEquals("A; B", result.getErrorsAsString("; "));
        assertTrue(result.toString().contains("errors"));
    }

    @Test
    @DisplayName("ValidationResult snapshots remain stable during concurrent source changes and reads")
    void testSnapshotRemainsStableDuringConcurrentReads() throws Exception {
        List<String> source = new ArrayList<>(Arrays.asList("original-a", "original-b"));
        ValidationResult result = ValidationResult.invalid(source);
        List<String> expected = List.of("original-a", "original-b");
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<?> sourceMutator = executor.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 10_000; i++) {
                        source.clear();
                        source.add("caller-change-" + i);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
            });
            Callable<Void> reader = () -> {
                start.await();
                for (int i = 0; i < 10_000; i++) {
                    assertEquals(expected, result.getErrors());
                }
                return null;
            };
            Future<Void> firstReader = executor.submit(reader);
            Future<Void> secondReader = executor.submit(reader);
            start.countDown();

            sourceMutator.get(5, TimeUnit.SECONDS);
            firstReader.get(5, TimeUnit.SECONDS);
            secondReader.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertEquals(expected, result.getErrors());
        assertFalse(source.equals(result.getErrors()));
    }
}
