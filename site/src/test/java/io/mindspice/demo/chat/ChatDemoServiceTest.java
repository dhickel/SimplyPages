package io.mindspice.demo.chat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatDemoServiceTest {
    @Test
    void appendPairAndCopiedHistoryStayCoherentWithPruningAndMonotonicCursor() {
        try (ChatDemoService service = new ChatDemoService(Clock.systemUTC(), false)) {
            MockHttpSession session = new MockHttpSession();
            var initial = service.initialize(session, false);
            for (int i = 0; i < 30; i++) {
                var snapshot = service.appendExchange(session, initial.conversationId(), "Message " + i, "Tester");
                assertEquals(3 + 2L * i, snapshot.cursor());
                assertEquals(Math.min(51, 3 + 2 * i), snapshot.messages().size());
                assertEquals("assistant", snapshot.messages().getFirst().role());
                for (int j = 1; j < snapshot.messages().size(); j += 2) {
                    assertEquals("user", snapshot.messages().get(j).role());
                    assertEquals("assistant", snapshot.messages().get(j + 1).role());
                    assertEquals("Received: " + snapshot.messages().get(j).body(), snapshot.messages().get(j + 1).body());
                }
            }
            assertEquals(1, initial.messages().size());
            assertThrows(UnsupportedOperationException.class, () -> initial.messages().clear());
            var retained = service.history(session, initial.conversationId());
            assertEquals("Message 5", retained.messages().get(1).body());
            assertEquals("Message 29", retained.messages().get(49).body());
            assertEquals(61, retained.cursor());
        }
    }

    @Test
    void decodedUtf16BoundPrecedesTrimAndUsesFixedBoundedNameFallback() {
        try (ChatDemoService service = new ChatDemoService(Clock.systemUTC(), false)) {
            MockHttpSession session = new MockHttpSession();
            String id = service.initialize(session, false).conversationId();
            for (String body : List.of("", "   ", "\t\n", "a".repeat(4097),
                    " " + "a".repeat(4096), "😀".repeat(2048) + " ")) {
                var error = assertThrows(ChatDemoService.ChatFailure.class, () -> service.appendExchange(session, id, body, "Tester"));
                assertEquals(400, error.status());
                assertEquals(1, service.history(session, id).cursor());
            }
            for (String body : List.of("a".repeat(4095), "a".repeat(4096), "😀".repeat(2048), " " + "a".repeat(4094) + " ")) {
                var snapshot = service.appendExchange(session, id, body, "n".repeat(65));
                assertEquals(body.trim(), snapshot.messages().get(snapshot.messages().size() - 2).body());
                assertEquals("Demo User", snapshot.messages().get(snapshot.messages().size() - 2).author());
            }
            var exactName = service.appendExchange(session, id, "ok", "n".repeat(64));
            assertEquals("n".repeat(64), exactName.messages().get(exactName.messages().size() - 2).author());
        }
    }

    @Test
    void messageAndInternalNameKeepLegacyTrimRatherThanANewUnicodeGrammar() {
        try (ChatDemoService service = new ChatDemoService(Clock.systemUTC(), false)) {
            MockHttpSession session = new MockHttpSession();
            String id = service.initialize(session, false).conversationId();
            var snapshot = service.appendExchange(session, id, " \u2003 ", " \u2003 ");
            assertEquals("\u2003", snapshot.messages().get(1).body());
            assertEquals("\u2003", snapshot.messages().get(1).author());
            assertEquals("Received: \u2003", snapshot.messages().get(2).body());
        }
    }

    @Test
    void lookupOnlyExactOpaqueOwnershipNeverAllocatesOrNormalizes() {
        try (ChatDemoService service = new ChatDemoService(Clock.systemUTC(), false)) {
            MockHttpSession owner = new MockHttpSession(), peer = new MockHttpSession();
            String id = service.initialize(owner, false).conversationId();
            for (String bad : List.of(id, "unknown", " " + id, id + " ", id.replace("conv-", "conv%2D"))) {
                assertEquals(404, assertThrows(ChatDemoService.ChatFailure.class, () -> service.history(peer, bad)).status());
                assertEquals(404, assertThrows(ChatDemoService.ChatFailure.class, () -> service.appendExchange(peer, bad, "attempt", "Tester")).status());
                assertEquals(404, assertThrows(ChatDemoService.ChatFailure.class, () -> service.stream(peer, bad)).status());
            }
            for (String bad : List.of("", " ", "\t\n")) {
                assertEquals(400, assertThrows(ChatDemoService.ChatFailure.class, () -> service.history(owner, bad)).status());
            }
            assertEquals(400, assertThrows(ChatDemoService.ChatFailure.class, () -> service.history(owner, null)).status());
            assertEquals(404, assertThrows(ChatDemoService.ChatFailure.class, () -> service.history(null, id)).status());
            assertEquals(404, assertThrows(ChatDemoService.ChatFailure.class, () -> service.history(owner, " " + id)).status());
            assertEquals(1, service.conversationCount());
            assertEquals(0, service.streamCount());
            assertEquals(1, service.history(owner, id).messages().size());
            assertNull(peer.getAttribute(ChatDemoService.SESSION_CONVERSATION));
        }
    }
}
