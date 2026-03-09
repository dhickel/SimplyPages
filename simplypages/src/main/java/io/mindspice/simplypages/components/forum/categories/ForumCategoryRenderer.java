package io.mindspice.simplypages.components.forum.categories;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Renderer for forum categories.
 */
public final class ForumCategoryRenderer<CATEGORY extends ForumCategoryData, CTX> {

    private final Supplier<? extends ForumCategoryComponent> componentSupplier;

    private ForumCategoryRenderer(Builder<CATEGORY, CTX> builder) {
        this.componentSupplier = builder.componentSupplier;
    }

    public static <CATEGORY extends ForumCategoryData, CTX> Builder<CATEGORY, CTX> builder() {
        return new Builder<>();
    }

    public Component render(Collection<CATEGORY> categories, CTX context) {
        HtmlTag root = new HtmlTag("div").withAttribute("class", "forum-categories-view");
        if (categories == null || categories.isEmpty()) {
            return root;
        }

        for (CATEGORY category : categories) {
            String id = requireId(category.id(), "category id");
            ForumCategoryComponent rendered = Objects.requireNonNull(
                componentSupplier.get(),
                "category component supplier returned null"
            );

            root.withChild(rendered
                .withCategoryId(id)
                .withTitle(safe(category.title()))
                .withDescription(safe(category.description()))
                .withTopicCount(category.topicCount()));
        }

        return root;
    }

    private String requireId(String id, String fieldName) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Missing required " + fieldName);
        }
        return id;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Builder<CATEGORY extends ForumCategoryData, CTX> {
        private Supplier<? extends ForumCategoryComponent> componentSupplier = DefaultForumCategoryComponent::create;

        public Builder<CATEGORY, CTX> withCategoryComponentSupplier(
                Supplier<? extends ForumCategoryComponent> componentSupplier
        ) {
            this.componentSupplier = Objects.requireNonNull(componentSupplier, "componentSupplier");
            return this;
        }

        public ForumCategoryRenderer<CATEGORY, CTX> build() {
            return new ForumCategoryRenderer<>(this);
        }
    }
}
