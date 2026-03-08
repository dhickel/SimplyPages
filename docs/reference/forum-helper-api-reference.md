[Previous](builders-shell-nav-banner-accountbar.md) | [Index](../INDEX.md)

# ForumHelper API Reference

This page summarizes public forum-helper related APIs and extension points.

## Core Entry Point

- `ForumHelper<CATEGORY, TOPIC, COMMENT, VIEWER>`

Factory:

- `ForumHelper.builder(CategoryAdapter, TopicAdapter, CommentAdapter)`

Render methods:

- `renderCategoriesView(Collection<CATEGORY>, VIEWER)`
- `renderTopicsView(Collection<TOPIC>, VIEWER)`
- `renderTopicsView(Collection<TOPIC>, VIEWER, TopicPagination)`
- `renderCommentsView(Collection<COMMENT>, VIEWER)`
- `renderCommentsView(Collection<COMMENT>, VIEWER, CommentPagination)`

## Required Adapter Contracts

### `CategoryAdapter<CATEGORY>`

Required:

- `id(CATEGORY)`
- `title(CATEGORY)`

Optional defaults:

- `description(CATEGORY)`
- `topicCount(CATEGORY)`

### `TopicAdapter<TOPIC>`

Required:

- `id(TOPIC)`
- `title(TOPIC)`
- `body(TOPIC)`

Optional defaults:

- `author(TOPIC)`
- `timestamp(TOPIC)`
- `likes(TOPIC)`
- `replies(TOPIC)`

### `CommentAdapter<COMMENT>`

Required:

- `id(COMMENT)`
- `topicId(COMMENT)`
- `body(COMMENT)`

Optional defaults:

- `parentId(COMMENT)`
- `author(COMMENT)`
- `timestamp(COMMENT)`
- `depth(COMMENT)`
- `avatarUrl(COMMENT)`
- `likes(COMMENT)`
- `replies(COMMENT)`

## Builder Customization Hooks

View rendering:

- `withCategoryRenderer(...)`
- `withTopicRenderer(...)`
- `withCommentRenderer(...)`

Tag system:

- `withTagParser(ForumTagParser)`
- `withResolverRegistry(ForumTagResolverRegistry)`

Action decoration:

- `withActionDecorator(ForumActionDecorator<VIEWER>)`

Composer integration:

- `withTopicComposerLauncher(Function<VIEWER, Component>)`
- `withCommentComposer(Function<VIEWER, Component>)`

Comment pagination HTMX configuration:

- `withTopicPaginationEndpointResolver(TopicPaginationEndpointResolver)`
- `withTopicsPaginationHxTarget(String)`
- `withTopicsPaginationHxSwap(String)`
- `withCommentPaginationEndpointResolver(CommentPaginationEndpointResolver)`
- `withCommentsPaginationHxTarget(String)`
- `withCommentsPaginationHxSwap(String)`

Parsing toggles:

- `parseTopicBodies(boolean)`
- `parseCommentBodies(boolean)`

## View Records Passed to Renderers

- `CategoryView<CATEGORY, VIEWER>`
- `TopicView<TOPIC, VIEWER>`
- `CommentView<COMMENT, VIEWER>`

These records carry both source object and resolved render fields.

## Comment Pagination Types

### `ForumHelper.TopicPagination`

Fields:

- `scopeId`
- `page` (1-based)
- `topicsPerPage`
- `totalTopics`

Helpers:

- `totalPages()`
- `currentPage()`
- `hasPrevious()`
- `hasNext()`

Validation:

- `scopeId` must be non-blank
- `page` and `topicsPerPage` must be `>= 1`
- `totalTopics` must be `>= 0`

### `ForumHelper.TopicPaginationEndpointResolver`

- `endpoint(String scopeId, int page, int topicsPerPage)`
- used to build `hx-get` for enabled topic `Previous`/`Next` buttons
- default endpoint: `/forum/topics?scope={scopeId}&page={page}&size={topicsPerPage}`

### `ForumHelper.CommentPagination`

Fields:

- `topicId`
- `page` (1-based)
- `commentsPerPage`
- `totalComments`

Helpers:

- `totalPages()`
- `currentPage()`
- `hasPrevious()`
- `hasNext()`

Validation:

- `topicId` must be non-blank
- `page` and `commentsPerPage` must be `>= 1`
- `totalComments` must be `>= 0`

### `ForumHelper.CommentPaginationEndpointResolver`

- `endpoint(String topicId, int page, int commentsPerPage)`
- used to build `hx-get` for enabled `Previous`/`Next` buttons
- default endpoint: `/forum/topics/{topicId}/comments?page={page}&size={commentsPerPage}`

## Tag Parsing

Type:

- `ForumTagParser`

Key details:

- token syntax: `[[key::value]]`
- escaped literal token: `\[[key::value]]`
- key normalization: lower-case dotted keys only (`a-z` and `.`)
- malformed tokens are preserved as text

Parser segment model:

- `ForumTagParser.TextSegment`
- `ForumTagParser.TagSegment`

## Resolver Contracts

### `ForumTagResolver`

- `key()`
- `resolveBatch(Set<String> values)`

### `ForumTagResolverRegistry`

Creation:

- `create()`
- `withBuiltIns()`

Registration:

- `register(ForumTagResolver)`
- `registerBuiltIns()`

Resolution:

- `resolve(key, values)`
- `resolveAll(valuesByKey)`

Duplicate normalized key registration throws `IllegalArgumentException`.

### `ForumTagResolvers` helper

- `of(key, batchResolver)`
- `quote()`
- `image()`
- `mention()`
- `link()`

## Action Decoration

### `ForumActionDecorator<VIEWER>`

- `decorate(ActionContext<VIEWER>)`
- `none()`

`ActionContext` includes:

- `itemType` (`TOPIC` or `COMMENT`)
- `itemId`
- `topicId`
- `source`
- `viewer`

### `DefaultForumActionDecorator<VIEWER>`

Visibility predicates:

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

## Composer Modules

### `ForumTopicComposerModule`

Common methods:

- `withTitle(...)`
- `withSubmitUrl(...)`
- `withTitleFieldName(...)`
- `withBodyFieldName(...)`
- `withTitlePlaceholder(...)`
- `withBodyPlaceholder(...)`
- `withSubmitLabel(...)`

### `ForumCommentComposerModule`

Common methods:

- `withTitle(...)`
- `withSubmitUrl(...)`
- `withBodyFieldName(...)`
- `withBodyPlaceholder(...)`
- `withSubmitLabel(...)`
- `withTopicId(...)`

## Primitive Compatibility

Existing primitives remain available:

- `ForumPost`
- `PostList`
- `Comment`
- `CommentThread`

Use `ForumHelper` as the primary forum rendering API and primitives for ad-hoc/manual composition when needed.
