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

        if (transport === 'SSE' && streamEndpoint && !activeStreams.has(conversationId)) {
            const refresh = coordinateTranscript(root, conversationId, historyEndpoint, historyTarget, historySwap);
            bindSse(conversationId, streamEndpoint, refresh);
        }
    }

    function coordinateTranscript(root, conversationId, historyEndpoint, historyTarget, historySwap) {
        const composer = root.querySelector('#chat-composer');
        const messages = [];
        let refreshing = false;
        let inFlight = false;

        function sendNext() {
            if (inFlight || !window.htmx || !root.isConnected) {
                return;
            }
            const message = messages.shift();
            if (!message && !refreshing) {
                return;
            }
            if (!message) {
                refreshing = false;
            }
            inFlight = true;
            // Resolve the outerHTML target only after the preceding request has swapped it.
            const request = window.htmx.ajax(message ? 'POST' : 'GET', message
                ? composer.getAttribute('hx-post')
                : appendQuery(historyEndpoint, 'conversationId', conversationId), {
                source: message ? composer : root,
                target: historyTarget,
                swap: historySwap,
                values: message || {}
            });
            function complete() {
                inFlight = false;
                sendNext();
            }
            // HTMX reports transport failures through its normal error events.
            request.then(complete, complete);
        }

        composer.addEventListener('submit', function(event) {
            if (!window.htmx) {
                return;
            }
            event.preventDefault();
            event.stopImmediatePropagation();
            // HTMX's own request queue reads form values when dequeued, not at submit.
            messages.push(Object.fromEntries(new FormData(composer)));
            sendNext();
        }, true);

        return function() {
            // Each signal invalidates the snapshot; a refresh during an in-flight request
            // stays pending. Multiple signals may share one subsequent latest-history GET.
            refreshing = true;
            sendNext();
        };
    }

    function bindSse(conversationId, streamEndpoint, refresh) {
        if (activeStreams.has(conversationId)) {
            return;
        }

        const url = appendQuery(streamEndpoint, 'conversationId', conversationId);
        const source = new EventSource(url);
        activeStreams.set(conversationId, source);

        source.addEventListener('chat-updated', refresh);

        source.onerror = function() {
            if (source.readyState === EventSource.CLOSED) {
                activeStreams.delete(conversationId);
            }
        };
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

    function clearComposerMessageInput(message) {
        const input = document.querySelector(MESSAGE_INPUT_SELECTOR);
        if (input && input.value === message) {
            input.value = '';
        }
    }

    document.addEventListener('DOMContentLoaded', initChatRoots);

    document.body.addEventListener('htmx:afterRequest', function(event) {
        const source = event.detail && event.detail.elt;
        const xhr = event.detail && event.detail.xhr;
        const request = event.detail && event.detail.requestConfig;
        if (!source || source.id !== 'chat-composer' || !xhr || xhr.status < 200 || xhr.status >= 300
            || !request || request.verb !== 'post') {
            return;
        }
        clearComposerMessageInput(request.parameters.message);
    });
})();
