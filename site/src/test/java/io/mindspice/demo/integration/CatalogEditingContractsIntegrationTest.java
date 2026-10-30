package io.mindspice.demo.integration;

import io.mindspice.demo.DemoApplication;
import io.mindspice.demo.pages.FeatureCatalog;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
class CatalogEditingContractsIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void editingContractsPointToTheSpecificWorkingComposition() throws Exception {
        var response = mvc.perform(get("/demos/features/editing")).andExpect(status().isOk()).andReturn();
        var doc = Jsoup.parse(response.getResponse().getContentAsString());
        var expected = java.util.Map.ofEntries(
            java.util.Map.entry("EditablePage", "editablepage"), java.util.Map.entry("EditableRow", "editablepage"),
            java.util.Map.entry("Editable", "live-editing-contracts"), java.util.Map.entry("EditableChild", "live-editing-contracts"),
            java.util.Map.entry("EditMode", "live-editing-contracts"), java.util.Map.entry("EditModalBuilder", "live-editing-contracts"),
            java.util.Map.entry("EditFormBuilder", "edit-form-builder"), java.util.Map.entry("FormFieldHelper", "edit-form-builder"),
            java.util.Map.entry("ValidationResult", "edit-form-builder"), java.util.Map.entry("ModuleEditHandler", "module-edit-handler"),
            java.util.Map.entry("AuthorizationChecker", "authwrapper"), java.util.Map.entry("AuthWrapper", "authwrapper"));
        FeatureCatalog.contracts().stream().filter(c -> c.symbol().contains(".editing.")).forEach(c -> {
            assertEquals(expected.get(c.name()), c.anchor(), c.name());
            var example = doc.getElementById(c.anchor());
            assertNotNull(example, c.destination());
            assertTrue(example.select("code").text().contains(c.name()), c.name());
        });
        assertNotNull(doc.selectFirst("#edit-form-builder input[name=draftTitle]"));
        assertNotNull(doc.selectFirst("#edit-form-builder input[type=checkbox][name=markdown]"));
        assertNotNull(doc.selectFirst("#module-edit-handler form[action='/demos/api/module-preview'] input[name=_csrf]"));
    }

    @Test void handlerExecutesUpdateDeleteAndProposalOnRequestOwnedCopiesAndRetainsInvalidDrafts() throws Exception {
        var page = mvc.perform(get("/demos/features/editing")).andReturn();
        var session = (MockHttpSession) page.getRequest().getSession(false);
        String token = page.getResponse().getHeader("X-Sandbox-CSRF");
        for (String mode : new String[]{"OWNER_EDIT", "USER_EDIT"}) {
            for (String operation : new String[]{"edit", "update", "delete"}) {
                var response = mvc.perform(post("/demos/api/module-preview").session(session)
                    .header("Origin", "http://localhost").param("_csrf", token)
                    .param("operation", operation).param("mode", mode).param("title", "Changed <title>")
                    .param("content", "Request <body>"))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")).andReturn();
                var doc = Jsoup.parse(response.getResponse().getContentAsString());
                assertEquals("Request <body>", doc.selectFirst("textarea[name=content]").val());
                assertEquals("Changed <title>", doc.selectFirst("input[name=title]").val());
                assertEquals(1, doc.select("form input[name=_csrf]").size());
                boolean deleted = operation.equals("delete") && mode.equals("OWNER_EDIT");
                assertEquals(deleted ? 0 : 1, doc.select("#handler-copy .module").size());
                if (mode.equals("USER_EDIT") && !operation.equals("edit")) assertTrue(doc.selectFirst("#handler-copy").text().contains("not queued"));
            }
        }
        var invalid = mvc.perform(post("/demos/api/module-preview").session(session)
            .header("Origin", "http://localhost").header("X-Sandbox-CSRF", token)
            .param("operation", "update").param("title", "Keep <draft>").param("content", "   "))
            .andExpect(status().isOk()).andReturn();
        var doc = Jsoup.parse(invalid.getResponse().getContentAsString());
        assertTrue(doc.selectFirst("[role=alert]").text().contains("Content cannot be empty"));
        assertEquals("   ", doc.selectFirst("textarea[name=content]").wholeText());
        assertEquals("Keep <draft>", doc.selectFirst("input[name=title]").val());
        String fresh = mvc.perform(get("/demos/features/editing").session(session)).andReturn().getResponse().getContentAsString();
        assertEquals("Request-owned title", Jsoup.parse(fresh).selectFirst("#handler-result input[name=title]").val());
        assertFalse(Jsoup.parse(fresh).selectFirst("#page-content").text().contains("Changed <title>"));
        mvc.perform(post("/demos/api/module-preview").session(session).param("operation", "delete")).andExpect(status().isForbidden());
        mvc.perform(post("/demos/api/module-preview").session(session).header("Origin", "http://evil.invalid")
            .header("X-Sandbox-CSRF", token).param("operation", "delete")).andExpect(status().isForbidden());
        mvc.perform(post("/demos/api/module-preview").session(session).header("Origin", "http://localhost")
            .header("X-Sandbox-CSRF", token).param("operation", "unknown")).andExpect(status().isBadRequest());
    }

    @Test void childProposalStaysOutOfLiveContentUntilActualApproval() throws Exception {
        var initial = mvc.perform(get("/demos/features/editing")).andReturn();
        var session = (MockHttpSession) initial.getRequest().getSession(false);
        String token = initial.getResponse().getHeader("X-Sandbox-CSRF");
        var staged = mvc.perform(post("/editing-demo/save-child/module-9/item-0").session(session)
            .header("Origin", "http://localhost").header("X-Sandbox-CSRF", token)
            .param("editMode", "USER_EDIT").param("text", "Checked child <proposal>"))
            .andExpect(status().isOk()).andReturn();
        var page = Jsoup.parse(staged.getResponse().getContentAsString());
        assertNotNull(page.selectFirst("#page-content[hx-swap-oob]"));
        assertNotNull(page.selectFirst("#edit-modal-container[hx-swap-oob]"));
        assertFalse(page.selectFirst("#page-content").text().contains("Checked child <proposal>"));
        String pending = mvc.perform(get("/editing-demo/pending-edits").session(session))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertNotNull(Jsoup.parse(pending).selectFirst("[hx-post='/editing-demo/pending-edits/module-9/approve']"));
        assertTrue(Jsoup.parse(pending).text().contains("Fields: content"));
        var approved = mvc.perform(post("/editing-demo/pending-edits/module-9/approve").session(session)
            .header("Origin", "http://localhost").header("X-Sandbox-CSRF", token))
            .andExpect(status().isOk()).andReturn();
        assertTrue(Jsoup.parse(approved.getResponse().getContentAsString()).selectFirst("#page-content").text().contains("Checked child <proposal>"));
    }

    @Test void liveModalReallyExposesEditableChildrenAndCarriesProposalMode() throws Exception {
        var page = mvc.perform(get("/demos/features/editing")).andReturn();
        var session = (MockHttpSession) page.getRequest().getSession(false);
        for (String mode : new String[]{"OWNER_EDIT", "USER_EDIT"}) {
            String html = mvc.perform(get("/editing-demo/edit/module-9").session(session).param("editMode", mode))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            var doc = Jsoup.parse(html);
            assertEquals(3, doc.select("[hx-get^='/editing-demo/edit-child/module-9/']").size());
            assertNotNull(doc.selectFirst("[hx-get='/editing-demo/edit-child/module-9/item-0?editMode=" + mode + "']"));
            assertNotNull(doc.selectFirst("[hx-delete='/editing-demo/delete-child/module-9/item-0?editMode=" + mode + "']"));
            assertNotNull(doc.selectFirst("[hx-post='/editing-demo/save/module-9?editMode=" + mode + "']"));
            if (mode.equals("USER_EDIT")) assertTrue(doc.text().contains("staged for approval"));
        }
    }
}
