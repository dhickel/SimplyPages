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
    const accordionItem = header.closest('.accordion-item');
    const content = header.nextElementSibling;
    const accordion = header.closest('.accordion');

    if (!content || !content.classList.contains('accordion-content')) return;

    // Check for single expansion mode
    if (accordion && accordion.getAttribute('data-single-expand') === 'true') {
        // If we are opening a closed item, close all others first
        if (!header.classList.contains('active')) {
            const allHeaders = accordion.querySelectorAll('.accordion-header');
            const allContents = accordion.querySelectorAll('.accordion-content');

            allHeaders.forEach(h => {
                h.classList.remove('active');
                h.setAttribute('aria-expanded', 'false');
            });
            allContents.forEach(c => c.classList.remove('expanded'));
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

    if (!tabsContainer) return;

    // Deactivate all tabs and panels in this container
    tabsContainer.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active');
        btn.setAttribute('aria-selected', 'false');
    });

    tabsContainer.querySelectorAll('.tab-panel').forEach(panel => {
        panel.classList.remove('active');
    });

    // Activate clicked tab and corresponding panel
    button.classList.add('active');
    button.setAttribute('aria-selected', 'true');

    const targetPanel = document.getElementById(panelId);
    if (targetPanel) {
        targetPanel.classList.add('active');
    }
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
