package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.core.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountBarBuilderTest {

    @Test
    @DisplayName("AccountBarBuilder should render left/right sections and styles")
    void testAccountBarWithWidgets() {
        Component bar = AccountBarBuilder.create()
            .addLeftLink("Home", "/")
            .addRightAuthenticatedWidget("alice")
            .withBackgroundColor("#f5f5f5")
            .withClass("custom-bar")
            .build();

        String html = bar.render();

        assertTrue(html.contains("account-bar custom-bar"));
        assertTrue(html.contains("background-color: #f5f5f5"));
        assertTrue(html.contains("account-bar-left"));
        assertTrue(html.contains("account-bar-right"));
        assertTrue(html.contains("Hello, alice"));
    }

    @Test
    @DisplayName("AccountBarBuilder should support dynamic account widgets")
    void testAccountBarDynamicWidget() {
        Component bar = AccountBarBuilder.create()
            .addRightAccountWidget("/api/account-status")
            .build();

        String html = bar.render();

        assertTrue(html.contains("hx-get=\"/api/account-status\""));
        assertTrue(html.contains("hx-trigger=\"load\""));
        assertTrue(html.contains("hx-swap=\"outerHTML\""));
    }
}
