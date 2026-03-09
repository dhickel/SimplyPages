package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.components.forum.categories.ForumCategoryComponent;
import io.mindspice.simplypages.components.forum.categories.ForumCategoryData;
import io.mindspice.simplypages.components.forum.categories.ForumCategoryRenderer;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ForumCategoryRendererTest {

    private record Category(String id, String title, String description, Integer topicCount) implements ForumCategoryData {}

    private static final class ProbeCategoryComponent implements ForumCategoryComponent {
        private String id = "";

        @Override
        public ForumCategoryComponent withCategoryId(String id) {
            this.id = id == null ? "" : id;
            return this;
        }

        @Override
        public ForumCategoryComponent withTitle(String title) {
            return this;
        }

        @Override
        public ForumCategoryComponent withDescription(String description) {
            return this;
        }

        @Override
        public ForumCategoryComponent withTopicCount(Integer topicCount) {
            return this;
        }

        @Override
        public String render(RenderContext context) {
            return new HtmlTag("div")
                .withAttribute("class", "probe-category")
                .withAttribute("data-category-id", id)
                .render(context);
        }
    }

    @Test
    @DisplayName("ForumCategoryRenderer should render default category components")
    void rendersCategories() {
        ForumCategoryRenderer<Category, String> renderer = ForumCategoryRenderer.<Category, String>builder().build();

        String html = renderer.render(List.of(
            new Category("cat-1", "General", "General discussion", 2)
        ), "ctx").render();

        HtmlAssert.assertThat(html)
            .hasElement("div.forum-categories-view > div.forum-category[data-category-id=cat-1]")
            .hasElement("div.forum-category > h3.forum-category-title")
            .hasElement("div.forum-category > p.forum-category-description")
            .hasElement("div.forum-category > span.forum-category-topic-count");
    }

    @Test
    @DisplayName("ForumCategoryRenderer should require category id")
    void requiresCategoryId() {
        ForumCategoryRenderer<Category, String> renderer = ForumCategoryRenderer.<Category, String>builder().build();

        assertThrows(IllegalArgumentException.class,
            () -> renderer.render(List.of(new Category("", "bad", "", null)), "ctx").render());
    }

    @Test
    @DisplayName("ForumCategoryRenderer should invoke supplied component once per rendered item")
    void categorySupplierIsUsedPerItem() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<ForumCategoryComponent> supplier = () -> {
            calls.incrementAndGet();
            return new ProbeCategoryComponent();
        };

        ForumCategoryRenderer<Category, String> renderer = ForumCategoryRenderer.<Category, String>builder()
            .withCategoryComponentSupplier(supplier)
            .build();

        String html = renderer.render(List.of(
            new Category("cat-1", "One", "", null),
            new Category("cat-2", "Two", "", null)
        ), "ctx").render();

        assertEquals(2, calls.get());
        HtmlAssert.assertThat(html)
            .hasElement("div.probe-category[data-category-id=cat-1]")
            .hasElement("div.probe-category[data-category-id=cat-2]");
    }

    @Test
    @DisplayName("ForumCategoryRenderer should return an empty root when input is null or empty")
    void nullAndEmptyInputsReturnEmptyRoot() {
        ForumCategoryRenderer<Category, String> renderer = ForumCategoryRenderer.<Category, String>builder().build();

        String nullHtml = renderer.render(null, "ctx").render();
        String emptyHtml = renderer.render(List.of(), "ctx").render();

        HtmlAssert.assertThat(nullHtml).hasElement("div.forum-categories-view");
        HtmlAssert.assertThat(nullHtml).hasElementCount("div.forum-categories-view > *", 0);
        HtmlAssert.assertThat(emptyHtml).hasElement("div.forum-categories-view");
        HtmlAssert.assertThat(emptyHtml).hasElementCount("div.forum-categories-view > *", 0);
    }
}
