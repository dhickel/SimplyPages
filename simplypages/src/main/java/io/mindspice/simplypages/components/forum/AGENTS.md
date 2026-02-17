# Forum Components Agent Guide

## Purpose
Owns discussion/community primitive components.

## Owns
- `ForumPost`, `PostList`, `Comment`, `CommentThread`

## Invariants
- Forum component structure remains composable and render-safe.
- Markdown/text content handling remains aligned with core escaping rules.
- List/thread ordering behavior is predictable.

## Do
- Keep components generic enough for multiple forum-like domains.
- Preserve fluent builders for post/comment metadata.
- Add tests for any structure or metadata output changes.

## Do Not
- Embed moderation/business workflow logic in component layer.
- Depend on demo controller behavior.

## Common Pitfalls
- Breaking nested comment/thread rendering structure.
- Mixing trusted/raw HTML without explicit intent.
- Drift between forum CSS classes and generated markup.

## Required Tests
- Existing forum component test suite and any new render-path tests
- Regression tests for hierarchy/nesting and metadata fields

## Dependencies
- Depends on `core` and base/shared components.
- Keep independent from service/controller concerns.

## Maintenance Requirement
Keep this file updated whenever forum component structure or rendering contracts change.

See root `AGENTS.md` for global standards.

Update this file in the same change whenever package-level behavior or conventions drift.
