package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.builders.SideNavBuilder;
import io.mindspice.simplypages.builders.TopNavBuilder;
import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.components.display.Modal;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generates the maintained Phase 13 browser inputs from real ShellBuilder, Page, Modal and Alert
 * components. The browser suite consumes the target files after this test runs.
 */
class Phase13ShellPageBrowserFixtureTest {

    private static final Path FIXTURE_ROOT = Path.of("target", "phase13-shell-page-fixtures");

    @Test
    @DisplayName("write fresh Java-generated ShellBuilder and Page browser fixtures")
    void writesFreshShellPageBrowserFixtures() throws IOException {
        Files.createDirectories(FIXTURE_ROOT);

        Page page = page("fixture-page", "Fixture page navigation", "Fixture page content");
        ShellBuilder shell = ShellBuilder.create()
            .withPageTitle("Phase 13 Fixture")
            .withTopNav(TopNavBuilder.create().addPrimaryLink("Shell home", "/home").build())
            .withSideNav(SideNavBuilder.create().addLink("Home", "/home").addLink("Reports", "/reports").build(), true)
            .withContent(page);

        write("integration/phase13-shell-page/full-shell", "full-shell.html", shell.build(),
            "body > div.main-container.has-sidebar.collapsible-sidebar");
        write("integration/phase13-shell-page/body-shell", "body-shell.html", shell.buildBody().render(),
            ".shell-body > div.main-container.has-sidebar.collapsible-sidebar");
        write("integration/phase13-shell-page/template-shell", "template-shell.html",
            shell.buildTemplate().renderWithContent(page),
            "body > div.main-container.has-sidebar.collapsible-sidebar");
        writePageFixtures();
        writeModalFixtures();
        writeHtmxFixtures();
    }

    private static void writePageFixtures() throws IOException {
        HtmlTag nestedDetails = new HtmlTag("details")
            .withId("ordinary-inner")
            .withChild(new HtmlTag("summary").withInnerText("Inner ordinary disclosure"))
            .withChild(new HtmlTag("button").withAttribute("type", "button")
                .withId("ordinary-hidden-control").withInnerText("Inner disclosure control"));
        HtmlTag ordinaryNestedDetails = new HtmlTag("details")
            .withId("ordinary-outer")
            .withChild(new HtmlTag("summary").withInnerText("Outer ordinary disclosure"))
            .withChild(nestedDetails);

        Page first = Page.builder()
            .withStickySidebar(new Div().withChild(navigation("first-page", "First Page"))
                .withChild(ordinaryNestedDetails))
            .addComponents(new HtmlTag("p").withInnerText("First content"))
            .build();
        first.withId("first-page");
        Page emptySidebar = Page.builder()
            .withStickySidebar(new Div(), 8, 4)
            .addComponents(new HtmlTag("p").withInnerText("Empty sidebar page"))
            .build();
        emptySidebar.withId("empty-page");
        Page second = page("second-page", "Second Page", "Second content");
        Div pages = new Div()
            .withId("phase13-pages")
            .withChild(first)
            .withChild(emptySidebar)
            .withChild(second);

        write("integration/phase13-shell-page/page-details", "page-details.html", pages.render(),
            "#phase13-pages details.sticky-sidebar-mobile-collapse");
    }

    private static void writeModalFixtures() throws IOException {
        Page modalPage = page("modal-page", "Modal Page", "Modal page content");
        Page nestedPage = page("nested-modal-page", "Nested Page", "Nested modal page content");

        Modal nested = Modal.create()
            .withModalId("phase13-nested-modal")
            .withTitle("Nested Phase 13 modal")
            .withBody(new Div().withChild(nestedPage));
        Modal outer = Modal.create()
            .withModalId("phase13-outer-modal")
            .withTitle("Outer Phase 13 modal")
            .withBody(new Div()
                .withChild(modalPage)
                .withChild(Alert.info("Alert alongside a Page").dismissible().withId("phase13-modal-alert"))
                .withChild(nested));
        Modal stacked = Modal.create()
            .withModalId("phase13-stacked-modal")
            .withTitle("Stacked Phase 13 modal")
            .withBody(new Div().withChild(page("stacked-page", "Stacked Page", "Stacked modal content")));

        Div fixture = new Div()
            .withId("phase13-modal-fixtures")
            .withChild(outer)
            .withChild(stacked);
        write("integration/phase13-shell-page/modal-page", "modal-page.html", fixture.render(),
            "#phase13-modal-fixtures .modal-backdrop .page-content");
    }

    private static void writeHtmxFixtures() throws IOException {
        Page responsePage = page("response-page", "Response Page", "HTMX response content");
        Div fragment = new Div()
            .withId("phase13-fragment-host")
            .withChild(responsePage);
        Div outOfBand = new Div()
            .withId("phase13-oob-page")
            .withAttribute("hx-swap-oob", "true")
            .withChild(page("oob-page", "OOB Page", "Out-of-band Page content"));
        Div payload = new Div()
            .withId("phase13-htmx-payload")
            .withChild(fragment)
            .withChild(outOfBand);

        write("integration/phase13-shell-page/htmx-fragments", "htmx-fragments.html", payload.render(),
            "#phase13-fragment-host details.sticky-sidebar-mobile-collapse");
    }

    private static Page page(String id, String navigation, String content) {
        Page page = Page.builder()
            .withStickySidebar(navigation(id, navigation), 8, 4)
            .addComponents(new HtmlTag("p").withInnerText(content))
            .build();
        page.withId(id);
        return page;
    }

    private static HtmlTag navigation(String id, String label) {
        return new HtmlTag("nav").withId(id + "-navigation")
            .withAttribute("aria-label", label)
            .withChild(new HtmlTag("a").withId(id + "-link")
                .withAttribute("href", "#" + id).withInnerText(label))
            .withChild(new HtmlTag("button").withId(id + "-button")
                .withAttribute("type", "button").withInnerText("Page action"));
    }

    private static void write(String snapshotKey, String fileName, String html, String requiredSelector)
        throws IOException {
        var document = Jsoup.parse(html);
        document.outputSettings().prettyPrint(false);
        assertFalse(html.isBlank(), fileName);
        assertTrue(document.select(requiredSelector).size() > 0,
            () -> "Missing generated fixture selector '" + requiredSelector + "' in " + fileName);
        if (fileName.equals("body-shell.html")) {
            assertFalse(html.contains("<script"), "Body fixture must not include scripts");
            assertFalse(html.contains("<link"), "Body fixture must not include assets");
        }
        if (fileName.equals("full-shell.html") || fileName.equals("template-shell.html")) {
            assertEquals(1, document.select("button.sidebar-toggle").size(), fileName);
            assertEquals("button", document.selectFirst("button.sidebar-toggle").attr("type"), fileName);
        }
        assertFalse(document.select("details.sticky-sidebar-mobile-collapse").isEmpty(), fileName);
        assertEquals(0, document.select("details.sticky-sidebar-mobile-collapse[open]").size(),
            "Fresh Page disclosures must preserve the closed mobile default: " + fileName);
        assertEquals(0, document.select("details.sticky-sidebar-mobile-collapse[tabindex], "
            + "summary.sticky-sidebar-mobile-summary[tabindex]").size(), fileName);
        assertEquals(document.select("details.sticky-sidebar-mobile-collapse").size(),
            document.select(".sticky-sidebar-aside[tabindex='-1']").size(), fileName);
        SnapshotAssert.assertMatches(snapshotKey, html);
        Path output = FIXTURE_ROOT.resolve(fileName);
        Files.writeString(output, html, StandardCharsets.UTF_8);
        assertTrue(Files.isRegularFile(output), "Fixture was not written: " + output);
        assertEquals(html, Files.readString(output, StandardCharsets.UTF_8));
    }
}
