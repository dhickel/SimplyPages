(() => {
    function documentationState() {
        const article = document.getElementById('docs-content');
        if (!article) return;
        if (article.dataset.docTitle) document.title = article.dataset.docTitle;
        const current = article.dataset.docPath || location.pathname;
        document.querySelectorAll('.docs-nav a').forEach(link => {
            if (new URL(link.href).pathname === current) link.setAttribute('aria-current', 'page');
            else link.removeAttribute('aria-current');
        });
    }
    function mobileDocs() {
        const disclosure = document.querySelector('.sp-site-docs-sidebar details');
        if (disclosure) disclosure.open = !matchMedia('(max-width:640px)').matches;
    }
    document.addEventListener('DOMContentLoaded', () => { documentationState(); mobileDocs(); });
    document.addEventListener('htmx:afterSettle', documentationState);
    document.addEventListener('htmx:pushedIntoHistory', documentationState);
    document.addEventListener('submit', async event => {
        const form = event.target.closest('[data-sp-reset]');
        if (!form) return;
        event.preventDefault();
        const status = form.parentElement.querySelector('[data-sp-reset-status]');
        const button = form.querySelector('button');
        button.disabled = true;
        status.textContent = 'Resetting your editing and forum sandbox…';
        try {
            const response = await fetch(form.action, {method:'POST', body:new FormData(form), credentials:'same-origin'});
            if (response.status === 204) { location.reload(); return; }
            status.textContent = response.status === 409 ? 'A sandbox action is still running. Finish it, then explicitly try reset again.'
                : response.status === 503 ? 'Sandbox capacity is full. Keep your draft and explicitly try again later.'
                : response.status === 403 ? 'Security verification failed. Reload the page before trying again; no action was replayed.'
                : 'Reset failed (' + response.status + '). Your action was not retried. Reload or explicitly try again.';
        } catch (_) {
            status.textContent = 'The response could not be received. Reset was not retried. Reload to check the current state.';
        } finally { button.disabled = false; }
    });
    document.addEventListener('htmx:responseError', event => {
        // Do not alter HTMX response handling, modal errors, chat generations, or drafts.
        const xhr = event.detail.xhr;
        if (!xhr || ![403,410,503].includes(xhr.status)) return;
        let notice = document.getElementById('sp-site-request-error');
        if (!notice) {
            notice = document.createElement('p'); notice.id = 'sp-site-request-error';
            notice.className = 'sp-site-notice'; notice.setAttribute('role','alert');
            const host = document.querySelector('.sp-site-content');
            if (!host) return;
            host.prepend(notice);
        }
        notice.textContent = xhr.status === 410 ? 'This temporary sandbox expired. Keep any draft, then explicitly reset editing/forum or Start New in chat. Nothing was retried.'
            : xhr.status === 503 ? 'Capacity is full. Your action was not retried. Keep your draft and explicitly try again later.'
            : 'This action was denied by permission or security checks. Your draft is retained; nothing was replayed. Check the simulated policy or reload before retrying.';
        const recovery = document.createElement('a');
        recovery.href = '/demos/recovery'; recovery.textContent = ' Open explicit sandbox recovery';
        notice.append(recovery);
    });
})();
