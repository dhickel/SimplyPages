package io.mindspice.simplypages.testutil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class HtmlAssert {

    private final String html;
    private final Document document;

    private HtmlAssert(String html) {
        this.html = html;
        this.document = Jsoup.parseBodyFragment(html);
    }

    public static HtmlAssert assertThat(String html) {
        return new HtmlAssert(html);
    }

    public HtmlAssert hasElement(String selector) {
        Element element = document.selectFirst(selector);
        assertNotNull(element, () -> "Expected element for selector '" + selector + "' in:\n" + html);
        return this;
    }

    public HtmlAssert doesNotHaveElement(String selector) {
        Element element = document.selectFirst(selector);
        assertNull(element, () -> "Did not expect element for selector '" + selector + "' in:\n" + html);
        return this;
    }

    public HtmlAssert hasElementCount(String selector, int expected) {
        int actual = document.select(selector).size();
        assertEquals(expected, actual, () -> "Selector '" + selector + "' count mismatch in:\n" + html);
        return this;
    }

    public HtmlAssert elementTextEquals(String selector, String expected) {
        Element element = document.selectFirst(selector);
        assertNotNull(element, () -> "Expected element for selector '" + selector + "' in:\n" + html);
        assertEquals(expected, element.text(),
            () -> "Text mismatch for selector '" + selector + "' in:\n" + html);
        return this;
    }

    public HtmlAssert attributeEquals(String selector, String attribute, String expected) {
        Element element = document.selectFirst(selector);
        assertNotNull(element, () -> "Expected element for selector '" + selector + "' in:\n" + html);
        assertEquals(expected, element.attr(attribute),
            () -> "Attribute mismatch for selector '" + selector + "' attribute '" + attribute + "' in:\n" + html);
        return this;
    }

    public HtmlAssert childOrder(String parentSelector, String... expectedChildSelectors) {
        Element parent = document.selectFirst(parentSelector);
        assertNotNull(parent, () -> "Expected parent element for selector '" + parentSelector + "' in:\n" + html);

        Elements children = parent.children();
        assertEquals(expectedChildSelectors.length, children.size(),
            () -> "Child count mismatch for parent '" + parentSelector + "' in:\n" + html);

        for (int i = 0; i < expectedChildSelectors.length; i++) {
            final int index = i;
            Element child = children.get(i);
            String expectedSelector = expectedChildSelectors[i];
            assertTrue(child.is(expectedSelector),
                () -> "Child order mismatch at index " + index + " for parent '" + parentSelector
                    + "'. Expected '" + expectedSelector + "', found <" + child.tagName() + "> in:\n" + html);
        }
        return this;
    }
}
