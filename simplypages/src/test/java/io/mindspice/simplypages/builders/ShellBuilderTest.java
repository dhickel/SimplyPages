package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.navigation.SideNav;
import io.mindspice.simplypages.core.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @DisplayName("ShellBuilder should include framework css by default")
    void testDefaultFrameworkCssIncluded() {
        String html = ShellBuilder.create().build();
        assertTrue(html.contains("href=\"/css/framework.css\""));
    }

    @Test
    @DisplayName("ShellBuilder should disable framework css when requested")
    void testFrameworkCssDisabled() {
        String html = ShellBuilder.create()
            .withFrameworkCss(false)
            .build();
        assertFalse(html.contains("href=\"/css/framework.css\""));
        assertFalse(html.contains("rel=\"stylesheet\""));
    }

    @Test
    @DisplayName("ShellBuilder should use custom framework css path")
    void testFrameworkCssPathOverride() {
        String html = ShellBuilder.create()
            .withFrameworkCssPath("/css/base.css")
            .build();
        assertTrue(html.contains("href=\"/css/base.css\""));
        assertFalse(html.contains("href=\"/css/framework.css\""));
    }

    @Test
    @DisplayName("ShellBuilder should support no-css mode")
    void testNoCssMode() {
        String html = ShellBuilder.create()
            .withFrameworkCss(false)
            .build();
        assertFalse(html.contains("rel=\"stylesheet\""));
        assertTrue(html.contains("<!DOCTYPE html>"));
    }

    @Test
    @DisplayName("ShellBuilder withCustomCss(String) should reset to a single stylesheet")
    void testWithCustomCssStringResetsList() {
        String html = ShellBuilder.create()
            .addCustomCss("/css/a.css")
            .addCustomCss("/css/b.css")
            .withCustomCss("/css/only.css")
            .build();

        assertTrue(html.contains("href=\"/css/only.css\""));
        assertFalse(html.contains("href=\"/css/a.css\""));
        assertFalse(html.contains("href=\"/css/b.css\""));
    }

    @Test
    @DisplayName("ShellBuilder addCustomCss should append in order and dedupe")
    void testAddCustomCssOrderAndDedupe() {
        String html = ShellBuilder.create()
            .addCustomCss("/css/a.css")
            .addCustomCss("/css/b.css")
            .addCustomCss("/css/a.css")
            .build();

        int frameworkIdx = html.indexOf("href=\"/css/framework.css\"");
        int aIdx = html.indexOf("href=\"/css/a.css\"");
        int bIdx = html.indexOf("href=\"/css/b.css\"");
        int secondAIdx = html.indexOf("href=\"/css/a.css\"", aIdx + 1);

        assertTrue(frameworkIdx >= 0);
        assertTrue(aIdx > frameworkIdx);
        assertTrue(bIdx > aIdx);
        assertTrue(secondAIdx == -1);
    }

    @Test
    @DisplayName("ShellBuilder withCustomCss(List) should set ordered custom stylesheets")
    void testWithCustomCssList() {
        String html = ShellBuilder.create()
            .withCustomCss(List.of("/css/app.css", "/css/page.css", "/css/module.css"))
            .build();

        int appIdx = html.indexOf("href=\"/css/app.css\"");
        int pageIdx = html.indexOf("href=\"/css/page.css\"");
        int moduleIdx = html.indexOf("href=\"/css/module.css\"");

        assertTrue(appIdx >= 0);
        assertTrue(pageIdx > appIdx);
        assertTrue(moduleIdx > pageIdx);
    }

    @Test
    @DisplayName("ShellBuilder should validate css path inputs")
    void testCssPathValidation() {
        assertThrows(IllegalArgumentException.class, () -> ShellBuilder.create().withCustomCss(" "));
        assertThrows(IllegalArgumentException.class, () -> ShellBuilder.create().addCustomCss(null));
        assertThrows(IllegalArgumentException.class, () -> ShellBuilder.create().withFrameworkCssPath(""));
        assertThrows(IllegalArgumentException.class, () -> ShellBuilder.create().withCustomCss((List<String>) null));
        assertThrows(
            IllegalArgumentException.class,
            () -> ShellBuilder.create().withCustomCss(List.of("/css/ok.css", " "))
        );
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
