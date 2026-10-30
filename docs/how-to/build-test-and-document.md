# Build, test, and browse the documentation

From the repository root, use JDK 25 and the repository's official Maven Wrapper 3.3.4
(`only-script`, checksum-pinned Maven 3.10.0). Both module POMs enforce the tool versions,
even when built independently. The library targets Java 25; the site's Java 21 bytecode
still requires a Java 25 runtime.

To package and run the site locally:

```bash
./build-site.sh
java -jar site/target/simplypages-site.jar
```

The script requires Python 3 for its packaged-resource verification. It runs a clean reactor
package with tests skipped, then checks the exact packaged documentation and generated API
inventories and bytes. Python is not required for library Maven builds or to run the JAR. `./deploy.sh` accepts no arguments
and forwards to this local build; it does not deploy remotely.

Open [the local site](http://localhost:8080/), [documentation](http://localhost:8080/docs),
and [Javadocs](http://localhost:8080/javadocs/index.html).

## Run relevant tests

```bash
./mvnw clean verify
./mvnw test
./mvnw -pl simplypages -Dtest=TemplateTest,ReviewRegressionTest test
./mvnw -pl site -am -Dtest=DocumentationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

The reactor builds the library before the site, generates Javadocs, and copies documentation
into the site resources. Run a reactor build after changing shared code or docs; a site-only
run can otherwise consume an older installed library or older generated documentation.

## Run browser regressions against the packaged site

```bash
./mvnw clean package
npm --prefix tests/browser ci
cd tests/browser
npx playwright install chromium
npm test
```

Do not skip Maven tests in this preparation: they generate the Java fixtures consumed by
the browser suite, and package creates the current executable JAR. `npm ci` installs the
locked browser-test dependencies; Node/npm and Chromium are test tools, not library consumer
requirements.

The harness pins Playwright 1.62.0 and uses Chromium with real framework assets, Java-rendered
fixtures, modal snapshots, and actual `/editing-demo` HTMX requests. Its main site process runs on
`127.0.0.1:18080`; some tests start additional owned loopback fixtures. It stops its own
processes and does not reuse or stop an existing service. Keep the required ports free.
Rebuild after changing Java, framework assets, or docs so checks cannot validate a stale JAR
or stale test fixtures. No live deployment is needed for this suite. This local harness is
not a claim of remote CI or all-browser validation.

## Update documentation

Choose the document type by its purpose:

- Tutorials teach through a small, reproducible exercise with expected output.
- How-to guides solve a specific task for an already oriented reader.
- Reference records APIs, constraints, defaults, and failure behavior.
- Explanation connects design decisions and tradeoffs.

Link new pages from their section README and `docs/INDEX.md`. Use relative Markdown links,
keep headings descriptive, and preserve old paths when reorganizing material. Verify both
repository links and the rendered `/docs` routes. The site normalizes document links and
adds heading anchors; navigation uses `outerHTML` to retain one content target.

`DocumentationIntegrityTest` checks local documentation destinations and anchors;
`DocumentationExamplesTest` compiles the complete listings in the two tutorials, the page/fragment
how-to, and the component/module testing guide against the current API and test dependencies.
It does not execute those listings or validate every partial snippet.
Generated Javadocs are build output, not hand-maintained source files.

Repository engineering records belong in `.internal-dev/` under the root agent guide.

[All how-to guides](README.md) · [Build reference](../reference/build-and-assets.md) ·
[Beta limitations](../reference/build-and-assets.md#beta-status-known-issues-and-support)
