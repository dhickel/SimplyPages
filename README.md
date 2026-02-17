# SimplyPages

SimplyPages is a Java-first server-side rendering framework for teams that want predictable HTML output, minimal frontend complexity, and direct control over rendering behavior.

It is built for practical web apps: admin portals, internal tools, content-heavy systems, and data views where server-rendered HTML is a better fit than client-heavy stacks.

## Table of Contents

1. [Why Use It](#why-use-it)
2. [Install](#install)
3. [Static Page (Build Once, Serve Fast)](#1-static-page-build-once-serve-fast)
4. [Dynamic Page (Template + SlotKey + RenderContext)](#2-dynamic-page-template--slotkey--rendercontext)
5. [Editable Module Flow (Minimal)](#3-editable-module-flow-minimal)
6. [Custom Components and Modules (Scaffolding)](#4-custom-components-and-modules-scaffolding)
7. [Documentation](#documentation)
8. [Build and Test](#build-and-test)
9. [Docs TOC](#docs-toc)

## Why Use It

- Server rendering first, not as an afterthought.
- UI composition in Java with fluent APIs.
- Strong dynamic rendering model with `Template`, `SlotKey`, and `RenderContext`.
- HTMX-friendly fragment update workflows.
- Editable module patterns for real-world content operations.

## Install

```xml
<dependency>
  <groupId>io.mindspice</groupId>
  <artifactId>simplypages</artifactId>
  <version>0.1.0</version>
</dependency>
```

## 1) Static Page (Build Once, Serve Fast)

```java
@Service
public class StaticPageService {
    private String homeHtml;

    @PostConstruct
    void init() {
        homeHtml = Page.builder()
            .addComponents(Header.H1("Welcome"))
            .addComponents(new Paragraph("This page was built once at startup."))
            .build()
            .render();
    }

    public String homeHtml() {
        return homeHtml;
    }
}

@GetMapping("/")
@ResponseBody
public String home(StaticPageService staticPages) {
    return staticPages.homeHtml();
}
```

## 2) Dynamic Page (Template + SlotKey + RenderContext)

```java
public final class UserCardView {
    public static final SlotKey<String> USER_NAME = SlotKey.of("user_name");
    public static final SlotKey<String> USER_ROLE = SlotKey.of("user_role");

    public static final Template USER_CARD_TEMPLATE = Template.of(
        new Div().withClass("user-card")
            .withChild(new HtmlTag("h2").withInnerText(USER_NAME))
            .withChild(new Paragraph().withChild(Slot.of(USER_ROLE)))
    );

    private UserCardView() {}
}

@GetMapping("/users/{id}/card")
@ResponseBody
public String userCard(@PathVariable String id) {
    User user = userService.load(id);

    RenderContext ctx = RenderContext.builder()
        .with(UserCardView.USER_NAME, user.name())
        .with(UserCardView.USER_ROLE, user.role())
        .build();

    return UserCardView.USER_CARD_TEMPLATE.render(ctx);
}
```

This pattern keeps structure stable and pushes per-request variability into typed slot values.

## 3) Editable Module Flow (Minimal)

```java
@GetMapping("/modules/{id}/edit")
@ResponseBody
public String editModule(@PathVariable String id, Principal principal) {
    ContentModule module = moduleService.loadContentModule(id);

    return AuthWrapper.requireForEdit(
        () -> authChecker.canEdit(id, principal.getName()),
        () -> EditModalBuilder.create()
            .withTitle("Edit Module")
            .withModuleId(id)
            .withEditable(module)
            .withSaveUrl("/modules/" + id + "/save")
            .withDeleteUrl("/modules/" + id + "/delete")
            .withPageContainerId("page-content")
            .withModalContainerId("edit-modal-container")
            .build()
            .render()
    );
}

@PostMapping("/modules/{id}/save")
@ResponseBody
public String saveModule(@PathVariable String id, @RequestParam Map<String, String> formData) {
    ContentModule module = moduleService.loadContentModule(id);

    ValidationResult vr = module.validate(formData);
    if (!vr.isValid()) {
        return Alert.danger(String.join(", ", vr.errors())).render();
    }

    module.applyEdits(formData);
    moduleService.save(module);

    String closeModal = "<div id=\"edit-modal-container\" hx-swap-oob=\"true\"></div>";
    String updateModule = "<div id=\"" + id + "\" hx-swap-oob=\"true\">" + module.render() + "</div>";
    return closeModal + updateModule;
}
```

## 4) Custom Components and Modules (Scaffolding)

Keep custom primitives small and modules focused.

```java
// Custom primitive component (scaffolding)
public class StatusPill extends HtmlTag {
    public StatusPill() { super("span"); }

    public static StatusPill create(String label) {
        StatusPill pill = new StatusPill();
        pill.withClass("status-pill");
        pill.withInnerText(label);
        return pill;
    }
}

// Custom module (scaffolding)
public class ProfileSummaryModule extends Module {
    private String titleText;

    public ProfileSummaryModule() {
        super("div");
        withClass("profile-summary-module");
    }

    public static ProfileSummaryModule create() { return new ProfileSummaryModule(); }

    public ProfileSummaryModule withTitleText(String titleText) {
        this.titleText = titleText;
        return this;
    }

    @Override
    protected void buildContent() {
        if (titleText != null) {
            super.withChild(Header.H3(titleText));
        }
        super.withChild(StatusPill.create("Active"));
    }
}
```

## Documentation

Start at `docs/README.md`.

Recommended sequence:

1. `docs/fundamentals/01-web-and-htmx-primer.md`
2. `docs/getting-started/01-installation-and-first-static-page.md`
3. `docs/getting-started/02-dynamic-pages-with-slotkey-rendercontext.md`
4. `docs/getting-started/03-editing-system-first-implementation.md`

## Build and Test

```bash
./mvnw clean install
./mvnw test
./mvnw -pl simplypages test
./mvnw -pl demo spring-boot:run
```

## Docs TOC

- `docs/README.md`
- `docs/INDEX.md`
- Fundamentals:
  - `docs/fundamentals/01-web-and-htmx-primer.md`
  - `docs/fundamentals/02-simplypages-mental-model.md`
  - `docs/fundamentals/03-css-fundamentals.md`
- Getting Started:
  - `docs/getting-started/README.md`
  - `docs/getting-started/01-installation-and-first-static-page.md`
  - `docs/getting-started/02-dynamic-pages-with-slotkey-rendercontext.md`
  - `docs/getting-started/03-editing-system-first-implementation.md`
- Core:
  - `docs/core/01-components-htmltag-and-module-lifecycle.md`
  - `docs/core/02-layout-page-row-column-grid.md`
  - `docs/core/03-template-rendercontext-slotkey-reference.md`
  - `docs/core/04-rendering-pipeline-high-and-low-level.md`
  - `docs/core/05-css-defaults-overrides-and-structure.md`
- Patterns:
  - `docs/patterns/01-static-page-serving-patterns.md`
  - `docs/patterns/02-dynamic-fragment-caching-patterns.md`
  - `docs/patterns/03-htmx-endpoint-and-swap-patterns.md`
  - `docs/patterns/04-editing-workflows-owner-user-approval.md`
- Security:
  - `docs/security/01-security-boundaries-and-safe-rendering.md`
  - `docs/security/02-authwrapper-authorizationchecker-integration.md`
- Operations:
  - `docs/operations/01-performance-threading-and-cache-lifecycles.md`
  - `docs/operations/02-testing-and-troubleshooting-playbook.md`
- Reference:
  - `docs/reference/components-and-modules-catalog.md`
  - `docs/reference/builders-shell-nav-banner-accountbar.md`
  - `docs/reference/editing-api-reference.md`
