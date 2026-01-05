package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Code component for displaying code snippets.
 *
 * <p>Supports both inline code and code blocks with optional syntax highlighting classes.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Inline code
 * Code.inline("System.out.println()");
 *
 * // Code block
 * Code.block("public static void main(String[] args) {\n    // code here\n}");
 *
 * // Code block with language
 * Code.block(codeString).withLanguage("java");
 *
 * // Code block with title
 * Code.block(codeString)
 *     .withLanguage("python")
 *     .withTitle("Example.py");
 * }</pre>
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
    public String render() {
        if (isBlock) {
            // Wrap in container if title is present
            if (title != null) {
                HtmlTag container = new HtmlTag("div")
                    .withAttribute("class", "code-container");

                HtmlTag titleDiv = new HtmlTag("div")
                    .withAttribute("class", "code-title")
                    .withInnerText(title);
                container.withChild(titleDiv);

                // Build the pre > code structure
                HtmlTag codeElement = new HtmlTag("code");
                if (language != null) {
                    codeElement.withAttribute("class", "language-" + language);
                }
                codeElement.withInnerText(code);

                super.withChild(codeElement);
                container.withChild(new HtmlTag("pre") {
                    @Override
                    public String render(io.mindspice.simplypages.core.RenderContext context) {
                        return Code.super.render(context);
                    }

                    @Override
                    public String render() {
                        return render(io.mindspice.simplypages.core.RenderContext.empty());
                    }
                });

                return container.render();
            } else {
                // No title, just pre > code
                HtmlTag codeElement = new HtmlTag("code");
                if (language != null) {
                    codeElement.withAttribute("class", "language-" + language);
                }
                codeElement.withInnerText(code);
                super.withChild(codeElement);
                return super.render();
            }
        } else {
            // Inline code
            super.withInnerText(code);
            return super.render();
        }
    }
}
