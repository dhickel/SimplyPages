# Media Components Agent Guide

## Purpose
Owns media rendering components for images, audio, video, and galleries.

## Owns
- `Gallery`, `Audio`, `Video`

## Invariants
- Media component HTML output remains standards-compliant.
- Gallery composition and child ordering behavior stays stable.
- Attribute rendering remains escaped/safe by core rules.

## Do
- Keep APIs focused on media configuration and layout hints.
- Preserve fluent chain consistency with other components.
- Add coverage for new media attributes and rendering variants.

## Do Not
- Add transport/storage logic for media files here.
- Hardcode app-specific URLs or media backends.

## Common Pitfalls
- Breaking gallery item ordering.
- Inconsistent defaults for autoplay/controls attributes.
- Introducing assumptions about static resource hosting.

## Required Tests
- `GalleryTest`, `AudioTest`, `VideoTest`
- Regression tests for any new media configuration API

## Dependencies
- Depends on `core` and shared components.
- Avoid coupling to `modules` or demo controllers.

## Maintenance Requirement
Keep this file updated whenever media component behavior or defaults change.

See root `AGENTS.md` for global standards.

Update this file in the same change whenever package-level behavior or conventions drift.
