package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Image;
import io.mindspice.simplypages.components.Markdown;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.display.*;
import io.mindspice.simplypages.components.forum.Comment;
import io.mindspice.simplypages.components.forum.CommentThread;
import io.mindspice.simplypages.components.forum.ForumPost;
import io.mindspice.simplypages.components.forum.PostList;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.media.Audio;
import io.mindspice.simplypages.components.media.Gallery;
import io.mindspice.simplypages.components.media.Video;
import io.mindspice.simplypages.components.navigation.Breadcrumb;
import io.mindspice.simplypages.components.navigation.NavBar;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.modules.ContentModule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DisplayDataDemoPage implements DemoPage {

    private record TeamMetric(String team, String uptime, String alerts) {}

    @Override
    public String render() {
        DataTable<TeamMetric> dataTable = DataTable.create(TeamMetric.class)
            .addColumn("Team", TeamMetric::team)
            .addColumn("Uptime", TeamMetric::uptime)
            .addColumn("Alerts", TeamMetric::alerts)
            .withData(List.of(
                new TeamMetric("API", "99.97%", "1"),
                new TeamMetric("Search", "99.92%", "2"),
                new TeamMetric("Edge", "99.88%", "3")
            ))
            .striped()
            .hoverable();

        Table simpleTable = Table.create()
            .withHeaders("Key", "Value")
            .addRow("Render mode", "Server-side")
            .addRow("Client JS", "Minimal")
            .bordered();

        Gallery gallery = Gallery.create()
            .withColumns(3)
            .addImage("https://picsum.photos/260/150?random=31", "sample one")
            .addImage("https://picsum.photos/260/150?random=32", "sample two")
            .addImage("https://picsum.photos/260/150?random=33", "sample three");

        NavBar navBar = NavBar.create()
            .withBrand("SimplyPages")
            .addItem("Overview", "/demos", true)
            .addItem("Display", "/demos/display-data")
            .addItem("HTMX", "/demos/htmx-editing");

        Modal modal = Modal.create()
            .withModalId("static-demo-modal")
            .withTitle("Modal Component")
            .withBody(new Paragraph("Modal supports server-rendered body/footer content and safe ID validation."))
            .withFooter(Button.create("Close").withOnClick("document.getElementById('static-demo-modal').remove()"));

        PostList postList = PostList.create().addPost(
            ForumPost.create()
                .withAuthor("maintainer")
                .withTimestamp("today")
                .withTitle("Release prep")
                .withContent("Consolidated demo structure with richer example coverage.")
                .withLikes(6)
                .withReplies(2)
        );

        CommentThread commentThread = CommentThread.create().addComment(
            Comment.create()
                .withAuthor("reviewer")
                .withContent("The grouped rows are much easier to scan.")
                .withTimestamp("1h ago")
        );

        return Page.builder()
            .addComponents(Header.H1("Display & Data"))
            .addComponents(new Markdown("""
                ## Sections
                - [Status Components](#status)
                - [Data Views](#tables)
                - [Navigation & Media](#media)
                - [Forum Components](#forum)
                """))

            .addRow(row -> row.withChild(new Div().withId("status").withChild(
                ContentModule.create()
                    .withTitle("Status Components")
                    .withContent("Display primitives grouped by responsibility instead of a single compressed row.")
            )))

            .addRow(row -> row
                .withChild(new Column().withWidth(3).withChild(section("Alerts", new Div()
                    .withChild(Alert.info("Info"))
                    .withChild(Alert.success("Healthy"))
                    .withChild(Alert.warning("Warning"))
                    .withChild(Alert.danger("Error")))))
                .withChild(new Column().withWidth(3).withChild(section("Badges + Tags", new Paragraph()
                    .withChild(Badge.info("beta"))
                    .withChild(Badge.success("stable"))
                    .withChild(Tag.create("release").withColor("success"))
                    .withChild(Tag.create("catalog").withColor("info")))))
                .withChild(new Column().withWidth(3).withChild(section("Indicators", new Div()
                    .withChild(Label.create("SSR"))
                    .withChild(new Paragraph(""))
                    .withChild(Spinner.create().small())
                    .withChild(new Paragraph(""))
                    .withChild(ProgressBar.create(72).success()))))
                .withChild(new Column().withWidth(3).withChild(section("InfoBox", InfoBox.create()
                    .withTitle("Requests")
                    .withValue("31k")
                    .withIcon("⚡")))))

            .addRow(row -> row.withChild(new Div().withId("tables").withChild(
                ContentModule.create()
                    .withTitle("Data Views")
                    .withContent("Manual and typed tables plus list-style data summaries.")
            )))

            .addRow(row -> row
                .withChild(new Column().withWidth(6).withChild(section("Table", simpleTable)))
                .withChild(new Column().withWidth(6).withChild(section("DataTable<T>", dataTable))))

            .addRow(row -> row
                .withChild(new Column().withWidth(6).withChild(section("OrderedList", OrderedList.create()
                    .addItem("Collect")
                    .addItem("Normalize")
                    .addItem("Render"))))
                .withChild(new Column().withWidth(6).withChild(section("UnorderedList", UnorderedList.create()
                    .addItem("Alert")
                    .addItem("Badge")
                    .addItem("Table")))))

            .addRow(row -> row.withChild(new Div().withId("media").withChild(
                ContentModule.create()
                    .withTitle("Navigation & Media")
                    .withContent("Navigation components, card surfaces, and media rendering examples.")
            )))

            .addRow(row -> row
                .withChild(new Column().withWidth(6).withChild(section("Breadcrumb", Breadcrumb.create()
                    .addItem("Home", "/")
                    .addItem("Demos", "/demos")
                    .addActiveItem("Display"))))
                .withChild(new Column().withWidth(6).withChild(section("NavBar", navBar))))

            .addRow(row -> row
                .withChild(new Column().withWidth(6).withChild(section("Cards", CardGrid.create().withColumns(2)
                    .addCard(Card.create().withHeader("Card A").withBody("CardGrid composition"))
                    .addCard(Card.create().withHeader("Card B").withBody("Reusable blocks")))))
                .withChild(new Column().withWidth(6).withChild(section("Modal", modal))))

            .addRow(row -> row
                .withChild(new Column().withWidth(4).withChild(section("Gallery", gallery)))
                .withChild(new Column().withWidth(4).withChild(section("Image", Image.create("https://picsum.photos/320/190?random=34", "single image"))))
                .withChild(new Column().withWidth(4).withChild(section("Video + Audio", new Div()
                    .withChild(Video.create("https://www.w3schools.com/html/mov_bbb.mp4").withControls().withWidth(280))
                    .withChild(new Paragraph(""))
                    .withChild(Audio.create("https://www.w3schools.com/html/horse.ogg").withControls())))))

            .addRow(row -> row.withChild(new Div().withId("forum").withChild(
                ContentModule.create()
                    .withTitle("Forum Components")
                    .withContent("ForumPost, PostList, and threaded comments in dedicated columns.")
            )))

            .addRow(row -> row
                .withChild(new Column().withWidth(6).withChild(section("PostList", postList)))
                .withChild(new Column().withWidth(6).withChild(section("CommentThread", commentThread))))

            .addRow(row -> row.withChild(section("Catalog Coverage", new Markdown("""
                Components covered on this page:

                Alert, Badge, Tag, Label, Spinner, ProgressBar, InfoBox, Card, CardGrid, Modal,
                Table, DataTable, OrderedList, UnorderedList, Breadcrumb, NavBar,
                Gallery, Image, Video, Audio, ForumPost, PostList, Comment, CommentThread.
                """))))
            .build()
            .render();
    }

    private ContentModule section(String title, io.mindspice.simplypages.core.Component content) {
        return ContentModule.create()
            .withTitle(title)
            .withCustomContent(content);
    }
}
