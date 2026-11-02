package io.mindspice.demo;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SiteShellTest {
    @Test
    void sharedNavigationIncludesOrdinaryGithubLinkWithoutChangingCurrentPage() {
        for (String path : new String[]{"/", "/demos", "/docs", "/api"}) {
            var document = Jsoup.parse(SiteShell.render("Navigation", path, "Content"));
            var nav = document.selectFirst("nav[aria-label='Main navigation']");
            assertNotNull(nav);
            var github = nav.select("a[href='https://github.com/dhickel/SimplyPages']");
            assertEquals(1, github.size());
            assertEquals("GitHub", github.text());
            assertFalse(github.first().hasAttr("aria-current"));
            assertFalse(github.first().hasAttr("hx-get"));
            assertEquals(path, nav.select("[aria-current='page']").attr("href"));
        }
    }
}
