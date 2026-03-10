[Previous](04-editing-workflows-owner-user-approval.md) | [Index](../INDEX.md)

# Forum Renderer Implementation and Customization

This guide defines the recommended integration model for forum rendering.

## What This Solves

The forum renderer stack provides one opinionated pipeline for:

- category list rendering
- topic list rendering
- comment thread rendering
- inline token hydration (`[[key::value]]`) with batch resolver lookup
- per-item action rendering (`quote`, `edit`, `delete`, custom)
- topic/comment pagination controls with HTMX previous/next wiring

## Design Rules

- Source types are data-only (`ForumCategoryData`, `ForumTopicData`, `ForumCommentData`).
- Final component assembly happens through fluent component interfaces (`withX(...)`).
- Component instances are created by suppliers (`MyTopicComponent::create`).
- Tag parsing/resolution is internal per render call; no renderer-held mutable intermediate state.
- Unresolved tags remain visible as literal tokens.

## Quick Start

```java
import io.mindspice.simplypages.components.forum.actions.DefaultForumActionProvider;
import io.mindspice.simplypages.components.forum.ForumCollapsibleComposer;
import io.mindspice.simplypages.components.forum.comments.DefaultForumCommentComponent;
import io.mindspice.simplypages.components.forum.comments.ForumCommentData;
import io.mindspice.simplypages.components.forum.comments.ForumCommentRenderer;
import io.mindspice.simplypages.components.forum.tags.ForumTagResolverRegistry;
import io.mindspice.simplypages.components.forum.tags.ForumTagResolvers;
import io.mindspice.simplypages.components.forum.topics.DefaultForumTopicComponent;
import io.mindspice.simplypages.components.forum.topics.ForumTopicData;
import io.mindspice.simplypages.components.forum.topics.ForumTopicRenderer;
import io.mindspice.simplypages.components.forum.topics.ForumTopicTitleLink;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.List;
import java.util.Map;

record TopicDto(String id, String title, String body, String author, String timestamp, Integer likes, Integer replies)
    implements ForumTopicData {}
record CommentDto(String id, String topicId, String parentId, int depth, String body, String author, String avatarUrl, String timestamp, Integer likes, Integer replies)
    implements ForumCommentData {}
record Viewer(String userId, boolean moderator) {}

ForumTagResolverRegistry tagRegistry = ForumTagResolverRegistry.withBuiltIns()
    .register(ForumTagResolvers.of("product.card", tags -> tags.stream().collect(java.util.stream.Collectors.toMap(
        t -> t,
        t -> new HtmlTag("span").withAttribute("class", "product-chip").withInnerText("Product #" + t.value())
    ))));

DefaultForumActionProvider<TopicDto, Viewer> topicActions = DefaultForumActionProvider.<TopicDto, Viewer>create()
    .showEditWhen(ctx -> ctx.context() != null && ctx.context().moderator());

ForumTopicRenderer<TopicDto, Viewer> topicRenderer = ForumTopicRenderer.<TopicDto, Viewer>builder()
    .withResolverRegistry(tagRegistry)
    .withActionProvider(topicActions)
    .withBodyTextResolver((topic, viewerCtx) -> topic.body()) // replace with preview text when needed
    .withTitleLinkResolver((topic, viewerCtx) -> ForumTopicTitleLink.htmx(
        "/forum?view=comments&topic=" + topic.id(),
        "/forum/topics/" + topic.id() + "/comments",
        "#forum-main",
        "outerHTML",
        "/forum?view=comments&topic=" + topic.id()
    ))
    .withTopicComponentSupplier(DefaultForumTopicComponent::create)
    .build();

Viewer viewer = new Viewer("u-1", true);

Component topics = topicRenderer.render(List.of(
    new TopicDto("topic-1", "Release", "Use [[quote::comment-42]] and [[product.card::abc]].", "alice", "2026-03-09", 3, 10)
), viewer);

ForumCommentRenderer<CommentDto, Viewer> commentRenderer = ForumCommentRenderer.<CommentDto, Viewer>builder()
    .withResolverRegistry(tagRegistry)
    .withCommentComponentSupplier(DefaultForumCommentComponent::create)
    .build();

Component comments = commentRenderer.render(List.of(
    new CommentDto("comment-42", "topic-1", null, 0, "See [[mention::sam]]", "bob", null, "2m ago", 1, null)
), viewer);
```

## Pagination

Topic pagination:

```java
Component pagedTopics = topicRenderer.render(
    loadedTopics,
    viewer,
    new ForumTopicRenderer.TopicPagination("cat-1", requestedPage, topicsPerPage, totalTopics)
);
```

Comment pagination:

```java
Component pagedComments = commentRenderer.render(
    loadedComments,
    viewer,
    new ForumCommentRenderer.CommentPagination("topic-1", requestedPage, commentsPerPage, totalComments)
);
```

Collapsible composer boxes (default collapsed):

```java
Component newTopicForm = buildTopicForm();   // application-owned form component
Component newCommentForm = buildCommentForm();

Component topicComposer = ForumCollapsibleComposer.create("New Topic", newTopicForm);
Component commentComposer = ForumCollapsibleComposer.create("New Comment", newCommentForm);

// Render topic composer above the topic list and comment composer below comment list.
// Expand explicitly when handling edit-in-progress states.
Component editTopicComposer = ForumCollapsibleComposer
    .create("Edit Topic", newTopicForm)
    .expandedByDefault();
```

Behavior:

- renderer limits output to requested page window
- renderer appends previous/next controls + page status
- controls enable/disable from computed bounds
- enabled controls emit `hx-get` with configured endpoint resolver
- enabled controls tag `data-sp-scroll-top="target"` so HTMX swaps return to the top of the rendered forum fragment

### Pagination Data Ownership

The renderer does not load data. Your application owns:

- scoped filtering (`categoryId` / `topicId`)
- total count calculation from the scoped dataset
- requested page and page size parsing/validation

`topicsPerPage` and `commentsPerPage` are strict render limits per request.

Example:

- `requestedPage=2`, `topicsPerPage=10` renders at most 10 items (`index 10..19`) from the scoped list.
- `totalTopics` must be the count of all scoped topics, not just the current page slice.

Important:

- `ForumTopicRenderer.TopicPagination.scopeId` is a routing key for pagination controls; topic filtering is application-owned.
- `ForumCommentRenderer.CommentPagination.topicId` is both a routing key and an in-render topic filter.

How many posts/comments render per response:

- `topicsPerPage` is the strict maximum number of topic cards rendered by `ForumTopicRenderer` for one request.
- `commentsPerPage` is the strict maximum number of comment cards rendered by `ForumCommentRenderer` for one request.
- If totals are larger than page size, the remainder is available through pagination controls.
- If requested page is out of range, renderers clamp to the last valid page.
- Preview/body length is separate from pagination size. Use `withBodyTextResolver(...)` when topic list pages should render snippets instead of full body text.

Optional topic scope filtering:

```java
ForumTopicRenderer<TopicDto, Viewer> scopedRenderer = ForumTopicRenderer.<TopicDto, Viewer>builder()
    .withTopicScopeExtractor(topic -> topic.id().split(":")[0]) // should match TopicPagination.scopeId
    .build();
```

## Tag Hydration

Pipeline:

1. Fast check (`contains("[[")`) to skip unnecessary parse work.
2. Parse body into text/tag segments.
3. Batch collect unique tags by `TagType`.
4. Resolve once per type through `ForumTagResolverRegistry`.
5. Compose final body component in-order (markdown text + resolved tag components).

Important:

- no raw HTML token replacement
- unresolved tags render as visible literals
- malformed tokens are preserved as text

## Actions

Use `ForumActionProvider<SOURCE, CTX>` for item actions.

- Context-driven permissions belong here.
- Final component interfaces receive precomputed action components only.
- Visibility in rendered output is not authorization; controllers must validate permissions.
- To set default HTMX response placement on generated buttons, use
  `DefaultForumActionProvider.withHxTarget(...)` and `withHxSwap(...)`.
- To preserve in-progress composer text when using quote actions, set
  `DefaultForumActionProvider.withQuoteHxInclude(...)` to include the active
  composer textarea/input selector in quote requests.

## Demo Pipeline Reference (`/forum`)

The demo module includes a full in-memory pipeline wired with these endpoints:

- `GET /forum` full page render (no sidebar shell)
- `GET /forum/topics` topic fragment refresh + pagination
- `GET /forum/topics/{topicId}/comments` thread fragment refresh + pagination
- `POST /forum/viewer` temporary session viewer identity
- `POST /forum/topics/create|{id}/quote|{id}/edit|{id}/update|{id}/delete`
- `POST /forum/comments/create|{id}/quote|{id}/edit|{id}/update|{id}/delete`

Recommended wiring pattern used in demo:

- staged UX: `view=categories|topics|comments` keeps one forum level visible at a time
- drill-down path: category card -> topic list -> thread comments
- renderers target one fragment root (for example `#forum-main`) with `outerHTML` swaps
- action endpoints return refreshed fragments, not raw status payloads
- ownership/moderation checks run in controller/service before update/delete

## Testing Checklist

- Tag resolution and unresolved literal fallback
- Batch resolver invocation count per type
- Topic/comment pagination controls and HTMX attributes
- Action rendering with context-sensitive predicates
- Required ID validation failures for topics/comments
