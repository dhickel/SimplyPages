[Previous](builders-shell-nav-banner-accountbar.md) | [Index](../INDEX.md)

# Forum Renderer API Reference

This page summarizes forum renderer APIs and extension points.

## Core Entry Points

- `ForumCategoryRenderer<CATEGORY extends ForumCategoryData, CTX>`
- `ForumTopicRenderer<TOPIC extends ForumTopicData, CTX>`
- `ForumCommentRenderer<COMMENT extends ForumCommentData, CTX>`
- `ForumCollapsibleComposer`

Factory:

- `ForumCategoryRenderer.builder()`
- `ForumTopicRenderer.builder()`
- `ForumCommentRenderer.builder()`

Render methods:

- `ForumCategoryRenderer.render(Collection<CATEGORY>, CTX)`
- `ForumTopicRenderer.render(Collection<TOPIC>, CTX)`
- `ForumTopicRenderer.render(Collection<TOPIC>, CTX, TopicPagination)`
- `ForumCommentRenderer.render(Collection<COMMENT>, CTX)`
- `ForumCommentRenderer.render(Collection<COMMENT>, CTX, CommentPagination)`

## Required Data Contracts

### `ForumCategoryData`

Required:

- `id()`
- `title()`

Optional defaults:

- `description()`
- `topicCount()`

### `ForumTopicData`

Required:

- `id()`
- `title()`
- `body()`

Optional defaults:

- `author()`
- `timestamp()`
- `likes()`
- `replies()`

### `ForumCommentData`

Required:

- `id()`
- `topicId()`
- `body()`

Optional defaults:

- `parentId()`
- `depth()`
- `author()`
- `avatarUrl()`
- `timestamp()`
- `likes()`
- `replies()`

## Final Component Contracts

### `ForumCategoryComponent`

Renderer-populated fluent methods:

- `withCategoryId(String)`
- `withTitle(String)`
- `withDescription(String)`
- `withTopicCount(Integer)`

### `ForumTopicComponent`

Renderer-populated fluent methods:

- `withTopicId(String)`
- `withTitle(String)`
- `withTitleLink(ForumTopicTitleLink)` (default no-op for custom components)
- `withAuthor(String)`
- `withTimestamp(String)`
- `withBody(Component)`
- `withActions(List<Component>)`
- `withLikes(Integer)`
- `withReplies(Integer)`

### `ForumCommentComponent`

Renderer-populated fluent methods:

- `withCommentId(String)`
- `withTopicId(String)`
- `withParentId(String)`
- `withDepth(int)`
- `withAuthor(String)`
- `withAvatarUrl(String)`
- `withTimestamp(String)`
- `withBody(Component)`
- `withActions(List<Component>)`
- `withLikes(Integer)`
- `withReplies(Integer)`

#### Default comment depth and layout

`DefaultForumCommentComponent` emits `data-depth` on every card as the exact configured
signed `int`, including zero, negative values and integer extremes. It stores the value
unchanged. A single `forum-comment-depth-0` through `forum-comment-depth-4` class controls
presentation: negative depths look like zero; depths above four look like four.
`parentId` is independent of depth. Null/blank parents omit `data-parent-id`; nonblank
values are emitted with attribute escaping.

Framework CSS uses logical-start indentation of 16px per visual level, or 8px at viewport
widths up to 920px, additionally limited to 8% of the containing inline size. Cards flow
into the remaining width. The default retains its 150x150 avatar reservation, including
blank/invalid-URL fallback, and stacks its owned layout in narrow hosts when needed.
Depth adds no inline style; ordinary host CSS can override these defaults.

Custom component suppliers receive the raw parent/depth unchanged once per selected item
and own their markup. Raw depth metadata or the common `forum-comment` class alone does
not opt custom markup into the generated depth styling. Default rendering preserves
configured state and passes the same context to body/actions exactly once. Templates
retain live default/subclass rendering; finish configuration before sharing and keep
contexts and child delegates appropriately owned.

### `ForumCollapsibleComposer`

Factory:

- `ForumCollapsibleComposer.create(String summaryText, Component content)`

Fluent methods:

- `withSummaryText(String)`
- `withContent(Component)`
- `withExpanded(boolean)`
- `expandedByDefault()`
- `withClass(String)`

Behavior:

- renders a `<details>` wrapper with a `<summary>` label and composer content region
- default state is collapsed (no `open` attribute)
- use `withExpanded(true)` or `expandedByDefault()` for edit-in-progress flows

## Builder Customization Hooks

Component suppliers:

- `withCategoryComponentSupplier(Supplier<? extends ForumCategoryComponent>)`
- `withTopicComponentSupplier(Supplier<? extends ForumTopicComponent>)`
- `withCommentComponentSupplier(Supplier<? extends ForumCommentComponent>)`

Topic content/title hooks:

- `withBodyTextResolver(BiFunction<TOPIC, CTX, String>)`
- `withTitleLinkResolver(BiFunction<TOPIC, CTX, ForumTopicTitleLink>)`

Actions:

- `withActionProvider(ForumActionProvider<..., CTX>)` on topic/comment renderers

Tags:

- `withTagParser(ForumTagParser)` on topic/comment renderers
- `withResolverRegistry(ForumTagResolverRegistry)` on topic/comment renderers

Pagination HTMX:

- Topic renderer:
  - `withPaginationEndpointResolver(TopicPaginationEndpointResolver)`
  - `withPaginationHxTarget(String)`
  - `withPaginationHxSwap(String)`
  - `withTopicScopeExtractor(Function<TOPIC, String>)`
- Comment renderer:
  - `withPaginationEndpointResolver(CommentPaginationEndpointResolver)`
  - `withPaginationHxTarget(String)`
  - `withPaginationHxSwap(String)`

## Action Contracts

### `ForumActionProvider<SOURCE, CTX>`

- `provide(ForumActionContext<SOURCE, CTX>)`
- `none()`

### `ForumActionContext<SOURCE, CTX>`

Fields:

- `itemType` (`ForumActionType.TOPIC` or `ForumActionType.COMMENT`)
- `itemId`
- `topicId`
- `source`
- `context`

### `DefaultForumActionProvider<SOURCE, CTX>`

Visibility:

- `showQuoteWhen(...)`
- `showEditWhen(...)`
- `showDeleteWhen(...)`

Endpoint mappers:

- `withQuoteEndpoint(...)`
- `withEditEndpoint(...)`
- `withDeleteEndpoint(...)`

Icon overrides:

- `withQuoteIcon(...)`
- `withEditIcon(...)`
- `withDeleteIcon(...)`

HTMX defaults:

- `withHxTarget(String)`
- `withHxSwap(String)`
- `withQuoteHxInclude(String)` (quote-action-only `hx-include` selector)

## Pagination Contracts

### `ForumTopicRenderer.TopicPagination`

Fields:

- `scopeId`
- `page` (1-based)
- `topicsPerPage`
- `totalTopics`

Notes:

- `scopeId` is used for pagination metadata and endpoint generation.
- Topic filtering is application-owned by default.
- If `withTopicScopeExtractor(...)` is configured, renderer filters to matching `scopeId` before page slicing.
- `totalTopics` should represent all scoped topics (not only current page slice).
- `topicsPerPage` is the hard render cap for one response. If `topicsPerPage=8`, the renderer outputs at most 8 topic items.
- `topicsPerPage` controls topic-card count only; it does not control body preview length.
- Enabled pagination controls include `data-sp-scroll-top="target"` for target-fragment top reset after HTMX swaps.

Helpers:

- `totalPages()`
- `currentPage()`
- `hasPrevious()`
- `hasNext()`

### `ForumTopicRenderer.TopicPaginationEndpointResolver`

- `endpoint(String scopeId, int page, int topicsPerPage)`
- default: `/forum/topics?scope={scopeId}&page={page}&size={topicsPerPage}`

### `ForumTopicTitleLink`

Fields:

- `href` (required)
- `hxGet` (optional)
- `hxTarget` (optional)
- `hxSwap` (optional)
- `hxPushUrl` (optional)

Factories:

- `ForumTopicTitleLink.href(String href)`
- `ForumTopicTitleLink.htmx(String href, String hxGet, String hxTarget, String hxSwap, String hxPushUrl)`

### `ForumCommentRenderer.CommentPagination`

Fields:

- `topicId`
- `page` (1-based)
- `commentsPerPage`
- `totalComments`

Notes:

- Without pagination, comments retain flat collection order, including comments from different topics.
- When pagination is supplied, renderer filters incoming comments by `topicId` before slicing.
- Parent/depth metadata does not reorder or group comments. A child can precede its parent or appear on a different page; missing, self, cyclic and cross-topic parent references are not reconciled or validated. There is no tree DOM or subtree pagination.
- `totalComments` should represent all comments for the topic scope (not only current page slice).
- `commentsPerPage` is the hard render cap for one response. If `commentsPerPage=8`, the renderer outputs at most 8 comment items.
- Enabled pagination controls include `data-sp-scroll-top="target"` for target-fragment top reset after HTMX swaps.

Helpers:

- `totalPages()`
- `currentPage()`
- `hasPrevious()`
- `hasNext()`

### `ForumCommentRenderer.CommentPaginationEndpointResolver`

- `endpoint(String topicId, int page, int commentsPerPage)`
- default: `/forum/topics/{topicId}/comments?page={page}&size={commentsPerPage}`

## Tag Contracts

### `TagType`

- normalized/validated key wrapper
- use `TagType.of("custom.tag")`

### `Tag`

- value token (`TagType` + token value)

### `ForumTagParser`

- token syntax: `[[key::value]]`
- escaped literal token: `\[[key::value]]`
- malformed tokens are preserved as text

### `ForumTagResolver`

- `tagType()`
- `resolveBatch(Set<Tag>)`

### `ForumTagResolverRegistry`

Creation:

- `create()`
- `withBuiltIns()`

Registration:

- `register(ForumTagResolver)`
- `registerBuiltIns()`

Resolution:

- `resolve(TagType, Set<Tag>)`
- `resolveAll(Map<TagType, Set<Tag>>)`
- `hasResolver(TagType)`

### `ForumTagResolvers`

- `of(key, batchResolver)`
- built-ins: `quote()`, `image()`, `mention()`, `link()`

## Defaults

- Default final components:
  - `DefaultForumCategoryComponent`
  - `DefaultForumTopicComponent`
  - `DefaultForumCommentComponent`
- All renderer builders default to these component suppliers.
