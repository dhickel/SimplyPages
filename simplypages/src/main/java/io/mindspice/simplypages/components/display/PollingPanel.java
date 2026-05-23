package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * HTMX polling container for app-owned status, job, or dashboard fragments.
 *
 * <p>The application owns the endpoint and returned fragment. This component only emits a stable
 * target container and HTMX polling attributes.</p>
 */
public class PollingPanel extends HtmlTag {

    public PollingPanel(String id, String endpoint) {
        super("section");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id cannot be null or blank");
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("endpoint cannot be null or blank");
        }
        this.withId(id);
        this.withAttribute("class", "polling-panel");
        this.withAttribute("hx-get", endpoint);
        this.withAttribute("hx-trigger", "load, every 5s");
        this.withAttribute("hx-target", "#" + id);
        this.withAttribute("hx-swap", "outerHTML");
        this.withAttribute("data-sp-polling-panel", "true");
    }

    public static PollingPanel create(String id, String endpoint) {
        return new PollingPanel(id, endpoint);
    }

    public PollingPanel everySeconds(int seconds) {
        if (seconds < 1) {
            throw new IllegalArgumentException("seconds must be >= 1");
        }
        this.withAttribute("hx-trigger", "load, every " + seconds + "s");
        return this;
    }

    public PollingPanel everyMillis(int millis) {
        if (millis < 1) {
            throw new IllegalArgumentException("millis must be >= 1");
        }
        this.withAttribute("hx-trigger", "load, every " + millis + "ms");
        return this;
    }

    public PollingPanel withTarget(String targetSelector) {
        this.withAttribute("hx-target", targetSelector);
        return this;
    }

    public PollingPanel withSwap(String swap) {
        this.withAttribute("hx-swap", swap);
        return this;
    }

    public PollingPanel withLoadingText(String loadingText) {
        this.withChild(new HtmlTag("div")
            .withAttribute("class", "polling-panel-loading")
            .withInnerText(loadingText == null ? "" : loadingText));
        return this;
    }

    @Override
    public PollingPanel withChild(Component component) {
        super.withChild(component);
        return this;
    }

    public PollingPanel withClass(String className) {
        super.addClass(className);
        return this;
    }
}
