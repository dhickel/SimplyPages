---
title: HTMX Endpoint Patterns with SimplyPages
summary: Small server endpoints, targeted swaps, and clear ownership of fragment boundaries.
date: 2026-03-08T12:00
author: HTMX Crew
tags: htmx,endpoints,fragments
slug: htmx-fragment-patterns
---
# HTMX Endpoint Patterns with SimplyPages

HTMX works best when each endpoint returns a clear fragment target with minimal side effects.

## Targeted Fragment Updates

Use endpoint-level rendering for specific module or container updates, not whole-page rebuilds.

### Keep Routing Intentional

Each fragment route should map to one visible swap responsibility to reduce coupling.

#### OOB Updates Where Needed

Use out-of-band updates for coordinated UI state changes across multiple page regions.

## Works with Existing Controllers

Controllers can return full shell for normal requests and fragments for `HX-Request` calls.
