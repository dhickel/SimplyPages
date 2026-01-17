package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

import java.util.stream.Stream;

/**
 * Blockquote component for quoted text.
 */
public class Blockquote extends HtmlTag {

    private String quote;
    private String citation;
    private String source;

    public Blockquote(String quote) {
        super("blockquote");
        this.quote = quote;
        this.withAttribute("class", "blockquote");
    }

    public static Blockquote create(String quote) {
        return new Blockquote(quote);
    }

    public Blockquote withCitation(String citation) {
        this.citation = citation;
        return this;
    }

    public Blockquote withSource(String source) {
        this.source = source;
        return this;
    }

    @Override
    public Blockquote withClass(String className) {
        super.addClass(className);
        return this;
    }

    @Override
    protected Stream<Component> getChildrenStream() {
        Stream.Builder<Component> builder = Stream.builder();

        // Quote text
        HtmlTag quoteText = new HtmlTag("p")
            .withAttribute("class", "blockquote-text")
            .withInnerText(quote);
        builder.add(quoteText);

        // Footer with citation and/or source
        if (citation != null || source != null) {
            HtmlTag footer = new HtmlTag("footer")
                .withAttribute("class", "blockquote-footer");

            if (citation != null) {
                HtmlTag cite = new HtmlTag("cite")
                    .withInnerText(citation);
                footer.withChild(cite);
            }

            if (source != null) {
                if (citation != null) {
                    footer.withInnerText(", ");
                }
                HtmlTag sourceSpan = new HtmlTag("span")
                    .withAttribute("class", "blockquote-source")
                    .withInnerText(source);
                footer.withChild(sourceSpan);
            }

            builder.add(footer);
        }

        return Stream.concat(builder.build(), super.getChildrenStream());
    }
}
