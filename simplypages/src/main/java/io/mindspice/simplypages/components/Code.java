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
    private boolean contentBuilt = false;

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
    public String render() {
        if (isBlock) {
            // Build the code element
            HtmlTag codeElement = new HtmlTag("code");
            if (language != null) {
                codeElement.withAttribute("class", "language-" + language);
            }
            codeElement.withInnerText(code);

            // Build the pre element manually
            StringBuilder preHtml = new StringBuilder();
            preHtml.append("<pre class=\"code-block\">");
            preHtml.append(codeElement.render());
            preHtml.append("</pre>");

            // Wrap in container if title is present
            if (title != null) {
                StringBuilder containerHtml = new StringBuilder();
                containerHtml.append("<div class=\"code-container\">");
                containerHtml.append("<div class=\"code-title\">").append(title).append("</div>");
                containerHtml.append(preHtml);
                containerHtml.append("</div>");
                return containerHtml.toString();
            } else {
                // No title, just return the pre element
                return preHtml.toString();
            }
        } else {
            // Inline code (content already set in constructor)
            return super.render();
        }
    }
}
