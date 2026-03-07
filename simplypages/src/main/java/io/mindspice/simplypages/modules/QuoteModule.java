package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.core.Module;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Module for rendering emphasized quote/testimonial content.
 *
 * <p>Mutability and thread-safety: mutable and not thread-safe. Mutate within a request-scoped flow. For reuse, stop mutating shared instances and render stable structures with per-request context data.</p>
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
