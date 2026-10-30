# Build requirements and assets

These values describe this checkout, version `1.1.1`.

| Item | Value |
| --- | --- |
| Library coordinates | `io.mindspice:simplypages:1.1.1` |
| Library compilation and runtime | JDK 25 |
| Demo compiler target | Java 21 bytecode; runtime remains JDK 25 |
| Build entry point | Official Maven Wrapper 3.3.4, `only-script`; checksum-pinned Maven 3.10.0 via `./mvnw` |
| Library module | `simplypages` |
| Demo module | `demo`, Spring Boot 4.1.1 / Spring MVC 7.0.9 / Servlet 6.1 |
| Framework CSS | `/css/framework.css` |
| Framework JavaScript | `/js/framework.js` |
| Default HTMX URL | `/webjars/htmx.org/dist/htmx.min.js` |
| Bundled HTMX version | 2.0.11, unmodified `org.webjars.npm:htmx.org` WebJar |
| Demo WebJars locator | `org.webjars:webjars-locator-lite`, Boot-managed 1.1.4 |
| Documentation browser | `/docs` |
| Generated Javadocs | `/javadocs/index.html` |

`ShellBuilder` emits framework CSS before custom CSS and deferred scripts in this order:
HTMX when enabled, framework JavaScript, custom JavaScript. Serving these URLs is the host
application's responsibility. The demo supplies Spring static-resource handling and a
lite WebJars locator for the versionless HTMX URL. The demo uses Boot's `spring-boot-starter-webmvc`
and test-scoped `spring-boot-starter-webmvc-test`; it does not require legacy locator-core or
Jackson 2 databind. The library remains independent of Spring.

The private demo overrides Boot's dependency properties with `jackson-bom.version=3.1.7`
and `tomcat.version=11.0.27`; Jackson annotations remain 2.21. These use the parent POM's
property mechanism, not an unmodified, vendor-tested Boot dependency set. They do not
establish future support or security certification for the demo or consumer applications.

`Page` renders content/layout without a complete HTML document. `ShellBuilder.build()` and
`ShellTemplate.render(...)` return complete documents. `ShellBuilder.buildBody()` returns
body content. Template shell mode uses its explicit content slot and does not auto-load `/home`.

## HTMX configuration ownership

Full documents and compiled shell templates that include HTMX preserve three 1.x defaults:
`selfRequestsOnly: false`, `methodsThatUseUrlParams: ['get']`, and `scrollBehavior: 'smooth'`.
DELETE form data therefore stays in the request body, and HTMX's own default scrolling stays smooth.
These are compatibility choices, not a destination-security policy.

A private generated head marker lets the existing framework script initialize those values before
deferred custom scripts. It is an implementation detail, not an application configuration API.
Immediate assignments in custom JS and its `DOMContentLoaded` handlers remain authoritative.
An explicit upstream `meta[name="htmx-config"]` is merged later by HTMX and also overrides these
initial values. Defaults are not reapplied after swaps, insertion, or settle. A standard compatibility
meta alone would overwrite immediate deferred custom-script choices during HTMX initialization;
the shell does not generate that meta.

`withHtmx(false)` emits neither the HTMX asset nor the private marker. `buildBody()` emits no
assets, scripts, or marker in either mode. Loading framework JS in an unmarked, manually assembled
host does not change HTMX defaults. Such hosts must explicitly choose their request policy before
issuing requests. For example, to select the legacy defaults with the existing upstream mechanism:

```html
<meta name="htmx-config" content='{"selfRequestsOnly":false,"methodsThatUseUrlParams":["get"],"scrollBehavior":"smooth"}'>
```

Use ordinary upstream configuration for a different policy. Applications own allowed destinations,
CORS responses, CSRF issuance/verification, authentication, and authorization. Permitting cross-origin
requests here does not grant a browser CORS exemption or make an endpoint safe.

## Form header compatibility boundary

With the bundled HTMX and framework JS loaded once, a delegated `htmx:configRequest` handler
recovers missing Object.prototype-name headers, including `__proto__`, from the source FORM's own
`hx-headers` attribute. The attribute must be literal JSON containing lexically exactly one string
pair; duplicate textual pairs, multiple pairs, expressions, non-string values, ancestor-only
attributes, and non-form sources are outside this repair. No `.form` CSS class is required. A
hand-authored form with the same observable shape also qualifies; DOM markup carries no Java provenance.

The handler defines a missing own data property without changing the header object's prototype.
Existing own case-insensitive spellings, including null, accessors, and non-enumerable entries,
remain untouched; nonextensible maps and earlier cancellation are respected. Later application
listeners may override or cancel normally. Ordinary headers deliberately deleted by an application
are not regenerated. To suppress a collision header explicitly, use an own null entry or cancel;
a deleted collision is indistinguishable from upstream omission at this event stage.

This is not a general HTMX options merger. An ajax options `__proto__` entry discarded by upstream
before `configRequest` cannot be recovered as that earlier override; use an own event-stage entry
for that case. Stopped propagation and host-controlled header maps retain their normal ownership.
No inherited/evaluated-header parser, retry, alternate transport, header rename, or token verification
is supplied. Form's raw JSON, getters, null-string behavior, attribute order, and UTF-16 representation
are unchanged. Exact JSON representability is broader than legal browser HTTP-header transport.

## Build outputs

- Library JAR: `simplypages/target/simplypages-1.1.1.jar`
- Executable demo JAR: `demo/target/simplypages-demo.jar`
- Generated API docs: `simplypages/target/reports/apidocs/`
- Packaged guide source: `demo/target/classes/static/docs/`
- Packaged API docs: `demo/target/classes/static/javadocs/`

The reactor and each independent module POM enforce JDK 25 and Maven `[3.10.0,4)`. The library
emits Java 25 class files (major version 69); the demo emits Java 21 class files but cannot
run on Java 21 because it loads that library. Use the wrapper rather than a system Maven.

The POMs pin build plugins and output timestamps. The library attaches source and Javadoc
JARs at package time. Javadoc uses `all,-missing` with warnings fatal: structural lint is
required, but missing API prose is not a build failure or evidence of complete documentation.

The demo build checks that generated Javadocs are present. Use a reactor build such as
`./mvnw clean install` before running the demo independently. `./mvnw clean verify` runs the
Maven verification lifecycle; browser tests are a separate command in the
[build guide](../how-to/build-test-and-document.md). There is no automatic coverage,
SBOM, continuous security, or remote CI acceptance gate claimed here. Public artifact
publication is not assumed; install the library locally or use your team's artifact repository.

## Beta status, known issues, and support

Version `1.1.1` is a controlled-beta checkout for local installation, not a new public
publication or an unqualified production-readiness claim. The support baseline is Java 25
and local Chromium checks at desktop and mobile viewports. Mobile viewports do not certify
physical devices. Other browsers, native Windows, and remote CI remain unverified.

Known open issues:

- [#81: Private chat shutdown warning](https://github.com/dhickel/SimplyPages/issues/81):
  the demo now catches `IllegalStateException` during terminal emitter completion to address
  the recycled-response destroy warning. That warning was absent in two packaged SIGTERM
  reproductions with 64 streams. The 30-second graceful shutdown timeout and async timeout
  warnings remained, so these checks do not establish that all shutdown issues are resolved.
  No leak has been proven; this is not evidence of a general framework chat crash.
- [#84: Documentation completeness](https://github.com/dhickel/SimplyPages/issues/84):
  remaining API catalog, missing-Javadoc, and executable-example coverage are follow-up
  work, not complete today. The 100-warning display cap is not a count of documentation debt.

The demo defaults to loopback and uses shared, non-durable simulations. It supplies no
production authentication, CSRF protection, rate protection, or production guarantees.
Hosts own HTTP security, authorization, persistence, and request policy; the HTMX defaults
and bounded Form-header repair above do not provide those controls. Report reproducible
problems through the linked GitHub issues without including credentials or private data.

Licensing, deployment approval, and publication are outside this beta closeout; this status
section does not grant rights, certify a deployment, or assert that unrun checks passed.

[Build and documentation tasks](../how-to/build-test-and-document.md) · [All reference](README.md)
