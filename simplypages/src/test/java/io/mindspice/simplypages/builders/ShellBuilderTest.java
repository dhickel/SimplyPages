package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.navigation.SideNav;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jsoup.Jsoup;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(html.contains("mobile-sidebar-toggle"));
        assertTrue(html.contains("toggleMobileSidebar()"));
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
    @DisplayName("ShellBuilder should include custom JS after framework JS in deterministic order")
    void testCustomJsOrder() {
        String html = ShellBuilder.create()
            .addCustomJs("/js/app.js")
            .addCustomJs("/js/feature.js")
            .build();

        List<String> scriptSources = Jsoup.parse(html)
            .select("head script[src]")
            .stream()
            .map(script -> script.attr("src"))
            .collect(Collectors.toList());

        assertEquals(
            List.of("/webjars/htmx.org/dist/htmx.min.js", "/js/framework.js", "/js/app.js", "/js/feature.js"),
            scriptSources
        );
        HtmlAssert.assertThat(html)
            .hasElement("head script[src='/webjars/htmx.org/dist/htmx.min.js'][defer]")
            .hasElement("head script[src='/js/framework.js'][defer]")
            .hasElement("head script[src='/js/app.js'][defer]")
            .hasElement("head script[src='/js/feature.js'][defer]");
    }

    @Test
    @DisplayName("ShellBuilder addCustomJs should dedupe while preserving insertion order")
    void testCustomJsDedupe() {
        String html = ShellBuilder.create()
            .addCustomJs("/js/app.js")
            .addCustomJs("/js/feature.js")
            .addCustomJs("/js/app.js")
            .build();

        HtmlAssert.assertThat(html)
            .hasElementCount("head script[src='/js/app.js']", 1)
            .hasElementCount("head script[src='/js/feature.js']", 1)
            .appearsBefore("head script[src='/js/app.js']", "head script[src='/js/feature.js']");
    }

    @Test
    @DisplayName("ShellBuilder withCustomJs(String) should reset to a single script")
    void testWithCustomJsStringResetsList() {
        String html = ShellBuilder.create()
            .addCustomJs("/js/a.js")
            .addCustomJs("/js/b.js")
            .withCustomJs("/js/only.js")
            .build();

        HtmlAssert.assertThat(html)
            .hasElement("head script[src='/js/only.js']")
            .doesNotHaveElement("head script[src='/js/a.js']")
            .doesNotHaveElement("head script[src='/js/b.js']");
    }

    @Test
    @DisplayName("ShellBuilder withCustomJs(List) should replace prior scripts with ordered list")
    void testWithCustomJsListReplacement() {
        String html = ShellBuilder.create()
            .addCustomJs("/js/legacy.js")
            .withCustomJs(List.of("/js/alpha.js", "/js/beta.js", "/js/alpha.js"))
            .build();

        HtmlAssert.assertThat(html)
            .doesNotHaveElement("head script[src='/js/legacy.js']")
            .hasElementCount("head script[src='/js/alpha.js']", 1)
            .hasElementCount("head script[src='/js/beta.js']", 1)
            .appearsBefore("head script[src='/js/alpha.js']", "head script[src='/js/beta.js']");
    }

    @Test
    @DisplayName("ShellBuilder should validate JS path inputs")
    void testJsPathValidation() {
        assertThrows(IllegalArgumentException.class, () -> ShellBuilder.create().withCustomJs(" "));
        assertThrows(IllegalArgumentException.class, () -> ShellBuilder.create().addCustomJs(null));
        assertThrows(IllegalArgumentException.class, () -> ShellBuilder.create().withCustomJs((List<String>) null));
        assertThrows(
            IllegalArgumentException.class,
            () -> ShellBuilder.create().withCustomJs(List.of("/js/ok.js", " "))
        );
    }

    @Test
    @DisplayName("ShellBuilder withContentTargetId should delegate to withContentTarget")
    void testContentTargetIdAlias() {
        String html = ShellBuilder.create()
            .withContentTargetId("page-content")
            .build();

        HtmlAssert.assertThat(html).hasElement("div#page-content[hx-get='/home'][hx-trigger='load']");
    }

    @Test
    @DisplayName("ShellBuilder should apply content target class")
    void testContentTargetClass() {
        String html = ShellBuilder.create()
            .withContentTargetClass("page-fragment")
            .build();

        HtmlAssert.assertThat(html).hasElement("div#content-area.page-fragment");
    }

    @Test
    @DisplayName("ShellBuilder should apply content wrapper in full build and body build")
    void testContentWrapperAppliedToBuildAndBody() {
        ShellBuilder builder = ShellBuilder.create()
            .withContentWrapper(contentTarget ->
                new HtmlTag("section")
                    .withAttribute("class", "content-shell")
                    .withChild(contentTarget)
            );

        String fullHtml = builder.build();
        String bodyHtml = builder.buildBody().render();

        HtmlAssert.assertThat(fullHtml)
            .hasElement("main.content-wrapper > section.content-shell > div#content-area[hx-get='/home'][hx-trigger='load']");
        HtmlAssert.assertThat(bodyHtml)
            .hasElement("main.content-wrapper > section.content-shell > div#content-area[hx-get='/home'][hx-trigger='load']");
    }

    @Test
    @DisplayName("ShellBuilder should guard content wrapper null usage")
    void testContentWrapperNullGuards() {
        assertThrows(IllegalArgumentException.class, () -> ShellBuilder.create().withContentWrapper(null));
        assertThrows(
            IllegalArgumentException.class,
            () -> ShellBuilder.create().withContentWrapper(contentTarget -> null).build()
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

    @Test
    @DisplayName("ShellBuilder should not render mobile sidebar toggle without side nav")
    void testNoMobileToggleWhenNoSidebar() {
        String html = ShellBuilder.create().build();
        assertFalse(html.contains("mobile-sidebar-toggle"));
        assertFalse(html.contains("toggleMobileSidebar()"));
    }

    @Test
    @DisplayName("ShellBuilder should render provided initial content and skip auto-load")
    void testInitialContentDisablesAutoLoad() {
        String html = ShellBuilder.create()
            .withContent(new Paragraph("Initial body"))
            .build();

        assertTrue(html.contains("Initial body"));
        assertFalse(html.contains("hx-get=\"/home\""));
        assertFalse(html.contains("hx-trigger=\"load\""));
    }
}
