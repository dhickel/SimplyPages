# Build, test, and browse the documentation

From the repository root, use JDK 25:

```bash
./mvnw clean install
./mvnw -pl demo spring-boot:run
```

Open [the local demo](http://localhost:8080/), [documentation](http://localhost:8080/docs),
and [Javadocs](http://localhost:8080/javadocs/index.html).

## Run relevant tests

```bash
./mvnw test
./mvnw -pl simplypages -Dtest=TemplateTest,ReviewRegressionTest test
./mvnw -pl demo -am -Dtest=DocumentationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

The reactor builds the library before the demo, generates Javadocs, and copies documentation
into the demo resources. Run a reactor build after changing shared code or docs; a demo-only
run can otherwise consume an older installed library or older generated documentation.

## Update documentation

Choose the document type by its purpose:

- Tutorials teach through a small, reproducible exercise with expected output.
- How-to guides solve a specific task for an already oriented reader.
- Reference records APIs, constraints, defaults, and failure behavior.
- Explanation connects design decisions and tradeoffs.

Link new pages from their section README and `docs/INDEX.md`. Use relative Markdown links,
keep headings descriptive, and preserve old paths when reorganizing material. Verify both
repository links and the rendered `/docs` routes. The demo normalizes document links and
adds heading anchors; navigation uses `outerHTML` to retain one content target.

`DocumentationIntegrityTest` checks local documentation destinations and anchors;
`DocumentationExamplesTest` compiles the complete tutorial Java listings against the library.
Generated Javadocs are build output, not hand-maintained source files.

Repository engineering records belong in `.internal-dev/` under the root agent guide.

[All how-to guides](README.md) · [Build reference](../reference/build-and-assets.md)
