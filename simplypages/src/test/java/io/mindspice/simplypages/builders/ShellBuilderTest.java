package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.navigation.SideNav;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.Slot;
import io.mindspice.simplypages.core.SlotKey;
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
    void compatibilityOwnershipMatchesFullAndTemplateAssetOrder() {
        for (boolean included : List.of(true, false)) {
            var builder = ShellBuilder.create().withHtmx(included)
                .withContent(new Paragraph("Configured content"))
                .withCustomJs(List.of("/js/app.js", "/js/feature.js"));
            for (String html : List.of(builder.build(), builder.buildTemplate().render(RenderContext.empty()))) {
                var document = Jsoup.parse(html);
                var markers = document.select("meta[name=simplypages-htmx-compatibility]");
                assertEquals(included ? 1 : 0, markers.size());
                assertTrue(document.select("meta[name=htmx-config]").isEmpty());
                var scripts = document.select("head script[src]");
                assertEquals(included
                    ? List.of("/webjars/htmx.org/dist/htmx.min.js", "/js/framework.js", "/js/app.js", "/js/feature.js")
                    : List.of("/js/framework.js", "/js/app.js", "/js/feature.js"), scripts.eachAttr("src"));
                assertTrue(scripts.stream().allMatch(script -> script.hasAttr("defer")));
                if (included) {
                    assertEquals("1", markers.first().attr("content"));
                    assertEquals("head", markers.first().parent().tagName());
                    assertTrue(html.indexOf("name=\"simplypages-htmx-compatibility\"") < html.indexOf("src=\"/webjars/"));
                }
            }
        }
    }

    @Test
    void bodyNeverEmitsCompatibilityMarkerOrAssetsInEitherHtmxMode() {
        for (boolean included : List.of(true, false)) {
            Component body = ShellBuilder.create().withHtmx(included)
                .withCustomJs("/js/app.js").withCustomCss("/css/app.css")
                .withContent(new Paragraph("Fragment")).buildBody();
            for (String html : List.of(body.render(), body.compile().render(RenderContext.empty()))) {
                assertTrue(Jsoup.parseBodyFragment(html).select("meta, script, link").isEmpty());
                assertFalse(html.contains("simplypages-htmx-compatibility"));
            }
        }
    }

    @Test
    void templateContextChangesContentWithoutChangingCompatibilityOwnership() {
        var template = ShellBuilder.create().withContent(new Paragraph("Fallback")).buildTemplate();
        var context = RenderContext.of(template.contentSlot(), new Paragraph("<request & value>"));
        String html = template.render(context);
        var document = Jsoup.parse(html);
        assertEquals(1, document.select("head meta[name=simplypages-htmx-compatibility][content=1]").size());
        assertEquals("<request & value>", document.selectFirst("#content-area").text());
        assertFalse(html.contains("<request & value>"));
        assertEquals(1, Jsoup.parse(template.render()).select("head meta[name=simplypages-htmx-compatibility]").size());
    }

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
            .withTopNav(TopNavBuilder.create().addPrimaryLink("Home", "/").build())
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
        assertTrue(html.contains("header-top-nav-wrap"));
        assertTrue(html.contains("top-nav"));
        assertTrue(html.contains("hx-get=\"/home\""));
        assertTrue(html.contains("/css/custom.css"));
        assertTrue(html.contains("htmx.min.js"));
    }

    @Test
    @DisplayName("ShellBuilder withAccountBar should map to embedded top nav region")
    void testAccountBarCompatibilityAlias() {
        String html = ShellBuilder.create()
            .withTopBanner(BannerBuilder.create().withTitle("Portal").build())
            .withAccountBar(AccountBarBuilder.create().addLeftLink("Home", "/").build())
            .build();

        assertTrue(html.contains("header-top-nav-wrap"));
        assertTrue(html.contains("account-bar"));
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
    void mobileToggleIsNamedInFullBodyAndTemplateOutput() {
        var builder = ShellBuilder.create().withSideNav(SideNavBuilder.create().addLink("Home", "/").build());
        for (String html : java.util.List.of(builder.build(), builder.buildBody().render(),
                builder.buildTemplate().render(io.mindspice.simplypages.core.RenderContext.empty()))) {
            HtmlAssert.assertThat(html)
                .attributeEquals(".mobile-sidebar-toggle", "type", "button")
                .attributeEquals(".mobile-sidebar-toggle", "aria-label", "Toggle navigation")
                .attributeEquals(".mobile-sidebar-toggle", "aria-controls", "main-sidebar")
                .attributeEquals(".mobile-sidebar-toggle", "aria-expanded", "false");
        }
    }

    @Test
    @DisplayName("ShellBuilder should keep the desktop toggle contract across full, body and template modes")
    void desktopToggleContractAcrossModes() {
        var builder = ShellBuilder.create()
            .withSideNav(SideNavBuilder.create().addLink("Home", "/").build(), true);

        String full = builder.build();
        String body = builder.buildBody().render();
        String template = builder.buildTemplate().render(RenderContext.empty());

        for (String html : List.of(full, body, template)) {
            HtmlAssert.assertThat(html)
                .hasElement("aside#main-sidebar > button.sidebar-toggle[type='button'][aria-label='Toggle sidebar'][onclick='toggleSidebar()']")
                .hasElement(".mobile-sidebar-toggle[type='button'][aria-label='Toggle navigation']");
            assertFalse(html.contains("function toggleSidebar()"));
        }
        assertEquals(1, Jsoup.parse(full).select("body > script:containsData(htmx:afterSettle)").size());
        assertEquals(0, Jsoup.parse(body).select("script").size());
        assertEquals(1, Jsoup.parse(template).select("body > script:containsData(htmx:afterSettle)").size());
    }

    @Test
    void bodyDirectAndCompiledContextPreserveToggleAndEscaping() {
        SlotKey<String> key = SlotKey.of("body.content");
        Component body = ShellBuilder.create()
            .withSideNav(SideNavBuilder.create().addLink("Home", "/").build(), true)
            .withContentTarget("target\"&value")
            .withContent(Slot.of(key)).buildBody();
        String value = "<request & text>";
        String direct = body.render(RenderContext.of(key, value));
        assertEquals(direct, body.compile().render(RenderContext.of(key, value)));
        assertFalse(direct.contains(value));
        assertEquals(value, Jsoup.parse(direct).selectFirst(".content-wrapper").text());
        assertEquals("target\"&value", Jsoup.parse(direct).selectFirst(".content-wrapper > div").id());
        HtmlAssert.assertThat(direct)
            .hasElement("button.sidebar-toggle[type=button][aria-label='Toggle sidebar']")
            .doesNotHaveElement("script");
    }

    @Test
    @DisplayName("ShellBuilder should keep body fragments free of document chrome, assets and scripts")
    void bodyFragmentOmitsDocumentChromeAndAssets() {
        String body = ShellBuilder.create()
            .withTopBanner(new Paragraph("omitted-banner"))
            .withTopNav(new Paragraph("omitted-top-nav"))
            .withCustomCss("/css/omitted-custom.css")
            .withCustomJs("/js/omitted-custom.js")
            .withSideNav(SideNavBuilder.create().addLink("Home", "/").build(), true)
            .buildBody()
            .render();

        assertFalse(body.contains("<!DOCTYPE html>"));
        assertFalse(body.contains("<html"));
        assertFalse(body.contains("<head"));
        assertFalse(body.contains("<body"));
        assertFalse(body.contains("<header"));
        assertFalse(body.contains("omitted-banner"));
        assertFalse(body.contains("omitted-top-nav"));
        assertFalse(body.contains("omitted-custom"));
        assertFalse(body.contains("<link"));
        assertFalse(body.contains("<script"));
        assertFalse(body.contains("/js/framework.js"));
        assertFalse(body.contains("htmx.min.js"));
    }

    @Test
    @DisplayName("ShellBuilder should omit desktop and mobile controls without a sidebar")
    void noSidebarOmitsAllSidebarControlsInEveryMode() {
        ShellBuilder builder = ShellBuilder.create();
        for (String html : List.of(builder.build(), builder.buildBody().render(),
                builder.buildTemplate().render(RenderContext.empty()))) {
            HtmlAssert.assertThat(html)
                .doesNotHaveElement(".mobile-sidebar-toggle")
                .doesNotHaveElement(".sidebar-toggle")
                .doesNotHaveElement("aside#main-sidebar");
        }
    }

    @Test
    @DisplayName("ShellBuilder should retain mobile navigation but omit desktop collapse wiring when noncollapsible")
    void noncollapsibleSidebarOmitsDesktopCollapseControl() {
        String html = ShellBuilder.create()
            .withSideNav(SideNavBuilder.create().addLink("Home", "/").build(), false)
            .build();

        HtmlAssert.assertThat(html)
            .hasElement(".mobile-sidebar-toggle")
            .hasElement(".main-container.has-sidebar")
            .doesNotHaveElement(".main-container.collapsible-sidebar")
            .doesNotHaveElement(".sidebar-toggle")
            .doesNotHaveElement("body > script:containsData(toggleSidebar)");
    }

    @Test
    @DisplayName("ShellBuilder should escape titles in raw output while preserving parsed text")
    void shellTitleEscapingIsSafeInRawAndParsedOutput() {
        String title = "<Portal & \\\"quoted\\\">";
        String html = ShellBuilder.create().withPageTitle(title).build();

        assertTrue(html.contains("<title>"));
        assertTrue(html.contains("&lt;Portal"));
        assertTrue(html.contains("&amp;"));
        assertFalse(html.contains("<Portal"));
        assertEquals(title, Jsoup.parse(html).selectFirst("title").text());
    }

    @Test
    @DisplayName("ShellBuilder should preserve full and template auto-load boundaries")
    void shellAutoloadBoundaries() {
        ShellBuilder builder = ShellBuilder.create();
        String full = builder.build();
        String body = builder.buildBody().render();
        String template = builder.buildTemplate().render(RenderContext.empty());
        String explicitTemplate = builder.buildTemplate().renderWithContent(new Paragraph("Fragment"));

        for (String html : List.of(full, body)) {
            HtmlAssert.assertThat(html)
                .hasElement("#content-area[hx-get='/home'][hx-trigger='load']");
        }
        for (String html : List.of(template, explicitTemplate)) {
            HtmlAssert.assertThat(html)
                .doesNotHaveElement("#content-area[hx-get]")
                .doesNotHaveElement("#content-area[hx-trigger]");
        }
    }

    @Test
    @DisplayName("ShellBuilder should retain active-nav script exactly once without desktop duplication")
    void activeNavigationScriptIsRetainedWithoutInlineDesktopDuplication() {
        String html = ShellBuilder.create()
            .withSideNav(SideNavBuilder.create().addLink("Home", "/").build(), true)
            .build();
        var scripts = Jsoup.parse(html).select("body > script");

        assertEquals(1, scripts.size());
        assertTrue(scripts.get(0).data().contains("htmx:afterSettle"));
        assertFalse(scripts.get(0).data().contains("sidebarCollapsed"));
        assertFalse(html.contains("function toggleSidebar()"));
    }

    @Test
    @DisplayName("ShellBuilder should not render mobile sidebar toggle without side nav")
    void testNoMobileToggleWhenNoSidebar() {
        String html = ShellBuilder.create().build();
        HtmlAssert.assertThat(html)
            .hasElement("body > div.main-container")
            .doesNotHaveElement("body > div.main-container.has-sidebar");
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

    @Test
    @DisplayName("ShellBuilder buildTemplate should render explicit content and skip shell auto-load attributes")
    void testBuildTemplateExplicitContentWithoutAutoLoad() {
        ShellTemplate template = ShellBuilder.create()
            .withTopNav(TopNavBuilder.create().addPrimaryLink("Home", "/").build())
            .buildTemplate();

        String html = template.renderWithContent(new Paragraph("Template body"));

        HtmlAssert.assertThat(html)
            .hasDoctype("html")
            .hasElement("main.content-wrapper > div#content-area > p")
            .elementTextEquals("main.content-wrapper > div#content-area > p", "Template body")
            .doesNotHaveElement("div#content-area[hx-get]")
            .doesNotHaveElement("div#content-area[hx-trigger]");
    }

    @Test
    @DisplayName("ShellBuilder buildTemplate should use withContent as default slot content")
    void testBuildTemplateUsesDefaultContentFallback() {
        ShellTemplate template = ShellBuilder.create()
            .withContent(new Paragraph("Default slot body"))
            .buildTemplate();

        String html = template.render();

        HtmlAssert.assertThat(html)
            .hasElement("main.content-wrapper > div#content-area > p")
            .elementTextEquals("main.content-wrapper > div#content-area > p", "Default slot body")
            .doesNotHaveElement("div#content-area[hx-get]")
            .doesNotHaveElement("div#content-area[hx-trigger]");
    }

    @Test
    @DisplayName("ShellTemplate should support context-based content overrides through content slot key")
    void testShellTemplateContentSlotContextOverride() {
        ShellTemplate template = ShellBuilder.create()
            .withContent(new Paragraph("Fallback body"))
            .buildTemplate();

        RenderContext context = RenderContext.of(template.contentSlot(), new Paragraph("Override body"));
        String html = template.render(context);

        assertTrue(html.contains("Override body"));
        assertFalse(html.contains("Fallback body"));
        HtmlAssert.assertThat(html)
            .hasElement("main.content-wrapper > div#content-area > p")
            .elementTextEquals("main.content-wrapper > div#content-area > p", "Override body");
    }
}
