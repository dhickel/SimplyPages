package io.mindspice.simplypages.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Small class-token utilities for components that need to preserve base/custom classes while
 * replacing one derived class family.
 */
public final class CssClassNames {

    private CssClassNames() {}

    public static void replacePrefixed(HtmlTag tag, String requiredBase, String className, String... prefixes) {
        Set<String> tokens = classTokens(tag);
        tokens.removeIf(token -> Arrays.stream(prefixes).anyMatch(token::startsWith));
        addRequired(tokens, requiredBase, className);
        write(tag, tokens);
    }

    public static void replaceMatching(HtmlTag tag, String requiredBase, String className, Predicate<String> matcher) {
        Set<String> tokens = classTokens(tag);
        tokens.removeIf(matcher);
        addRequired(tokens, requiredBase, className);
        write(tag, tokens);
    }

    public static void addTokens(HtmlTag tag, String requiredBase, String classNames) {
        Set<String> tokens = classTokens(tag);
        addRequired(tokens, requiredBase, classNames);
        write(tag, tokens);
    }

    private static void addRequired(Set<String> tokens, String requiredBase, String classNames) {
        if (requiredBase != null && !requiredBase.isBlank()) {
            tokens.add(requiredBase);
        }
        if (classNames != null && !classNames.isBlank()) {
            tokens.addAll(List.of(classNames.trim().split("\\s+")));
        }
    }

    private static Set<String> classTokens(HtmlTag tag) {
        Set<String> tokens = new LinkedHashSet<>();
        for (Attribute attr : tag.attributes) {
            if ("class".equals(attr.name()) && attr.value() != null && !attr.value().isBlank()) {
                tokens.addAll(List.of(attr.value().trim().split("\\s+")));
            }
        }
        return tokens;
    }

    private static void write(HtmlTag tag, Set<String> tokens) {
        tag.attributes.removeIf(attr -> "class".equals(attr.name()));
        tag.attributes.add(new Attribute("class", String.join(" ", new ArrayList<>(tokens))));
    }
}
