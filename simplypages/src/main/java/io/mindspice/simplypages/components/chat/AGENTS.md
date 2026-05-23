# Chat Components Agent Guide

## Purpose
Owns chat-focused rendering contracts and default helper components.

## Owns
- Chat data contracts (`ChatMessageData`)
- Chat transport/config contracts (`ChatTransportMode`, `ChatUiConfig`)
- Transcript renderer (`ChatTranscriptRenderer`)
- Timeline transcript renderer (`TimelineTranscriptRenderer`) and generic embedded disclosure contracts
- Message component contracts/defaults (`ChatMessageComponent`, `DefaultChatMessageComponent`)

## Invariants
- Chat rendering remains composable and render-safe.
- Data ownership (history/session/networking) remains application-owned.
- Renderer instances do not store mutable per-render intermediate state.

## Do
- Keep APIs fluent and lightweight.
- Keep rendering boundaries clear from transport or persistence concerns.
- Keep embedded transcript blocks generic; do not encode tool-call, model, or persistence semantics in the component layer.
- Add tests for any output-shape or contract change.

## Do Not
- Introduce transport adapters/factory layers.
- Move controller/service logic into component contracts.

## Required Tests
- Chat renderer/component structural tests
- Module integration tests when contract changes affect `ChatModule`

## Maintenance Requirement
Keep this file updated whenever chat helper contracts or usage patterns change.

See root `AGENTS.md` for global standards.
