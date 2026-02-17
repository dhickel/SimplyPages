package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.navigation.SideNav;
import io.mindspice.simplypages.core.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShellBuilderTest {

    @Test
    @DisplayName("ShellBuilder should render full document with side nav and HTMX")
    void testShellBuilderFullDocument() {
        SideNav sideNav = SideNavBuilder.create()
            .addLink("Dashboard", "/dashboard")
            .build();

        String html = ShellBuilder.create()
            .withTopBanner(
                BannerBuilder.create()
                    .withLayout(BannerBuilder.BannerLayout.HORIZONTAL)
                    .withTitle("Portal")
                    .build()
            )
            .withAccountBar(AccountBarBuilder.create().addLeftLink("Home", "/").build())
            .withSideNav(sideNav, true)
            .withContentTarget("page-content")
            .withCustomCss("/css/custom.css")
            .withPageTitle("My Portal")
            .build();

        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("<title>My Portal</title>"));
        assertTrue(html.contains("main-container has-sidebar"));
        assertTrue(html.contains("collapsible-sidebar"));
        assertTrue(html.contains("sidebar-toggle"));
        assertTrue(html.contains("toggleSidebar"));
        assertTrue(html.contains("id=\"page-content\""));
        assertTrue(html.contains("hx-get=\"/home\""));
        assertTrue(html.contains("/css/custom.css"));
        assertTrue(html.contains("htmx.min.js"));
    }

    @Test
    @DisplayName("ShellBuilder body should respect HTMX flag")
    void testShellBuilderBodyWithoutHtmx() {
        Component body = ShellBuilder.create()
            .withHtmx(false)
            .withContentTarget("content-area")
            .buildBody();

        String html = body.render();

        assertTrue(html.contains("shell-body"));
        assertTrue(html.contains("id=\"content-area\""));
        assertFalse(html.contains("hx-get=\"/home\""));
        assertFalse(html.contains("hx-trigger=\"load\""));
    }
}
