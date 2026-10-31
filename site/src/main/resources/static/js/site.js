(() => {
    function highlightCode(root) {
        const highlighter = window.hljs;
        if (!highlighter || typeof highlighter.highlightElement !== 'function' || typeof highlighter.getLanguage !== 'function') return;
        const selector = '.sp-site-main pre code';
        function highlight(code) {
            if (code.dataset.highlighted || code.children.length) return;
            const classes = code.className + ' ' + code.parentElement.className;
            if (/\bno-?highlight\b/i.test(classes)) return;
            const language = classes.match(/\blang(?:uage)?-([\w-]+)\b/i)?.[1];
            if (!language || /^(text|plain|plaintext|txt|none|mermaid)$/i.test(language) || !highlighter.getLanguage(language)) return;
            highlighter.highlightElement(code);
        }
        if (root.matches?.(selector)) highlight(root);
        root.querySelectorAll(selector).forEach(highlight);
    }
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
    function demoNavigation() {
        const disclosure = document.querySelector('.sp-site-demo-disclosure');
        if (!disclosure) return;
        const summary = disclosure.querySelector('summary');
        const mobile = matchMedia('(max-width:900px)');
        let mobileOpen = false;
        const observer = new MutationObserver(() => {
            mobileOpen = disclosure.open;
            layout();
        });
        observer.observe(disclosure, {attributes:true, attributeFilter:['open']});
        function layout() {
            // Native toggle events are delayed/coalesced. Capture pending native changes
            // before resizing, and exclude our synchronous presentation writes.
            if (observer.takeRecords().length) mobileOpen = disclosure.open;
            const open = !mobile.matches || mobileOpen;
            if (!open && disclosure.contains(document.activeElement)) summary.focus();
            if (disclosure.open !== open) {
                disclosure.open = open;
                observer.takeRecords();
            }
        }
        disclosure.addEventListener('keydown', event => {
            if (event.key !== 'Escape' || !mobile.matches || !disclosure.open) return;
            event.preventDefault();
            summary.focus();
            mobileOpen = false;
            disclosure.open = false;
        });
        disclosure.addEventListener('click', event => {
            const link = event.target.closest('a[href]');
            if (!link || !mobile.matches || link.pathname !== location.pathname || !link.hash) return;
            const target = document.getElementById(link.hash.slice(1));
            if (!target) return;
            mobileOpen = false;
            disclosure.open = false;
            // Native anchors still own navigation; return keyboard focus to their section.
            const borrowedFocus = !target.hasAttribute('tabindex');
            if (borrowedFocus) target.setAttribute('tabindex', '-1');
            target.focus({preventScroll:true});
            if (borrowedFocus) target.addEventListener('blur', () => {
                if (target.getAttribute('tabindex') === '-1') target.removeAttribute('tabindex');
            }, {once:true});
        });
        mobile.addEventListener('change', layout);
        layout();
        if (!mobile.matches) {
            const rail = disclosure.parentElement;
            const active = disclosure.querySelector('[aria-current]');
            if (active && active.getBoundingClientRect().bottom > rail.getBoundingClientRect().bottom) {
                rail.scrollTop += active.getBoundingClientRect().top - rail.getBoundingClientRect().top - 48;
            }
        }
    }
    document.addEventListener('DOMContentLoaded', () => { documentationState(); mobileDocs(); demoNavigation(); highlightCode(document); });
    document.addEventListener('htmx:load', event => highlightCode(event.detail.elt));
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
