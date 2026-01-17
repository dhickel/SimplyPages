package io.mindspice.demo.pages;

import io.mindspice.jhf.components.*;
import io.mindspice.jhf.components.forum.ForumPost;
import io.mindspice.jhf.core.*;
import io.mindspice.jhf.layout.Grid;
import io.mindspice.jhf.layout.Page;
import io.mindspice.jhf.modules.ContentModule;
import io.mindspice.jhf.modules.FormModule;
import io.mindspice.jhf.modules.ForumModule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DynamicUpdatesPage implements DemoPage {

    // --- Example 1: Template & Slot Keys ---

    public static final SlotKey<String> CARD_TITLE = SlotKey.of("card_title");
    public static final SlotKey<String> CARD_BODY = SlotKey.of("card_body");
    public static final SlotKey<io.mindspice.jhf.core.Component> LIST_CONTENT = SlotKey.of("list_content");
    public static final SlotKey<io.mindspice.jhf.core.Component> TABLE_BODY = SlotKey.of("table_body");

    // We add hx-swap-oob="true" to the modules in the template.

    public static final Template CARD_TEMPLATE = Template.of(
            ContentModule.create()
                    .withTitle("Card Module")
                    .withModuleId("card-module")
                    .withCustomContent(
                            new HtmlTag("div")
                                    .withChild(new HtmlTag("h3").withChild(Slot.of(CARD_TITLE)))
                                    .withChild(new HtmlTag("p").withChild(Slot.of(CARD_BODY)))
                    )
                    .withAttribute("hx-swap-oob", "true")
    );

    public static final Template LIST_TEMPLATE = Template.of(
            ContentModule.create()
                    .withTitle("List Module")
                    .withModuleId("list-module")
                    .withCustomContent(
                            new HtmlTag("ul").withClass("list-group").withChild(Slot.of(LIST_CONTENT))
                    )
                    .withAttribute("hx-swap-oob", "true")
    );

    public static final Template TABLE_TEMPLATE = Template.of(
            ContentModule.create()
                    .withTitle("Table Module")
                    .withModuleId("table-module")
                    .withCustomContent(
                            new HtmlTag("table").withClass("table")
                                    .withChild(new HtmlTag("thead").withChild(
                                            new HtmlTag("tr")
                                                    .withChild(new HtmlTag("th").withInnerText("Col 1"))
                                                    .withChild(new HtmlTag("th").withInnerText("Col 2"))
                                                    .withChild(new HtmlTag("th").withInnerText("Col 3"))
                                    ))
                                    .withChild(new HtmlTag("tbody").withChild(Slot.of(TABLE_BODY)))
                    )
                    .withAttribute("hx-swap-oob", "true")
    );


    @Override
    public String render() {
        return Page.builder()
                .addComponents(Header.H1("Dynamic Updates Demo"))
                .addComponents(new Markdown("This page demonstrates dynamic updates using **Templates**, **SlotKeys**, and **HTMX**."))

                // --- Example 1 ---
                .addComponents(Header.H2("Example 1: Targeted Template Updates"))
                .addComponents(new Markdown("Select a module to update, fill in the values, and click update. The specific module will update in place using `hx-swap-oob`."))

                // Row 1: The 3 Modules
                .addComponents(
                        Grid.create()
                                .withColumns(3)
                                .withChild(new io.mindspice.jhf.core.Component() {
                                    @Override
                                    public String render(RenderContext context) {
                                        return renderCard("Initial Title", "Initial content...");
                                    }
                                })
                                .withChild(new io.mindspice.jhf.core.Component() {
                                    @Override
                                    public String render(RenderContext context) {
                                        return renderList(List.of("Item A", "Item B", "Item C"));
                                    }
                                })
                                .withChild(new io.mindspice.jhf.core.Component() {
                                    @Override
                                    public String render(RenderContext context) {
                                        return renderTable("Cell X", "Cell Y", "Cell Z");
                                    }
                                })
                )

                // Row 2: Control Module
                .addRow(row -> {
                    row.withChild(
                            ContentModule.create()
                                    .withTitle("Control Panel")
                                    .withCustomContent(
                                            new HtmlTag("form")
                                                    .withAttribute("hx-post", "/demo/dynamic-updates/update-module")
                                                    .withAttribute("hx-swap", "none") // Important: prevents form from being replaced by empty response (OOB only)
                                                    .withAttribute("class", "form-layout")
                                                    // Dropdown
                                                    .withChild(new HtmlTag("div").withClass("form-group")
                                                            .withChild(new HtmlTag("label").withInnerText("Target Module"))
                                                            .withChild(new HtmlTag("select").withAttribute("name", "target")
                                                                    .withChild(new HtmlTag("option").withAttribute("value", "card").withInnerText("Card Module"))
                                                                    .withChild(new HtmlTag("option").withAttribute("value", "list").withInnerText("List Module"))
                                                                    .withChild(new HtmlTag("option").withAttribute("value", "table").withInnerText("Table Module"))
                                                            )
                                                    )
                                                    // Inputs
                                                    .withChild(createInput("val1", "Input 1 (Title / Item 1 / Col 1)"))
                                                    .withChild(createInput("val2", "Input 2 (Body / Item 2 / Col 2)"))
                                                    .withChild(createInput("val3", "Input 3 (N/A / Item 3 / Col 3)"))
                                                    // Button
                                                    .withChild(new HtmlTag("button")
                                                            .withAttribute("type", "submit")
                                                            .withClass("btn btn-primary")
                                                            .withInnerText("Update Selected Module")
                                                    )
                                    )
                    );
                })

                // --- Example 2 ---
                .addComponents(Header.H2("Example 2: Appending Content"))
                .addComponents(new Markdown("Post a message to the forum below. The list will refresh with the new post, and the input will reset."))

                // Row 3: Forum Module
                .addRow(row -> {
                    // Initial render of forum posts
                    row.withChild(renderForumModule(getInitialPosts()));
                })

                // Row 4: Input Module
                .addRow(row -> {
                    row.withChild(
                            ContentModule.create()
                                    .withTitle("New Post")
                                    .withCustomContent(
                                            new HtmlTag("form")
                                                    .withAttribute("hx-post", "/demo/dynamic-updates/add-post")
                                                    .withAttribute("hx-target", "#forum-module") // Targets the forum module to replace it
                                                    .withAttribute("hx-swap", "outerHTML") // Replace the entire module, not just inner content
                                                    .withAttribute("hx-on::after-request", "this.reset()") // Reset form
                                                    .withClass("form-layout")
                                                    .withChild(createInput("content", "Write a post..."))
                                                    .withChild(new HtmlTag("button")
                                                            .withAttribute("type", "submit")
                                                            .withClass("btn btn-success")
                                                            .withInnerText("Submit Post")
                                                    )
                                    )
                    );
                })

                .build()
                .render();
    }

    private io.mindspice.jhf.core.Component createInput(String name, String label) {
        return new HtmlTag("div").withClass("form-group")
                .withChild(new HtmlTag("label").withInnerText(label))
                .withChild(new HtmlTag("input")
                        .withAttribute("type", "text")
                        .withAttribute("name", name)
                        .withAttribute("class", "form-control")
                );
    }

    // --- Helper Methods for Rendering Templates ---

    public static String renderCard(String title, String body) {
        return CARD_TEMPLATE.render(
                RenderContext.builder()
                        .with(CARD_TITLE, title)
                        .with(CARD_BODY, body)
                        .build()
        );
    }

    public static String renderList(List<String> items) {
        io.mindspice.jhf.core.Component itemsComponent = new io.mindspice.jhf.core.Component() {
            @Override
            public String render(RenderContext context) {
                StringBuilder sb = new StringBuilder();
                for (String item : items) {
                    sb.append(new HtmlTag("li").withClass("list-group-item").withInnerText(item).render(context));
                }
                return sb.toString();
            }
        };
        return LIST_TEMPLATE.render(RenderContext.builder().with(LIST_CONTENT, itemsComponent).build());
    }

    public static String renderTable(String col1, String col2, String col3) {
        io.mindspice.jhf.core.Component rowComponent = new HtmlTag("tr")
                .withChild(new HtmlTag("td").withInnerText(col1))
                .withChild(new HtmlTag("td").withInnerText(col2))
                .withChild(new HtmlTag("td").withInnerText(col3));

        return TABLE_TEMPLATE.render(RenderContext.builder().with(TABLE_BODY, rowComponent).build());
    }

    // --- Helper Methods for Forum ---

    public static io.mindspice.jhf.core.Component renderForumModule(List<ForumPost> posts) {
        ForumModule module = ForumModule.create()
                .withModuleId("forum-module")
                .withTitle("Recent Posts");

        for (ForumPost post : posts) {
            module.addPost(post);
        }
        return module;
    }

    public static List<ForumPost> getInitialPosts() {
        List<ForumPost> posts = new ArrayList<>();
        posts.add(ForumPost.create()
                .withAuthor("User123")
                .withTimestamp("2 hours ago")
                .withTitle("Welcome!")
                .withContent("This is a randomly generated initial post.")
                .withLikes(5));
        return posts;
    }
}
