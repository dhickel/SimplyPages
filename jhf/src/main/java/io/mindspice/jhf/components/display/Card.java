package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.Objects;
import java.util.stream.Stream;

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
    }

    public static Card create() {
        return new Card();
    }

    public Card withHeader(String headerText) {
        headerContainer.withInnerText(headerText);
        hasHeader = true;
        return this;
    }

    public Card withHeader(Component headerComponent) {
        headerContainer.withChild(headerComponent);
        hasHeader = true;
        return this;
    }

    public Card withBody(String bodyText) {
        bodyContainer.withInnerText(bodyText);
        hasBody = true;
        return this;
    }

    public Card withBody(Component bodyComponent) {
        bodyContainer.withChild(bodyComponent);
        hasBody = true;
        return this;
    }

    public Card withFooter(String footerText) {
        footerContainer.withInnerText(footerText);
        hasFooter = true;
        return this;
    }

    public Card withFooter(Component footerComponent) {
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
    protected Stream<Component> getChildrenStream() {
        // Construct the stream of components in the enforced order.
        // We include explicit slots first, then any other children added via withChild (if any).
        // Since Card logic usually doesn't involve generic withChild, we could ignore super.children,
        // but it's safer to include them at the end or begin depending on policy.
        // Given Card structure is rigid, let's put them after footer or not at all?
        // Let's assume standard children go into the body if we wanted to be smart, but here
        // let's just append them to the end of the card to support custom overlays etc.

        Stream<Component> slotStream = Stream.of(
                imageComponent,
                hasHeader ? headerContainer : null,
                hasBody ? bodyContainer : null,
                hasFooter ? footerContainer : null
        ).filter(Objects::nonNull).map(c -> (Component) c);

        return Stream.concat(slotStream, super.getChildrenStream());
    }
}
