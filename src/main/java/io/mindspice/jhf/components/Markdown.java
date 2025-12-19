package io.mindspice.jhf.components;

import io.mindspice.jhf.core.Component;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.Collections;

public class Markdown implements Component {

    private final String markdownText;
    private static final Parser parser = Parser.builder().build();
    private static final HtmlRenderer renderer = HtmlRenderer.builder().build();

    public Markdown(String markdownText) {
        this.markdownText = markdownText;
    }

    @Override
    public String render() {
        Node document = parser.parse(markdownText);
        return renderer.render(document);
    }
}
