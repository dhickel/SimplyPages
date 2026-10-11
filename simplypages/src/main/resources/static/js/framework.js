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

/** Modal-only lifecycle: one stack and delegated handlers for direct and HTMX insertion/removal. */
(function() {
    const modals = [];
    const swapOpeners = new WeakMap();
    let lastFocused = document.activeElement;
    const topModal = () => modals[modals.length - 1];
    const owned = (element, modal) => element.closest('.modal-backdrop') === modal.backdrop;
    const usable = element => element instanceof HTMLElement && element.isConnected
        && !element.matches(':disabled') && !element.closest('[inert]')
        && getComputedStyle(element).visibility === 'visible' && element.getClientRects().length > 0;

    function tabStops(modal) {
        const candidates = Array.from(modal.dialog.querySelectorAll(
            'a[href], area[href], button, input, select, textarea, iframe, [tabindex], [contenteditable="true"]'
        )).filter(element => owned(element, modal) && usable(element) && element.tabIndex >= 0);
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
        const swapOpener = event && event.detail && swapOpeners.get(event.detail.target);
        const previousTop = topModal();
        const removed = modals.filter(modal => !modal.backdrop.isConnected);
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
            modal.backdrop.style.zIndex = 1000 + index;
        });
        if (added && top) {
            focusInitial(top);
        } else if (previousTop && removed.includes(previousTop)) {
            const opener = connectedOpener(previousTop.opener);
            if (usable(opener) && (!top || owned(opener, top))) {
                if (top) focusInDialog(top, opener);
                else opener.focus({preventScroll: true});
            } else if (top) focusInitial(top);
        } else if (top && !owned(document.activeElement, top)) {
            focusInitial(top);
        }
    }

    document.addEventListener('focusin', event => {
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
})();

function toggleMobileSidebar() {
    const sidebar = document.getElementById('main-sidebar');
    if (!sidebar) {
        return;
    }

    const isOpen = sidebar.classList.toggle('mobile-open');
    const mobileToggle = document.querySelector('.mobile-sidebar-toggle');
    if (mobileToggle) {
        mobileToggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
    }
}

/**
 * Handle Accordion Logic
 * - Toggles active/expanded state
 * - Respects data-single-expand="true" on parent
 */
function handleAccordion(header) {
    const accordion = header.closest('.accordion');
    const panelId = header.getAttribute('aria-controls');

    if (!accordion || !panelId) return;

    const content = document.getElementById(panelId);
    if (!content
        || !content.classList.contains('accordion-content')
        || content.closest('.accordion') !== accordion) {
        return;
    }

    // Check for single expansion mode
    if (accordion.getAttribute('data-single-expand') === 'true') {
        // If we are opening a closed item, close all other items in this accordion.
        if (!header.classList.contains('active')) {
            const allHeaders = Array.from(accordion.querySelectorAll('.accordion-header'))
                .filter(candidate => candidate.closest('.accordion') === accordion);
            const allContents = Array.from(accordion.querySelectorAll('.accordion-content'))
                .filter(candidate => candidate.closest('.accordion') === accordion);

            allHeaders.forEach(candidate => {
                candidate.classList.remove('active');
                candidate.setAttribute('aria-expanded', 'false');
            });
            allContents.forEach(candidate => candidate.classList.remove('expanded'));
        }
    }

    // Toggle current state
    header.classList.toggle('active');
    content.classList.toggle('expanded');

    // Update ARIA
    const isExpanded = content.classList.contains('expanded');
    header.setAttribute('aria-expanded', isExpanded);
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
    });

    ownedPanels.forEach(candidate => {
        candidate.classList.remove('active');
    });

    // Activate clicked tab and corresponding panel
    button.classList.add('active');
    button.setAttribute('aria-selected', 'true');
    targetPanel.classList.add('active');
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

document.addEventListener('DOMContentLoaded', function() {
    const mobileToggle = document.querySelector('.mobile-sidebar-toggle');
    if (mobileToggle) {
        mobileToggle.setAttribute('aria-expanded', 'false');
    }
});

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
