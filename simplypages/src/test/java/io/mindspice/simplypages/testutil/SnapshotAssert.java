package io.mindspice.simplypages.testutil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public final class SnapshotAssert {

    private static final String SNAPSHOT_UPDATE_PROPERTY = "updateSnapshots";
    private static final String SNAPSHOT_ROOT_PROPERTY = "simplypages.snapshotRootDir";
    private static final Path DEFAULT_SNAPSHOT_ROOT = Paths.get("src", "test", "resources", "snapshots");

    private SnapshotAssert() {
    }

    public static void assertMatches(String snapshotKey, String html) {
        validateSnapshotKey(snapshotKey);
        String normalizedActual = HtmlNormalizer.normalize(html);
        Path root = getSnapshotRoot();
        Path snapshotFile = resolveSnapshotPath(root, snapshotKey);
        boolean updateMode = Boolean.getBoolean(SNAPSHOT_UPDATE_PROPERTY);

        try {
            Files.createDirectories(snapshotFile.getParent());
            if (!Files.exists(snapshotFile)) {
                if (updateMode) {
                    Files.writeString(snapshotFile, normalizedActual, StandardCharsets.UTF_8);
                    return;
                }
                fail("Missing snapshot: " + snapshotFile
                    + ". Re-run with -DupdateSnapshots=true to create it.");
            }

            String expected = Files.readString(snapshotFile, StandardCharsets.UTF_8).trim();
            if (!expected.equals(normalizedActual)) {
                if (updateMode) {
                    Files.writeString(snapshotFile, normalizedActual, StandardCharsets.UTF_8);
                    return;
                }
                assertEquals(expected, normalizedActual,
                    "Snapshot mismatch: " + snapshotFile
                        + ". Re-run with -DupdateSnapshots=true to accept changes.");
            }
        } catch (IOException e) {
            fail("Snapshot assertion failed for key '" + snapshotKey + "': " + e.getMessage(), e);
        }
    }

    private static void validateSnapshotKey(String snapshotKey) {
        if (snapshotKey == null || snapshotKey.isBlank() || !snapshotKey.contains("/")) {
            throw new IllegalArgumentException(
                "Snapshot key must match '<group>/<scenario>', got: '" + snapshotKey + "'.");
        }
    }

    private static Path getSnapshotRoot() {
        String configuredRoot = System.getProperty(SNAPSHOT_ROOT_PROPERTY);
        if (configuredRoot == null || configuredRoot.isBlank()) {
            return DEFAULT_SNAPSHOT_ROOT.toAbsolutePath().normalize();
        }
        return Paths.get(configuredRoot).toAbsolutePath().normalize();
    }

    private static Path resolveSnapshotPath(Path root, String snapshotKey) {
        Path snapshotFile = root.resolve(snapshotKey + ".snap.html").normalize();
        if (!snapshotFile.startsWith(root)) {
            throw new IllegalArgumentException("Snapshot key resolves outside snapshot root: " + snapshotKey);
        }
        return snapshotFile;
    }
}
