package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.Module;

import java.util.Map;

/**
 * Interface for modules that support editing.
 *
 * <p>This interface defines the contract for modules that can be edited in-place.
 * Modules implementing this interface must provide:</p>
 * <ul>
 *   <li>An edit view (form UI) for editing the module</li>
 *   <li>Logic to apply form data back to the module</li>
 *   <li>Optional validation of form data</li>
 * </ul>
 *
 * <h3>Pattern</h3>
 * <ul>
 *   <li>{@link #buildEditView()}: Returns Component containing form fields</li>
 *   <li>{@link #applyEdits(Map)}: Mutates module with form data, returns self</li>
 *   <li>{@link #validate(Map)}: Optional validation before applying</li>
 * </ul>
 *
 * <h3>Example Implementation</h3>
 * <pre>
 * public class ContentModule extends Module implements EditAdapter&lt;ContentModule&gt; {
 *
 *     {@literal @}Override
 *     public Component buildEditView() {
 *         return new Div()
 *             .withChild(TextInput.create("title").withValue(title))
 *             .withChild(TextArea.create("content").withValue(content));
 *     }
 *
 *     {@literal @}Override
 *     public ContentModule applyEdits(Map&lt;String, String&gt; formData) {
 *         this.title = formData.get("title");
 *         this.content = formData.get("content");
 *         return this;
 *     }
 *
 *     {@literal @}Override
 *     public ValidationResult validate(Map&lt;String, String&gt; formData) {
 *         if (formData.get("title") != null && formData.get("title").length() > 200) {
 *             return ValidationResult.invalid("Title must be less than 200 characters");
 *         }
 *         return ValidationResult.valid();
 *     }
 * }
 * </pre>
 *
 * @param <T> The module type (self-referential for fluent API)
 */
public interface EditAdapter<T extends Module> {
    /**
     * Build the edit form UI for this module.
     *
     * @return Component with form fields for editing this module
     */
    Component buildEditView();

    /**
     * Apply form data to this module (mutates in place).
     *
     * @param formData Form field name → value map
     * @return this (for method chaining)
     */
    T applyEdits(Map<String, String> formData);

    /**
     * Validate form data before applying.
     *
     * @param formData Form field name → value map
     * @return Validation result indicating success or failure with errors
     */
    default ValidationResult validate(Map<String, String> formData) {
        return ValidationResult.valid();
    }
}
