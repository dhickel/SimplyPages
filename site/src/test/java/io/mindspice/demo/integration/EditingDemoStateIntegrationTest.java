package io.mindspice.demo.integration;

import io.mindspice.demo.EditingDemoController;
import io.mindspice.simplypages.editing.EditMode;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Private-controller tests: the monitor gates requests, not response delivery order. */
class EditingDemoStateIntegrationTest {
    private Fixture fixture;

    @BeforeEach
    void freshState() {
        fixture = new Fixture();
    }

    static Stream<Arguments> flagsAndModes() {
        var cases = new ArrayList<Arguments>();
        for (String id : List.of("module-1", "module-9"))
            for (boolean edit : List.of(false, true))
                for (boolean delete : List.of(false, true))
                    for (EditMode stored : EditMode.values())
                        for (String requested : List.of("", "OWNER_EDIT", "USER_EDIT", "invalid"))
                            cases.add(Arguments.of(id, edit, delete, stored, requested));
        return cases.stream();
    }

    @ParameterizedTest
    @MethodSource("flagsAndModes")
    void parentPolicyUsesCurrentFlagsWithoutTreatingModesAsAuthorization(
            String id, boolean edit, boolean deleteParent, EditMode stored, String requested) throws Exception {
        fixture.setModule(id, "editMode", stored);
        // Stage while editable, then change the current flag to prove approval rechecks it.
        fixture.call(post("/editing-demo/save/" + id).param("editMode", "USER_EDIT")
                .param("title", "Earlier proposal").param("content", "Earlier content"), 200);
        fixture.setModule(id, "canEdit", edit);
        fixture.setModule(id, "canDelete", deleteParent);
        State before = fixture.state();
        fixture.call(mode(get("/editing-demo/edit/" + id), requested), edit ? 200 : 403);
        assertEquals(before, fixture.state(), "Opening a modal must not change state");
        fixture.call(mode(post("/editing-demo/save/" + id).param("title", "Current title")
                .param("content", "Current content").param("width", "4"), requested), edit ? 200 : 403);
        EditMode resolved = requested.equals("OWNER_EDIT") ? EditMode.OWNER_EDIT
                : requested.equals("USER_EDIT") ? EditMode.USER_EDIT : stored;
        if (!edit) {
            assertEquals(before, fixture.state());
            fixture.call(post("/editing-demo/pending-edits/" + id + "/approve"), 403);
            assertEquals(before, fixture.state());
        } else {
            assertEquals(resolved == EditMode.OWNER_EDIT ? "Current title" : before.module(id).title(),
                    fixture.state().module(id).title());
            assertEquals(resolved == EditMode.OWNER_EDIT ? "Earlier proposal" : "Current title",
                    fixture.state().proposals().getFirst().changes().get("title"));
            fixture.call(post("/editing-demo/pending-edits/" + id + "/approve"), 200);
            assertEquals(resolved == EditMode.USER_EDIT ? "Current title" : "Earlier proposal",
                    fixture.state().module(id).title());
            assertTrue(fixture.state().proposals().isEmpty());
        }
        State beforeDelete = fixture.state();
        fixture.call(delete("/editing-demo/delete/" + id).param("editMode", "USER_EDIT"), deleteParent ? 200 : 403);
        if (deleteParent) {
            assertNull(fixture.state().module(id));
            assertTrue(fixture.state().proposals().isEmpty(), "Immediate parent deletion clears proposals");
        } else {
            assertEquals(beforeDelete, fixture.state());
            if (!edit) fixture.call(delete("/editing-demo/pending-edits/" + id + "/reject"), 200);
        }
        fixture.assertInvariants();
    }

    @ParameterizedTest
    @MethodSource("flagsAndModes")
    void childPolicyDependsOnImmutableKindAndCanEditAloneAcrossModes(
            String id, boolean edit, boolean deleteParent, EditMode stored, String requested) throws Exception {
        boolean list = id.equals("module-9");
        fixture.setModule(id, "title", list ? "Renamed list" : "Nested Editing Demo");
        fixture.setModule(id, "content", "One,Two,Three");
        fixture.setModule(id, "canEdit", edit);
        fixture.setModule(id, "canDelete", deleteParent);
        fixture.setModule(id, "editMode", stored);
        State before = fixture.state();
        int status = !list ? 404 : edit ? 200 : 403;
        fixture.call(mode(get("/editing-demo/edit-child/" + id + "/item-0"), requested), status);
        assertEquals(before, fixture.state());
        fixture.call(mode(post("/editing-demo/save-child/" + id + "/item-0").param("text", "Changed"), requested), status);
        fixture.call(mode(delete("/editing-demo/delete-child/" + id + "/item-0"), requested), status);
        if (!list || !edit) {
            assertEquals(before, fixture.state());
        } else {
            boolean user = requested.equals("USER_EDIT") || (!requested.equals("OWNER_EDIT") && stored == EditMode.USER_EDIT);
            assertEquals(user ? "One,Two,Three" : "Two,Three", fixture.state().module(id).content());
            if (user) {
                assertEquals(List.of(new Proposal(id, Map.of("content", "Two,Three"))), fixture.state().proposals());
                fixture.call(post("/editing-demo/pending-edits/" + id + "/approve"), 200);
                assertEquals("Two,Three", fixture.state().module(id).content());
            } else assertTrue(fixture.state().proposals().isEmpty());
            ModuleState original = before.module(id);
            ModuleState updated = fixture.state().module(id);
            assertEquals(new ModuleState(original.id(), original.kind(), original.title(), "Two,Three", original.width(),
                    original.markdown(), original.canEdit(), original.canDelete(), original.mode()), updated);
        }
        fixture.assertInvariants();
    }

    @Test
    void seededDeleteOnlyEditOnlyAndFullyLockedModulesEnforceTheirIndependentFlags() throws Exception {
        fixture.call(get("/editing-demo/edit/module-3"), 403);
        fixture.call(post("/editing-demo/save/module-3").param("content", "Denied"), 403);
        fixture.call(delete("/editing-demo/delete/module-3"), 403);
        fixture.call(get("/editing-demo/edit/module-4"), 200);
        fixture.call(post("/editing-demo/save/module-4").param("content", "Allowed"), 200);
        fixture.call(delete("/editing-demo/delete/module-4"), 403);
        fixture.call(get("/editing-demo/edit/module-5"), 403);
        fixture.call(post("/editing-demo/save/module-5").param("editMode", "OWNER_EDIT"), 403);
        fixture.call(delete("/editing-demo/delete/module-5"), 200);
        assertNull(fixture.state().module("module-5"));
        fixture.assertInvariants();
    }

    static Stream<Arguments> kindsAndRenames() {
        return Stream.of(Arguments.of("module-9", "OWNER_EDIT", "Renamed list"),
                Arguments.of("module-9", "USER_EDIT", "Renamed list"),
                Arguments.of("module-1", "OWNER_EDIT", "Nested Editing Demo"),
                Arguments.of("module-1", "USER_EDIT", "Nested Editing Demo"));
    }

    @ParameterizedTest
    @MethodSource("kindsAndRenames")
    void intrinsicKindSurvivesOwnerAndApprovedRenames(String id, String mode, String title) throws Exception {
        boolean list = id.equals("module-9");
        fixture.call(post("/editing-demo/save/" + id).param("editMode", mode).param("title", title)
                .param("content", "Alpha,Beta"), 200);
        if (mode.equals("USER_EDIT")) fixture.call(post("/editing-demo/pending-edits/" + id + "/approve"), 200);
        assertEquals(list ? "LIST" : "CONTENT", fixture.state().module(id).kind());
        assertEquals(title, fixture.state().module(id).title());
        var modal = Jsoup.parse(fixture.call(get("/editing-demo/edit/" + id), 200).html());
        assertEquals(list ? 2 : 0, modal.select("[hx-get^=/editing-demo/edit-child]").size());
        for (var request : List.of(get("/editing-demo/edit-child/" + id + "/item-0"),
                post("/editing-demo/save-child/" + id + "/item-0").param("text", "Changed"),
                delete("/editing-demo/delete-child/" + id + "/item-0"))) {
            fixture.call(request, list ? 200 : 404);
        }
        String page = fixture.call(get("/editing-demo").header("HX-Request", "true"), 200).html();
        assertTrue(Jsoup.parse(page).text().contains(title));
        assertEquals(list ? "Beta" : "Alpha,Beta", fixture.state().module(id).content());
        Field kind = Fixture.declared(fixture.moduleObject(id), "kind");
        assertTrue(Modifier.isFinal(kind.getModifiers()));
        assertTrue(Modifier.isPrivate(kind.getType().getModifiers()));
    }

    static Stream<Arguments> newModuleOptions() {
        var cases = new ArrayList<Arguments>();
        for (boolean edit : List.of(false, true)) for (boolean delete : List.of(false, true))
            for (boolean markdown : List.of(false, true))
                for (String mode : List.of("OWNER_EDIT", "USER_EDIT", "invalid"))
                    cases.add(Arguments.of(edit, delete, markdown, mode));
        return cases.stream();
    }

    @ParameterizedTest
    @MethodSource("newModuleOptions")
    void everyNewModuleIsContentEvenWithSeedListTitle(boolean edit, boolean deleteParent,
            boolean markdown, String mode) throws Exception {
        var request = post("/editing-demo/add-module/row-5").param("title", "Nested Editing Demo")
                .param("content", "Alpha,Beta").param("width", "8").param("editMode", mode);
        if (edit) request.param("canEdit", "true");
        if (deleteParent) request.param("canDelete", "true");
        if (markdown) request.param("useMarkdown", "true");
        fixture.call(request, 200);
        ModuleState added = fixture.state().module("module-101");
        assertEquals("CONTENT", added.kind());
        assertEquals(8, added.width());
        assertEquals(edit, added.canEdit());
        assertEquals(deleteParent, added.canDelete());
        assertEquals(markdown, added.markdown());
        assertEquals(mode.equals("USER_EDIT") ? "USER_EDIT" : "OWNER_EDIT", added.mode());
        fixture.call(get("/editing-demo/edit/module-101"), edit ? 200 : 403);
        for (var child : List.of(get("/editing-demo/edit-child/module-101/item-0"),
                post("/editing-demo/save-child/module-101/item-0").param("text", "Denied"),
                delete("/editing-demo/delete-child/module-101/item-0"))) fixture.call(child, 404);
        assertEquals(1, fixture.state().rows().stream().flatMap(row -> row.modules().stream())
                .filter(module -> module.kind().equals("LIST")).count());
        fixture.assertInvariants();
    }

    @Test
    void addModalAndSubmissionHaveIdenticalExistencePolicyAndCapacityChecks() throws Exception {
        for (String row : List.of("missing", "row-3", "row-2")) {
            int status = row.equals("missing") ? 404 : row.equals("row-3") ? 403 : 409;
            State before = fixture.state();
            fixture.call(get("/editing-demo/add-module-modal/" + row), status);
            fixture.call(post("/editing-demo/add-module/" + row).param("title", "Denied"), status);
            assertEquals(before, fixture.state());
        }
        // When both blocked and full, the policy failure is still 403.
        fixture.setRow("row-2", "canAddModule", false);
        fixture.call(get("/editing-demo/add-module-modal/row-2"), 403);
        fixture.call(post("/editing-demo/add-module/row-2"), 403);
        fixture.call(get("/editing-demo/add-module-modal/row-5"), 200);
        fixture.call(post("/editing-demo/add-module/row-5"), 200);
        fixture.call(post("/editing-demo/add-module/row-5"), 200);
        State full = fixture.state();
        fixture.call(get("/editing-demo/add-module-modal/row-5"), 409);
        fixture.call(post("/editing-demo/add-module/row-5"), 409);
        assertEquals(full, fixture.state());
    }

    @Test
    void proposalFailuresAreAtomicAndRejectIgnoresFlags() throws Exception {
        for (String id : List.of("missing", "module-9")) {
            fixture.call(post("/editing-demo/pending-edits/" + id + "/approve"), 404);
            fixture.call(delete("/editing-demo/pending-edits/" + id + "/reject"), 404);
        }
        fixture.call(post("/editing-demo/save/module-9").param("editMode", "USER_EDIT")
                .param("title", "Pending title").param("content", "Pending content"), 200);
        fixture.setModule("module-9", "canEdit", false);
        fixture.setModule("module-9", "canDelete", false);
        State before = fixture.state();
        fixture.call(post("/editing-demo/pending-edits/module-9/approve"), 403);
        assertEquals(before, fixture.state());
        fixture.call(delete("/editing-demo/delete/module-9"), 403);
        assertEquals(before, fixture.state());
        fixture.call(delete("/editing-demo/pending-edits/module-9/reject"), 200);
        assertEquals(before.rows(), fixture.state().rows());
        assertTrue(fixture.state().proposals().isEmpty());
        fixture.call(delete("/editing-demo/pending-edits/module-9/reject"), 404);
        // Missing target must not apply or consume an existing proposal.
        Fixture orphan = new Fixture();
        orphan.call(post("/editing-demo/save/module-9").param("editMode", "USER_EDIT").param("content", "Proposal"), 200);
        orphan.underLock(() -> orphan.rowsObjects().getLast().modules().clear());
        State missing = orphan.state();
        orphan.call(post("/editing-demo/pending-edits/module-9/approve"), 404);
        assertEquals(missing, orphan.state());
        orphan.call(delete("/editing-demo/pending-edits/module-9/reject"), 200);
    }

    @Test
    void ordinaryFailuresPreserveAllStateAndAllocationCounter() throws Exception {
        fixture.call(post("/editing-demo/save/module-9").param("editMode", "USER_EDIT").param("title", "Retained"), 200);
        fixture.setModule("module-9", "content", "a, ,b");
        State before = fixture.state();
        for (var request : List.of(post("/editing-demo/insert-row/-2"), post("/editing-demo/insert-row/5"),
                post("/editing-demo/insert-row/2147483648"), post("/editing-demo/insert-row/junk"),
                post("/editing-demo/save-child/module-9/item-0").param("text", "bad,label"),
                delete("/editing-demo/delete-child/module-9/item-2").param("editMode", "USER_EDIT"))) {
            fixture.call(request, 400);
            assertEquals(before, fixture.state());
        }
        for (var request : List.of(get("/editing-demo/edit/missing"), post("/editing-demo/save/missing"),
                delete("/editing-demo/delete/missing"), get("/editing-demo/add-module-modal/missing"),
                post("/editing-demo/add-module/missing"), post("/editing-demo/pending-edits/missing/approve"),
                delete("/editing-demo/pending-edits/missing/reject"))) {
            fixture.call(request, 404);
            assertEquals(before, fixture.state());
        }
    }

    private static Op read() { return new Op("read", () -> get("/editing-demo").header("HX-Request", "true")); }
    private static Op save(String title) { return new Op(title, () -> post("/editing-demo/save/module-9")
            .param("editMode", "OWNER_EDIT").param("title", title).param("content", title + ",Tail")); }
    private static Op stage() { return new Op("stage", () -> post("/editing-demo/save/module-9")
            .param("editMode", "USER_EDIT").param("title", "Staged title").param("content", "Stage,Tail")); }
    private static Op child(String mode) { return new Op("child", () -> post("/editing-demo/save-child/module-9/item-0")
            .param("editMode", mode).param("text", "Child")); }
    private static Op deleteChild(String mode) { return new Op("delete-child", () -> delete("/editing-demo/delete-child/module-9/item-0")
            .param("editMode", mode)); }
    private static Op approve() { return new Op("approve", () -> post("/editing-demo/pending-edits/module-9/approve")); }
    private static Op reject() { return new Op("reject", () -> delete("/editing-demo/pending-edits/module-9/reject")); }
    private static Op deleteParent() { return new Op("delete", () -> delete("/editing-demo/delete/module-9")); }
    private static Op add(String title) { return new Op(title, () -> post("/editing-demo/add-module/row-5")
            .param("title", title).param("content", title).param("canEdit", "true").param("canDelete", "true")); }
    private static Op insert(String name) { return new Op(name, () -> post("/editing-demo/insert-row/-1")); }

    static Stream<Arguments> concurrentScenarios() {
        Consumer<Fixture> noSetup = ignored -> { };
        Consumer<Fixture> pending = f -> f.performUnchecked(stage().request().get());
        return Stream.of(
                Arguments.of("parent save/save/read", noSetup, List.of(save("First"), save("Second"), read())),
                Arguments.of("child save/delete/read", noSetup, List.of(child("OWNER_EDIT"), deleteChild("OWNER_EDIT"), read())),
                Arguments.of("parent/child proposals and approval", noSetup, List.of(stage(), child("USER_EDIT"), approve())),
                Arguments.of("approval/rejection/read", pending, List.of(approve(), reject(), read())),
                Arguments.of("parent delete/stage/read", noSetup, List.of(deleteParent(), stage(), read())),
                Arguments.of("parent delete/approve/reject", pending, List.of(deleteParent(), approve(), reject())),
                Arguments.of("insert/remove last module/read", noSetup, List.of(insert("insert"), deleteParent(), read())),
                Arguments.of("add/add/modal at capacity", noSetup, List.of(add("One"), add("Two"),
                        new Op("modal", () -> get("/editing-demo/add-module-modal/row-5")))),
                Arguments.of("insert/insert/add unique allocations", noSetup, List.of(insert("insert-one"), insert("insert-two"), add("Added"))),
                Arguments.of("rejected save/approve with permitted reject", (Consumer<Fixture>) f -> {
                    pending.accept(f); f.setModule("module-9", "canEdit", false);
                }, List.of(save("Denied"), approve(), reject())),
                Arguments.of("invalid child/approve/read", pending, List.of(
                        new Op("invalid", () -> post("/editing-demo/save-child/module-9/item-0").param("text", "bad,label")), approve(), read())),
                Arguments.of("user child proposals/pending modal", noSetup, List.of(child("USER_EDIT"), deleteChild("USER_EDIT"),
                        new Op("pending", () -> get("/editing-demo/pending-edits")))),
                Arguments.of("three competing additions cannot exceed cap", noSetup, List.of(add("One"), add("Two"), add("Three"), read())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("concurrentScenarios")
    void barrierControlledRequestsMatchACompleteSerialOutcome(String name, Consumer<Fixture> setup, List<Op> ops) throws Exception {
        // Each oracle contains the *entire* returned HTML snapshot for each named operation,
        // together with final state. Only generated presentation IDs are normalized.
        var allowed = new HashSet<Outcome>();
        for (List<Op> order : permutations(ops)) {
            Fixture serial = new Fixture();
            setup.accept(serial);
            Map<String, Response> responses = new TreeMap<>();
            for (Op op : order) responses.put(op.name(), serial.perform(op.request().get()));
            serial.assertInvariants();
            allowed.add(new Outcome(responses, serial.state()));
        }
        for (int repeat = 0; repeat < 3; repeat++) {
            Fixture concurrent = new Fixture();
            setup.accept(concurrent);
            Outcome actual = new Outcome(runBlockedTogether(concurrent, ops), concurrent.state());
            concurrent.assertInvariants();
            assertTrue(allowed.contains(actual), name + " returned a non-serial response/state combination on repeat " + repeat);
        }
    }

    static Stream<Arguments> statefulRoutes() {
        return Stream.of(read(), save("Save"), stage(), child("OWNER_EDIT"), deleteChild("OWNER_EDIT"), approve(), reject(),
                deleteParent(), add("Added"), insert("Insert"),
                new Op("parent modal", () -> get("/editing-demo/edit/module-9")),
                new Op("child modal", () -> get("/editing-demo/edit-child/module-9/item-0")),
                new Op("add modal", () -> get("/editing-demo/add-module-modal/row-5")),
                new Op("pending modal", () -> get("/editing-demo/pending-edits")))
                .map(op -> Arguments.of(op.name(), op));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("statefulRoutes")
    void everyStatefulEndpointWaitsForThePrivateStateMonitor(String name, Op operation) throws Exception {
        fixture.perform(stage().request().get());
        Field lock = Fixture.declared(fixture.controller, "stateLock");
        assertTrue(Modifier.isPrivate(lock.getModifiers()));
        assertTrue(Modifier.isFinal(lock.getModifiers()));
        runBlockedTogether(fixture, List.of(operation));
        fixture.assertInvariants();
    }

    private static Map<String, Response> runBlockedTogether(Fixture f, List<Op> operations) throws Exception {
        var pool = Executors.newFixedThreadPool(operations.size());
        var barrier = new CyclicBarrier(operations.size() + 1);
        var threads = new AtomicReferenceArray<Thread>(operations.size());
        var futures = new ArrayList<java.util.concurrent.Future<Response>>();
        try {
            synchronized (f.lock) {
                for (int i = 0; i < operations.size(); i++) {
                    final int index = i;
                    futures.add(pool.submit(() -> {
                        threads.set(index, Thread.currentThread());
                        barrier.await(10, TimeUnit.SECONDS);
                        return f.perform(operations.get(index).request().get());
                    }));
                }
                barrier.await(10, TimeUnit.SECONDS);
                long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
                for (int i = 0; i < operations.size(); i++) {
                    boolean blocked = false;
                    while (System.nanoTime() < deadline) {
                        var info = ManagementFactory.getThreadMXBean().getThreadInfo(threads.get(i).threadId());
                        blocked = info != null && info.getThreadState() == Thread.State.BLOCKED
                                && info.getLockOwnerId() == Thread.currentThread().threadId()
                                && info.getLockInfo().getIdentityHashCode() == System.identityHashCode(f.lock);
                        if (blocked || futures.get(i).isDone()) break;
                        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
                    }
                    assertTrue(blocked, operations.get(i).name() + " did not block on the controller state monitor");
                }
            }
            Map<String, Response> result = new TreeMap<>();
            for (int i = 0; i < futures.size(); i++) result.put(operations.get(i).name(), futures.get(i).get(20, TimeUnit.SECONDS));
            return result;
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "Request workers did not stop");
        }
    }

    private static <T> List<List<T>> permutations(List<T> values) {
        if (values.isEmpty()) return List.of(List.of());
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            var rest = new ArrayList<>(values);
            T first = rest.remove(i);
            for (List<T> tail : permutations(rest)) {
                var order = new ArrayList<T>(); order.add(first); order.addAll(tail); result.add(order);
            }
        }
        return result;
    }

    private static MockHttpServletRequestBuilder mode(MockHttpServletRequestBuilder request, String mode) {
        return mode.isEmpty() ? request : request.param("editMode", mode);
    }

    private record Op(String name, Supplier<MockHttpServletRequestBuilder> request) { }
    private record Response(int status, String html, String reswap, String modalHeader) { }
    private record Outcome(Map<String, Response> responses, State state) { }
    private record ModuleState(String id, String kind, String title, String content, int width,
                               boolean markdown, boolean canEdit, boolean canDelete, String mode) { }
    private record RowState(String id, int position, boolean canAdd, List<ModuleState> modules) { }
    private record Proposal(String id, Map<String, String> changes) { }
    private record State(int counter, List<RowState> rows, List<Proposal> proposals) {
        ModuleState module(String id) {
            return rows.stream().flatMap(row -> row.modules().stream()).filter(module -> module.id().equals(id)).findFirst().orElse(null);
        }
    }
    private record RowObjects(Object row, List<Object> modules) { }

    private static final class Fixture {
        private static final Pattern PRESENTATION_IDS = Pattern.compile(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|editable-module-[0-9]+");
        final EditingDemoController controller = new EditingDemoController();
        final Object lock = get(controller, "stateLock");
        final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        Response call(MockHttpServletRequestBuilder request, int status) throws Exception {
            Response response = perform(request);
            assertEquals(status, response.status(), response.html());
            return response;
        }

        Response perform(MockHttpServletRequestBuilder request) throws Exception {
            var response = mvc.perform(request).andReturn().getResponse();
            assertNotNull(response.getContentType());
            assertTrue(response.getContentType().startsWith("text/html"));
            assertTrue(response.getHeaders("Vary").stream().anyMatch(value -> value.contains("HX-Request")));
            String html = response.getContentAsString();
            Map<String, String> ids = new HashMap<>();
            // Preserve reference equivalence and distinct IDs without depending on random/global counters.
            html = PRESENTATION_IDS.matcher(html).replaceAll(match -> ids.computeIfAbsent(match.group(),
                    ignored -> "presentation-id-" + ids.size()));
            return new Response(response.getStatus(), html, response.getHeader("HX-Reswap"), response.getHeader("X-Editing-Demo-Modal"));
        }

        void performUnchecked(MockHttpServletRequestBuilder request) {
            try { call(request, 200); } catch (Exception error) { throw new AssertionError(error); }
        }

        @SuppressWarnings("unchecked")
        List<RowObjects> rowsObjects() {
            Object page = ((Map<String, Object>) get(controller, "pages")).get("demo-page");
            return ((List<Object>) get(page, "rows")).stream()
                    .map(row -> new RowObjects(row, (List<Object>) get(row, "modules"))).toList();
        }

        Object moduleObject(String id) {
            synchronized (lock) {
                return rowsObjects().stream().flatMap(row -> row.modules().stream())
                        .filter(module -> get(module, "id").equals(id)).findFirst().orElseThrow();
            }
        }

        void setModule(String id, String name, Object value) {
            underLock(() -> set(moduleObject(id), name, value));
        }

        void setRow(String id, String name, Object value) {
            underLock(() -> set(rowsObjects().stream().map(RowObjects::row)
                    .filter(row -> get(row, "id").equals(id)).findFirst().orElseThrow(), name, value));
        }

        void underLock(Runnable action) { synchronized (lock) { action.run(); } }

        @SuppressWarnings("unchecked")
        State state() {
            synchronized (lock) {
                var rows = rowsObjects().stream().map(row -> new RowState((String) get(row.row(), "id"),
                        (int) get(row.row(), "position"), (boolean) get(row.row(), "canAddModule"),
                        row.modules().stream().map(module -> new ModuleState((String) get(module, "id"),
                                get(module, "kind").toString(), (String) get(module, "title"), (String) get(module, "content"),
                                (int) get(module, "width"), (boolean) get(module, "useMarkdown"), (boolean) get(module, "canEdit"),
                                (boolean) get(module, "canDelete"), get(module, "editMode").toString())).toList())).toList();
                var pending = ((Map<String, List<Object>>) get(controller, "pendingEdits")).get("demo-page");
                var proposals = pending.stream().map(proposal -> new Proposal((String) get(proposal, "moduleId"),
                        new TreeMap<>((Map<String, String>) get(proposal, "changes"))))
                        .sorted(java.util.Comparator.comparing(Proposal::id)).toList();
                return new State(((AtomicInteger) get(controller, "idCounter")).get(), rows, proposals);
            }
        }

        void assertInvariants() {
            State state = state();
            var ids = new HashSet<String>();
            for (int i = 0; i < state.rows().size(); i++) {
                RowState row = state.rows().get(i);
                assertTrue(ids.add(row.id()), "Duplicate row ID");
                assertEquals(i, row.position());
                assertTrue(row.modules().size() <= 3, "Row exceeded module cap");
                for (ModuleState module : row.modules()) assertTrue(ids.add(module.id()), "Duplicate module ID");
            }
            var proposed = new HashSet<String>();
            for (Proposal proposal : state.proposals()) {
                assertNotNull(state.module(proposal.id()), "Orphan proposal");
                assertTrue(proposed.add(proposal.id()), "Multiple proposals for one module");
            }
        }

        static Field declared(Object object, String name) {
            try {
                Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field;
            } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        }
        static Object get(Object object, String name) {
            try { return declared(object, name).get(object); }
            catch (IllegalAccessException error) { throw new AssertionError(error); }
        }
        static void set(Object object, String name, Object value) {
            try { declared(object, name).set(object, value); }
            catch (IllegalAccessException error) { throw new AssertionError(error); }
        }
    }
}
