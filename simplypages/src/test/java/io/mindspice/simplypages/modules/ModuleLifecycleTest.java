package io.mindspice.simplypages.modules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModuleLifecycleTest {

    @Test
    @DisplayName("rebuildContent should avoid duplicate children")
    void testRebuildContentNoDuplicates() {
        ContentModule module = ContentModule.create()
            .withTitle("Title")
            .withContent("Content");

        String initial = module.render();
        assertEquals(1, countOccurrences(initial, "module-title"));

        module.withContent("Unapplied model change");
        assertEquals(initial, module.render(), "ordinary rendering must not rebuild configuration-only changes");

        Map<String, String> edits = new HashMap<>();
        edits.put("title", "Updated");
        edits.put("content", "Updated Content");
        edits.put("useMarkdown", "on");
        module.applyEdits(edits);

        String updated = module.render();
        assertEquals(1, countOccurrences(updated, "module-title"));
        assertEquals(1, countOccurrences(updated, "Updated Content"));
        assertFalse(updated.contains("Unapplied model change"));
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = 0;
        while ((index = haystack.indexOf(needle, index)) != -1) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
