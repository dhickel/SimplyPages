/**
 * SimplyPages Framework JavaScript
 * Provides interactive functionality for components and modules
 */

/**
 * Initialize all interactive components
 */
function initializeComponents() {
    initializeTabs();
    initializeAccordion();
    initializeCallouts();
}

/**
 * Tabs functionality
 */
function initializeTabs() {
    // Remove existing event listeners by cloning nodes (prevents duplicate listeners)
    const tabButtons = document.querySelectorAll('.tab-button');

    tabButtons.forEach(button => {
        // Clone to remove old listeners
        const newButton = button.cloneNode(true);
        button.parentNode.replaceChild(newButton, button);

        newButton.addEventListener('click', function() {
            const panelId = this.getAttribute('aria-controls');
            const tabsContainer = this.closest('.tabs-container');

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
            this.classList.add('active');
            this.setAttribute('aria-selected', 'true');

            const targetPanel = document.getElementById(panelId);
            if (targetPanel) {
                targetPanel.classList.add('active');
            }
        });
    });
}

/**
 * Accordion functionality
 */
function initializeAccordion() {
    const accordionHeaders = document.querySelectorAll('.accordion-header');

    accordionHeaders.forEach(header => {
        // Clone to remove old listeners
        const newHeader = header.cloneNode(true);
        header.parentNode.replaceChild(newHeader, header);

        newHeader.addEventListener('click', function() {
            const isActive = this.classList.contains('active');
            const content = this.nextElementSibling;

            if (!content) return;

            // Toggle active state
            this.classList.toggle('active');
            content.classList.toggle('expanded');

            // Update ARIA attributes
            this.setAttribute('aria-expanded', !isActive);
        });
    });
}

/**
 * Callout dismissible functionality
 */
function initializeCallouts() {
    const calloutCloseButtons = document.querySelectorAll('.callout-close');

    calloutCloseButtons.forEach(button => {
        // Clone to remove old listeners
        const newButton = button.cloneNode(true);
        button.parentNode.replaceChild(newButton, button);

        newButton.addEventListener('click', function() {
            const callout = this.closest('.callout');
            if (callout) {
                callout.style.display = 'none';
            }
        });
    });
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', initializeComponents);

// Re-initialize after HTMX swaps content
document.body.addEventListener('htmx:afterSwap', function(event) {
    initializeComponents();
    // Scroll to top on navigation
    window.scrollTo({top: 0, behavior: 'instant'});
});

// Also handle browser back/forward
window.addEventListener('popstate', function() {
    // Re-initialize components after browser navigation
    setTimeout(initializeComponents, 100);
});
