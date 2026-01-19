package io.mindspice.simplypages.components;

import io.mindspice.simplypages.components.navigation.Link;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IdRenderingTest {

    @Test
    @DisplayName("Header ID set via reflection should be rendered")
    void testHeaderIdReflection() throws Exception {
        Header header = Header.H1("Title");
        setPrivateId(header, "header-1");

        String html = header.render();
        assertTrue(html.contains("id=\"header-1\""), "Header should render ID set via reflection. Got: " + html);
    }

    @Test
    @DisplayName("Paragraph ID set via reflection should be rendered")
    void testParagraphIdReflection() throws Exception {
        Paragraph p = new Paragraph("Text");
        setPrivateId(p, "para-1");

        String html = p.render();
        assertTrue(html.contains("id=\"para-1\""), "Paragraph should render ID set via reflection. Got: " + html);
    }

    @Test
    @DisplayName("Link ID set via reflection should be rendered")
    void testLinkIdReflection() throws Exception {
        Link link = new Link("#", "Link");
        setPrivateId(link, "link-1");

        String html = link.render();
        assertTrue(html.contains("id=\"link-1\""), "Link should render ID set via reflection. Got: " + html);
    }

    private void setPrivateId(Object component, String id) throws Exception {
        Class<?> clazz = component.getClass();
        Field idField = null;
        while (clazz != null) {
            try {
                idField = clazz.getDeclaredField("id");
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (idField == null) {
            throw new NoSuchFieldException("id field not found in hierarchy of " + component.getClass().getName());
        }
        idField.setAccessible(true);
        idField.set(component, id);
    }
}
