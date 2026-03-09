[Previous](builders-shell-nav-banner-accountbar.md) | [Index](../INDEX.md)

# Forum Renderer API Reference

This page summarizes forum renderer APIs and extension points.

## Core Entry Points

- `ForumCategoryRenderer<CATEGORY extends ForumCategoryData, CTX>`
- `ForumTopicRenderer<TOPIC extends ForumTopicData, CTX>`
- `ForumCommentRenderer<COMMENT extends ForumCommentData, CTX>`

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

## Builder Customization Hooks

Component suppliers:

- `withCategoryComponentSupplier(Supplier<? extends ForumCategoryComponent>)`
- `withTopicComponentSupplier(Supplier<? extends ForumTopicComponent>)`
- `withCommentComponentSupplier(Supplier<? extends ForumCommentComponent>)`

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

Helpers:

- `totalPages()`
- `currentPage()`
- `hasPrevious()`
- `hasNext()`

### `ForumTopicRenderer.TopicPaginationEndpointResolver`

- `endpoint(String scopeId, int page, int topicsPerPage)`
- default: `/forum/topics?scope={scopeId}&page={page}&size={topicsPerPage}`

### `ForumCommentRenderer.CommentPagination`

Fields:

- `topicId`
- `page` (1-based)
- `commentsPerPage`
- `totalComments`

Notes:

- When pagination is supplied, renderer filters incoming comments by `topicId` before slicing.
- `totalComments` should represent all comments for the topic scope (not only current page slice).
- `commentsPerPage` is the hard render cap for one response. If `commentsPerPage=8`, the renderer outputs at most 8 comment items.

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
