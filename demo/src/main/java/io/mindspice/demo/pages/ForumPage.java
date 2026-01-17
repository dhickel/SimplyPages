package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.forum.*;
import io.mindspice.simplypages.layout.*;
import io.mindspice.simplypages.modules.*;
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
                                    .withAuthor("DataMaster420")
                                    .withTimestamp("2 hours ago")
                                    .withTitle("Best practices for quality control stage?")
                                    .withContent("I'm looking for recommendations on the best practices to use during the quality control stage. Currently using a basic process but wondering if there are better options out there.")
                                    .withReplies(12)
                                    .withLikes(8))
                            .addPost(ForumPost.create()
                                    .withAuthor("ScienceResearcher")
                                    .withTimestamp("5 hours ago")
                                    .withTitle("New study on data analysis methods")
                                    .withContent("Just read an interesting study about how different analysis methods affect research outcomes. Would love to discuss the findings with the community!")
                                    .withReplies(23)
                                    .withLikes(45))
                            .addPost(ForumPost.create()
                                    .withAuthor("NewResearcher2024")
                                    .withTimestamp("1 day ago")
                                    .withTitle("Laboratory vs Field research - pros and cons")
                                    .withContent("Starting my first research project and trying to decide between laboratory and field research. What are the main advantages of each approach? I have resources for both but need help deciding.")
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
                                    .withAuthor("LabResearcher")
                                    .withTimestamp("10 minutes ago")
                                    .withContent("Laboratory research gives you complete control over environment, conditions, temperature, and variables. Perfect for year-round data collection.")
                                    .withDepth(0))
                            .addComment(Comment.create()
                                    .withAuthor("BudgetResearcher")
                                    .withTimestamp("8 minutes ago")
                                    .withContent("True, but don't forget about the equipment costs! My laboratory setup tripled my research budget.")
                                    .withDepth(1))
                            .addComment(Comment.create()
                                    .withAuthor("GrantResearcher")
                                    .withTimestamp("5 minutes ago")
                                    .withContent("You can offset costs with grants and partnerships. I secured research funding and my costs dropped significantly.")
                                    .withDepth(2))
                            .addComment(Comment.create()
                                    .withAuthor("FieldResearcher")
                                    .withTimestamp("3 minutes ago")
                                    .withContent("Field research is much more realistic and cost-effective if you have good location. Natural conditions!")
                                    .withDepth(0));

                    row.withChild(ContentModule.create()
                            .withTitle("Discussion Thread Example")
                            .withCustomContent(comments));
                })

                .build();

        return page.render();
    }
}
