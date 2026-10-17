[How-to index](README.md) | [Learning path](../getting-started/README.md)

# Build a read-only forum browser

Add a paginated topic list and topic-detail pages with paginated comments. This
recipe deliberately implements browsing only: every displayed link has a real
GET handler, and there are no pretend create, edit, or delete controls.

## Prerequisites

- An existing Spring MVC application running on JDK 25 with SimplyPages **1.1.0a**
  installed locally; follow [Getting started](../getting-started/README.md) and
  [serve pages and fragments](serve-pages-and-fragments.md) first.
- Spring static-resource handling serving framework CSS/JavaScript and the HTMX
  WebJar linked by `ShellBuilder`.
- An application mounted at `/` for these root-relative URLs.

The example publishes a small, fixed set of application-owned discussion data.
It has no persistence or user accounts. Use public content only; private forums
must authorize both page and fragment requests before rendering.

## 1. Define the data and wire the browsing routes

Place `ReadOnlyForumController.java` below your application's component-scan
root. The package used throughout these how-tos is `com.example.app.guides`;
change it consistently to match your application's root package.

The nested records implement the actual framework data contracts. Renderers
consume these records directly, without a framework `ForumService` or
`ForumModule` (neither is provided).

```java
package com.example.app.guides;

import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.components.forum.comments.ForumCommentData;
import io.mindspice.simplypages.components.forum.comments.ForumCommentRenderer;
import io.mindspice.simplypages.components.forum.topics.ForumTopicData;
import io.mindspice.simplypages.components.forum.topics.ForumTopicRenderer;
import io.mindspice.simplypages.components.forum.topics.ForumTopicTitleLink;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ReadOnlyForumController {
    record Topic(String id, String title, String body, String author)
        implements ForumTopicData {}
    record Comment(String id, String topicId, String parentId, int depth,
                   String body, String author) implements ForumCommentData {}

    private static final String BASE = "/guide/forum";
    private static final int PAGE_SIZE = 2;
    private final List<Topic> topics = List.of(
        new Topic("welcome", "Welcome", "Introduce your project here.", "Alex"),
        new Topic("markdown", "Writing posts", "Use **Markdown** for emphasis.", "Sam"),
        new Topic("links", "Useful links", "Keep links relevant to the discussion.", "Jo")
    );
    private final List<Comment> comments = List.of(
        new Comment("c1", "welcome", null, 0, "I am building a reading list.", "Sam"),
        new Comment("c2", "welcome", "c1", 1, "A shared list sounds useful.", "Alex"),
        new Comment("c3", "welcome", null, 0, "I am documenting our tools.", "Jo"),
        new Comment("c4", "markdown", null, 0, "Code fences help too.", "Alex")
    );
    private final ForumTopicRenderer<Topic, Void> topicRenderer =
        ForumTopicRenderer.<Topic, Void>builder()
            .withTitleLinkResolver((topic, context) -> ForumTopicTitleLink.htmx(
                topicPath(topic.id()), topicPath(topic.id()),
                "#forum-view", "outerHTML", "true"))
            .withPaginationEndpointResolver((scope, page, size) -> BASE + "?page=" + page)
            .withPaginationHxTarget("#forum-view")
            .withPaginationHxSwap("outerHTML")
            .build();
    private final ForumCommentRenderer<Comment, Void> commentRenderer =
        ForumCommentRenderer.<Comment, Void>builder()
            .withPaginationEndpointResolver((topicId, page, size) ->
                topicPath(topicId) + "?page=" + page)
            .withPaginationHxTarget("#forum-view")
            .withPaginationHxSwap("outerHTML")
            .build();

    @GetMapping(value = BASE, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> browse(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        if (page < 1) {
            return respond(400, "Topics", text("Page must be positive."), hxRequest);
        }
        Component list = topicRenderer.render(topics, null,
            new ForumTopicRenderer.TopicPagination("all", page, PAGE_SIZE, topics.size()));
        return respond(200, "Topics", list, hxRequest);
    }

    @GetMapping(value = BASE + "/topics/{topicId}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> detail(
        @PathVariable("topicId") String topicId,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        Topic topic = topics.stream().filter(t -> t.id().equals(topicId))
            .findFirst().orElse(null);
        if (topic == null) {
            return respond(404, "Not found", text("Topic not found."), hxRequest);
        }
        if (page < 1) {
            return respond(400, topic.title(), text("Page must be positive."), hxRequest);
        }
        List<Comment> scoped = comments.stream()
            .filter(c -> c.topicId().equals(topicId)).toList();
        HtmlTag thread = new HtmlTag("div")
            .withChild(new HtmlTag("a").withAttribute("href", BASE)
                .withAttribute("hx-get", BASE)
                .withAttribute("hx-target", "#forum-view")
                .withAttribute("hx-swap", "outerHTML")
                .withInnerText("Back to topics"))
            .withChild(topicRenderer.render(List.of(topic), null))
            .withChild(new HtmlTag("h2").withInnerText("Comments"))
            .withChild(commentRenderer.render(scoped, null,
                new ForumCommentRenderer.CommentPagination(
                    topicId, page, PAGE_SIZE, scoped.size())));
        return respond(200, topic.title(), thread, hxRequest);
    }

    private ResponseEntity<String> respond(
        int status, String title, Component content, String hxRequest
    ) {
        HtmlTag view = new HtmlTag("div").withId("forum-view")
            .withAttribute("hx-push-url", "true")
            .withChild(new HtmlTag("h1").withInnerText(title))
            .withChild(content);
        String body = "true".equals(hxRequest) ? view.render()
            : ShellBuilder.create().withPageTitle(title).withContent(view).build();
        return ResponseEntity.status(status).header("Vary", "HX-Request")
            .contentType(MediaType.TEXT_HTML).body(body);
    }

    private static Component text(String message) {
        return new HtmlTag("p").withInnerText(message);
    }

    private static String topicPath(String id) {
        return BASE + "/topics/" + id;
    }
}
```

## 2. Check the page and fragment contract

Start your application and open `/guide/forum`.

1. Confirm two topics appear and Next loads the third topic. Pagination targets
   `#forum-view` with `outerHTML`; every successful response recreates that ID.
2. Open **Welcome**. The URL is `/guide/forum/topics/welcome`. The topic and its
   first two comments appear, including the indented reply to `c1`.
3. Follow the comment Next control. The third Welcome comment appears; the
   comment for **Writing posts** must not leak into this response.
4. Follow Back to topics, then directly reload a topic URL. Both navigation and
   direct requests should work. Direct requests get a full shell with assets;
   exact `HX-Request: true` requests get only the matching view wrapper.
5. Inspect responses for `Vary: HX-Request` and a single `id="forum-view"`.
   Check `/guide/forum/topics/missing` for 404 and `?page=0` for 400. HTMX leaves
   the current view unchanged on these error responses by default.

For direct Java smoke checks, construct `new ReadOnlyForumController()` and call
`browse(1, null)`, `browse(2, "true")`, `detail("welcome", 1, "true")`, and
`detail("welcome", 2, "true")`. These return 200 and exercise the real handler
rendering. `detail("missing", 1, null)` returns 404; `browse(0, null)` returns 400.
Noninteger HTTP page parameters are rejected by Spring before the handler.

## 3. Preserve ordering and pagination when changing the data source

These renderers receive **flat, ordered collections**. They do not fetch data,
sort chronologically, discover replies, or build a tree from `parentId`.
Here the application orders `c1` before its child `c2` and supplies depth 1 for
the child. `parentId` records the relationship; `depth` controls indentation.
Choose stable database ordering and calculate both values in your application.
A paginated flat list can split a parent and reply across pages.

This example passes the full ordered collection for each scope; the renderer
slices it once. Do not pass only an already-paged database slice into these
pagination overloads: their offset would slice it again. Counts must describe
all items in the scope, not the number visible on the current page.

- Topic scope `"all"` is a routing/metadata key, not an automatic filter. This
  controller supplies only the topics belonging to that scope. For categories,
  filter in the application or configure `withTopicScopeExtractor(...)` and
  calculate the matching total yourself.
- Comment pagination filters by `topicId` before slicing; the controller also
  filters explicitly so `totalComments` is correct.
- Both page sizes are fixed at two here. Renderers clamp an oversized positive
  page to the final valid page and cap the number of cards by the page size.

The IDs in this dataset are fixed URL-safe slugs. Encode/validate dynamic IDs
before putting them into paths; do not concatenate arbitrary user-provided IDs.
Markdown bodies use the default safe renderer, which escapes raw HTML. Keep
that default when switching to user-authored discussion content.

## 4. Add writing actions only when their endpoints exist

For a writable forum, implement authorized HTTP create/quote/edit/delete
operations and storage first. Then supply a `ForumActionProvider<Topic, Viewer>`
or `ForumActionProvider<Comment, Viewer>` using `withActionProvider(...)`.
`DefaultForumActionProvider` offers `withQuoteEndpoint(...)`,
`withEditEndpoint(...)`, `withDeleteEndpoint(...)`, visibility predicates, and
`withHxTarget(...)`/`withHxSwap(...)`. Creation uses an application-owned form
and POST handler, optionally wrapped in `ForumCollapsibleComposer`.

Follow the [forum implementation pattern](../patterns/05-forum-helper-implementation-and-customization.md)
and `demo/src/main/java/io/mindspice/demo/ForumDemoController.java` for concrete
composer/action wiring. Demo classes are not framework dependencies. Do not copy
the demo's temporary viewer-selection UI as an authentication system. Every
mutation handler must independently enforce identity, ownership/moderation,
validation, and CSRF policy; a hidden action is not endpoint authorization.

## Related material

- [How-to index](README.md)
- [Forum helper reference](../reference/forum-helper-api-reference.md)
- [HTMX endpoint and swap patterns](../patterns/03-htmx-endpoint-and-swap-patterns.md)
- [Security boundaries](../security/01-security-boundaries-and-safe-rendering.md)
