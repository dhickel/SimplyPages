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

Use JDK 25. Install this checkout locally with `./mvnw -pl simplypages install` before
using the dependency; public Maven publication is not assumed.

```xml
<dependency>
  <groupId>io.mindspice</groupId>
  <artifactId>simplypages</artifactId>
  <version>1.1.0a</version>
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
public String saveModule(@PathVariable String id, @RequestParam Map<String, String> formData,
                         Principal principal) {
    if (principal == null || !authChecker.canEdit(id, principal.getName())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    ContentModule module = moduleService.loadContentModule(id);

    ValidationResult vr = module.validate(formData);
    if (!vr.isValid()) {
        return new Div().withId("edit-errors").withAttribute("hx-swap-oob", "true")
            .withChild(Alert.danger(String.join(", ", vr.errors()))).render();
    }

    module.applyEdits(formData);
    moduleService.save(module);

    String closeModal = new Div().withId("edit-modal-container")
        .withAttribute("hx-swap-oob", "true").render();
    String updateModule = new Div().withId("container-" + id)
        .withAttribute("hx-swap-oob", "true").withChild(module).render();
    return closeModal + updateModule;
}
```

The page must already contain `edit-errors`, `edit-modal-container`, and the distinct
`container-<id>` module wrapper. The save example assumes HTTP-layer CSRF validation and
request-owned module instances; error updates use OOB because the modal saves with
`hx-swap="none"`. `ResponseStatusException` and `HttpStatus` are Spring types.

The default shell loads the framework modal assets. Focus enters an inserted edit dialog,
Tab stays contained, and Escape or Close returns to the connected launcher. Editing modals
disable backdrop dismissal; external/OOB removal also restores focus when the launcher
survives. See the [Modal contract](docs/reference/components-and-modules-catalog.md#modal).

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

Start at the [documentation home](docs/README.md), organized by reader need:

- [Tutorials](docs/tutorials/README.md): runnable first page and dynamic rendering lessons.
- [How-to guides](docs/how-to/README.md): serving, editing, content publishing, and testing.
- [Reference](docs/reference/README.md): APIs, constraints, defaults, and assets.
- [Explanation](docs/explanation/README.md): rendering, reuse, security, and integration.

The [complete index](docs/INDEX.md) includes the existing topic-based guides.
The running demo serves these pages at `/docs` and generated API docs at `/javadocs/index.html`.

## Build and Test

```bash
./mvnw clean install
./mvnw test
./mvnw -pl simplypages test
./mvnw -pl demo spring-boot:run
```

## Docs TOC

See the [complete documentation index](docs/INDEX.md).
