// Private demo only: opt in to server-marked child-modal 4xx responses under HTMX 2.0.11.
(() => {
    'use strict';
    document.addEventListener('htmx:beforeSwap', event => {
        const detail = event.detail;
        const hosts = document.querySelectorAll('#edit-modal-container');
        const host = hosts.length === 1 ? hosts[0] : null;
        const source = detail.requestConfig && detail.requestConfig.elt;
        const path = detail.requestConfig && detail.requestConfig.path;
        if (!host || detail.target !== host || !(source instanceof Element) || !host.contains(source)
            || typeof path !== 'string'
            || !/^\/editing-demo\/(edit-child|save-child|delete-child)\/[^/?#]+\/[^/?#]+(?:\?|$)/.test(path)
            || detail.xhr.status < 400 || detail.xhr.status >= 500
            || detail.xhr.getResponseHeader('X-Editing-Demo-Modal') !== 'true') return;
        // The server's HX-Reswap: innerHTML overrides the successful form's swap:none.
        // Keep the real HTTP status; only these expected validation errors are handled here.
        detail.shouldSwap = true;
        detail.isError = false;
    });
})();
