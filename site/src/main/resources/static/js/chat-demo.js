(function() {
    function initChatRoots() {
        document.querySelectorAll('[data-sp-chat="true"]').forEach(bindRoot);
    }

    function bindRoot(root) {
        if (root.hasAttribute('data-chat-bound')) return;
        const conversationId = root.getAttribute('data-sp-chat-conversation-id');
        const historyEndpoint = root.getAttribute('data-sp-chat-history-endpoint');
        const historyTarget = root.getAttribute('data-sp-chat-history-target');
        const historySwap = root.getAttribute('data-sp-chat-history-swap') || 'outerHTML';
        const streamEndpoint = root.getAttribute('data-sp-chat-stream-endpoint');
        if (!conversationId || !historyEndpoint || !historyTarget || !streamEndpoint) return;
        root.setAttribute('data-chat-bound', 'true');
        const main = root.closest('#chat-main');
        const composer = root.querySelector('#chat-composer');
        const input = composer.querySelector('input[name="message"]');
        const status = main.querySelector('#chat-connection-message');
        const resume = main.querySelector('#chat-resume');
        const startNew = main.querySelector('#chat-new');
        const send = composer.querySelector('button[type="submit"]');
        const messages = [];
        let source = null;
        let refreshing = false;
        let inFlight = false;
        let pendingSettles = 0;
        let transitioning = false;
        let expired = main.getAttribute('data-chat-expired') === 'true';

        function closeStream() {
            if (source) source.close();
            source = null;
        }

        function hideResume(hidden) {
            resume.hidden = hidden;
            // Framework button display rules override the UA [hidden] rule.
            // Scope visibility to this private recovery control, not framework widgets.
            resume.style.display = hidden ? 'none' : '';
        }

        function connectionEnded(isExpired) {
            closeStream(); // Never leave native EventSource's implicit retry running.
            expired = expired || isExpired;
            status.classList.replace('alert-info', 'alert-warning');
            status.textContent = expired
                ? 'This private chat expired. Start New explicitly; your draft is retained.'
                : 'Connection ended. Resume explicitly for this same chat, or Start New. Your draft is retained.';
            hideResume(expired || transitioning);
        }

        function ajaxTracked(method, url, options) {
            let delivery;
            const settlement = new Promise(function(resolve, reject) {
                let xhr = null;
                let settled = false;
                let delivered = false;
                const swapped = new Set();
                function afterSwap(event) {
                    if (event.detail.xhr !== xhr) return;
                    // A subsequent outerHTML response may detach this node before its
                    // settle event bubbles. Keep the listener on its actual swap owner.
                    swapped.add(event.target);
                    event.target.addEventListener('htmx:afterSettle', afterSettle);
                }
                function capture(event) {
                    if (event.detail.elt === options.source && !xhr) {
                        xhr = event.detail.xhr;
                        document.removeEventListener('htmx:beforeRequest', capture);
                    }
                }
                function cleanup() {
                    document.removeEventListener('htmx:beforeRequest', capture);
                    document.removeEventListener('htmx:afterSettle', afterSettle);
                    document.removeEventListener('htmx:afterSwap', afterSwap);
                    swapped.forEach(node => node.removeEventListener('htmx:afterSettle', afterSettle));
                }
                function finish() {
                    if (!delivered || (xhr && xhr.status >= 200 && xhr.status < 300 && !settled)) return;
                    cleanup();
                    resolve();
                }
                function afterSettle(event) {
                    if (event.detail.xhr === xhr) { settled = true; finish(); }
                }
                document.addEventListener('htmx:beforeRequest', capture);
                document.addEventListener('htmx:afterSettle', afterSettle);
                document.addEventListener('htmx:afterSwap', afterSwap);
                delivery = window.htmx.ajax(method, url, options);
                delivery.then(function() {
                    delivered = true;
                    finish();
                }, function(error) { cleanup(); reject(error); });
            });
            return {delivery, settlement};
        }

        function sendNext() {
            if (inFlight || !window.htmx || !root.isConnected) return;
            const message = messages.shift();
            if (!message && !refreshing) {
                if (transitioning && pendingSettles === 0) replaceGeneration();
                return;
            }
            if (!message) refreshing = false;
            inFlight = true;
            // The submission snapshot owns its original exact ID. Resolve the target
            // only after the preceding outerHTML swap, as in the original coordinator.
            const request = ajaxTracked(message ? 'POST' : 'GET', message
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
            // Keep the original delivery/swap queue timing. Only generation replacement
            // additionally waits for every successful response's actual HTMX settlement.
            pendingSettles++;
            function settled() { pendingSettles--; sendNext(); }
            request.settlement.then(settled, settled);
            // Normal HTMX failures remain observable. No resending or dropping queued work.
            request.delivery.then(complete, complete);
        }

        function refresh() {
            if (transitioning || !root.isConnected) return;
            refreshing = true; // Signals coalesce into the latest snapshot, including in flight.
            sendNext();
        }

        function openStream() {
            if (source || expired || transitioning || !root.isConnected) return;
            hideResume(true);
            status.classList.replace('alert-warning', 'alert-info');
            status.textContent = 'Connecting to a finite private stream.';
            const current = new EventSource(appendQuery(streamEndpoint, 'conversationId', conversationId));
            source = current;
            current.onopen = function() {
                if (source === current) status.textContent = 'Connected. This stream ends after five minutes; Resume is explicit.';
            };
            current.addEventListener('chat-updated', function() { if (source === current) refresh(); });
            current.addEventListener('chat-ended', function(event) {
                if (source === current) connectionEnded(event.data === 'expired');
            });
            current.onerror = function() {
                if (source !== current) return;
                connectionEnded(false);
                // EventSource hides the HTTP status. Lookup-only history can distinguish
                // an owned expired chat, without allocating or retrying the stream.
                refresh();
            };
        }

        function replaceGeneration() {
            // Every original POST/history response has settled before requesting a reset.
            // The old input object retains the latest draft even after its DOM is replaced.
            inFlight = true;
            ajaxTracked('POST', '/chat/new', {
                source: startNew, target: '#chat-main', swap: 'outerHTML'
            }).settlement.then(function() {
                if (!main.isConnected) {
                    window.removeEventListener('pagehide', closeStream);
                    initChatRoots();
                    const nextInput = document.querySelector('#chat-composer input[name="message"]');
                    if (nextInput) nextInput.value = input.value;
                } else resetFailed();
            }, resetFailed);
        }

        function resetFailed() {
            inFlight = false;
            transitioning = false;
            send.disabled = false;
            startNew.disabled = false;
            connectionEnded(expired);
        }

        composer.addEventListener('submit', function(event) {
            if (!window.htmx) return;
            event.preventDefault();
            event.stopImmediatePropagation();
            if (transitioning) return; // Send is visibly disabled while original work drains.
            messages.push(Object.fromEntries(new FormData(composer)));
            sendNext();
        }, true);
        resume.addEventListener('click', openStream);
        startNew.addEventListener('click', function() {
            if (transitioning || !window.htmx) return;
            transitioning = true;
            closeStream();
            send.disabled = true;
            startNew.disabled = true;
            hideResume(true);
            status.textContent = 'Finishing original submissions and history before starting a new chat. Your draft is retained.';
            sendNext();
        });
        root.addEventListener('htmx:afterRequest', function(event) {
            const detail = event.detail;
            if (!detail || !detail.xhr) return;
            if (detail.xhr.status === 410) connectionEnded(true);
            if (detail.elt === composer && detail.xhr.status >= 200 && detail.xhr.status < 300
                && detail.requestConfig && detail.requestConfig.verb === 'post'
                && input.value === detail.requestConfig.parameters.message) input.value = '';
        });
        window.addEventListener('pagehide', closeStream, {once: true});
        const transcriptRegion = root.querySelector('.chat-module-transcript-region');
        if (transcriptRegion) transcriptRegion.scrollTop = transcriptRegion.scrollHeight;
        if (expired) connectionEnded(true);
        else openStream();
    }

    function appendQuery(url, key, value) {
        return url + (url.indexOf('?') >= 0 ? '&' : '?') + encodeURIComponent(key) + '=' + encodeURIComponent(value);
    }
    document.addEventListener('DOMContentLoaded', initChatRoots);
})();
