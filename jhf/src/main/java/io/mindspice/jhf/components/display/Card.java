package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

/**
 * Card component for displaying content in a contained, styled box.
 * Common for displaying items in a grid or list.
 */
public class Card extends HtmlTag {

    private final HtmlTag headerContainer = new HtmlTag("div").addClass("card-header");
    private final HtmlTag bodyContainer = new HtmlTag("div").addClass("card-body");
    private final HtmlTag footerContainer = new HtmlTag("div").addClass("card-footer");
    private HtmlTag imageComponent;

    private boolean hasHeader = false;
    private boolean hasBody = false;
    private boolean hasFooter = false;

    public Card() {
        super("div");
        this.withAttribute("class", "card");
        // We defer adding children until we know what we have,
        // OR we add them now and control visibility via emptiness or null?
        // But Card enforces order: Image, Header, Body, Footer.
        // If we add them to children now, they are in children list.
        // If we construct them but don't add, we need to add them in render().

        // Option A (from review): Clear children in render().
        // Option B: Add immediately but manage order carefully? Hard if user calls methods in random order.

        // Let's go with Option A as it is robust for this specific component structure
        // where we have fixed slots.
    }

    public static Card create() {
        return new Card();
    }

    public Card withHeader(String headerText) {
        headerContainer.clearChildren(); // Clear previous content if any
        headerContainer.withInnerText(headerText);
        hasHeader = true;
        return this;
    }

    public Card withHeader(Component headerComponent) {
        headerContainer.clearChildren();
        headerContainer.withChild(headerComponent);
        hasHeader = true;
        return this;
    }

    public Card withBody(String bodyText) {
        bodyContainer.clearChildren();
        bodyContainer.withInnerText(bodyText);
        hasBody = true;
        return this;
    }

    public Card withBody(Component bodyComponent) {
        bodyContainer.clearChildren();
        bodyContainer.withChild(bodyComponent);
        hasBody = true;
        return this;
    }

    public Card withFooter(String footerText) {
        footerContainer.clearChildren();
        footerContainer.withInnerText(footerText);
        hasFooter = true;
        return this;
    }

    public Card withFooter(Component footerComponent) {
        footerContainer.clearChildren();
        footerContainer.withChild(footerComponent);
        hasFooter = true;
        return this;
    }

    public Card withImage(String src, String alt) {
        this.imageComponent = new HtmlTag("img", true)
            .withAttribute("src", src)
            .withAttribute("alt", alt)
            .addClass("card-img-top");
        return this;
    }

    @Override
    public Card withClass(String className) {
        super.withClass(className);
        return this;
    }

    @Override
    public String render() {
        // Clear children to avoid duplication on re-render
        // Note: children is protected in HtmlTag
        children.clear();

        if (imageComponent != null) children.add(imageComponent);
        if (hasHeader) children.add(headerContainer);
        if (hasBody) children.add(bodyContainer);
        if (hasFooter) children.add(footerContainer);

        return super.render();
    }
}
