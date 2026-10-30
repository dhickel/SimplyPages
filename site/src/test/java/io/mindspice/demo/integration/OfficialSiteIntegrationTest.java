package io.mindspice.demo.integration;

import io.mindspice.demo.*;
import io.mindspice.demo.pages.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
class OfficialSiteIntegrationTest {
    @Autowired MockMvc mvc;
    @Test void completeCatalogRoutesHaveOneMainHeadingAndLocalAssets() throws Exception {
        for (String route : SiteCatalog.routes()) {
            var response = mvc.perform(get(route)).andExpect(status().isOk()).andReturn().getResponse();
            var doc = Jsoup.parse(response.getContentAsString());
            assertEquals(1, doc.select("main").size(), route);
            assertEquals(1, doc.select("main h1").size(), route);
            assertEquals(1, doc.select("a[href='#content-area']").size(), route);
            assertNotNull(doc.selectFirst("link[href='/css/site.css']"), route);
            assertFalse(doc.select("[src^=http],link[href^=http]").iterator().hasNext(), route);
            assertEquals("no-store", response.getHeader("Cache-Control"), route);
            for (var example : SiteCatalog.examples().stream().filter(e -> e.route().equals(route)).toList())
                assertNotNull(doc.getElementById(example.id()), example.destination());
        }
    }
    @Test void groupedCollectionsHaveRealRoutesCurrentNavigationAndProtectedFragmentForms() throws Exception {
        for (var category : DemoNavigation.categories()) {
            if (category.members().isEmpty()) continue;
            var result = mvc.perform(get(category.route())).andExpect(status().isOk()).andReturn();
            var doc = Jsoup.parse(result.getResponse().getContentAsString());
            assertEquals(category.route(), doc.selectFirst(".sp-site-demo-sidebar [aria-current]").attr("href"));
            for (var example : DemoNavigation.examples(category)) assertNotNull(doc.getElementById(example.id()));
            for (var form : doc.select("form[method=POST]"))
                assertEquals(1, form.select("input[name=_csrf]").size(), category.route());
            var fragment = Jsoup.parse(mvc.perform(get(category.route()).header("HX-Request", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertTrue(fragment.select(".sp-site-demo-sidebar, .sp-site-nav").isEmpty());
            assertEquals(doc.select("[data-catalog-symbol]").size(), fragment.select("[data-catalog-symbol]").size());
        }
    }
    @Test void allSixBuildersHaveIsolatedCompleteLocalPreviews() throws Exception {
        for (String slug : new String[]{"shell", "shell-template", "banner", "top-nav", "side-nav", "account-bar"}) {
            String html = mvc.perform(get("/demos/builders/preview/" + slug))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            var doc = Jsoup.parse(html);
            assertTrue(html.startsWith("<!DOCTYPE html>"), slug);
            assertEquals(1, doc.select("html").size(), slug);
            assertEquals(1, doc.select("main").size(), slug);
            assertEquals(1, doc.select("h1").size(), slug);
            assertNotNull(doc.selectFirst("a[href='/demos/builders/" + slug + "']"), slug);
        }
    }
    @Test void supportingContractsResolveWithinWorkingFeaturesAndExactPackagedReferences() throws Exception {
        var pages = new java.util.HashMap<String, org.jsoup.nodes.Document>();
        for (var contract : FeatureCatalog.contracts()) {
            String route = contract.destination().split("#")[0];
            if (!pages.containsKey(route)) pages.put(route, Jsoup.parse(mvc.perform(get(route))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()));
            assertNotNull(pages.get(route).getElementById(contract.anchor()), contract.destination());
            mvc.perform(get(contract.api())).andExpect(status().isOk());
            mvc.perform(get(contract.docs())).andExpect(status().isOk());
        }
    }
    @Test void canonicalSandboxPagesInitializeThroughTheFilterAndRenderProtectedNativeForms() throws Exception {
        for (String path : new String[]{"/demos/features/editing", "/demos/features/forum", "/demos/features/chat"}) {
            var result = mvc.perform(get(path)).andExpect(status().isOk()).andReturn();
            String token = result.getResponse().getHeader("X-Sandbox-CSRF");
            assertNotNull(token);
            var doc = Jsoup.parse(result.getResponse().getContentAsString());
            for (var form : doc.select("form[hx-post],form[method=post],form[method=POST]")) {
                assertEquals(1, form.select("input[name=_csrf]").size(), path);
                assertEquals(token, form.selectFirst("input[name=_csrf]").val());
                assertFalse(form.attr("action").isBlank(), path);
            }
            assertNotNull(doc.selectFirst("body[hx-headers]"));
        }
        var hx = mvc.perform(get("/demos/features/chat").header("HX-Request", "TrUe"))
            .andExpect(status().isOk()).andExpect(header().string("HX-Redirect", "/chat"))
            .andExpect(content().string("")).andReturn();
        assertNull(hx.getRequest().getSession(false));
    }
    @Test void tokenFromPresentationWorksForNativeAndHtmxAndNoUnauthenticatedMutation() throws Exception {
        var initial = mvc.perform(get("/demos/features/templates")).andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) initial.getRequest().getSession(false);
        String token = initial.getResponse().getHeader("X-Sandbox-CSRF");
        mvc.perform(post("/demos/api/template-card").session(session).header("Origin", "http://localhost")
            .param("_csrf", token).param("title", "Native <value>").param("body", "Fresh body"))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Native &lt;value&gt;")));
        mvc.perform(post("/demos/api/dynamic").session(session).header("Origin", "http://localhost")
            .header("X-Sandbox-CSRF", token).param("kind", "dynamic-list").param("value", "Changed,Again"))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Changed")));
        mvc.perform(post("/demos/api/template-card").session(session).param("title", "Denied").param("body", "Denied"))
            .andExpect(status().isForbidden());
    }
    @Test void editablePageAdapterUsesExistingInsertionAndCanonicalModalOobHosts() throws Exception {
        var page = mvc.perform(get("/demos/features/editing")).andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) page.getRequest().getSession(false);
        String token = page.getResponse().getHeader("X-Sandbox-CSRF");
        var result = mvc.perform(post("/demos/features/editing/insert-wrapper-row").session(session)
            .header("Origin", "http://localhost").param("_csrf", token))
            .andExpect(status().isOk()).andReturn();
        var doc = Jsoup.parse(result.getResponse().getContentAsString());
        assertEquals(1, doc.select("#edit-modal-container[hx-swap-oob=innerHTML] [role=dialog]").size());
        assertEquals(1, doc.select("#page-content[hx-swap-oob]").size());
        assertEquals(6, doc.select("#page-content .editable-row-wrapper").size());
        mvc.perform(post("/demos/features/editing/insert-wrapper-row")).andExpect(status().isForbidden());
    }
    @Test void allocationFreeRecoveryPageOffersAnExplicitProtectedNativeReset() throws Exception {
        var page = mvc.perform(get("/demos/recovery")).andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) page.getRequest().getSession(false);
        String token = page.getResponse().getHeader("X-Sandbox-CSRF");
        var doc = Jsoup.parse(page.getResponse().getContentAsString());
        assertEquals(1, doc.select("form[action='/sandbox/new'] input[name=_csrf]").size());
        mvc.perform(post("/sandbox/new").session(session).header("Origin", "http://localhost").param("_csrf", token))
            .andExpect(status().isNoContent());
        mvc.perform(get("/demos/features/editing").session(session)).andExpect(status().isOk());
    }
    @Test void docsErrorsAndApiAreBrandedTruthfulAndCanonicalAliasesResolve() throws Exception {
        var missing = mvc.perform(get("/docs/no-such-document.md")).andExpect(status().isNotFound()).andReturn();
        var doc = Jsoup.parse(missing.getResponse().getContentAsString());
        assertEquals(1, doc.select("#docs-content").size());
        assertEquals(1, doc.select("main h1").size());
        assertTrue(doc.title().contains("not found"));
        mvc.perform(get("/api")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("/javadocs/index.html")));
        mvc.perform(get("/demos/modules/editable")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/demos/features/editing#editablemodule"));
        mvc.perform(get("/demos/modules/chat")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/demos/features/chat#chatmodule"));
    }
}
