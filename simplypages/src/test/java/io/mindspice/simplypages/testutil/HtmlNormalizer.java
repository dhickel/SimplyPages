package io.mindspice.simplypages.testutil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HtmlNormalizer {

    private HtmlNormalizer() {
    }

    public static String normalize(String html) {
        Document document = Jsoup.parseBodyFragment(html);
        document.outputSettings().prettyPrint(false);
        canonicalizeAttributes(document.body());
        String normalized = document.body().html();
        normalized = normalized.replaceAll(">\\s+<", "><");
        return normalized.trim();
    }

    private static void canonicalizeAttributes(Element element) {
        List<Attribute> attributes = new ArrayList<>();
        element.attributes().forEach(attributes::add);
        attributes.sort(Comparator.comparing(Attribute::getKey));
        element.clearAttributes();
        for (Attribute attribute : attributes) {
            element.attr(attribute.getKey(), attribute.getValue());
        }
        for (Element child : element.children()) {
            canonicalizeAttributes(child);
        }
    }
}
