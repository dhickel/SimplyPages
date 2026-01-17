package io.mindspice.simplypages.modules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuleLifecycleTest {

    @Test
    @DisplayName("rebuildContent should avoid duplicate children")
    void testRebuildContentNoDuplicates() {
        ContentModule module = ContentModule.create()
            .withTitle("Title")
            .withContent("Content");

        String initial = module.render();
        assertEquals(1, countOccurrences(initial, "module-title"));

        Map<String, String> edits = new HashMap<>();
        edits.put("title", "Updated");
        edits.put("content", "Updated Content");
        edits.put("useMarkdown", "on");
        module.applyEdits(edits);

        String updated = module.render();
        assertEquals(1, countOccurrences(updated, "module-title"));
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
