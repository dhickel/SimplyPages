package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

/**
 * Inline/code-block renderer with optional language and title metadata.
 *
 * <p>Mutable and not thread-safe. For block mode, markup is assembled lazily during render. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class Code extends HtmlTag {

    private String code;
    private String language;
    private String title;
    private boolean isBlock;

    private Code(String code, boolean isBlock) {
        super(isBlock ? "pre" : "code");
        this.code = code;
        this.isBlock = isBlock;
        if (!isBlock) {
            this.withAttribute("class", "code-inline");
            this.withInnerText(code);  // Set content immediately for inline
        } else {
            this.withAttribute("class", "code-block");
        }
    }

    /**
     * Creates an inline code element.
     *
     * @param code the code text
     */
    public static Code inline(String code) {
        return new Code(code, false);
    }

    /**
     * Creates a code block element.
     *
     * @param code the code text
     */
    public static Code block(String code) {
        return new Code(code, true);
    }

    /**
     * Sets the programming language for syntax highlighting.
     * This adds a language-specific class for syntax highlighters.
     *
     * @param language the language name (e.g., "java", "python", "javascript")
     */
    public Code withLanguage(String language) {
        this.language = language;
        return this;
    }

    /**
     * Adds a title/filename to display above the code block.
     *
     * @param title the title or filename
     */
    public Code withTitle(String title) {
        this.title = title;
        return this;
    }

    public Code withClass(String className) {
        String currentClass = isBlock ? "code-block" : "code-inline";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    public String render(RenderContext context) {
        if (!isBlock) {
            return super.render(context);
        }
        HtmlTag pre = new HtmlTag("pre");
        attributes.forEach(attribute -> pre.withAttribute(attribute.name(), attribute.value()));
        HtmlTag codeElement = new HtmlTag("code").withInnerText(code);
        if (language != null) {
            codeElement.withAttribute("class", "language-" + language);
        }
        pre.withChild(codeElement);
        children.forEach(pre::withChild);
        if (title == null) {
            return pre.render(context);
        }
        return new HtmlTag("div").withClass("code-container")
            .withChild(new HtmlTag("div").withClass("code-title").withInnerText(title))
            .withChild(pre).render(context);
    }

    @Override
    public String render() {
        return render(RenderContext.empty());
    }
}
