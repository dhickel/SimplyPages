# Assistant Workspace Demo Coverage

## Summary
- Added assistant workspace demo coverage to `/demos/modules`.
- Demonstrated `AssistantChatModule`, `ChatRoomListModule`, `TimelineTranscriptModule`, `MasterDetailBrowserModule`, `HtmxTabNav`, `PollingPanel`, and `StatusBadge` together.
- Added HTMX fragment endpoints for room, tab, status, and workspace detail swaps.
- Added integration coverage for the demo page and fragment endpoints.

## Validation
- `./mvnw -pl demo -am test` passed.
- `./mvnw -pl simplypages install -DskipTests` passed to make the branch-local framework artifact available to `spring-boot:run`.
- `timeout 35s ./mvnw -pl demo spring-boot:run -Dspring-boot.run.arguments=--server.port=0` started successfully on a random port, then exited with timeout code 124 after graceful shutdown.
