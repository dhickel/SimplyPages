[Previous](03-writing-tests-for-components-and-modules.md) | [Index](../INDEX.md)

# Migrating to SimplyPages 1.0.1

SimplyPages 1.0.1 is a contract-hardening release. Most applications using valid
component inputs and request-scoped mutable objects should upgrade without behavioral
changes, but applications that relied on permissive invalid input now need cleanup.

Use this guide as the migration plan for applications, demos, and extension libraries
moving from `1.0.0` to `1.0.1`.

## Migration Strategy

1. Upgrade dependency versions to `1.0.1`.
2. Compile and run the framework or application test suite before changing code.
3. Treat new `IllegalArgumentException` failures as invalid-input findings, not as
   rendering regressions.
4. Fix public URL, style, layout, editing ID, and validation result call sites in
   small targeted commits.
5. Add concurrency tests for objects that are intentionally shared across requests.
6. Run package tests, then run a browser or endpoint smoke test for editing and HTMX
   flows.

Rollback is straightforward: stay on `1.0.0` while cleaning invalid inputs. Do not
work around validation with trusted escape hatches unless the value is already
application-owned and sanitized.

## Upgrade Targets

### Build Files

Update all SimplyPages artifacts and demo references:

```xml
<dependency>
    <groupId>io.mindspice</groupId>
    <artifactId>simplypages</artifactId>
    <version>1.0.1</version>
</dependency>
```

Check parent POMs, module POMs, sample applications, README snippets, and generated
project templates for stale `1.0.0` references.

### Public URL Inputs

Review components and builders that accept externally visible links:

- `Link`
- `NavBar`
- `SideNav`
- `Breadcrumb`
- `Dropdown`
- `AccountWidget`
- `HeroModule`
- `BannerBuilder`

Use root-relative paths or explicit safe schemes such as `http`, `https`, `mailto`,
and `tel`. Remove `javascript:` links, unsupported schemes, control characters, and
malformed public URLs before they reach component builders.

### Style Inputs

Review all direct style calls:

- `addStyle(...)`
- sizing helpers that write CSS values
- background image and color helpers
- custom components that forward style strings

Normal CSS values should continue to work. Values containing CSS rule breakouts,
script-oriented URLs, or malformed declarations should be rejected. Use trusted style
APIs only for static application-owned CSS that is not derived from user input.

### Layout and Module Sizing

Modules remain composition units, not layout containers. Move sizing calls from
modules to parent layout primitives:

```java
Row.create()
    .addColumn(Column.create()
        .withWidth("1/2")
        .addModule(ProfileCardModule.create("profile")));
```

Review usages of:

- `Module.withWidth(...)`
- `Module.withMinWidth(...)`
- `Module.withMaxWidth(...)`
- `Row.withGap(...)`
- `Row.withAlign(...)`
- `Row.withJustify(...)`
- `Grid.withColumns(...)`
- `Grid.withGap(...)`
- `Column.withWidth(...)`

Replace unsupported tokens with documented layout values instead of relying on pass
through CSS class generation.

### Render Context and Template Sharing

`render(RenderContext)` is the canonical render path for dynamic request data.
`render()` should remain the empty-context convenience path.

Recommended sharing model:

- Share compiled `Template` instances across requests.
- Create a fresh `RenderContext` per request.
- Share immutable generated static content bundles and route indexes.
- Share `TemplateComponent` only when its bound context is stable and not mutated
  during render.

Do not share mutable modules, `HtmlTag` instances, or mutable `RenderContext`
instances across concurrent requests while also mutating them.

### Validation Results

`ValidationResult.getErrors()` returns an immutable defensive copy. Update code that
previously modified the returned list:

```java
ValidationResult result = validator.validate(input);
List<String> errors = new ArrayList<>(result.getErrors());
errors.add("Application-specific message");
```

Prefer creating a new collection at the boundary where the application combines
framework validation errors with domain validation errors.

### Editing Path IDs

Generated editing paths now require safe path segments for editable page, row, module,
and modal IDs. Use stable IDs that start with an alphanumeric character and contain
only letters, digits, underscores, dashes, and dots.

Recommended examples:

- `home`
- `profile-card`
- `settings.main`
- `row_01`

Avoid IDs containing slashes, whitespace, query strings, fragments, or encoded route
syntax. If a custom editing endpoint accepts arbitrary child identifiers, URL-encode
those values at the transport boundary and decode them in the controller.

### Endpoint Authorization

The demo now treats hidden controls as a usability hint, not an authorization boundary.
Application controllers should enforce create, edit, delete, and reorder permissions
server-side for both normal and HTMX requests.

Review editing endpoints for:

- owner or role checks before mutation
- bounds checks before list reorder or deletion
- clear 403/404 behavior for unauthorized or missing resources
- no reliance on omitted buttons as a security control

## Testing Targets

Run targeted tests after each migration group, then run the full suite before release:

```bash
./mvnw -pl simplypages test
./mvnw -pl demo -am test
./mvnw clean install
```

Add or adapt tests for:

- unsafe URL rejection
- unsafe style rejection
- invalid layout token rejection
- module width mutator failures
- immutable `ValidationResult.getErrors()` behavior
- editing path ID validation
- endpoint authorization and bounds checks
- shared `Template` and generated-content rendering under concurrent requests

Use `simplypages/src/test/java/io/mindspice/simplypages/integration/SharedRenderingConcurrencyTest.java`
as the reference pattern for race-condition coverage.

## Acceptance Checklist

1. All POM and documentation snippets reference `1.0.1`.
2. Application code compiles without relying on mutable validation error lists.
3. Public links and style values pass the hardened validators intentionally.
4. Module sizing is owned by `Row` and `Column` layout containers.
5. Editing IDs used in generated paths are path-safe.
6. Server endpoints enforce authorization independently of rendered controls.
7. Request-shared objects have concurrency tests or are explicitly documented as
   request-scoped.
8. `./mvnw clean install` passes.
