package io.mindspice.simplypages.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StyleTest {

    @Test
    @DisplayName("Style should build class strings")
    void testClassString() {
        Style style = Style.create()
            .padding("4")
            .margin("2")
            .addClass("custom");

        String classes = style.getClassString();

        assertTrue(classes.contains("p-4"));
        assertTrue(classes.contains("m-2"));
        assertTrue(classes.contains("custom"));
    }

    @Test
    @DisplayName("Style should build utility classes and flags")
    void testUtilityClassesAndFlags() {
        Style style = Style.create()
            .textAlign("center")
            .fontSize("xl")
            .fontWeight("bold")
            .color("primary")
            .backgroundColor("light")
            .border("2")
            .rounded("lg")
            .width("full")
            .height("screen")
            .flex()
            .flexDirection("col")
            .justifyContent("between")
            .alignItems("center")
            .gap("4")
            .shadow("md")
            .addClass("custom")
            .addClasses("extra", "extra-2");

        assertTrue(style.hasClasses());
        assertFalse(style.hasInlineStyles());

        String classes = style.getClassString();
        assertTrue(classes.contains("text-center"));
        assertTrue(classes.contains("text-xl"));
        assertTrue(classes.contains("font-bold"));
        assertTrue(classes.contains("text-primary"));
        assertTrue(classes.contains("bg-light"));
        assertTrue(classes.contains("border-2"));
        assertTrue(classes.contains("rounded-lg"));
        assertTrue(classes.contains("w-full"));
        assertTrue(classes.contains("h-screen"));
        assertTrue(classes.contains("flex"));
        assertTrue(classes.contains("flex-col"));
        assertTrue(classes.contains("justify-between"));
        assertTrue(classes.contains("items-center"));
        assertTrue(classes.contains("gap-4"));
        assertTrue(classes.contains("shadow-md"));
        assertTrue(classes.contains("custom"));
        assertTrue(classes.contains("extra"));
        assertTrue(classes.contains("extra-2"));
    }

    @Test
    @DisplayName("Style should build inline styles")
    void testInlineStyles() {
        Style style = Style.create()
            .addInlineStyle("color", "red")
            .addInlineStyle("width", "100%");

        String styles = style.getStyleString();

        assertFalse(style.hasClasses());
        assertTrue(style.hasInlineStyles());
        assertTrue(styles.contains("color: red"));
        assertTrue(styles.contains("width: 100%"));
    }
}
