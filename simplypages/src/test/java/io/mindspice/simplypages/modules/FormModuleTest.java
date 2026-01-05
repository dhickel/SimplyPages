package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.components.forms.Form;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormModuleTest {

    @Test
    @DisplayName("FormModule should render form fields and submit URL")
    void testFormModuleRendering() {
        FormModule module = FormModule.create()
            .withTitle("Contact")
            .withDescription("Send us a message")
            .withSubmitUrl("/submit")
            .addField("Name", TextInput.create("name"));

        String html = module.render();

        assertTrue(html.contains("form-module"));
        assertTrue(html.contains("Contact"));
        assertTrue(html.contains("module-description"));
        assertTrue(html.contains("hx-post=\"/submit\""));
        assertTrue(html.contains("name=\"name\""));
    }

    @Test
    @DisplayName("FormModule should render custom form without optional sections")
    void testFormModuleCustomForm() {
        Form customForm = Form.create()
            .withAction("/send")
            .withMethod(Form.Method.POST);

        FormModule module = FormModule.create()
            .withForm(customForm)
            .addField("Email", TextInput.email("email"));

        String html = module.render();

        assertTrue(html.contains("action=\"/send\""));
        assertTrue(html.contains("name=\"email\""));
        assertFalse(html.contains("module-title"));
        assertFalse(html.contains("module-description"));
        assertFalse(html.contains("hx-post=\""));
    }
}
