/**
 * SimplyPages Framework JavaScript
 * Provides interactive functionality for components and modules
 * Uses event delegation for better performance and HTMX compatibility
 */

// Global event delegation for all components
document.addEventListener('click', function(event) {
    // 1. Accordion
    const accordionHeader = event.target.closest('.accordion-header');
    if (accordionHeader) {
        handleAccordion(accordionHeader);
        return;
    }

    // 2. Tabs
    const tabButton = event.target.closest('.tab-button');
    if (tabButton) {
        handleTabs(tabButton);
        return;
    }

    // 3. Callout Dismiss
    const calloutClose = event.target.closest('.callout-close');
    if (calloutClose) {
        handleCallout(calloutClose);
        return;
    }
});

/** Ordinary disclosure navigation: native buttons/links, not an ARIA menu pattern. */
(function() {
    function disclosure(toggle) {
        const owner = toggle.closest('[data-sp-disclosure]');
        const panel = document.getElementById(toggle.getAttribute('aria-controls'));
        return owner && panel && panel.closest('[data-sp-disclosure]') === owner
            ? {owner, toggle, panel} : null;
    }
    function close(item, restoreFocus = false) {
        item.panel.hidden = true;
        item.toggle.setAttribute('aria-expanded', 'false');
        if (restoreFocus) item.toggle.focus({preventScroll: true});
    }
    document.addEventListener('click', event => {
        const toggle = event.target.closest('[data-sp-disclosure-toggle]');
        const current = toggle && disclosure(toggle);
        document.querySelectorAll('[data-sp-disclosure-toggle][aria-expanded="true"]').forEach(button => {
            const item = disclosure(button);
            if (item && !item.owner.contains(event.target)) close(item);
        });
        if (!current) return;
        const open = current.panel.hidden;
        current.panel.hidden = !open;
        current.toggle.setAttribute('aria-expanded', String(open));
    });
    // Register before the dialog handler so child Escape cancels the outer close action.
    document.addEventListener('keydown', event => {
        if (event.key !== 'Escape' || event.defaultPrevented) return;
        const owner = event.target.closest('[data-sp-disclosure]');
        if (!owner) return;
        const toggle = Array.from(owner.querySelectorAll('[data-sp-disclosure-toggle]'))
            .find(button => button.closest('[data-sp-disclosure]') === owner);
        const item = toggle && disclosure(toggle);
        if (item && !item.panel.hidden) {
            event.preventDefault();
            close(item, true);
        }
    });
})();

/** Shared ShellBuilder desktop collapse and off-canvas navigation; Page details stay independent. */
const synchronizeShellNavigation = (function() {
    const mobile = matchMedia('(max-width: 768px)');
    const sidebar = () => document.getElementById('main-sidebar');
    const toggles = () => Array.from(document.querySelectorAll('.mobile-sidebar-toggle[aria-controls="main-sidebar"]'));
    const modalOpen = () => !!document.querySelector('.modal-backdrop > .modal-container[role="dialog"]');
    const initialized = new WeakMap();
    const settlingSidebars = new WeakSet();
    let desktopPreference;
    let navigationFocus = null;
    function collapseOwner(nav) {
        const container = nav?.closest('.main-container');
        return nav?.classList.contains('collapsible') && container?.classList.contains('collapsible-sidebar')
            ? container : null;
    }
    function initializeCollapse(nav) {
        const container = collapseOwner(nav);
        const previousOwner = initialized.get(nav);
        if (!container || previousOwner === container) return;
        if (desktopPreference === undefined) {
            desktopPreference = false;
            try { desktopPreference = localStorage.getItem('sidebarCollapsed') === 'true'; } catch (_) { /* Best effort. */ }
        }
        const collapsed = previousOwner ? nav.classList.contains('collapsed') : desktopPreference;
        nav.classList.toggle('collapsed', collapsed);
        container.classList.toggle('sidebar-collapsed', collapsed);
        initialized.set(nav, container);
    }
    window.toggleSidebar = function() {
        const nav = sidebar();
        const container = collapseOwner(nav);
        if (!container || mobile.matches || modalOpen()) return;
        initializeCollapse(nav);
        desktopPreference = !nav.classList.contains('collapsed');
        nav.classList.toggle('collapsed', desktopPreference);
        container.classList.toggle('sidebar-collapsed', desktopPreference);
        try { localStorage.setItem('sidebarCollapsed', String(desktopPreference)); } catch (_) { /* Document-local state survives. */ }
    };
    document.addEventListener('focusin', event => {
        const nav = sidebar();
        navigationFocus = (nav && nav.contains(event.target)) || toggles().includes(event.target)
            ? event.target : null;
    });
    const firstControl = element => Array.from(element.querySelectorAll('a[href], button, input, select, textarea, [tabindex]'))
        .find(control => !control.matches(':disabled') && control.tabIndex >= 0
            && getComputedStyle(control).visibility === 'visible' && control.getClientRects().length);

    function synchronize() {
        const nav = sidebar();
        if (!nav) return;
        initializeCollapse(nav);
        if (!mobile.matches) nav.classList.remove('mobile-open');
        const open = mobile.matches && nav.classList.contains('mobile-open');
        const blocked = mobile.matches && !open;
        const dialog = modalOpen();
        // A responsive visibility change can blur a control before the media-query event runs.
        const focused = document.activeElement === document.body && navigationFocus && navigationFocus.isConnected
            ? navigationFocus : document.activeElement;
        // Avoid making a dialog hosted inside navigation inert along with its ancestors.
        nav.inert = blocked || (dialog && !nav.querySelector('.modal-backdrop > .modal-container[role="dialog"]'));
        if (blocked) nav.setAttribute('aria-hidden', 'true');
        else nav.removeAttribute('aria-hidden');
        toggles().forEach(toggle => {
            toggle.setAttribute('aria-expanded', String(open));
            toggle.inert = dialog;
        });
        if (!dialog && blocked && nav.contains(focused)) {
            const toggle = toggles()[0];
            if (toggle) toggle.focus({preventScroll: true});
        } else if (!dialog && !mobile.matches && toggles().includes(focused)) {
            const control = firstControl(nav);
            if (control) control.focus({preventScroll: true});
        }
    }
    function close(restore = false) {
        const nav = sidebar();
        if (!nav) return;
        nav.classList.remove('mobile-open');
        synchronize();
        if (restore && !modalOpen() && toggles()[0]) toggles()[0].focus({preventScroll: true});
    }
    window.toggleMobileSidebar = function() {
        const nav = sidebar();
        if (!nav || !mobile.matches || modalOpen()) return;
        if (nav.classList.contains('mobile-open')) close(nav.contains(document.activeElement));
        else {
            nav.classList.add('mobile-open');
            synchronize();
            const control = firstControl(nav);
            if (control) control.focus({preventScroll: true});
        }
    };
    document.addEventListener('click', event => {
        const nav = sidebar();
        if (!nav || !mobile.matches || !nav.classList.contains('mobile-open') || modalOpen()) return;
        if (nav.contains(event.target)) {
            if (event.target.closest('a[href]')) close(true);
        } else if (!event.target.closest('.mobile-sidebar-toggle')) close(nav.contains(document.activeElement));
    });
    document.addEventListener('keydown', event => {
        const nav = sidebar();
        if (event.key === 'Escape' && !event.defaultPrevented && !modalOpen()
            && mobile.matches && nav && nav.classList.contains('mobile-open')) {
            event.preventDefault();
            close(true);
        }
    });
    mobile.addEventListener('change', synchronize);
    document.addEventListener('DOMContentLoaded', synchronize);
    function insertedShell() {
        const nav = sidebar();
        // HTMX temporarily copies the outgoing node's classes and restores incoming attributes
        // at settle. Complete this new identity's initialization after that one restoration.
        if (nav && !initialized.has(nav)) settlingSidebars.add(nav);
        synchronize();
    }
    document.addEventListener('htmx:afterSwap', insertedShell);
    document.addEventListener('htmx:oobAfterSwap', insertedShell);
    document.addEventListener('htmx:afterSettle', () => {
        const nav = sidebar();
        if (nav && settlingSidebars.delete(nav)) {
            const container = collapseOwner(nav);
            if (container) {
                nav.classList.toggle('collapsed', desktopPreference);
                container.classList.toggle('sidebar-collapsed', desktopPreference);
            }
        }
        synchronize();
    });
    new MutationObserver(synchronize).observe(document.documentElement, {childList: true, subtree: true});
    synchronize();
    return synchronize;
})();

/** Modal lifecycle; accordion and alert focus reuse its internal-only boundaries. */
const revealAccordionHeader = (function() {
    const modals = [];
    // Alert-only provenance lets a containing element remain a restorable modal opener
    // after its borrowed tabindex is released when focus enters the dialog.
    const alertFocusOpeners = new WeakSet();
    const alertOpenerChanges = new WeakSet();
    const alertOpenerObservers = new WeakMap();
    let borrowedAlertFocus = null;
    const swapOpeners = new WeakMap();
    let lastFocused = document.activeElement;
    const topModal = () => modals[modals.length - 1];
    const owned = (element, modal) => element.closest('.modal-backdrop') === modal.backdrop;
    function usable(element) {
        if (!(element instanceof HTMLElement) || !element.isConnected
            || element.matches(':disabled') || element.closest('[inert], [hidden]')) return false;
        // Closed details expose only their first direct summary subtree, including when CSS leaves boxes.
        for (let ancestor = element.parentElement; ancestor; ancestor = ancestor.parentElement) {
            if (ancestor.matches('details:not([open])')) {
                const summary = Array.from(ancestor.children).find(child => child.matches('summary'));
                if (!summary || !summary.contains(element)) return false;
            }
        }
        return (!element.checkVisibility || element.checkVisibility({checkVisibilityCSS: true}))
            && getComputedStyle(element).visibility === 'visible' && element.getClientRects().length > 0;
    }

    // Page-only native state. Each target owns its bounded observer; the weak map
    // retains identity across reparenting without a strong registry of detached disclosures.
    const pageSidebars = new WeakMap();
    const pageMobile = matchMedia('(max-width: 768px)');
    function pageParts(details) {
        if (!(details instanceof HTMLDetailsElement)
            || !details.matches('.sticky-sidebar-mobile-collapse')
            || !details.parentElement?.matches('.sticky-sidebar-aside')
            || !details.closest('.page-content.with-sticky-sidebar')) return null;
        const summary = Array.from(details.children).find(child => child.matches('summary'));
        const content = Array.from(details.children).find(child => child.matches('.sticky-sidebar-content'));
        return summary?.matches('.sticky-sidebar-mobile-summary') && content
            ? {summary, content, aside: details.parentElement, page: details.closest('.page-content.with-sticky-sidebar')} : null;
    }
    function samePageParts(details, parts) {
        const current = pageParts(details);
        return current && current.summary === parts.summary && current.content === parts.content
            && current.aside === parts.aside && current.page === parts.page;
    }
    function readPageChanges(state, records) {
        // oldValue of the following record supplies the intermediate native boolean state.
        // Reading only the final property would lose close/open sequences in one microtask.
        records.forEach((record, index) => {
            const open = index + 1 < records.length
                ? records[index + 1].oldValue !== null : state.details.open;
            state.nativeChanged ||= (record.oldValue !== null) !== open;
            state.preference = open;
        });
    }
    function drainPageChanges(state) {
        readPageChanges(state, state.observer.takeRecords());
    }
    function writePageOpen(state, open) {
        drainPageChanges(state);
        if (state.details.open === open) return;
        state.details.open = open;
        // Isolate this synchronous, owned write instead of guessing which later toggle event
        // belongs to it. Native toggle delivery can coalesce across several responsive changes.
        const records = state.observer.takeRecords();
        readPageChanges(state, records.slice(1));
    }
    function pageFocusOwner(details) {
        // A callback can insert a cover before reconciliation updates an existing stack too.
        // Until every connected dialog is tracked, initialize native state without focus/reveal.
        const pending = Array.from(document.querySelectorAll('.modal-backdrop > .modal-container[role="dialog"]'))
            .some(dialog => !modals.some(modal => modal.dialog === dialog));
        if (pending) return false;
        const top = topModal();
        if (!top) return null;
        return top.backdrop.isConnected && owned(details, top) && top.dialog.contains(details) ? top : false;
    }
    function correctPageFocus(state, parts, original, mobile) {
        const details = state.details;
        const owner = pageFocusOwner(details);
        if (owner === false || !original || !details.contains(original)) return;
        const untouched = () => {
            const active = document.activeElement;
            return active === original || (active === document.body && !active.matches(':focus'));
        };
        // Mobile closure leaves only the native summary. Desktop removes summary presentation;
        // retained summary focus stays visible in CSS until this bounded transfer succeeds.
        let targets;
        if (mobile) {
            if (details.open) {
                if (document.activeElement !== original || !usable(original)) return;
                targets = [original]; // Reflow can clip retained focus; reveal inside its existing boundary.
            } else {
                if (!parts.content.contains(original)) return;
                targets = [parts.summary];
            }
        } else {
            if (!details.open) return;
            if (original !== parts.summary && document.activeElement === original && usable(original)) {
                targets = [original];
            } else {
                const controls = Array.from(parts.content.querySelectorAll(
                    'a[href], area[href], button, input, select, textarea, iframe, summary, [tabindex], [contenteditable="true"]'
                )).filter(target => target.tabIndex >= 0 && usable(target));
                targets = [...(original !== parts.summary && usable(original) ? [original] : []), ...controls, details];
            }
        }
        const expectedOpen = details.open;
        for (const target of targets) {
            if (!untouched() || !details.isConnected || pageFocusOwner(details) !== owner
                || pageMobile.matches !== mobile || details.open !== expectedOpen
                || !samePageParts(details, parts)) return;
            if (!usable(target) || (owner && !owned(target, owner))) continue;
            target.focus({preventScroll: true});
            // Application focus callbacks may mutate native state, move/remove nodes, open a
            // covering dialog, or choose a different target. Never reveal a stale transition.
            if (document.activeElement !== target) return;
            if (!details.isConnected || !target.isConnected || details.open !== expectedOpen
                || pageMobile.matches !== mobile || pageFocusOwner(details) !== owner
                || !samePageParts(details, parts) || !usable(target)) return;
            if (owner) focusInDialog(owner, target);
            else target.scrollIntoView({block: 'nearest', inline: 'nearest', behavior: 'instant'});
            return;
        }
    }
    function synchronizePageSidebar(state) {
        drainPageChanges(state);
        const details = state.details;
        const parts = pageParts(details);
        if (!details.isConnected || !parts) return;
        const mobile = pageMobile.matches;
        // Capture live focus before our presentation write; native changes are processed through
        // attribute records rather than delayed/coalesced toggle events. CSS retains focused
        // desktop summaries. Never replay focusout, which also describes deliberate caller blur.
        const active = document.activeElement;
        const original = details.contains(active) ? active : null;
        const desired = mobile ? state.preference : true;
        const transition = state.mobile !== mobile || details.open !== desired || state.nativeChanged;
        writePageOpen(state, desired);
        state.mobile = mobile;
        state.nativeChanged = false;
        if (transition) correctPageFocus(state, parts, original, mobile);
    }
    function synchronizePageSidebars() {
        document.querySelectorAll('details.sticky-sidebar-mobile-collapse').forEach(details => {
            if (!pageParts(details)) return;
            let state = pageSidebars.get(details);
            if (!state) {
                state = {details, preference: details.open, nativeChanged: false,
                    mobile: pageMobile.matches, observer: null};
                state.observer = new MutationObserver(records => {
                    readPageChanges(state, records);
                    synchronizePageSidebar(state);
                });
                state.observer.observe(details, {attributes: true, attributeFilter: ['open'], attributeOldValue: true});
                pageSidebars.set(details, state);
            }
            synchronizePageSidebar(state);
        });
    }
    pageMobile.addEventListener('change', synchronizePageSidebars);

    function watchAlertOpener(target) {
        if (!(target instanceof HTMLElement) || alertOpenerObservers.has(target)) return;
        const observer = new MutationObserver(records => {
            if (records.length) alertOpenerChanges.add(target);
        });
        observer.observe(target, {attributes: true, attributeFilter: ['tabindex']});
        alertOpenerObservers.set(target, observer);
    }

    function unwatchAlertOpener(target) {
        if (!(target instanceof HTMLElement)) return false;
        const observer = alertOpenerObservers.get(target);
        const pendingChange = !!observer?.takeRecords().length;
        observer?.disconnect();
        alertOpenerObservers.delete(target);
        const changed = pendingChange || alertOpenerChanges.has(target);
        alertOpenerChanges.delete(target);
        return changed;
    }

    function releaseAlertFocus(record = borrowedAlertFocus, callerChanged = false) {
        if (!record || record.done) return;
        record.done = true;
        // Drain synchronous caller writes before blur cleanup, including same-value writes.
        callerChanged ||= record.observer.takeRecords().length > 0;
        record.observer.disconnect();
        record.target.removeEventListener('blur', record.release);
        if (borrowedAlertFocus === record) borrowedAlertFocus = null;
        const isModalOpener = modals.some(modal => modal.opener === record.target);
        if (!callerChanged && record.owns && record.target.getAttribute('tabindex') === '-1') {
            record.target.removeAttribute('tabindex');
        }
        if (isModalOpener && !callerChanged) {
            // The library-owned value is gone, but keep a weak, target-only watcher so
            // caller writes while the modal is open are not mistaken for a reason to
            // borrow tabindex again during opener restoration.
            watchAlertOpener(record.target);
            alertFocusOpeners.add(record.target);
        } else {
            alertFocusOpeners.delete(record.target);
            unwatchAlertOpener(record.target);
        }
    }

    function alertFocusTaken(owner, original) {
        const active = document.activeElement;
        // Hiding can leave body's default activeElement without actually focusing it.
        return active !== original && (!owner || !owner.contains(active))
            && (active !== document.body || active.matches(':focus'));
    }

    function focusAlertContainer(target, owner, original) {
        // Summary/contenteditable and other native targets need no attribute. tabIndex
        // alone does not tell whether focus() will work, especially for malformed values.
        target.focus({preventScroll: true});
        if (document.activeElement === target && target.matches(':focus') && usable(target)) return true;
        if (alertFocusTaken(owner, original) || !usable(target) || target.hasAttribute('tabindex')) return false;

        releaseAlertFocus();
        const record = {target, done: false, owns: false, observer: null, release: null};
        record.release = () => releaseAlertFocus(record);
        record.observer = new MutationObserver(records => {
            if (records.length) releaseAlertFocus(record, true);
        });
        // Target-only observation lasts just as long as this borrowed focus. Even a
        // same-value application write transfers ownership; never infer it from value alone.
        record.observer.observe(target, {attributes: true, attributeFilter: ['tabindex'], attributeOldValue: true});
        target.setAttribute('tabindex', '-1');
        const initial = record.observer.takeRecords();
        record.owns = initial.length === 1 && initial[0].oldValue === null
            && target.getAttribute('tabindex') === '-1';
        target.addEventListener('blur', record.release);
        borrowedAlertFocus = record;
        if (record.owns) alertFocusOpeners.add(target);
        target.focus({preventScroll: true});
        const focused = document.activeElement === target && target.matches(':focus') && usable(target);
        if (!focused || !record.owns) releaseAlertFocus(record, !record.owns);
        return focused;
    }

    document.addEventListener('click', event => {
        if (event.defaultPrevented || !(event.target instanceof Element)) return;
        const button = event.target.closest('.alert-close');
        if (!(button instanceof HTMLButtonElement) || button.type !== 'button' || !usable(button)) return;
        const owner = button.closest('.alert');
        // An invalid intervening alert blocks ownership instead of falling through outward.
        if (!owner || !owner.classList.contains('alert-dismissible') || !usable(owner)) return;
        const top = topModal();
        if (top && (!top.backdrop.isConnected || !top.dialog.contains(button)
            || !top.dialog.contains(owner) || !owned(button, top) || !owned(owner, top))) return;

        const original = document.activeElement;
        const containedFocus = owner.contains(original);
        const containers = [];
        for (let target = owner.parentElement; target && target !== document.documentElement; target = target.parentElement) {
            containers.push(target);
        }
        // Capture the chain and hide before any focus callback can remove/reparent it
        // or activate this alert again. Dismissal retains the DOM and application semantics.
        owner.hidden = true;
        if (!containedFocus || alertFocusTaken(owner, original)) return;
        if (top) {
            if (top === topModal() && top.backdrop.isConnected) focusInDialog(top, top.dialog);
            return;
        }
        for (const target of containers) {
            if (alertFocusTaken(owner, original)) return;
            if (!usable(target)) continue;
            if (alertFocusTaken(owner, original)) return;
            if (focusAlertContainer(target, owner, original)) return;
        }
    });

    function tabStops(modal) {
        const candidates = Array.from(modal.dialog.querySelectorAll(
            'a[href], area[href], button, input, select, textarea, iframe, summary, [tabindex], [contenteditable="true"]'
        )).filter(element => owned(element, modal) && usable(element) && element.tabIndex >= 0
            && (!element.matches('summary') || element.hasAttribute('tabindex')
                || (element.parentElement.matches('details')
                    && Array.from(element.parentElement.children).find(child => child.matches('summary')) === element)));
        // Native tab order has one stop per named radio group, and positive tabindex first.
        return candidates.filter(element => {
            if (!element.matches('input[type="radio"]') || !element.name) return true;
            const group = candidates.filter(other => other.matches('input[type="radio"]')
                && other.name === element.name && other.form === element.form);
            return element === (group.find(other => other.checked) || group[0]);
        }).sort((a, b) => (a.tabIndex || Infinity) - (b.tabIndex || Infinity));
    }

    function nearestScroll(start, end, visibleStart, visibleEnd) {
        // Keep oversized controls spanning the viewport in place, like native nearest scrolling.
        if (start < visibleStart && end > visibleEnd) return 0;
        if (start < visibleStart) return Math.max(start - visibleStart, end - visibleEnd);
        if (end > visibleEnd) return Math.min(start - visibleStart, end - visibleEnd);
        return 0;
    }

    function focusInDialog(modal, target) {
        target.focus({preventScroll: true});
        if (document.activeElement !== target) return false;
        // Reveal inner clipping/scroll boundaries first, never scrolling the backdrop or page.
        for (let container = target.parentElement; modal.dialog.contains(container); container = container.parentElement) {
            const style = getComputedStyle(container);
            const rect = target.getBoundingClientRect();
            const box = container.getBoundingClientRect();
            const top = box.top + container.clientTop;
            const left = box.left + container.clientLeft;
            const scrollable = overflow => ['auto', 'scroll', 'hidden'].includes(overflow);
            const y = scrollable(style.overflowY)
                ? nearestScroll(rect.top, rect.bottom, top, top + container.clientHeight) : 0;
            const x = scrollable(style.overflowX)
                ? nearestScroll(rect.left, rect.right, left, left + container.clientWidth) : 0;
            if (x || y) container.scrollBy({top: y, left: x, behavior: 'instant'});
        }
        return true;
    }

    function focusInitial(modal) {
        const autofocus = Array.from(modal.dialog.querySelectorAll('[autofocus]'))
            .filter(element => owned(element, modal) && usable(element));
        const stops = tabStops(modal);
        const bodyStops = stops.filter(element => {
            const body = element.closest('.modal-body');
            return body && owned(body, modal);
        });
        const targets = [...autofocus, ...bodyStops, ...stops, modal.dialog];
        for (const target of targets) {
            if (focusInDialog(modal, target)) return;
        }
    }

    function reconcile(event) {
        // Native desktop disclosure state must be established before modal first-focus enumeration.
        synchronizePageSidebars();
        if (borrowedAlertFocus && (!borrowedAlertFocus.target.isConnected
            || document.activeElement !== borrowedAlertFocus.target)) releaseAlertFocus();
        const swapOpener = event && event.detail && swapOpeners.get(event.detail.target);
        const previousTop = topModal();
        const removed = modals.filter(modal => !modal.backdrop.isConnected);
        const removedOpeners = new Set(removed.map(modal => modal.opener));
        function connectedOpener(opener) {
            const seen = new Set();
            while (opener && !opener.isConnected && !seen.has(opener)) {
                seen.add(opener);
                const owner = removed.find(modal => modal.backdrop.contains(opener));
                opener = owner ? owner.opener : null;
            }
            return opener;
        }
        for (let i = modals.length - 1; i >= 0; i--) {
            if (!modals[i].backdrop.isConnected) modals.splice(i, 1);
        }
        modals.forEach(modal => { modal.opener = connectedOpener(modal.opener); });

        let added = false;
        document.querySelectorAll('.modal-backdrop').forEach(backdrop => {
            if (modals.some(modal => modal.backdrop === backdrop)) return;
            const dialog = Array.from(backdrop.children)
                .find(element => element.matches('.modal-container[role="dialog"]'));
            if (!dialog) return;
            let host = backdrop;
            while (host && !swapOpeners.has(host)) host = host.parentElement;
            // outerHTML swaps detach the old target, so afterSwap also carries its saved opener.
            const opener = connectedOpener(host ? swapOpeners.get(host) : swapOpener || lastFocused);
            modals.push({backdrop, dialog, opener});
            added = true;
        });

        const top = topModal();
        modals.forEach((modal, index) => {
            modal.dialog.setAttribute('aria-modal', modal === top ? 'true' : 'false');
            modal.backdrop.style.setProperty('--sp-modal-stack', index);
        });
        // Clear navigation's modal-owned inert state before testing/restoring an opener.
        synchronizeShellNavigation();
        if (added && top) {
            focusInitial(top);
        } else if (previousTop && removed.includes(previousTop)) {
            const opener = connectedOpener(previousTop.opener);
            if (usable(opener) && (!top || owned(opener, top))) {
                if (top) {
                    focusInDialog(top, opener);
                } else if (alertFocusOpeners.has(opener)) {
                    const callerChanged = alertOpenerChanges.has(opener);
                    // Stop observing before a fresh borrowed lifetime can write -1.
                    const pendingChange = unwatchAlertOpener(opener);
                    alertFocusOpeners.delete(opener);
                    if (callerChanged || pendingChange) opener.focus({preventScroll: true});
                    else focusAlertContainer(opener, null, document.activeElement);
                } else {
                    opener.focus({preventScroll: true});
                }
            } else if (top) focusInitial(top);
        } else if (top && !owned(document.activeElement, top)) {
            focusInitial(top);
        }
        for (const opener of removedOpeners) {
            if ((!borrowedAlertFocus || borrowedAlertFocus.target !== opener)
                && !modals.some(current => current.opener === opener)) {
                alertFocusOpeners.delete(opener);
                unwatchAlertOpener(opener);
            }
        }
    }

    document.addEventListener('focusin', event => {
        if (borrowedAlertFocus && borrowedAlertFocus.target !== event.target) releaseAlertFocus();
        const top = topModal();
        if (top && top.backdrop.isConnected && !owned(event.target, top)) {
            focusInitial(top);
            return;
        }
        lastFocused = event.target;
    });

    document.addEventListener('keydown', event => {
        const top = topModal();
        if (!top || !top.backdrop.isConnected) return;
        if (event.key === 'Escape') {
            // Let a focused control or application handler cancel Escape before the modal acts.
            if (event.defaultPrevented) return;
            if (top.backdrop.dataset.closeOnEscape === 'true') {
                event.preventDefault();
                top.backdrop.remove();
                reconcile();
            }
        } else if (event.key === 'Tab') {
            // A child that deliberately handles Tab keeps its in-dialog focus movement.
            if (event.defaultPrevented) return;
            const stops = tabStops(top);
            const index = stops.indexOf(document.activeElement);
            const next = event.shiftKey
                ? (index <= 0 ? stops.length - 1 : index - 1)
                : (index + 1) % stops.length;
            event.preventDefault();
            focusInDialog(top, stops[next] || top.dialog);
        }
    });

    document.addEventListener('click', event => {
        const top = topModal();
        if (!top || !owned(event.target, top)) return;
        const close = event.target.closest('.modal-close');
        if ((close && owned(close, top))
            || (event.target === top.backdrop && top.backdrop.dataset.closeOnBackdrop === 'true')) {
            top.backdrop.remove();
            reconcile();
        }
    });

    document.addEventListener('htmx:beforeSwap', event => {
        const target = event.detail.target;
        const source = event.detail.requestConfig && event.detail.requestConfig.elt;
        if (!(target instanceof Element) || !(source instanceof Element)) return;
        // Replacing an edit dialog must retain its original launcher, not a disappearing child button.
        const replacing = modals.find(modal => modal.backdrop.contains(source)
            && target.contains(modal.backdrop));
        swapOpeners.set(target, replacing ? replacing.opener : source);
    });
    document.addEventListener('htmx:afterSwap', reconcile);
    document.addEventListener('htmx:afterSettle', event => {
        reconcile();
        if (event.detail.target instanceof Element) swapOpeners.delete(event.detail.target);
    });
    new MutationObserver(reconcile).observe(document.documentElement, {childList: true, subtree: true});
    reconcile();
    return header => {
        const modal = modals.find(candidate => owned(header, candidate));
        if (modal) focusInDialog(modal, header);
        else header.scrollIntoView({block: 'nearest', inline: 'nearest', behavior: 'instant'});
    };
})();

/**
 * Synchronize one accordion item's visual, ARIA and interaction state.
 * Focus must leave a closing panel before inert/hidden are applied so a nested
 * control is not stranded in a subtree that has just become unavailable.
 */
function setAccordionState(header, content, expanded) {
    const restoreFocus = !expanded && content.contains(document.activeElement);
    if (restoreFocus) header.focus({preventScroll: true});

    header.classList.toggle('active', expanded);
    header.setAttribute('aria-expanded', String(expanded));
    content.classList.toggle('expanded', expanded);
    content.setAttribute('aria-hidden', String(!expanded));
    content.hidden = !expanded;
    content.inert = !expanded;
    return restoreFocus ? header : null;
}

/** Find an item's first header or panel without assuming direct child wrappers. */
function ownedAccordionPart(item, selector, accordion) {
    return Array.from(item.querySelectorAll(selector)).find(candidate =>
        candidate.closest('.accordion') === accordion
        && candidate.closest('.accordion-item') === item);
}

/**
 * Handle Accordion Logic
 * - Toggles active/expanded state
 * - Respects data-single-expand="true" on the nearest owner
 * - Leaves nested accordion state untouched
 */
function handleAccordion(header) {
    const accordion = header.closest('.accordion');
    const panelId = header.getAttribute('aria-controls');
    const item = header.closest('.accordion-item');

    if (!accordion || !panelId || !item || item.closest('.accordion') !== accordion) return;

    const content = document.getElementById(panelId);
    if (!content
        || content.closest('.accordion') !== accordion
        || content.closest('.accordion-item') !== item
        || ownedAccordionPart(item, '.accordion-header', accordion) !== header
        || ownedAccordionPart(item, '.accordion-content', accordion) !== content) {
        return;
    }

    const isExpanded = content.classList.contains('expanded');
    const ownedItems = Array.from(accordion.querySelectorAll('.accordion-item'))
        .filter(candidate => candidate.closest('.accordion') === accordion);
    let restoredHeader = null;

    if (accordion.getAttribute('data-single-expand') === 'true' && !isExpanded) {
        // Close only items owned by this accordion. Nested panels keep their own
        // classes and are restored when an outer panel is reopened.
        ownedItems.forEach(candidateItem => {
            const candidateHeader = ownedAccordionPart(candidateItem, '.accordion-header', accordion);
            const candidateContent = ownedAccordionPart(candidateItem, '.accordion-content', accordion);
            if (candidateHeader && candidateContent && candidateContent !== content) {
                restoredHeader = setAccordionState(candidateHeader, candidateContent, false) || restoredHeader;
            }
        });
    }

    restoredHeader = setAccordionState(header, content, !isExpanded) || restoredHeader;
    // Opening another panel can move the restored header; reveal after the whole transition.
    if (restoredHeader && document.activeElement === restoredHeader) revealAccordionHeader(restoredHeader);
}

/**
 * Handle Tabs Logic
 * - Switches active tab and panel
 * - Accessibility attributes
 */
function handleTabs(button) {
    const panelId = button.getAttribute('aria-controls');
    const tabsContainer = button.closest('.tabs-container');

    if (!tabsContainer || !panelId) return;

    const targetPanel = document.getElementById(panelId);
    if (!targetPanel
        || !targetPanel.classList.contains('tab-panel')
        || targetPanel.closest('.tabs-container') !== tabsContainer) {
        return;
    }

    // Deactivate all tabs and panels owned by this container, excluding nested tabs.
    const ownedButtons = Array.from(tabsContainer.querySelectorAll('.tab-button'))
        .filter(candidate => candidate.closest('.tabs-container') === tabsContainer);
    const ownedPanels = Array.from(tabsContainer.querySelectorAll('.tab-panel'))
        .filter(candidate => candidate.closest('.tabs-container') === tabsContainer);

    ownedButtons.forEach(candidate => {
        candidate.classList.remove('active');
        candidate.setAttribute('aria-selected', 'false');
        candidate.tabIndex = -1;
    });

    ownedPanels.forEach(candidate => {
        candidate.classList.remove('active');
        candidate.hidden = true;
    });

    // Activate clicked tab and corresponding panel
    button.classList.add('active');
    button.setAttribute('aria-selected', 'true');
    button.tabIndex = 0;
    targetPanel.classList.add('active');
    targetPanel.hidden = false;
}

/**
 * Handle Callout Dismiss Logic
 * - Hides the callout
 */
function handleCallout(button) {
    const callout = button.closest('.callout');
    if (callout) {
        callout.style.display = 'none';
    }
}

// Tab semantics follow the actual responsive flex axis without resetting selection or focus.
(function() {
    function orientation(list) {
        const direction = getComputedStyle(list).flexDirection;
        const axis = direction.startsWith('column') ? 'vertical' : 'horizontal';
        if (list.getAttribute('aria-orientation') !== axis) list.setAttribute('aria-orientation', axis);
        return axis;
    }
    function synchronize() {
        document.querySelectorAll('.tabs-container .tab-nav').forEach(orientation);
    }
    document.addEventListener('DOMContentLoaded', synchronize);
    window.addEventListener('load', synchronize);
    window.addEventListener('resize', synchronize);
    new MutationObserver(synchronize).observe(document.documentElement, {childList: true, subtree: true});
    synchronize();

    // Automatic activation and one native tab stop per owned tablist. Perpendicular and
    // modified keys keep their native behavior (including scrolling).
    document.addEventListener('keydown', event => {
        if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey
            || !['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Home', 'End'].includes(event.key)) return;
        const button = event.target.closest('.tab-button');
        const owner = button && button.closest('.tabs-container');
        const list = button && button.closest('.tab-nav');
        if (!owner || !list || list.closest('.tabs-container') !== owner) return;
        const vertical = orientation(list) === 'vertical';
        const forward = vertical ? 'ArrowDown' : 'ArrowRight';
        const backward = vertical ? 'ArrowUp' : 'ArrowLeft';
        if (![forward, backward, 'Home', 'End'].includes(event.key)) return;
        const buttons = Array.from(list.querySelectorAll('.tab-button'))
            .filter(candidate => candidate.closest('.tabs-container') === owner
                && candidate.closest('.tab-nav') === list && !candidate.disabled
                && (() => {
                    const panel = document.getElementById(candidate.getAttribute('aria-controls'));
                    return panel && panel.classList.contains('tab-panel') && panel.closest('.tabs-container') === owner;
                })());
        const index = buttons.indexOf(button);
        if (index < 0) return;
        const next = event.key === 'Home' ? 0 : event.key === 'End' ? buttons.length - 1
            : (index + (event.key === forward ? 1 : -1) + buttons.length) % buttons.length;
        event.preventDefault();
        handleTabs(buttons[next]);
        buttons[next].focus();
    });
})();

function parseScrollTopDirective(sourceElement) {
    if (!(sourceElement instanceof Element)) {
        return null;
    }

    const directive = sourceElement.getAttribute('data-sp-scroll-top');
    if (directive == null) {
        return null;
    }

    const normalized = directive.trim().toLowerCase();
    if (normalized === '' || normalized === 'true') {
        return 'target';
    }
    return normalized;
}

function resolveSwapTarget(event, sourceElement) {
    const detail = event.detail || {};
    if (detail.elt instanceof Element && document.contains(detail.elt)) {
        return detail.elt;
    }
    if (detail.target instanceof Element && document.contains(detail.target)) {
        return detail.target;
    }

    const requestConfig = detail.requestConfig;
    if (requestConfig) {
        if (requestConfig.target instanceof Element && document.contains(requestConfig.target)) {
            return requestConfig.target;
        }
        if (typeof requestConfig.target === 'string' && requestConfig.target.trim() !== '') {
            const configuredTarget = document.querySelector(requestConfig.target.trim());
            if (configuredTarget) {
                return configuredTarget;
            }
        }
    }

    if (!(sourceElement instanceof Element)) {
        return null;
    }

    const hxTarget = sourceElement.getAttribute('hx-target');
    if (!hxTarget) {
        return null;
    }

    const normalizedTarget = hxTarget.trim();
    const unsupportedResolver = normalizedTarget.startsWith('closest ')
        || normalizedTarget.startsWith('find ')
        || normalizedTarget.startsWith('next ')
        || normalizedTarget.startsWith('previous ');
    if (unsupportedResolver) {
        return null;
    }

    return document.querySelector(normalizedTarget);
}

function scrollTargetToTop(target) {
    if (target === window) {
        window.scrollTo({top: 0, left: 0, behavior: 'auto'});
        return true;
    }
    if (!(target instanceof Element)) {
        return false;
    }

    target.scrollIntoView({block: 'start', inline: 'nearest', behavior: 'auto'});
    return true;
}

function applyTaggedScrollReset(event, sourceElement) {
    const directive = parseScrollTopDirective(sourceElement);
    if (!directive) {
        return false;
    }

    if (directive === 'window') {
        return scrollTargetToTop(window);
    }
    if (directive === 'target') {
        const target = resolveSwapTarget(event, sourceElement);
        return scrollTargetToTop(target);
    }

    const selected = document.querySelector(directive);
    return scrollTargetToTop(selected);
}

function shouldScrollWindowForPushUrl(requestConfig, sourceElement) {
    const pushUrlAttr = sourceElement ? sourceElement.getAttribute('hx-push-url') : null;
    const normalizedPushUrlAttr = pushUrlAttr == null ? null : pushUrlAttr.trim().toLowerCase();
    const pushesUrlViaAttr = normalizedPushUrlAttr === ''
        || normalizedPushUrlAttr === 'true'
        || (normalizedPushUrlAttr != null && normalizedPushUrlAttr !== 'false');

    const pushUrlRequest = requestConfig.pushURL;
    const pushesUrlViaRequest = pushUrlRequest === true
        || (typeof pushUrlRequest === 'string'
            && pushUrlRequest.trim() !== ''
            && pushUrlRequest.trim().toLowerCase() !== 'false');

    return pushesUrlViaAttr || pushesUrlViaRequest;
}

// Tagged HTMX requests can reset the swapped target to top.
// Fallback: history-pushing HTMX requests reset the window scroll position.
document.body.addEventListener('htmx:afterSettle', function(event) {
    const requestConfig = event.detail && event.detail.requestConfig;
    if (!requestConfig) {
        return;
    }

    const sourceElement = requestConfig.elt instanceof Element ? requestConfig.elt : null;
    if (applyTaggedScrollReset(event, sourceElement)) {
        return;
    }

    if (!shouldScrollWindowForPushUrl(requestConfig, sourceElement)) {
        return;
    }

    window.scrollTo({top: 0, left: 0, behavior: 'auto'});
});
