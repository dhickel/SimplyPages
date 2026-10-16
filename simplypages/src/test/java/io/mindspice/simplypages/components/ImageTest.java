package io.mindspice.simplypages.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.Template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageTest {

    @Test
    void rejectedSetterPreservesAllFieldsAttributesOrderAndStaticSnapshot() {
        Image image = Image.create("/old?x=1&y=2", "<alt>").withId("image").withSize("120", "80");
        Template before = image.compile();
        String original = image.render();
        for (String prohibited : new String[] {"JaVa\tScRiPt:x", "da\nta:text/plain,x", "DATA:IM\rAGELESS/x"}) {
            assertThrows(IllegalArgumentException.class, () -> image.withSrc(prohibited));
            assertEquals("/old?x=1&y=2", image.getSrc());
            assertEquals("<alt>", image.getAlt());
            assertEquals("120", image.getWidth());
            assertEquals("80", image.getHeight());
            assertEquals(original, image.render());
            assertEquals(original, before.render(RenderContext.empty()));
        }
        String raw = " DATA:IM\tAGE/svg+xml,<svg/>\r ";
        assertSame(image, image.withSrc(raw));
        assertEquals(raw, image.getSrc());
        assertEquals(original, before.render(RenderContext.empty()));
        assertEquals(image.render(), image.compile().render(RenderContext.empty()));
        assertTrue(image.render().indexOf("height=") < image.render().indexOf("src="));
        assertTrue(image.render().contains("&lt;svg/>"));
    }

    @Test
    void customRendererRemainsLiveAndTypedFluentReturnsStayIntact() {
        SlotKey<String> value = SlotKey.of("image-context");
        Image custom = new Image("/first", null) {
            @Override public String render(RenderContext context) {
                return super.render(context) + context.get(value).orElseThrow();
            }
        };
        Template compiled = custom.compile();
        assertSame(custom, custom.withSrc("data:im\tage/gif,AA").withId("image").withClass("picture").withSize("20", "30"));
        assertEquals("", custom.getAlt());
        for (String text : new String[] {"one", "two"}) {
            RenderContext context = RenderContext.of(value, text);
            assertEquals(custom.render(context), compiled.render(context));
            assertTrue(compiled.render(context).endsWith(text));
        }
    }

    @Test
    @DisplayName("Image should render src and alt attributes")
    void testImageRender() {
        Image image = Image.create("https://example.com/img.png", "Alt");
        String html = image.render();

        assertTrue(html.contains("src=\"https://example.com/img.png\""));
        assertTrue(html.contains("alt=\"Alt\""));
    }

    @Test
    @DisplayName("Image should render id and size attributes")
    void testImageIdAndSize() {
        Image image = Image.create("/img.png", "Alt")
            .withId("image-1")
            .withSize("100", "200");

        String html = image.render();

        assertTrue(html.contains("id=\"image-1\""));
        assertTrue(html.contains("width=\"100\""));
        assertTrue(html.contains("height=\"200\""));
    }

    @Test
    @DisplayName("Image should update src with withSrc")
    void testImageWithSrc() {
        Image image = Image.create("/img.png")
            .withSrc("data:image/png;base64,AAA");

        String html = image.render();

        assertTrue(html.contains("src=\"data:image/png;base64,AAA\""));
    }
}
