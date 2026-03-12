package io.mindspice.demo.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatDemoServiceTest {

    @Test
    @DisplayName("appendExchange should add user and assistant messages")
    void appendExchangeAddsMessages() {
        ChatDemoService service = new ChatDemoService();
        String conversationId = "conv-service-test";

        int beforeCount = service.history(conversationId).size();
        assertTrue(service.appendExchange(conversationId, "Hello", "Tester").isPresent());

        int afterCount = service.history(conversationId).size();
        assertEquals(beforeCount + 2, afterCount);
    }

    @Test
    @DisplayName("appendExchange should reject blank message body")
    void appendExchangeRejectsBlankBody() {
        ChatDemoService service = new ChatDemoService();

        assertTrue(service.appendExchange("conv-blank", "   ", "Tester").isEmpty());
    }
}
