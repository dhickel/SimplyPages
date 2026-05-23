# 2026-05-23 Render Context Contracts

## Summary

Hardened custom component rendering so direct, nested, context-aware, and template render paths
preserve the same behavior.

## Changes

- Made `Code`, `Spinner`, `Paragraph`, `Modal`, and `Column` use context-aware render paths for
  custom behavior.
- Updated `Modal` body/footer rendering to pass the supplied `RenderContext` to child content.
- Updated `Column` class normalization to match the exact `col` token and run during
  context-aware rendering.
- Updated `Template` to delegate `HtmlTag` subclasses that declare custom render methods instead
  of statically compiling past their behavior.
- Added regression coverage for direct, nested, context, and template render paths.
- Updated rendering docs, component catalog notes, and affected package `AGENTS.md` guidance.

## Validation

- `./mvnw -pl simplypages -Dtest=CodeTest,SpinnerTest,BasicComponentsTest,ModalTest,ColumnTest,TemplateTest test`
- `./mvnw -pl simplypages test`
- `./mvnw -pl demo test`
