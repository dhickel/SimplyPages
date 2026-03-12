(function() {
    const activeStreams = new Map();
    const MESSAGE_INPUT_SELECTOR = '#chat-composer input[name="message"]';

    function initChatRoots() {
        const roots = document.querySelectorAll('[data-sp-chat="true"]');
        roots.forEach(bindRoot);
    }

    function bindRoot(root) {
        const conversationId = root.getAttribute('data-sp-chat-conversation-id');
        const transport = root.getAttribute('data-sp-chat-transport');
        const historyEndpoint = root.getAttribute('data-sp-chat-history-endpoint');
        const historyTarget = root.getAttribute('data-sp-chat-history-target');
        const historySwap = root.getAttribute('data-sp-chat-history-swap') || 'outerHTML';
        const streamEndpoint = root.getAttribute('data-sp-chat-stream-endpoint');

        if (!conversationId || !transport || !historyEndpoint || !historyTarget) {
            return;
        }

        if (!root.hasAttribute('data-sp-chat-initial-scroll-done')) {
            scrollTranscriptToBottom(root);
            root.setAttribute('data-sp-chat-initial-scroll-done', 'true');
        }

        if (transport === 'SSE' && streamEndpoint) {
            bindSse(conversationId, streamEndpoint, historyEndpoint, historyTarget, historySwap);
        }
    }

    function bindSse(conversationId, streamEndpoint, historyEndpoint, historyTarget, historySwap) {
        if (activeStreams.has(conversationId)) {
            return;
        }

        const url = appendQuery(streamEndpoint, 'conversationId', conversationId);
        const source = new EventSource(url);
        activeStreams.set(conversationId, source);

        source.addEventListener('chat-updated', function() {
            refreshTranscript(historyEndpoint, conversationId, historyTarget, historySwap);
        });

        source.onerror = function() {
            if (source.readyState === EventSource.CLOSED) {
                activeStreams.delete(conversationId);
            }
        };
    }

    function refreshTranscript(historyEndpoint, conversationId, historyTarget, historySwap) {
        if (!window.htmx) {
            return;
        }
        const url = appendQuery(historyEndpoint, 'conversationId', conversationId);
        window.htmx.ajax('GET', url, {
            target: historyTarget,
            swap: historySwap
        });
    }

    function scrollTranscriptToBottom(root) {
        const transcriptRegion = root.querySelector('.chat-module-transcript-region');
        if (!transcriptRegion) {
            return;
        }
        transcriptRegion.scrollTop = transcriptRegion.scrollHeight;
    }

    function appendQuery(url, key, value) {
        const separator = url.indexOf('?') >= 0 ? '&' : '?';
        return url + separator + encodeURIComponent(key) + '=' + encodeURIComponent(value);
    }

    function clearComposerMessageInput() {
        const input = document.querySelector(MESSAGE_INPUT_SELECTOR);
        if (!input) {
            return;
        }
        input.value = '';
    }

    document.addEventListener('DOMContentLoaded', initChatRoots);

    document.body.addEventListener('htmx:afterRequest', function(event) {
        const source = event.detail && event.detail.elt;
        const xhr = event.detail && event.detail.xhr;
        if (!source || source.id !== 'chat-composer' || !xhr || xhr.status < 200 || xhr.status >= 300) {
            return;
        }
        clearComposerMessageInput();
    });
})();
