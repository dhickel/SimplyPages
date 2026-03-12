package io.mindspice.simplypages.components.chat;

/**
 * Simple UI hook config for chat refresh/send wiring.
 *
 * <p>History and message ownership remain application concerns; this config only defines
 * client-facing hook metadata.</p>
 */
public record ChatUiConfig(
    String conversationId,
    ChatTransportMode transportMode,
    String historyEndpoint,
    String streamEndpoint,
    String historyTargetSelector,
    String historySwap,
    Integer pollingIntervalMs
) {
    public ChatUiConfig {
        conversationId = requireNonBlank(conversationId, "conversationId");
        transportMode = transportMode == null ? ChatTransportMode.SSE : transportMode;
        historyEndpoint = requireNonBlank(historyEndpoint, "historyEndpoint");
        historyTargetSelector = historyTargetSelector == null || historyTargetSelector.isBlank()
            ? "#chat-history"
            : historyTargetSelector;
        historySwap = historySwap == null || historySwap.isBlank() ? "outerHTML" : historySwap;
        if (pollingIntervalMs != null && pollingIntervalMs < 1) {
            throw new IllegalArgumentException("pollingIntervalMs must be >= 1 when provided");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value;
    }
}
