package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class SelectTest {

    @Test
    @DisplayName("Select should render name and options")
    void testSelectRendersOptions() {
        Select select = Select.create("choice")
            .addOption("a", "Alpha")
            .addOption("b", "Beta", true);

        String html = select.render();

        HtmlAssert.assertThat(html)
            .hasElement("select.form-select")
            .attributeEquals("select.form-select", "name", "choice")
            .hasElementCount("select.form-select > option", 2)
            .attributeEquals("select.form-select > option:nth-child(1)", "value", "a")
            .elementTextEquals("select.form-select > option:nth-child(1)", "Alpha")
            .attributeEquals("select.form-select > option:nth-child(2)", "value", "b")
            .attributeEquals("select.form-select > option:nth-child(2)", "selected", "")
            .elementTextEquals("select.form-select > option:nth-child(2)", "Beta");
    }

    @Test
    @DisplayName("Select should render multiple and required attributes")
    void testSelectAttributes() {
        Select select = Select.create("items")
            .multiple()
            .required()
            .withSize(3);

        String html = select.render();

        HtmlAssert.assertThat(html)
            .hasElement("select.form-select")
            .attributeEquals("select.form-select", "multiple", "")
            .attributeEquals("select.form-select", "required", "")
            .attributeEquals("select.form-select", "size", "3");
    }

    @Test
    @DisplayName("Select should support additional attributes and list options")
    void testSelectAdditionalAttributes() {
        Select select = Select.create("items")
            .withId("items-id")
            .disabled()
            .withClass("wide")
            .addOptions(List.of("One", "Two"))
            .withWidth("300px")
            .withMaxWidth("500px")
            .withMinWidth("200px");

        String html = select.render();

        HtmlAssert.assertThat(html)
            .hasElement("select#items-id.form-select.wide")
            .attributeEquals("select#items-id", "name", "items")
            .attributeEquals("select#items-id", "disabled", "")
            .hasElement("select#items-id[style*=width]")
            .hasElement("select#items-id[style*=max-width]")
            .hasElement("select#items-id[style*=min-width]")
            .attributeEquals("select#items-id > option:nth-child(1)", "value", "One")
            .elementTextEquals("select#items-id > option:nth-child(1)", "One")
            .attributeEquals("select#items-id > option:nth-child(2)", "value", "Two")
            .elementTextEquals("select#items-id > option:nth-child(2)", "Two");
    }

    @Test
    @DisplayName("Select option should escape labels and support disabled")
    void testSelectOptionEscaping() {
        Select.Option option = new Select.Option("<script>", "<b>Label</b>", true)
            .disabled();

        String html = option.render();

        HtmlAssert.assertThat(html)
            .hasElement("option")
            .attributeEquals("option", "selected", "")
            .attributeEquals("option", "disabled", "")
            .attributeEquals("option", "value", "<script>")
            .elementTextEquals("option", "<b>Label</b>");
    }
}
