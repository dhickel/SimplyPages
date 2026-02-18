package io.mindspice.simplypages.testutil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

class StructuralAssertionPolicyTest {

    private static final List<Path> ENFORCED_FILES = List.of(
        Path.of("src/test/java/io/mindspice/simplypages/core/HtmlTagTest.java"),
        Path.of("src/test/java/io/mindspice/simplypages/components/display/DataTableTest.java"),
        Path.of("src/test/java/io/mindspice/simplypages/integration/HtmxIntegrationTest.java")
    );

    @Test
    @DisplayName("Policy gate should block brittle structural assertions in enforced pilot files")
    void testForbiddenPatternsAreNotUsedInEnforcedFiles() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : ENFORCED_FILES) {
            if (!Files.exists(file)) {
                violations.add(file + ": file not found");
                continue;
            }
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("assertTrue(") && line.contains(".contains(")) {
                    violations.add(file + ":" + (i + 1) + " forbidden assertTrue(...contains(...))");
                }
                if (line.contains("assertFalse(") && line.contains(".contains(")) {
                    violations.add(file + ":" + (i + 1) + " forbidden assertFalse(...contains(...))");
                }
                if (line.contains(".indexOf(")) {
                    violations.add(file + ":" + (i + 1) + " forbidden indexOf() rendering assertion pattern");
                }
            }
        }

        if (!violations.isEmpty()) {
            fail("Structural assertion policy violations:\n - " + String.join("\n - ", violations));
        }
    }
}
