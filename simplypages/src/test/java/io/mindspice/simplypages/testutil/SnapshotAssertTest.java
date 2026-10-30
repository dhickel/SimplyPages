package io.mindspice.simplypages.testutil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

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

    @ParameterizedTest
    @MethodSource("semanticChanges")
    void testSemanticChangesFailWithoutUpdating(String expected, String actual) throws IOException {
        System.setProperty("simplypages.snapshotRootDir", tempDir.toString());
        System.clearProperty("updateSnapshots");
        Path snapshotPath = tempDir.resolve("group/semantic.snap.html");
        Files.createDirectories(snapshotPath.getParent());
        Files.writeString(snapshotPath, HtmlNormalizer.normalize(expected), StandardCharsets.UTF_8);

        assertThrows(AssertionError.class,
            () -> SnapshotAssert.assertMatches("group/semantic", actual));
    }

    private static Stream<Arguments> semanticChanges() {
        return Stream.of(
            Arguments.of("<div>Main</div>", "<div> Main </div>"),
            Arguments.of("<p>Hello world</p>", "<p>Hello  world</p>"),
            Arguments.of("<p>Hello <em>world</em></p>", "<p>Hello<em>world</em></p>"),
            Arguments.of("<pre>first\n second</pre>", "<pre>first second</pre>"),
            Arguments.of("<textarea> draft </textarea>", "<textarea>draft</textarea>"),
            Arguments.of("<script>let value = 'a  b';</script>", "<script>let value = 'a b';</script>"),
            Arguments.of("<style>.a { color: red; }</style>", "<style>.a { color: blue; }</style>"),
            Arguments.of("<a href=\"/safe\">Open</a>", "<a href=\"/other\">Open</a>"),
            Arguments.of("<p><em>Text</em></p>", "<p><strong>Text</strong></p>")
        );
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
