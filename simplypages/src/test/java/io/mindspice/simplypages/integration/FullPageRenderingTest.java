package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.builders.AccountBarBuilder;
import io.mindspice.simplypages.builders.BannerBuilder;
import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.builders.SideNavBuilder;
import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.navigation.SideNav;
import io.mindspice.simplypages.components.display.DataTable;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.modules.ContentModule;
import io.mindspice.simplypages.modules.DataModule;
import io.mindspice.simplypages.modules.EditableModule;
import io.mindspice.simplypages.testutil.HtmlAssert;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class FullPageRenderingTest {

    @Test
    @DisplayName("Full shell should render sidebar layout with document and section integrity")
    void testShellSidebarDefault() {
        String html = renderShellWithInjectedContent(
            ShellBuilder.create()
                .withPageTitle("Portal Home")
                .withTopBanner(BannerBuilder.create()
                    .withLayout(BannerBuilder.BannerLayout.HORIZONTAL)
                    .withTitle("SimplyPages")
                    .withSubtitle("Internal Portal")
                    .build())
                .withAccountBar(AccountBarBuilder.create()
                    .addLeftLink("Home", "/")
                    .addLeftLink("Reports", "/reports")
                    .addRightLink("Account", "/account")
                    .build())
                .withSideNav(defaultSideNav(), false)
                .withContentTarget("page-content")
                .withCustomCss("/css/portal.css"),
            basicPageContent(),
            "page-content"
        );

        HtmlAssert.assertThat(html)
            .hasDoctype("html")
            .hasElement("html > head > title")
            .elementTextEquals("html > head > title", "Portal Home")
            .hasElement("body > header.main-header .banner.banner-horizontal")
            .hasElement("body > div.account-bar > div.account-bar-left")
            .hasElement("body > div.main-container.has-sidebar > aside#main-sidebar > nav.sidenav")
            .hasElement("#page-content > div.page-content")
            .attributeEquals("#page-content", "hx-get", "/home")
            .attributeEquals("nav.sidenav a.sidenav-item[href='/home']", "hx-target", "#page-content")
            .childOrder("body > div.main-container.has-sidebar", "aside#main-sidebar", "main.content-wrapper");

        SnapshotAssert.assertMatches("integration/full-page/shell-sidebar-default", html);
    }

    @Test
    @DisplayName("Full shell should render without sidebar and without HTMX content bootstrapping")
    void testShellNoSidebar() {
        String html = renderShellWithInjectedContent(
            ShellBuilder.create()
                .withPageTitle("No Sidebar")
                .withTopBanner(BannerBuilder.create()
                    .withLayout(BannerBuilder.BannerLayout.CENTERED)
                    .withTitle("SimplyPages")
                    .build())
                .withAccountBar(AccountBarBuilder.create()
                    .addLeftLink("Home", "/")
                    .build())
                .withHtmx(false)
                .withContentTarget("page-content"),
            basicPageContent(),
            "page-content"
        );

        HtmlAssert.assertThat(html)
            .hasDoctype("html")
            .hasElement("body > div.main-container")
            .doesNotHaveElement("body > div.main-container > aside#main-sidebar")
            .hasElement("body > div.main-container > main.content-wrapper > #page-content")
            .doesNotHaveElement("#page-content[hx-get]")
            .doesNotHaveElement("#page-content[hx-trigger]");

        SnapshotAssert.assertMatches("integration/full-page/shell-no-sidebar", html);
    }

    @Test
    @DisplayName("Full shell should render collapsible sidebar wiring and controls")
    void testShellCollapsibleSidebar() {
        String html = renderShellWithInjectedContent(
            ShellBuilder.create()
                .withPageTitle("Collapsible Sidebar")
                .withTopBanner(BannerBuilder.create()
                    .withLayout(BannerBuilder.BannerLayout.HORIZONTAL)
                    .withTitle("SimplyPages")
                    .build())
                .withSideNav(defaultSideNav(), true)
                .withContentTarget("page-content"),
            basicPageContent(),
            "page-content"
        );

        HtmlAssert.assertThat(html)
            .hasDoctype("html")
            .hasElement("body > div.main-container.has-sidebar.collapsible-sidebar")
            .hasElement("aside#main-sidebar.collapsible > button.sidebar-toggle")
            .attributeEquals("aside#main-sidebar > button.sidebar-toggle", "aria-label", "Toggle sidebar")
            .hasElementCount("body > script", 2)
            .hasElement("body > script:containsData(toggleSidebar)")
            .hasElement("body > script:containsData(htmx:afterSettle)")
            .childOrder("aside#main-sidebar", "button.sidebar-toggle", "nav.sidenav");

        SnapshotAssert.assertMatches("integration/full-page/shell-collapsible-sidebar", html);
    }

    @Test
    @DisplayName("Full shell should preserve framework/custom CSS asset order")
    void testShellCustomCssAssetOrder() {
        String html = ShellBuilder.create()
            .withPageTitle("Asset Order")
            .withFrameworkCssPath("/css/base.css")
            .withCustomCss(List.of("/css/theme.css", "/css/app.css"))
            .withHtmx(false)
            .build();

        HtmlAssert.assertThat(html)
            .hasDoctype("html")
            .childOrder(
                "html > head",
                "meta[charset]",
                "meta[name=viewport]",
                "title",
                "link[href='/css/base.css']",
                "link[href='/css/theme.css']",
                "link[href='/css/app.css']",
                "script[src='/js/framework.js']"
            )
            .appearsBefore("link[href='/css/base.css']", "link[href='/css/theme.css']")
            .appearsBefore("link[href='/css/theme.css']", "link[href='/css/app.css']");

        SnapshotAssert.assertMatches("integration/full-page/shell-custom-css-asset-order", html);
    }

    @Test
    @DisplayName("Full shell should preserve rich layout structure for module/component composition")
    void testContentRichLayout() {
        String html = renderShellWithInjectedContent(
            ShellBuilder.create()
                .withPageTitle("Content Rich")
                .withTopBanner(BannerBuilder.create()
                    .withLayout(BannerBuilder.BannerLayout.HORIZONTAL)
                    .withTitle("SimplyPages")
                    .build())
                .withAccountBar(AccountBarBuilder.create()
                    .addLeftLink("Dashboard", "/")
                    .addRightLink("Profile", "/profile")
                    .build())
                .withSideNav(defaultSideNav(), false)
                .withContentTarget("page-content"),
            contentRichPage(),
            "page-content"
        );

        HtmlAssert.assertThat(html)
            .hasElement("#page-content > div.page-content")
            .hasElementCount("#page-content div.row", 2)
            .hasElement("#page-content div.row#hero-row > div.col.col-8")
            .hasElement("#page-content div.row#hero-row > div.col.col-4")
            .hasElement("#page-content .editable-module-wrapper button.module-edit-btn[hx-get='/modules/hero/edit']")
            .hasElement("#page-content .data-module table.data-table")
            .childOrder("#page-content div.row#hero-row", "div.col.col-8", "div.col.col-4");

        SnapshotAssert.assertMatches("integration/full-page/content-rich-layout", html);
    }

    private static SideNav defaultSideNav() {
        return SideNavBuilder.create()
            .withContentTarget("#page-content")
            .addSection("Main")
            .addLink("Home", "/home", true)
            .addLink("Reports", "/reports")
            .addLink("Settings", "/settings")
            .build();
    }

    private static String basicPageContent() {
        return Page.builder()
            .addRow(row -> row
                .withId("welcome-row")
                .addColumn(Column.create().withWidth(12)
                    .withChild(new Div().withClass("welcome-card").withInnerText("Welcome to SimplyPages"))))
            .build()
            .render();
    }

    private static String contentRichPage() {
        ContentModule heroContent = ContentModule.create()
            .withModuleId("hero-content")
            .withTitle("Team Dashboard")
            .withContent("Weekly status and ownership details.");

        EditableModule editableHero = EditableModule.wrap(heroContent)
            .withModuleId("hero-editable")
            .withEditUrl("/modules/hero/edit")
            .withDeleteUrl("/modules/hero/delete");

        DataTable<TeamMember> table = DataTable.create(TeamMember.class)
            .addColumn("Name", TeamMember::name)
            .addColumn("Role", TeamMember::role)
            .addColumn("Location", TeamMember::location)
            .withData(List.of(
                new TeamMember("Sam", "Lead", "Remote"),
                new TeamMember("Ari", "Engineer", "New York"),
                new TeamMember("Maya", "QA", "Austin")
            ))
            .striped()
            .hoverable();

        DataModule<TeamMember> dataModule = DataModule.create(TeamMember.class)
            .withModuleId("team-data")
            .withTitle("Team Roster")
            .withDataTable(table);

        return Page.builder()
            .addRow(row -> row
                .withId("hero-row")
                .addColumn(Column.create().withWidth(8).withChild(editableHero))
                .addColumn(Column.create().withWidth(4)
                    .withChild(new Div().withClass("quick-actions")
                        .withChild(new Div().withClass("quick-action-title").withInnerText("Quick Actions"))
                        .withChild(new Div().withInnerText("Open incidents: 4"))
                        .withChild(new Div().withInnerText("Pending approvals: 2")))))
            .addRow(row -> row
                .withId("data-row")
                .addColumn(Column.create().withWidth(12).withChild(dataModule)))
            .build()
            .render();
    }

    private static String renderShellWithInjectedContent(ShellBuilder shellBuilder, String contentHtml, String contentTargetId) {
        String shellHtml = shellBuilder.build();
        Document document = Jsoup.parse(shellHtml);
        Element contentTarget = document.getElementById(contentTargetId);
        if (contentTarget == null) {
            throw new IllegalStateException("Expected content target '#" + contentTargetId + "' in shell output.");
        }
        contentTarget.html(contentHtml);
        return document.outerHtml();
    }

    private record TeamMember(String name, String role, String location) {}
}
