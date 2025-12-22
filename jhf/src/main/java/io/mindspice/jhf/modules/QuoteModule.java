package io.mindspice.jhf.modules;

import io.mindspice.jhf.core.Module;

import io.mindspice.jhf.components.Div;
import io.mindspice.jhf.components.Header;
import io.mindspice.jhf.core.HtmlTag;

/**
 * Quote module for displaying testimonials, quotes, or highlighted text.
 *
 * <p>Useful for testimonials, research quotes, user feedback, or any
 * content that benefits from visual emphasis.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Simple quote
 * QuoteModule.create()
 *     .withQuote("This platform revolutionized our research process.")
 *     .withAuthor("Dr. Jane Smith");
 *
 * // Quote with attribution details
 * QuoteModule.create()
 *     .withQuote("Collaborative research yields better results.")
 *     .withAuthor("Dr. John Doe")
 *     .withAttribution("Professor of Botany, University of California")
 *     .withDate("January 2024");
 *
 * // Centered large quote
 * QuoteModule.create()
 *     .withQuote("Science is the future.")
 *     .large()
 *     .centered();
 * }</pre>
 */
public class QuoteModule extends Module {

    private String quote;
    private String author;
    private String attribution;
    private String date;
    private boolean large = false;
    private boolean centered = false;

    public QuoteModule() {
        super("div");
        this.withClass("quote-module");
    }

    public static QuoteModule create() {
        return new QuoteModule();
    }

    @Override
    public QuoteModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public QuoteModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    /**
     * Sets the quote text.
     *
     * @param quote the quote content
     */
    public QuoteModule withQuote(String quote) {
        this.quote = quote;
        return this;
    }

    /**
     * Sets the author of the quote.
     *
     * @param author the author name
     */
    public QuoteModule withAuthor(String author) {
        this.author = author;
        return this;
    }

    /**
     * Sets additional attribution information (title, organization, etc.).
     *
     * @param attribution attribution details
     */
    public QuoteModule withAttribution(String attribution) {
        this.attribution = attribution;
        return this;
    }

    /**
     * Sets the date of the quote.
     *
     * @param date the quote date
     */
    public QuoteModule withDate(String date) {
        this.date = date;
        return this;
    }

    /**
     * Makes the quote display in a larger size.
     */
    public QuoteModule large() {
        this.large = true;
        return this;
    }

    /**
     * Centers the quote content.
     */
    public QuoteModule centered() {
        this.centered = true;
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }

        HtmlTag blockquote = new HtmlTag("blockquote")
            .withAttribute("class", "quote-content" +
                (large ? " quote-large" : "") +
                (centered ? " quote-centered" : ""));

        // Quote text
        if (quote != null && !quote.isEmpty()) {
            HtmlTag quoteText = new HtmlTag("p")
                .withAttribute("class", "quote-text")
                .withInnerText("\"" + quote + "\"");
            blockquote.withChild(quoteText);
        }

        // Citation
        if (author != null || attribution != null || date != null) {
            HtmlTag cite = new HtmlTag("cite").withAttribute("class", "quote-cite");

            // Author
            if (author != null && !author.isEmpty()) {
                HtmlTag authorSpan = new HtmlTag("span")
                    .withAttribute("class", "quote-author")
                    .withInnerText(author);
                cite.withChild(authorSpan);
            }

            // Attribution
            if (attribution != null && !attribution.isEmpty()) {
                HtmlTag attrSpan = new HtmlTag("span")
                    .withAttribute("class", "quote-attribution")
                    .withInnerText(attribution);
                cite.withChild(attrSpan);
            }

            // Date
            if (date != null && !date.isEmpty()) {
                HtmlTag dateSpan = new HtmlTag("span")
                    .withAttribute("class", "quote-date")
                    .withInnerText(date);
                cite.withChild(dateSpan);
            }

            blockquote.withChild(cite);
        }

        super.withChild(blockquote);
    }
}
