package io.mindspice.demo.pages;

import io.mindspice.jhf.components.*;
import io.mindspice.jhf.components.forum.*;
import io.mindspice.jhf.layout.*;
import io.mindspice.jhf.modules.*;
import org.springframework.stereotype.Component;

/**
 * Forum components page - ForumPost, PostList, Comment, CommentThread.
 */
@Component
public class ForumPage implements DemoPage {

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1("Forum & Discussion Components"))

                .addRow(row -> row.withChild(new Markdown(
                        """
                        Forum components build discussion platforms and community features.

                        ## Forum Post

                        ```java
                        ForumPost.create()
                            .withAuthor("Username")
                            .withTimestamp("2 hours ago")
                            .withTitle("Discussion Topic")
                            .withContent("Post content...")
                            .withReplies(12)
                            .withLikes(34);
                        ```
                        """)))

                .addRow(row -> {
                    ForumModule forumModule = ForumModule.create()
                            .withTitle("Recent Discussions")
                            .addPost(ForumPost.create()
                                    .withAuthor("GrowMaster420")
                                    .withTimestamp("2 hours ago")
                                    .withTitle("Best nutrients for flowering stage?")
                                    .withContent("I'm looking for recommendations on the best nutrients to use during the flowering stage. Currently using a basic NPK but wondering if there are better options out there.")
                                    .withReplies(12)
                                    .withLikes(8))
                            .addPost(ForumPost.create()
                                    .withAuthor("CannaResearcher")
                                    .withTimestamp("5 hours ago")
                                    .withTitle("New study on terpene profiles")
                                    .withContent("Just read an interesting study about how different terpene profiles affect the entourage effect. Would love to discuss the findings with the community!")
                                    .withReplies(23)
                                    .withLikes(45))
                            .addPost(ForumPost.create()
                                    .withAuthor("GreenThumb2024")
                                    .withTimestamp("1 day ago")
                                    .withTitle("Indoor vs Outdoor growing - pros and cons")
                                    .withContent("Starting my first grow and trying to decide between indoor and outdoor. What are the main advantages of each approach? I have space for both but need help deciding.")
                                    .withReplies(34)
                                    .withLikes(19));

                    row.withChild(forumModule);
                })

                // Comment Thread
                .addComponents(Header.H2("Comment Threads"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **CommentThread** displays nested discussions:

                        ```java
                        CommentThread.create()
                            .addComment(Comment.create()
                                .withAuthor("User1")
                                .withContent("Main comment")
                                .withDepth(0))
                            .addComment(Comment.create()
                                .withAuthor("User2")
                                .withContent("Reply to comment")
                                .withDepth(1));  // Indented reply
                        ```
                        """)))

                .addRow(row -> {
                    CommentThread comments = CommentThread.create()
                            .addComment(Comment.create()
                                    .withAuthor("IndoorGrower")
                                    .withTimestamp("10 minutes ago")
                                    .withContent("Indoor gives you complete control over environment, lighting, temperature, and humidity. Perfect for year-round growing.")
                                    .withDepth(0))
                            .addComment(Comment.create()
                                    .withAuthor("BudgetGrower")
                                    .withTimestamp("8 minutes ago")
                                    .withContent("True, but don't forget about the electricity costs! My indoor setup tripled my power bill.")
                                    .withDepth(1))
                            .addComment(Comment.create()
                                    .withAuthor("SolarGrower")
                                    .withTimestamp("5 minutes ago")
                                    .withContent("You can offset costs with solar panels. I installed a small solar array and my costs dropped significantly.")
                                    .withDepth(2))
                            .addComment(Comment.create()
                                    .withAuthor("OutdoorFan")
                                    .withTimestamp("3 minutes ago")
                                    .withContent("Outdoor growing is much more natural and cost-effective if you have good climate. Free sunlight!")
                                    .withDepth(0));

                    row.withChild(ContentModule.create()
                            .withTitle("Discussion Thread Example")
                            .withCustomContent(comments));
                })

                .build();

        return page.render();
    }
}
