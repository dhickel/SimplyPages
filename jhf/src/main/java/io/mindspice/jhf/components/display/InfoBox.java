package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

/**
 * InfoBox component for displaying highlighted information.
 * More structured than an Alert, typically used for displaying metadata or statistics.
 */
public class InfoBox extends HtmlTag {

    private String title;
    private String value;
    private String icon;

    public InfoBox() {
        super("div");
        this.withAttribute("class", "info-box");
    }

    public static InfoBox create() {
        return new InfoBox();
    }

    public InfoBox withTitle(String title) {
        this.title = title;
        return this;
    }

    public InfoBox withValue(String value) {
        this.value = value;
        return this;
    }

    public InfoBox withIcon(String icon) {
        this.icon = icon;
        return this;
    }

    public InfoBox withClass(String className) {
        String currentClass = "info-box";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    public String render() {
        if (icon != null) {
            HtmlTag iconDiv = new HtmlTag("div")
                .withAttribute("class", "info-box-icon")
                .withInnerText(icon);
            super.withChild(iconDiv);
        }

        HtmlTag contentDiv = new HtmlTag("div").withAttribute("class", "info-box-content");

        if (title != null) {
            HtmlTag titleDiv = new HtmlTag("div")
                .withAttribute("class", "info-box-title")
                .withInnerText(title);
            contentDiv.withChild(titleDiv);
        }

        if (value != null) {
            HtmlTag valueDiv = new HtmlTag("div")
                .withAttribute("class", "info-box-value")
                .withInnerText(value);
            contentDiv.withChild(valueDiv);
        }

        super.withChild(contentDiv);
        return super.render();
    }
}
