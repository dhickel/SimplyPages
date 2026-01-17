package io.mindspice.jhf.components;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

/**
 * Blockquote component for quoted text.
 *
 * <p>Blockquotes are used to quote text from external sources or highlight
 * important passages.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Simple blockquote
 * Blockquote.create("This is a quote from a research paper.");
 *
 * // Blockquote with citation
 * Blockquote.create("Knowledge is power.")
 *     .withCitation("Francis Bacon");
 *
 * // Blockquote with source
 * Blockquote.create("Research findings show...")
 *     .withCitation("Dr. Smith")
 *     .withSource("Journal of Cannabis Research, 2024");
 * }</pre>
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

    /**
     * Adds a citation (author or source name).
     *
     * @param citation the citation text
     */
    public Blockquote withCitation(String citation) {
        this.citation = citation;
        return this;
    }

    /**
     * Adds a source reference (publication, URL, etc.).
     *
     * @param source the source reference
     */
    public Blockquote withSource(String source) {
        this.source = source;
        return this;
    }

    public Blockquote withClass(String className) {
        String currentClass = "blockquote";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    public String render() {
        // Quote text
        HtmlTag quoteText = new HtmlTag("p")
            .withAttribute("class", "blockquote-text")
            .withInnerText(quote);
        super.withChild(quoteText);

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

            super.withChild(footer);
        }

        return super.render();
    }
}
