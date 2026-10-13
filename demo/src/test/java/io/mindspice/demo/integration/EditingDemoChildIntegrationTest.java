package io.mindspice.demo.integration;

import io.mindspice.demo.EditingDemoController;
import io.mindspice.simplypages.editing.EditMode;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.stream.Stream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class EditingDemoChildIntegrationTest {
    private EditingDemoController controller;
    private MockMvc mvc;
    private Object module;

    @BeforeEach
    void freshState() throws Exception {
        controller = new EditingDemoController();
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        var find = EditingDemoController.class.getDeclaredMethod("findModule", String.class);
        find.setAccessible(true);
        module = find.invoke(controller, "module-9");
    }

    private Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private void set(String name, Object value) throws Exception {
        Field field = module.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(module, value);
    }

    private String content() throws Exception { return (String) field(module, "content"); }

    @SuppressWarnings("unchecked")
    private List<Object> pending() throws Exception {
        return ((Map<String, List<Object>>) field(controller, "pendingEdits")).get("demo-page");
    }

    private MvcResult call(MockHttpServletRequestBuilder request, int status) throws Exception {
        MvcResult result = mvc.perform(request).andReturn();
        assertEquals(status, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        assertTrue(result.getResponse().getContentType().startsWith("text/html"));
        assertTrue(result.getResponse().getHeader("Vary").contains("HX-Request"));
        return result;
    }

    private String body(MvcResult result) throws Exception { return result.getResponse().getContentAsString(); }
    private String route(String action, String id) { return "/editing-demo/" + action + "/module-9/" + id; }

    @Test
    void generatedPositionsAndPrefilledFormCarryMode() throws Exception {
        String parent = body(call(get("/editing-demo/edit/module-9").param("editMode", "USER_EDIT"), 200));
        var doc = Jsoup.parse(parent);
        for (int i = 0; i < 3; i++) {
            assertEquals(route("edit-child", "item-" + i) + "?editMode=USER_EDIT",
                    doc.select("[hx-get]").get(i).attr("hx-get"));
            assertEquals(route("delete-child", "item-" + i) + "?editMode=USER_EDIT",
                    doc.select("[hx-delete^=/editing-demo/delete-child]").get(i).attr("hx-delete"));
        }
        String child = body(call(get(route("edit-child", "item-1")).param("editMode", "USER_EDIT"), 200));
        var form = Jsoup.parse(child);
        assertEquals("\nItem 2", form.selectFirst("textarea[name=text]").wholeText());
        assertEquals(route("save-child", "item-1") + "?editMode=USER_EDIT", form.selectFirst("form").attr("hx-post"));
        assertTrue(form.text().contains("Prior pending child and property changes are discarded"));
        assertFalse(child.contains("hx-swap-oob"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"item-00", "item-01", "item--1", "item-+1", "item-", "Item-0", "junk", "item-2147483648", "item-999999999999999999999"})
    void malformedIdsAre400AndDoNotMutate(String id) throws Exception {
        for (var request : List.of(get(route("edit-child", id)), post(route("save-child", id)).param("text", "draft"), delete(route("delete-child", id)))) {
            call(request, 400);
            assertEquals("Item 1,Item 2,Item 3", content());
            assertTrue(pending().isEmpty());
        }
    }

    static Stream<Arguments> rawIdentitySuffixes() {
        return Stream.of(
                Arguments.of("?evil?editMode=OWNER_EDIT", "%3Fevil%3FeditMode%3DOWNER_EDIT"),
                Arguments.of("#tail?editMode=OWNER_EDIT", "%23tail%3FeditMode%3DOWNER_EDIT"),
                Arguments.of("%30%3Fevil", "%2530%253Fevil"),
                Arguments.of("<&\"' >", "%3C%26%22%27%20%3E"),
                Arguments.of("雪😀", "%E9%9B%AA%F0%9F%98%80"),
                Arguments.of(";editMode=OWNER_EDIT", "%3BeditMode%3DOWNER_EDIT"),
                Arguments.of("{id}", "%7Bid%7D"));
    }

    @ParameterizedTest
    @MethodSource("rawIdentitySuffixes")
    void errorActionsRetainRawIdentityAndSelectedModeAcrossRetries(String suffix, String encoded) throws Exception {
        for (boolean malformedModule : List.of(false, true)) for (String mode : List.of("USER_EDIT", "OWNER_EDIT")) {
            String moduleId = "module-9" + (malformedModule ? suffix : "");
            String childId = "item-0" + (malformedModule ? "" : suffix);
            String rawPath = "/" + moduleId + "/" + childId;
            String encodedPath = "/module-9" + (malformedModule ? encoded : "")
                    + "/item-0" + (malformedModule ? "" : encoded);
            String action = "/editing-demo/save-child" + encodedPath + "?editMode=" + mode;
            int status = malformedModule ? 404 : 400;
            for (boolean existingProposal : List.of(false, true)) {
                if (existingProposal) call(post("/editing-demo/save/module-9")
                        .param("editMode", "USER_EDIT").param("width", "4"), 200);
                Object proposal = existingProposal ? pending().getFirst() : null;
                String draft = "\n\n<雪> & \"draft\"\t\n";
                for (var request : List.of(
                        get(URI.create("/editing-demo/edit-child" + encodedPath + "?editMode=" + mode)),
                        post(URI.create(action)).param("text", draft),
                        delete(URI.create("/editing-demo/delete-child" + encodedPath + "?editMode=" + mode)))) {
                    MvcResult result = call(request, status);
                    for (int retry = 0; retry < 2; retry++) {
                        String html = body(result);
                        var doc = Jsoup.parse(html);
                        var form = doc.selectFirst("#editing-child-form");
                        assertEquals(action, form.attr("hx-post"));
                        assertEquals(action, form.attr("action"));
                        assertTrue(html.contains("hx-post=\"" + action + "\""));
                        assertTrue(html.contains("action=\"" + action + "\""));
                        URI parsed = URI.create(form.attr("action"));
                        assertEquals("/editing-demo/save-child" + rawPath, parsed.getPath());
                        assertEquals("/editing-demo/save-child" + encodedPath, parsed.getRawPath());
                        assertEquals("editMode=" + mode, parsed.getRawQuery());
                        assertNull(parsed.getRawFragment());
                        assertEquals(mode.equals("USER_EDIT"), doc.text().contains("Prior pending child and property changes are discarded"));
                        assertEquals("true", result.getResponse().getHeader("X-Editing-Demo-Modal"));
                        assertEquals("innerHTML", result.getResponse().getHeader("HX-Reswap"));
                        assertFalse(html.contains("hx-swap-oob"));
                        assertTrue(doc.select("script").isEmpty());
                        assertEquals("Item 1,Item 2,Item 3", content());
                        assertEquals(existingProposal ? 1 : 0, pending().size());
                        if (existingProposal) assertSame(proposal, pending().getFirst());
                        result = call(post(parsed).param("text", draft), status);
                        assertEquals("\n" + draft, Jsoup.parse(body(result)).selectFirst("textarea").wholeText());
                    }
                }
                call(delete("/editing-demo/pending-edits/module-9/reject"), existingProposal ? 200 : 404);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"item-3", "item-2147483647"})
    void absentCurrentPositionsAre404(String id) throws Exception {
        call(get(route("edit-child", id)), 404);
        call(post(route("save-child", id)).param("text", "draft"), 404);
        call(delete(route("delete-child", id)), 404);
        assertEquals("Item 1,Item 2,Item 3", content());
        assertTrue(pending().isEmpty());
    }

    @Test
    void missingAndWrongKindAre404() throws Exception {
        for (String id : List.of("missing", "module-1")) {
            for (var request : List.of(get("/editing-demo/edit-child/" + id + "/item-0"),
                    post("/editing-demo/save-child/" + id + "/item-0").param("text", "draft"),
                    delete("/editing-demo/delete-child/" + id + "/item-0"))) call(request, 404);
        }
        assertEquals("Item 1,Item 2,Item 3", content());
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void canEditAloneGovernsChildControlsAndEnforcement(boolean edit, boolean deleteParent) throws Exception {
        set("canEdit", edit);
        set("canDelete", deleteParent);
        var parent = Jsoup.parse(body(call(get("/editing-demo/edit/module-9"), edit ? 200 : 403)));
        assertEquals(edit ? 3 : 0, parent.select("[hx-get^=/editing-demo/edit-child]").size());
        assertEquals(edit ? 3 : 0, parent.select("[hx-delete^=/editing-demo/delete-child]").size());
        assertEquals(edit && deleteParent ? 1 : 0, parent.select("[hx-delete^=/editing-demo/delete/module-9]").size());
        call(get(route("edit-child", "item-0")), edit ? 200 : 403);
        call(post(route("save-child", "item-0")).param("text", "Changed"), edit ? 200 : 403);
        call(delete(route("delete-child", "item-0")), edit ? 200 : 403);
        assertEquals(edit ? "Item 2,Item 3" : "Item 1,Item 2,Item 3", content());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n", "\u2003", "a,b", "<script>,&\"",
            "\n\n<script>雪 & \"quoted\" 😀</script>\t,\n \t", " \t\n\n", " \t a,b \t ",
            "\r\n<b>comma,</b>\r\n\tend\r"})
    void invalidLabelsKeepDraftEscapedAndPendingUnchanged(String draft) throws Exception {
        call(post("/editing-demo/save/module-9").param("editMode", "USER_EDIT").param("width", "4"), 200);
        Object oldProposal = pending().getFirst();
        MvcResult response = call(post(route("save-child", "item-0")).param("text", draft).param("editMode", "USER_EDIT"), 400);
        String html = body(response);
        var doc = Jsoup.parse(html);
        // Jsoup .val() trims textarea edges, and wholeText() retains the parser padding LF.
        // Assert the full payload here; the packaged browser tests prove the native value.
        assertEquals("\n" + draft, doc.selectFirst("textarea[name=text]").wholeText());
        assertEquals("true", response.getResponse().getHeader("X-Editing-Demo-Modal"));
        assertEquals("innerHTML", response.getResponse().getHeader("HX-Reswap"));
        assertNotNull(doc.selectFirst("[role=alert]"));
        assertEquals("true", doc.selectFirst("textarea").attr("aria-invalid"));
        assertTrue(doc.select("script").isEmpty());
        assertFalse(html.contains("hx-swap-oob"));
        assertEquals("Item 1,Item 2,Item 3", content());
        assertSame(oldProposal, pending().getFirst());
    }

    @Test
    void missingLabelIsControlled400() throws Exception {
        var doc = Jsoup.parse(body(call(post(route("save-child", "item-0")), 400)));
        assertEquals("\n", doc.selectFirst("textarea").wholeText());
        assertEquals("Item 1,Item 2,Item 3", content());
    }

    @Test
    void ownerStoresPlainUnicodeTextAndPreservesAllOtherFieldsAndStoredEntities() throws Exception {
        set("content", "&lt;stored&gt;,Item 2,尾");
        set("useMarkdown", false);
        set("width", 8);
        set("canDelete", false);
        String value = "  <b>雪 & \"quoted\" 'single' 😀</b>  ";
        String html = body(call(post(route("save-child", "item-1")).param("text", value), 200));
        assertEquals("&lt;stored&gt;," + value.trim() + ",尾", content());
        var doc = Jsoup.parse(html);
        assertTrue(doc.select("script,b").isEmpty());
        assertTrue(doc.text().contains(value.trim()));
        assertTrue(doc.text().contains("&lt;stored&gt;"));
        assertEquals("Nested Editing Demo", field(module, "title"));
        assertEquals(8, field(module, "width"));
        assertEquals(false, field(module, "useMarkdown"));
        assertEquals(false, field(module, "canDelete"));
        assertEquals(true, field(module, "canEdit"));
        assertEquals(EditMode.OWNER_EDIT, field(module, "editMode"));
        assertTrue(pending().isEmpty());
        assertEquals(1, doc.select("#page-content[hx-swap-oob]").size());
        assertEquals(1, doc.select("#edit-modal-container[hx-swap-oob]").size());
    }

    @Test
    void longTextHasNoInventedLimit() throws Exception {
        String value = "雪<&".repeat(600);
        call(post(route("save-child", "item-2")).param("text", value), 200);
        assertEquals("Item 1,Item 2," + value, content());
        var doc = Jsoup.parse(body(call(get(route("edit-child", "item-2")), 200)));
        assertEquals("\n" + value, doc.selectFirst("textarea").wholeText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"First\nLine", "First\n\nLine", "First\r\nLine", "First\rLine",
            "Tabs\tinside", "<b>雪 & \"quoted\" 😀</b>\tline\nnext"})
    void acceptedInternalWhitespacePrefillsAndRoundTripsWithoutChangingPolicy(String label) throws Exception {
        set("content", " \t" + label + "\t ,Item 2,Item 3");
        set("useMarkdown", false);
        call(post("/editing-demo/save/module-9").param("editMode", "USER_EDIT").param("width", "4"), 200);
        Object proposal = pending().getFirst();
        var doc = Jsoup.parse(body(call(get(route("edit-child", "item-0")), 200)));
        var input = doc.selectFirst("textarea[name=text]");
        assertNotNull(input);
        assertEquals("\n" + label, input.wholeText());
        assertEquals("soft", input.attr("wrap"));
        assertFalse(input.hasAttr("maxlength"));
        assertFalse(input.hasAttr("pattern"));
        call(post(route("save-child", "item-0")).param("text", label), 200);
        assertEquals(label + ",Item 2,Item 3", content());
        assertEquals(false, field(module, "useMarkdown"));
        assertSame(proposal, pending().getFirst());
        assertEquals("\n" + label, Jsoup.parse(body(call(get(route("edit-child", "item-0")), 200)))
                .selectFirst("textarea").wholeText());
        call(post(route("save-child", "item-1")).param("text", " \t" + label + "\n "), 200);
        assertEquals(label + "," + label + ",Item 3", content());
        assertSame(proposal, pending().getFirst());
    }

    @Test
    void lossyRetainedEmptyItemsRejectBeforeLiveOrPendingMutation() throws Exception {
        set("content", "a, ,b");
        call(post("/editing-demo/save/module-9").param("editMode", "USER_EDIT").param("width", "4"), 200);
        Object proposal = pending().getFirst();
        call(delete(route("delete-child", "item-2")).param("editMode", "USER_EDIT"), 400);
        assertEquals("a, ,b", content());
        assertSame(proposal, pending().getFirst());
        set("content", ",a");
        call(delete(route("delete-child", "item-1")), 400);
        assertEquals(",a", content());
        call(post(route("save-child", "item-1")).param("text", "b"), 200);
        assertEquals(",b", content());
        call(delete(route("delete-child", "item-0")), 200);
        assertEquals("b", content());
    }

    @Test
    void deletionReindexesCurrentPositionsAndLastItemLeavesModule() throws Exception {
        var result = call(delete(route("delete-child", "item-0")), 200);
        assertEquals("none", result.getResponse().getHeader("HX-Reswap"));
        assertEquals("Item 2,Item 3", content());
        // A stale positional item-0 now denotes Item 2, not a persistent Item 1 identity.
        assertEquals("\nItem 2", Jsoup.parse(body(call(get(route("edit-child", "item-0")), 200))).selectFirst("textarea").wholeText());
        call(delete(route("delete-child", "item-1")), 200);
        call(delete(route("delete-child", "item-0")), 200);
        assertEquals("", content());
        assertEquals("Nested Editing Demo", field(module, "title"));
        call(get(route("edit-child", "item-0")), 404);
        assertTrue(Jsoup.parse(body(call(get("/editing-demo/edit/module-9"), 200))).select("[hx-get]").isEmpty());
    }

    @Test
    void userReplacesWholeProposalFromLiveAndApproveChangesContentOnly() throws Exception {
        set("useMarkdown", true);
        set("canDelete", false);
        set("editMode", EditMode.USER_EDIT);
        call(post("/editing-demo/save/module-9").param("editMode", "USER_EDIT")
                .param("title", "Discarded title").param("width", "4").param("content", "Discarded content"), 200);
        call(post(route("save-child", "item-0")).param("text", "First proposal"), 200);
        assertEquals(Map.of("content", "First proposal,Item 2,Item 3"), field(pending().getFirst(), "changes"));
        call(post(route("save-child", "item-1")).param("text", "Second proposal"), 200);
        assertEquals(1, pending().size());
        assertEquals(Map.of("content", "Item 1,Second proposal,Item 3"), field(pending().getFirst(), "changes"));
        assertEquals("Item 1,Item 2,Item 3", content());
        assertTrue(body(call(get("/editing-demo/pending-edits"), 200)).contains("Fields: content"));
        call(post("/editing-demo/pending-edits/module-9/approve"), 200);
        assertEquals("Item 1,Second proposal,Item 3", content());
        assertEquals("Nested Editing Demo", field(module, "title"));
        assertEquals(12, field(module, "width"));
        assertEquals(true, field(module, "useMarkdown"));
        assertEquals(false, field(module, "canDelete"));
        assertEquals(EditMode.USER_EDIT, field(module, "editMode"));
        assertTrue(pending().isEmpty());
    }

    @Test
    void userDeletionRejectAndCancelDoNotChangeLive() throws Exception {
        call(delete(route("delete-child", "item-1")).param("editMode", "USER_EDIT"), 200);
        assertEquals(Map.of("content", "Item 1,Item 3"), field(pending().getFirst(), "changes"));
        assertEquals("Item 1,Item 2,Item 3", content());
        call(delete("/editing-demo/pending-edits/module-9/reject"), 200);
        call(get(route("edit-child", "item-0")), 200); // Opening/cancelling has no state effect.
        assertEquals("Item 1,Item 2,Item 3", content());
        assertTrue(pending().isEmpty());
    }

    @Test
    void explicitModeAndInvalidModeKeepExistingResolutionSemantics() throws Exception {
        set("editMode", EditMode.USER_EDIT);
        call(post(route("save-child", "item-0")).param("text", "Immediate").param("editMode", "OWNER_EDIT"), 200);
        assertEquals("Immediate,Item 2,Item 3", content());
        call(post(route("save-child", "item-1")).param("text", "Pending").param("editMode", "junk"), 200);
        assertEquals("Immediate,Item 2,Item 3", content());
        assertEquals(Map.of("content", "Immediate,Pending,Item 3"), field(pending().getFirst(), "changes"));
    }
}
