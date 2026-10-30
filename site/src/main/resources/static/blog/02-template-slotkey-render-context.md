---
title: Template, SlotKey, and RenderContext in Real Projects
summary: How to split static structure from request-time values with the built-in template system.
date: 2026-03-10T08:30
author: Render Lab
tags: templates,slotkey,rendering
slug: template-slotkey-render-context
---
# Template, SlotKey, and RenderContext in Real Projects

The template model compiles stable page structure once and injects request-specific values at render time.

## Build Static Structure Once

Create reusable templates for repeated shells and avoid mutating module build structure per request.

### Keep Dynamic Values in Context

Put runtime values in `RenderContext` using typed `SlotKey` instances for safer composition.

#### Practical Benefit

This reduces accidental cross-request mutation and keeps dynamic behavior explicit.

## Good Fit for HTMX Fragments

Template-based fragments are straightforward to return from controller endpoints.
