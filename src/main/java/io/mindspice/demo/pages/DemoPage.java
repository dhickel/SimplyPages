package io.mindspice.demo.pages;

/**
 * Interface for demo page classes.
 *
 * <p>Each demo page implements this interface to render its content.
 * This separates page logic from controller routing.</p>
 */
public interface DemoPage {

    /**
     * Renders the page content as HTML.
     *
     * @return HTML string for the page
     */
    String render();
}
