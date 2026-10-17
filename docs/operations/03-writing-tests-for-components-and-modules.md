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

This complete JUnit example uses Jsoup for structural assertions without depending on the
repository's test helpers:

```java
package example;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.modules.RichContentModule;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class RichContentExampleTest {
    @Test
    void rendersRichModuleStructure() {
        RichContentModule module = RichContentModule.create("Docs");
        module.withModuleId("m-1");
        module.addParagraph(new Paragraph("Hello"));
        String html = module.render();

        var document = Jsoup.parseBodyFragment(html);
        assertEquals(1, document.select("#m-1 .rich-content-module").size());
        assertEquals("Docs", document.select("#m-1 .module-title").text());
        assertEquals("Hello", document.select("#m-1 .content-container").text());
    }
}
```

`create(String)` takes the title, not the ID. Set the ID separately: inherited
`withModuleId` returns `Module`, so chaining into `addParagraph` would lose the concrete
type. `addParagraph` takes a `Paragraph`, not a string. The content wrapper is
`.content-container`, not `.module-content`.

For complex output in the library test suite, add a `SnapshotAssert.assertMatches(...)`
assertion with an intentionally reviewed baseline; see the existing `RichContentModuleTest`.

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
  - `simplypages/src/test/java/io/mindspice/simplypages/components/forum/ForumTopicRendererTest.java`
  - `simplypages/src/test/java/io/mindspice/simplypages/modules/RichContentModuleTest.java`
- Integration placement checks from:
  - `simplypages/src/test/java/io/mindspice/simplypages/integration/HtmxIntegrationTest.java`
