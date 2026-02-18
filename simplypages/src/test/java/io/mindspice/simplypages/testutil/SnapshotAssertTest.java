package io.mindspice.simplypages.testutil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotAssertTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty("simplypages.snapshotRootDir");
        System.clearProperty("updateSnapshots");
    }

    @Test
    @DisplayName("SnapshotAssert should fail when snapshot is missing and update mode is off")
    void testMissingSnapshotFails() {
        System.setProperty("simplypages.snapshotRootDir", tempDir.toString());
        System.clearProperty("updateSnapshots");
        String html = "<div><p>A</p></div>";

        assertThrows(AssertionError.class, () -> SnapshotAssert.assertMatches("group/missing", html));
    }

    @Test
    @DisplayName("SnapshotAssert should create missing snapshot when update mode is on")
    void testCreateSnapshotInUpdateMode() {
        System.setProperty("simplypages.snapshotRootDir", tempDir.toString());
        System.setProperty("updateSnapshots", "true");
        String html = "<div><p>A</p></div>";

        SnapshotAssert.assertMatches("group/create", html);
        Path snapshotPath = tempDir.resolve("group/create.snap.html");
        assertTrue(Files.exists(snapshotPath));
    }

    @Test
    @DisplayName("SnapshotAssert should fail on mismatch when update mode is off")
    void testMismatchFailsWhenNotUpdating() throws IOException {
        System.setProperty("simplypages.snapshotRootDir", tempDir.toString());
        Path snapshotPath = tempDir.resolve("group/mismatch.snap.html");
        Files.createDirectories(snapshotPath.getParent());
        Files.writeString(snapshotPath, "<div><p>Old</p></div>", StandardCharsets.UTF_8);

        assertThrows(AssertionError.class,
            () -> SnapshotAssert.assertMatches("group/mismatch", "<div><p>New</p></div>"));
    }

    @Test
    @DisplayName("SnapshotAssert should rewrite mismatch when update mode is on")
    void testMismatchRewritesInUpdateMode() throws IOException {
        System.setProperty("simplypages.snapshotRootDir", tempDir.toString());
        System.setProperty("updateSnapshots", "true");
        Path snapshotPath = tempDir.resolve("group/rewrite.snap.html");
        Files.createDirectories(snapshotPath.getParent());
        Files.writeString(snapshotPath, "<div><p>Old</p></div>", StandardCharsets.UTF_8);

        SnapshotAssert.assertMatches("group/rewrite", "<div><p>New</p></div>");
        String updated = Files.readString(snapshotPath, StandardCharsets.UTF_8).trim();
        assertTrue(updated.contains("<p>New</p>"));
    }
}
