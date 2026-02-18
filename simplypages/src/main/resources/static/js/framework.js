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

// HTMX history navigation should reset the window scroll position.
// This keeps sidebar-driven page-to-page navigation predictable while avoiding
// scroll jumps for non-navigation fragment updates.
document.body.addEventListener('htmx:afterSettle', function(event) {
    const requestConfig = event.detail && event.detail.requestConfig;
    if (!requestConfig) {
        return;
    }

    const sourceElement = requestConfig.elt instanceof Element ? requestConfig.elt : null;
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

    if (!pushesUrlViaAttr && !pushesUrlViaRequest) {
        return;
    }

    window.scrollTo({top: 0, left: 0, behavior: 'auto'});
});
