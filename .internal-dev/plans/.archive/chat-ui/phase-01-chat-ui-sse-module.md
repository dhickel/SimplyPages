# Context
We needed a chat UI implementation aligned to SimplyPages SSR conventions, with framework-level pluggable rendering contracts and a demo smoke-test page at `/chat`.

# Goal
Implement an embeddable `ChatModule` and forum-style chat helper contracts, wire a demo `/chat` page, and support SSE notify + HTMX transcript refresh.

# In Scope
- Framework chat contracts (`components/chat`) and `ChatModule`
- Demo chat page/controller/service and SSE endpoint
- Top-nav and demo-nav exposure
- Tests, docs sync, AGENTS sync, live deployment verification

# Out of Scope
- Production WebSocket runtime handlers
- Persistent chat storage backend
- Agent orchestration/authorization policies beyond demo scope

# Implementation Steps
1. Add `components/chat` contracts and transcript renderer.
2. Add `modules/ChatModule` with transport hook attributes.
3. Add `/chat` demo route, page composition, in-memory service, and SSE endpoint.
4. Add `/js/chat-demo.js` to connect SSE and trigger HTMX history refresh.
5. Update navigation surfaces and integration tests.
6. Update docs and AGENTS files for new chat surface.
7. Run full test loop and deploy to live host for verification.

# Validation
- `./mvnw test`
- Deploy command: `./deploy.sh host 192.168.1.113 host`
- Live checks:
  - `GET http://192.168.1.113:8080/chat`
  - `GET /chat/history?conversationId=...`
  - `GET /chat/stream?conversationId=...` emits `chat-updated`

# Exit Criteria
- Chat module/helper contracts available in framework.
- `/chat` renders and can post messages with SSE-triggered transcript refresh.
- Navigation includes Chat in top nav and demo navigation.
- Tests pass and live deployment behavior is verified.
