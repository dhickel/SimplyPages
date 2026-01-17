package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.Module;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EditAdapterTest {

    @Test
    @DisplayName("EditAdapter default validate should return valid result")
    void testDefaultValidate() {
        TestModule module = new TestModule();

        ValidationResult result = module.validate(Map.of());

        assertTrue(result.isValid());
    }

    private static class TestModule extends Module implements EditAdapter<TestModule> {
        private TestModule() {
            super("div");
        }

        @Override
        public Component buildEditView() {
            return new Div();
        }

        @Override
        public TestModule applyEdits(Map<String, String> formData) {
            return this;
        }

        @Override
        protected void buildContent() {
            // No-op for test module
        }
    }
}
