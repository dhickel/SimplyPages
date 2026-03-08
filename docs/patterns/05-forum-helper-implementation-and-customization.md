[Previous](04-editing-workflows-owner-user-approval.md) | [Index](../INDEX.md)

# ForumHelper Implementation and Customization

This guide defines the recommended integration model for `ForumHelper` and related forum classes.

## What This Solves

`ForumHelper` gives you one public forum rendering surface for:

- category list views
- topic list/thread views
- comment thread views
- inline token hydration (`[[key::value]]`) for quote/image/mention/link and custom tags
- per-item action annotations (`quote`, `edit`, `delete`, or custom)
- topic/comment composer integration

It is designed to keep rendering deterministic while allowing application-owned data, policy, and endpoint logic.

## Boundary Contract (Framework vs App)

Framework owns:

- rendering structure and output contracts
- token parsing, batching, and resolver orchestration
- extension points (adapters, renderers, decorators, resolvers)
- default helper markup, built-in token resolvers, and composer module primitives

Application owns:

- data loading and persistence
- authorization/moderation policy
- endpoint/controller implementation
- meaning of custom token keys and resolver lookup logic

Important rule:
Do not perform ad-hoc data fetches inside render methods. Use batch resolver APIs.

## Quick Start (End-to-End)

```java
import io.mindspice.simplypages.components.forum.*;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.List;
import java.util.Map;
import java.util.Set;

record CategoryDto(String id, String title, String description, int topicCount) {}
record TopicDto(String id, String title, String body, String author, String timestamp, Integer likes, Integer replies) {}
record CommentDto(String id, String topicId, String parentId, int depth, String body, String author, String avatarUrl, String timestamp) {}
record Viewer(String userId, boolean loggedIn, boolean moderator) {}

ForumTagResolverRegistry tagRegistry = ForumTagResolverRegistry.withBuiltIns()
    .register(ForumTagResolvers.of("product.card", values -> {
        // Batch lookup (DB/API) should happen once per key
        return values.stream().collect(java.util.stream.Collectors.toMap(
            v -> v,
            v -> new HtmlTag("span")
                .withAttribute("class", "product-chip")
                .withInnerText("Product #" + v)
        ));
    }));

DefaultForumActionDecorator<Viewer> actions = DefaultForumActionDecorator.<Viewer>create()
    .showQuoteWhen(ctx -> ctx.viewer() != null && ctx.viewer().loggedIn())
    .showEditWhen(ctx -> {
        if (ctx.viewer() == null || !ctx.viewer().loggedIn()) return false;
        // source is your original topic/comment object from adapter pipeline
        return true;
    })
    .showDeleteWhen(ctx -> ctx.viewer() != null && ctx.viewer().moderator())
    .withQuoteEndpoint(ctx -> "/forum/comments/" + ctx.itemId() + "/quote")
    .withEditEndpoint(ctx -> "/forum/" + (ctx.itemType() == ForumActionDecorator.ItemType.TOPIC ? "topics" : "comments") + "/" + ctx.itemId() + "/edit")
    .withDeleteEndpoint(ctx -> "/forum/" + (ctx.itemType() == ForumActionDecorator.ItemType.TOPIC ? "topics" : "comments") + "/" + ctx.itemId() + "/delete");

ForumHelper<CategoryDto, TopicDto, CommentDto, Viewer> helper = ForumHelper
    .builder(
        new ForumHelper.CategoryAdapter<>() {
            public String id(CategoryDto c) { return c.id(); }
            public String title(CategoryDto c) { return c.title(); }
            public String description(CategoryDto c) { return c.description(); }
            public Integer topicCount(CategoryDto c) { return c.topicCount(); }
        },
        new ForumHelper.TopicAdapter<>() {
            public String id(TopicDto t) { return t.id(); }
            public String title(TopicDto t) { return t.title(); }
            public String body(TopicDto t) { return t.body(); }
            public String author(TopicDto t) { return t.author(); }
            public String timestamp(TopicDto t) { return t.timestamp(); }
            public Integer likes(TopicDto t) { return t.likes(); }
            public Integer replies(TopicDto t) { return t.replies(); }
        },
        new ForumHelper.CommentAdapter<>() {
            public String id(CommentDto c) { return c.id(); }
            public String topicId(CommentDto c) { return c.topicId(); }
            public String body(CommentDto c) { return c.body(); }
            public String parentId(CommentDto c) { return c.parentId(); }
            public int depth(CommentDto c) { return c.depth(); }
            public String author(CommentDto c) { return c.author(); }
            public String avatarUrl(CommentDto c) { return c.avatarUrl(); }
            public String timestamp(CommentDto c) { return c.timestamp(); }
        }
    )
    .withResolverRegistry(tagRegistry)
    .withActionDecorator(actions)
    .build();

Viewer viewer = new Viewer("u-1", true, false);

Component categories = helper.renderCategoriesView(List.of(
    new CategoryDto("cat-1", "General", "Product and release discussion", 12)
), viewer);

Component topics = helper.renderTopicsView(List.of(
    new TopicDto("topic-1", "Release Notes", "Use [[quote::comment-42]] and [[product.card::abc]].", "alice", "2026-03-08", 3, 10)
), viewer);

Component comments = helper.renderCommentsView(List.of(
    new CommentDto("comment-42", "topic-1", null, 0, "See [[mention::sam]]", "bob", null, "2m ago")
), viewer);
```

## Topic Thread Pagination (Previous / Next)

For multi-page topic threads, use the pagination-aware comments render overload.

```java
ForumHelper<CommentCategory, CommentTopic, CommentDto, Viewer> helper = ForumHelper
    .builder(categoryAdapter, topicAdapter, commentAdapter)
    .withCommentPaginationEndpointResolver((topicId, page, size) ->
        "/forum/topics/" + topicId + "/comments?page=" + page + "&size=" + size)
    .withCommentsPaginationHxTarget("#topic-comments")
    .withCommentsPaginationHxSwap("outerHTML")
    .build();

int commentsPerPage = 25;        // app-configured page size
int requestedPage = 3;           // from request query/path
int totalComments = 113;         // app-provided count for this topic

Component paged = helper.renderCommentsView(
    loadedComments,
    viewer,
    new ForumHelper.CommentPagination("topic-1", requestedPage, commentsPerPage, totalComments)
);
```

Behavior:

- helper limits comments rendered to the requested page window
- helper appends `Previous` / `Next` controls and page status
- buttons are enabled/disabled from pagination bounds
- enabled buttons emit `hx-get` to your resolver endpoint with topic id + page + page size
- `hx-target`/`hx-swap` are framework-configurable (default target: `closest .forum-comments-view`)

Boundary:

- framework renders controls and computes button state
- application provides `totalComments`, `commentsPerPage`, current page, and server endpoint behavior

## Topic List Pagination (Categories/Forum Topic Index)

Use pagination on `renderTopicsView` when category/topic indexes span multiple pages.

```java
ForumHelper<CategoryDto, TopicDto, CommentDto, Viewer> helper = ForumHelper
    .builder(categoryAdapter, topicAdapter, commentAdapter)
    .withTopicPaginationEndpointResolver((scopeId, page, size) ->
        "/forum/topics?scope=" + scopeId + "&page=" + page + "&size=" + size)
    .withTopicsPaginationHxTarget("#topics-view")
    .withTopicsPaginationHxSwap("outerHTML")
    .build();

int topicsPerPage = 20;
int requestedPage = 2;
int totalTopics = 87;

Component pagedTopics = helper.renderTopicsView(
    loadedTopics,
    viewer,
    new ForumHelper.TopicPagination("cat-1", requestedPage, topicsPerPage, totalTopics)
);
```

Behavior:

- helper limits topic rows to the requested page window
- helper appends `Previous` / `Next` controls and status text
- buttons are enabled/disabled from page bounds
- enabled buttons emit `hx-get` via your topic endpoint resolver
- default `hx-target` is `closest .forum-topics-view`

Boundary:

- framework owns topic pagination UI rendering/state math
- application owns topic count, page size/current page, and endpoint implementation

## Required ID Rules

`ForumHelper` enforces stable IDs:

- category id is required for categories
- topic id is required for topics
- comment id and topic id are required for comments

Missing required IDs throw `IllegalArgumentException` during render.

## Token Parsing and Hydration

### Token syntax

- canonical token: `[[key::value]]`
- escaped literal token: `\[[key::value]]`
- key rules:
  - normalized to lower-case
  - allowed format: `a-z` segments separated by `.` (examples: `quote`, `image.profile`, `product.card`)

### Parser behavior

`ForumTagParser` is fault-tolerant:

- malformed tokens are preserved as plain literal text
- parse does not throw for malformed user content

### Resolution behavior

`ForumTagResolverRegistry` resolves in batches per key:

1. parse content into `TextSegment` + `TagSegment`
2. group tag values by normalized key
3. call each resolver once with the full value set
4. render resolved components inline
5. unresolved valid tags render as visible literals

This avoids N+1 lookup behavior in render paths.

### Built-ins and custom keys

Built-ins from `ForumTagResolvers`:

- `quote`
- `image`
- `mention`
- `link`

Custom keys are registered with `ForumTagResolvers.of(...)`.

Duplicate key registration fails fast.

## Action Annotation Customization

Action injection is controlled by `ForumActionDecorator<VIEWER>`.

- invoked per item (topic/comment)
- receives `ActionContext` with:
  - `itemType`
  - `itemId`
  - `topicId`
  - `source` (original object)
  - `viewer`

Use `DefaultForumActionDecorator` when you want fast defaults with policy hooks:

- visibility predicates:
  - `showQuoteWhen(...)`
  - `showEditWhen(...)`
  - `showDeleteWhen(...)`
- endpoint builders:
  - `withQuoteEndpoint(...)`
  - `withEditEndpoint(...)`
  - `withDeleteEndpoint(...)`
- icon overrides:
  - `withQuoteIcon(...)`
  - `withEditIcon(...)`
  - `withDeleteIcon(...)`

Security note:
Button visibility in rendering is not authorization. Controllers must validate permission again.

## Comment Identity Rail and Avatar Contract

Primitive `Comment` and default helper comment rendering now use a left identity rail:

- top-left: author
- below author: avatar slot (`150x150`)
- right side: timestamp, actions, comment body, footer stats

Behavior when avatar is missing or invalid:

- no image is rendered
- blank `150x150` slot remains
- layout does not collapse

This keeps comment rows visually stable across mixed avatar availability.

## Composer Integration

### Default helper behavior

- topics view includes a default "New Topic" launcher link
- comments view includes a default comment composer form

### Override entry points

Use builder hooks:

- `withTopicComposerLauncher(Function<VIEWER, Component>)`
- `withCommentComposer(Function<VIEWER, Component>)`

### Dedicated modules

Framework composer modules are available for standalone routes/pages:

- `ForumTopicComposerModule`
- `ForumCommentComposerModule`

These modules provide HTMX-ready forms and can be composed into app routes without using helper defaults.

## Renderer Replacement (Full View Customization)

Replace default category/topic/comment renderers with your own components:

- `withCategoryRenderer(...)`
- `withTopicRenderer(...)`
- `withCommentRenderer(...)`

Use this when:

- you need custom semantic tags
- you need a strict design-system structure
- you want alternate identity/stats/action placement

Keep adapters and resolver flow the same, and only swap view composition.

## Legacy Primitive Interop

You can still use primitives directly:

- `ForumPost`
- `PostList`
- `Comment`
- `CommentThread`

Recommended approach:

- use `ForumHelper` for full categories/topics/comments flows
- use primitives for one-off/manual forum UI composition

## Performance and Lifecycle Guidance

- Build helper, parser, registry, and resolver wiring at startup where possible.
- Keep resolver registry immutable after startup registration.
- Treat render as pure composition over already-available models plus batch hydration.
- Avoid mutation of shared component instances during concurrent request rendering.

## Testing Checklist for ForumHelper Integrations

For every integration change, cover:

1. adapter field mapping and required ID enforcement
2. token parser edge cases (`[[...]]`, escapes, malformed literals)
3. batch resolver invocation count and unresolved token fallback
4. action visibility/endpoint annotation output
5. avatar slot contract (`150x150` blank slot when missing)
6. composer output placement and HTMX attributes

Use selector-based `HtmlAssert` for structure and snapshots for complex composed output.
