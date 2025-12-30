package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * InfoBox component for displaying highlighted information.
 */
public class InfoBox extends HtmlTag {

    private final HtmlTag iconDiv;
    private final HtmlTag contentDiv;
    private final HtmlTag titleDiv;
    private final HtmlTag valueDiv;

    public InfoBox() {
        super("div");
        this.withAttribute("class", "info-box");

        // Initialize structure
        this.iconDiv = new HtmlTag("div").withAttribute("class", "info-box-icon");
        this.contentDiv = new HtmlTag("div").withAttribute("class", "info-box-content");
        this.titleDiv = new HtmlTag("div").withAttribute("class", "info-box-title");
        this.valueDiv = new HtmlTag("div").withAttribute("class", "info-box-value");

        // Assemble (even if empty, they will be hidden via CSS or updated later)
        // Note: Logic in getChildrenStream was conditional.
        // We can replicate that by only adding to children if set, or managing visibility.
        // For now, let's assume we want them in DOM.
        // Or better: don't add iconDiv yet.
        this.withChild(contentDiv);
        contentDiv.withChild(titleDiv);
        contentDiv.withChild(valueDiv);
    }

    public static InfoBox create() {
        return new InfoBox();
    }

    public InfoBox withTitle(String title) {
        titleDiv.withInnerText(title);
        return this;
    }

    public InfoBox withValue(String value) {
        valueDiv.withInnerText(value);
        return this;
    }

    public InfoBox withIcon(String icon) {
        iconDiv.withInnerText(icon);
        // Ensure iconDiv is first child if not already present
        if (!children.contains(iconDiv)) {
            children.add(0, iconDiv);
        }
        return this;
    }

    @Override
    public InfoBox withClass(String className) {
        super.addClass(className);
        return this;
    }

    // Removed getChildrenStream override
}
