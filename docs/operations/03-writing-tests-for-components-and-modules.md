[Previous](02-testing-and-troubleshooting-playbook.md) | [Index](../INDEX.md)

# Writing Tests for Components and Modules

This guide is the default contribution path for testing new components, modules, and contribution libraries.

## Who Should Use This

- Contributors adding or changing `components/*`
- Contributors adding or changing `modules/*`
- Teams building extension libraries on top of SimplyPages

## Required Test Rule

Every new feature or behavior change must include targeted tests in the same workstream.

Minimum expectation:
- Component changes: selector-based structural assertions for rendered HTML
- Module changes: structural assertions plus one snapshot for complex composed output
- Integration-level behavior changes: placement assertions for IDs, `hx-*` attributes, and containers

## Default Test Stack

Use these utilities from `simplypages/src/test/java/io/mindspice/simplypages/testutil/`:

- `HtmlAssert` for structural verification
- `SnapshotAssert` for stable complex-output regression checks
- `HtmlNormalizer` via `SnapshotAssert` (already used internally)

Do not use brittle primary assertions for structure/order:
- `assertTrue(html.contains(...))`
- `assertFalse(html.contains(...))`
- `indexOf(...)` ordering checks

## Component Test Pattern

```java
@Test
void rendersCardStructure() {
    String html = Card.create()
        .withHeader("Title")
        .withBody("Body")
        .render();

    HtmlAssert.assertThat(html)
        .hasElement("div.card")
        .elementTextEquals(".card-header", "Title")
        .elementTextEquals(".card-body", "Body")
        .childOrder(".card", ".card-header", ".card-body");
}
```

## Module Test Pattern

```java
@Test
void rendersRichModuleStructureAndSnapshot() {
    String html = RichContentModule.create("m-1")
        .withTitle("Docs")
        .addParagraph("Hello")
        .render();

    HtmlAssert.assertThat(html)
        .hasElement("#m-1.module")
        .hasElement("#m-1 .module-title")
        .hasElement("#m-1 .module-content");

    SnapshotAssert.assertMatches("modules/rich-content/default", html);
}
```

## Contribution Library Pattern

For custom component/module libraries outside core packages, mirror the same strategy:

1. Assert semantic structure with selectors.
2. Assert exact location for key attributes (`id`, `hx-*`, ARIA, form linkage).
3. Add snapshots for any complex or highly nested output.
4. Keep negative tests for escaping and invalid-state handling.

## Snapshot Workflow

- Run tests normally for compare mode:
  - `./mvnw -pl simplypages test`
- Update baselines intentionally:
  - `./mvnw -pl simplypages -DupdateSnapshots=true test`

Snapshot review rules:
- Treat snapshot diffs like source code changes.
- Verify structure assertions still prove semantics.
- Never update snapshots blindly.

## Contribution Checklist

1. Added/updated tests for every behavior change.
2. Structural assertions validate element location and hierarchy.
3. Security-sensitive output is covered (escaping and unsafe element absence).
4. Snapshot coverage added for complex render shapes.
5. Package tests pass locally (`./mvnw -pl simplypages test`).

## Fast Start: What to Copy

- Structural assertion style from:
  - `simplypages/src/test/java/io/mindspice/simplypages/components/display/DataTableTest.java`
  - `simplypages/src/test/java/io/mindspice/simplypages/components/display/ModalTest.java`
- Module + snapshot pattern from:
  - `simplypages/src/test/java/io/mindspice/simplypages/modules/ForumModuleTest.java`
  - `simplypages/src/test/java/io/mindspice/simplypages/modules/RichContentModuleTest.java`
- Integration placement checks from:
  - `simplypages/src/test/java/io/mindspice/simplypages/integration/HtmxIntegrationTest.java`
