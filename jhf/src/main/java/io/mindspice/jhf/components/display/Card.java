package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

/**
 * Card component for displaying content in a contained, styled box.
 * Common for displaying items in a grid or list.
 */
public class Card extends HtmlTag {

    private Component header;
    private Component body;
    private Component footer;
    private Component image;

    public Card() {
        super("div");
        this.withAttribute("class", "card");
    }

    public static Card create() {
        return new Card();
    }

    public Card withHeader(String headerText) {
        this.header = new HtmlTag("div")
            .withAttribute("class", "card-header")
            .withInnerText(headerText);
        return this;
    }

    public Card withHeader(Component headerComponent) {
        HtmlTag headerDiv = new HtmlTag("div").withAttribute("class", "card-header");
        headerDiv.withChild(headerComponent);
        this.header = headerDiv;
        return this;
    }

    public Card withBody(String bodyText) {
        this.body = new HtmlTag("div")
            .withAttribute("class", "card-body")
            .withInnerText(bodyText);
        return this;
    }

    public Card withBody(Component bodyComponent) {
        HtmlTag bodyDiv = new HtmlTag("div").withAttribute("class", "card-body");
        bodyDiv.withChild(bodyComponent);
        this.body = bodyDiv;
        return this;
    }

    public Card withFooter(String footerText) {
        this.footer = new HtmlTag("div")
            .withAttribute("class", "card-footer")
            .withInnerText(footerText);
        return this;
    }

    public Card withFooter(Component footerComponent) {
        HtmlTag footerDiv = new HtmlTag("div").withAttribute("class", "card-footer");
        footerDiv.withChild(footerComponent);
        this.footer = footerDiv;
        return this;
    }

    public Card withImage(String src, String alt) {
        this.image = new HtmlTag("img", true)
            .withAttribute("src", src)
            .withAttribute("alt", alt)
            .withAttribute("class", "card-img-top");
        return this;
    }

    public Card withClass(String className) {
        String currentClass = "card";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    protected void build() {
        if (image != null) super.withChild(image);
        if (header != null) super.withChild(header);
        if (body != null) super.withChild(body);
        if (footer != null) super.withChild(footer);
    }
}
