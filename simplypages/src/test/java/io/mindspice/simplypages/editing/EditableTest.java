package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.Module;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EditableTest {

    @Test
    @DisplayName("Editable implementations should be testable")
    void testEditableImplementation() {
        TestModule module = new TestModule();
        // Since validate is not default, we just test that our test implementation works
        ValidationResult result = module.validate(Map.of());
        assertTrue(result.isValid());
    }

    private static class TestModule extends Module implements Editable<TestModule> {
        private TestModule() {
            super("div");
        }

        @Override
        public Component buildEditView() {
            return new Div();
        }

        @Override
        public ValidationResult validate(Map<String, String> formData) {
            return ValidationResult.valid();
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
