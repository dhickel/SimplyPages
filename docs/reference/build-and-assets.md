# Build requirements and assets

These values describe this checkout, version `1.1.0a`.

| Item | Value |
| --- | --- |
| Library coordinates | `io.mindspice:simplypages:1.1.0a` |
| Library compilation and runtime | JDK 25 |
| Demo compiler target | Java 21 bytecode; runtime remains JDK 25 |
| Build entry point | Repository Maven wrapper, `./mvnw` |
| Library module | `simplypages` |
| Demo module | `demo`, Spring Boot 3.4.3 |
| Framework CSS | `/css/framework.css` |
| Framework JavaScript | `/js/framework.js` |
| Default HTMX URL | `/webjars/htmx.org/dist/htmx.min.js` |
| Bundled HTMX version | 1.9.10 |
| Documentation browser | `/docs` |
| Generated Javadocs | `/javadocs/index.html` |

`ShellBuilder` emits framework CSS before custom CSS and deferred scripts in this order:
HTMX when enabled, framework JavaScript, custom JavaScript. Serving these URLs is the host
application's responsibility. The demo supplies Spring static-resource handling and a
WebJars locator for the versionless HTMX URL.

`Page` renders content/layout without a complete HTML document. `ShellBuilder.build()` and
`ShellTemplate.render(...)` return complete documents. `ShellBuilder.buildBody()` returns
body content. Template shell mode uses its explicit content slot and does not auto-load `/home`.

## Build outputs

- Library JAR: `simplypages/target/simplypages-1.1.0a.jar`
- Executable demo JAR: `demo/target/simplypages-demo.jar`
- Generated API docs: `simplypages/target/reports/apidocs/`
- Packaged guide source: `demo/target/classes/static/docs/`
- Packaged API docs: `demo/target/classes/static/javadocs/`

The demo build checks that generated Javadocs are present. Use a reactor build such as
`./mvnw clean install` before running the demo independently. Public artifact publication is
not assumed; install the library locally or use your team's artifact repository.

[Build and documentation tasks](../how-to/build-test-and-document.md) · [All reference](README.md)
