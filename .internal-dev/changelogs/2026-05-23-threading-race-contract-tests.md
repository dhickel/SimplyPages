# 2026-05-23 Threading and Race Contract Tests

## Summary

- Added concurrent rendering coverage for request-shared `Template` instances with per-request contexts.
- Added race coverage proving compile-on-first-hit slot caching stays inside each request context.
- Added concurrent rendering coverage for `TemplateComponent` with a stable bound context.
- Added concurrent route resolution/rendering coverage for generated static content route indexes.
- Documented the shared request-object concurrency test pattern in the operations testing guide.

## Tests

- `./mvnw -pl simplypages -Dtest=SharedRenderingConcurrencyTest test`
- `git diff --check`
- `./mvnw -pl simplypages test`
- `./mvnw -pl demo -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test`
