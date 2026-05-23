package io.mindspice.simplypages.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountWidgetTest {

    @Test
    @DisplayName("AccountWidget should render guest links")
    void testGuestWidget() {
        String html = AccountWidget.createGuest()
            .withLoginUrl("/login")
            .withSignupUrl("/signup")
            .render();

        assertTrue(html.contains("account-widget-guest"));
        assertTrue(html.contains("href=\"/login\""));
        assertTrue(html.contains("href=\"/signup\""));
    }

    @Test
    @DisplayName("AccountWidget should render authenticated links")
    void testAuthenticatedWidget() {
        String html = AccountWidget.createAuthenticated("alice")
            .withProfileUrl("/profile")
            .withLogoutUrl("/logout")
            .render();

        assertTrue(html.contains("Hello, alice"));
        assertTrue(html.contains("href=\"/profile\""));
        assertTrue(html.contains("href=\"/logout\""));
    }

    @Test
    @DisplayName("AccountWidget should reject unsafe URLs")
    void testUnsafeUrls() {
        assertThrows(IllegalArgumentException.class, () -> AccountWidget.createGuest().withLoginUrl("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class, () -> AccountWidget.createDynamic("data:text/html,<svg>"));
    }
}
