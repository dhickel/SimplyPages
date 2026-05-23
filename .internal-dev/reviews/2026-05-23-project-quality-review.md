# Scope

Full SimplyPages repository review covering framework packages (`core`, `layout`, `components`, `modules`, `editing`, `builders`), the demo app, docs/process, `.internal-dev` hygiene, and validation baselines.

Review criteria:
- Code quality and robustness
- Issue and production-readiness risk
- API/contract quality
- High-benefit refactor targets
- Test coverage gaps
- Documentation depth, correctness, and production readiness

Review method:
- Archived completed/stale plan artifacts into `.internal-dev/plans/.archive/`.
- Ran package/domain review agents using `gpt-5.5` with `xhigh` reasoning.
- Ran validation baselines:
  - `./mvnw -pl simplypages test`: passed, 450 tests.
  - `./mvnw -pl demo test`: passed, 37 tests.

# Findings

## Critical / High

1. Core tag and attribute names are not validated.
   - `HtmlTag` and `Attribute` encode values but emit `tagName` and attribute names raw.
   - Impact: weakens the documented safe-default boundary if names are derived from dynamic input.
   - Evidence: `simplypages/src/main/java/io/mindspice/simplypages/core/HtmlTag.java:61`, `:97`, `:396`; `core/Attribute.java:21`.

2. Multiple public APIs bypass centralized URL validation.
   - `HeroModule` CTA links, `NavBar`, `SideNav`, `Breadcrumb`, `Dropdown`, and `AccountWidget` build raw anchors instead of using `Link` validation.
   - Impact: `javascript:` and other disallowed schemes can pass through public helper APIs.
   - Evidence: `modules/HeroModule.java:133`; `components/navigation/NavBar.java:80`; `SideNav.java:88`; `Breadcrumb.java:67`; `components/Dropdown.java:48`; `components/AccountWidget.java:154`.

3. Manual CSS/style assembly bypasses hardened style paths.
   - `HeroModule`, `BannerBuilder`, `AccountBarBuilder`, and `Divider` concatenate raw style strings.
   - Impact: style hardening is inconsistent and depends on which API a user happens to call.
   - Evidence: `modules/HeroModule.java:183`; `builders/BannerBuilder.java:178`; `builders/AccountBarBuilder.java:122`; `components/Divider.java:129`.

4. Render-context contract is inconsistent across components.
   - Components overriding only zero-arg `render()` lose behavior when nested, compiled, or rendered with `RenderContext`.
   - Impact: `Code.block`, `Spinner.withMessage`, `Paragraph` alignment, `Column` base classes, and modal slot content can render incorrectly in real composition.
   - Evidence: `components/Code.java:75`; `display/Spinner.java:82`; `components/Paragraph.java:123`; `layout/Column.java:121`; `components/display/Modal.java:186`.

5. Demo editing authorization is UI-only.
   - `canEdit`/`canDelete` hide controls, but edit/save/delete endpoints do not enforce the same permission.
   - Impact: direct requests can mutate locked demo content, teaching the wrong production pattern.
   - Evidence: `demo/src/main/java/io/mindspice/demo/EditingDemoController.java:274`, `:317`, `:334`, `:360`.

6. Editing and module correctness bugs are user-visible.
   - `EditableRow.wrap(...)` advertises wrapping a supplied row but render creates a fresh row and drops the wrapped row.
   - `TabsModule` default IDs collide across multiple instances and JS uses global `document.getElementById(...)`.
   - Nested editing emits child edit/delete URLs without matching demo endpoints.
   - Evidence: `editing/EditableRow.java:61`, `:115`; `modules/TabsModule.java:133`; `framework.js:105`; `EditingDemoController.java:639`.

7. Public docs contain non-compiling examples and understate endpoint security.
   - README/getting-started use `vr.errors()` while the API exposes `getErrors()` / `getErrorsAsString()`.
   - Editing save examples omit auth/CSRF handling despite security docs requiring consumer-side enforcement.
   - Evidence: `README.md:127`; `docs/getting-started/03-editing-system-first-implementation.md:52`; `editing/ValidationResult.java:62`.

8. Deploy workflow is not production-ready and conflicts with process records.
   - `deploy.sh` accepts a password as a CLI argument and disables host-key checking.
   - Changelog/process records conflict with current deploy script behavior, and two open bugs track the same remote `curl` false-failure mode.
   - Evidence: `deploy.sh:4`, `:70`, `:81`; `.internal-dev/bugs/deploy-remote-curl-missing/report.md`; `.internal-dev/bugs/deploy-script-remote-curl-prereq/report.md`.

## Medium

1. Style replacement in `HtmlTag` is regex-based and not property-boundary aware.
   - Adding `width` after `max-width` can corrupt the existing declaration.
   - Evidence: `core/HtmlTag.java:318`.

2. Layout APIs drift from CSS contracts.
   - Row/Grid gap and alignment classes do not consistently map to shipped CSS.
   - Chained class mutators can overwrite prior/custom classes.
   - Evidence: `layout/Row.java:90`; `layout/Grid.java:66`; `framework.css:410`, `:3437`.

3. Editing identifiers are interpolated into DOM ids and HTMX URLs without validation or encoding.
   - Evidence: `editing/EditablePage.java:36`, `:68`; `editing/EditableRow.java:124`; `editing/EditModalBuilder.java:182`.

4. `ValidationResult` documents immutability/thread-safety but stores and exposes mutable lists.
   - Evidence: `editing/ValidationResult.java:13`, `:28`, `:43`, `:53`, `:62`.

5. `Form.withHxPostCsrf(...)` builds JSON by string interpolation.
   - Quotes or backslashes in header names/tokens can produce invalid or altered `hx-headers`.
   - Evidence: `components/forms/Form.java:240`.

6. Module markup and docs are out of sync with CSS/runtime contracts.
   - `QuoteModule`, `TimelineModule`, and `StatsModule` emit classes/styles that do not match intended responsive CSS.
   - `ComparisonModule` documents row arity requirements without enforcing them.
   - Evidence: `modules/QuoteModule.java:106`; `TimelineModule.java:127`; `StatsModule.java:104`; `ComparisonModule.java:107`, `:177`.

7. Demo state is mutable and weakly bounded.
   - Editing stores mutable lists inside concurrent maps; chat/forum accept unbounded user content; SSE emitters can accumulate.
   - Evidence: `EditingDemoController.java:107`; `ChatDemoController.java:40`; `chat/ChatDemoService.java:26`; `forum/ForumDemoService.java:168`.

8. Version, JDK, and docs navigation contracts drift.
   - Docs still publish `1.0.0` while current poms are `1.1.0a`.
   - Framework targets Java 25 while demo targets Java 21 without clear toolchain docs.
   - AGENTS and `.internal-dev/AGENTS.md` reference stale or missing docs.

## Low / Hygiene

1. Dynamic modules render placeholder sample data by default.
   - Risk: production consumers may accidentally ship placeholder UI.
   - Evidence: `DynamicCardModule.java:19`; `DynamicListModule.java:20`; `DynamicTableModule.java:24`.

2. Empty-valued attributes and null ids have ambiguous semantics.
   - `Attribute` treats null and empty values as boolean attributes.
   - Evidence: `core/Attribute.java:22`; `core/HtmlTag.java:83`; `core/Module.java:43`.

3. Test policy and actual tests are inconsistent.
   - Docs discourage brittle `html.contains(...)`, but enforcement is narrow and module/layout tests still use it heavily.

# Risk Assessment

The main risk is not test health; current automated tests pass. The risk is contract drift: several public APIs bypass framework hardening, render differently depending on render path, or document behavior that is not enforced.

Highest-risk areas:
- Security/safety contract drift: raw hrefs, raw style strings, raw tag/attribute names.
- Rendering contract drift: components that only implement zero-arg `render()` correctly.
- Editing/demo correctness: UI-only permissions and incomplete nested edit endpoints.
- Documentation trust: public examples that do not compile or omit endpoint security.

The current project is usable internally, but the review found enough contract inconsistencies that a production-facing release should be gated on remediation of the high findings.

# Recommendations

1. Create a high-priority hardening plan for safe names, URLs, and styles.
   - Validate HTML tag names and attribute names.
   - Centralize safe URL validation and reuse it in all anchor-producing APIs.
   - Replace manual style string assembly with `HtmlTag.addStyle(...)` / `addTrustedStyle(...)` or a structured style declaration model.

2. Normalize render contracts.
   - Require behavior-preserving `render(RenderContext)` for every component/module that overrides rendering.
   - Add nested/template/context regression tests for every custom renderer.
   - Treat zero-arg-only overrides as a release blocker for public components.

3. Fix editing and demo endpoint contracts.
   - Enforce edit/delete permissions on direct endpoints.
   - Add nested child endpoints or stop emitting child URLs.
   - Split `EditingDemoController` into state, authorization, and rendering/controller layers.

4. Repair docs before external release.
   - Fix non-compiling examples (`vr.errors()`).
   - Add auth/CSRF warnings to editing save examples.
   - Expand reference docs into parameter-contract tables for components, modules, builders, layout, and editing.
   - Add explicit Java/toolchain/version guidance.

5. Make `.internal-dev` and deployment records coherent.
   - Keep open bug reports for unresolved deploy-script and security findings.
   - Consolidate duplicate deploy-curl bug reports.
   - Replace password CLI deploy guidance with a safer key/known-hosts workflow.

# Follow-ups

Recommended remediation sequence:

1. Security hardening phase:
   - Tag/attribute name validation.
   - URL validation reuse across modules/components/builders.
   - Structured style path cleanup.
   - Tests for every unsafe URL/style/name finding.

2. Render-contract phase:
   - Fix `Column`, `Code`, `Spinner`, `Paragraph`, `Modal`, and other custom renderers.
   - Add template/context/nested regression tests.

3. Editing/demo phase:
   - Fix `EditableRow.wrap`.
   - Make `ValidationResult` immutable.
   - Enforce endpoint authorization.
   - Add nested edit endpoint tests.

4. CSS/layout/module contract phase:
   - Align layout gap/alignment tokens with CSS.
   - Fix `TabsModule` IDs.
   - Align `QuoteModule`, `TimelineModule`, and `StatsModule` markup with CSS.

5. Docs/process phase:
   - Correct README/getting-started examples.
   - Expand reference docs.
   - Normalize release version and Java requirements.
   - Modernize deploy workflow.

Open bugs retained as active:
- `.internal-dev/bugs/2026-02-18-radiogroup-inline-class-overwrite/report.md`
- `.internal-dev/bugs/2026-02-18-sticky-sidebar-responsive-default-state/report.md`
- `.internal-dev/bugs/2026-02-18-style-attribute-double-semicolon/report.md`
- `.internal-dev/bugs/2026-03-09-forumhelper-builder-missing-actiondecorator-field/report.md`
- `.internal-dev/bugs/2026-03-11-chat-role-class-injection/report.md`
- `.internal-dev/bugs/2026-03-11-forum-chat-image-url-scheme/report.md`
- `.internal-dev/bugs/deploy-remote-curl-missing/report.md`
- `.internal-dev/bugs/deploy-script-remote-curl-prereq/report.md`

Archived completed/stale plan artifacts:
- `.internal-dev/plans/2026-03-07-simplypages-1-0-release-readiness/`
- `.internal-dev/plans/chat-ui/`
- `.internal-dev/plans/javadoc-rewrite/`
- `.internal-dev/plans/mobile-release-readiness/`
- `.internal-dev/plans/slotkey-refactor/`
- `.internal-dev/plans/2026-03-22-beta-robustness-fixes.md`
- `.internal-dev/plans/2026-03-22-beta-security-flexibility-fixes.md`
- `.internal-dev/plans/css-remediation-plan.md`
