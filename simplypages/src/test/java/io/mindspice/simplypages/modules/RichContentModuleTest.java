package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Image;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.editing.ValidationResult;
import io.mindspice.simplypages.testutil.HtmlAssert;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RichContentModuleTest {

    @Test
    @DisplayName("RichContentModule should render title and items")
    void testRichContentRendering() {
        RichContentModule module = RichContentModule.create("Gallery")
            .addParagraph(new Paragraph("Intro"))
            .addLink(Link.create("/link", "Link"))
            .addImage(Image.create("/img.png", "Alt"));

        String html = module.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.module.rich-content-module")
            .hasElement("div.module.rich-content-module > p.module-title")
            .hasElement("div.module.rich-content-module > div.content-container")
            .hasElementCount("div.content-container > div.content-item", 3)
            .elementTextEquals("p.module-title", "Gallery")
            .hasElement("div.content-item:nth-child(1) > p")
            .hasElement("div.content-item:nth-child(2) > a")
            .hasElement("div.content-item:nth-child(3) > img")
            .childOrder("div.content-container", "div.content-item", "div.content-item", "div.content-item");

        SnapshotAssert.assertMatches("modules/rich-content/paragraph-link-image", html);
    }

    @Test
    @DisplayName("RichContentModule should update title after applyEdits")
    void testRichContentApplyEdits() {
        RichContentModule module = RichContentModule.create("Old Title");

        module.render();
        module.applyEdits(Map.of("title", "New Title"));

        String html = module.render();

        HtmlAssert.assertThat(html)
            .elementTextEquals("p.module-title", "New Title");
    }

    @Test
    @DisplayName("RichContentModule should reject empty titles")
    void testRichContentValidation() {
        RichContentModule module = RichContentModule.create("Title");

        ValidationResult result = module.validate(Map.of("title", " "));

        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("RichContentModule should render content items without title")
    void testRichContentWithoutTitle() {
        RichContentModule module = RichContentModule.create("Title")
            .withTitle("")
            .addHeader(Header.H4("Heading"))
            .addParagraph(new Paragraph("Text"));

        String html = module.render();

        HtmlAssert.assertThat(html)
            .doesNotHaveElement("p.module-title")
            .hasElementCount("div.content-item", 2)
            .hasElement("div.content-item:nth-child(1) > h4")
            .hasElement("div.content-item:nth-child(2) > p");
    }

    @Test
    @DisplayName("RichContentModule should expose module id when set")
    void testModuleIdAccessors() {
        RichContentModule module = RichContentModule.create("Title")
            .setModuleId("module-1");

        assertEquals("module-1", module.getModuleId());
    }
}
