---
title: Module Lifecycle Contracts That Scale
summary: Why `build()` and `buildContent()` boundaries make modules easier to reason about.
date: 2026-03-09T14:10
author: Module Guild
tags: modules,lifecycle,design
slug: module-lifecycle-build-contract
---
# Module Lifecycle Contracts That Scale

SimplyPages modules are built lazily and idempotently through the module lifecycle contract.

## Structure in buildContent

`buildContent()` should describe structure and composition, not request-time mutation logic.

### Keep Dynamic State External

If values change per request, favor templates and context values over rebuilding module internals.

#### Integration Benefit

This keeps modules reusable across full-page render and HTMX partial render paths.

## Layout Owns Width Decisions

Module width methods are intentionally blocked; layout containers handle sizing.
