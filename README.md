# SimplyPages

**SimplyPages** is a robust server-side framework for building modern, data-intensive web applications in pure Java. It combines the type safety and structure of Java with the dynamism of HTMX, eliminating the need for complex frontend build chains.

Designed for enterprise dashboards, research portals, and content management systems where data integrity, performance, and development velocity are paramount.

## 🚀 Why SimplyPages?

- **Server-First Architecture**: Render everything on the server. No React, Vue, or Angular required.
- **Java-Native**: Build your UI with fluent, type-safe Java APIs. Refactor with confidence.
- **High Performance**: Features a compiled `Template` system for high-throughput rendering.
- **HTMX Integration**: Seamlessly interactive UIs using HTML-over-the-wire.
- **Built-in Editing System**: A comprehensive framework for in-place content editing, modals, and permission management.

---

## 🏗️ Core Patterns

SimplyPages offers two distinct rendering patterns to balance flexibility and performance:

### Pattern A: Composition (Flexible)
Build component trees dynamically per-request. Ideal for static pages, forms, and complex one-off layouts.

```java
Page page = Page.builder()
    .addComponents(Header.H1("Welcome"))
    .addRow(row -> row
        .withChild(new Card("User Info", "Details about the user..."))
    )
    .build();
```

### Pattern B: Templates (High Performance)
"Compile" component trees once into static strings with dynamic `Slots`. Ideal for lists, feeds, and high-traffic views.

```java
// 1. Define the template (Static/Singleton)
static final SlotKey<String> TITLE = SlotKey.of("title");
static final Template CARD_TEMPLATE = Template.of(
    Card.create()
        .withHeader(Slot.of(TITLE))
        .withBody(Slot.of(SlotKey.of("body")))
);

// 2. Render with context (Per-Request)
String html = CARD_TEMPLATE.render(
    RenderContext.of(TITLE, "My Card Title")
        .with("body", "Dynamic content goes here")
);
```

---

## 🧩 Key Features

### 1. The Module System
Modules are high-level, reusable units (e.g., `ContentModule`, `DataModule`, `GalleryModule`) that encapsulate structure and logic. They follow a **build-once** lifecycle, making them immutable and cache-friendly.

```java
ContentModule module = ContentModule.create()
    .withTitle("Project Overview")
    .withContent("This project uses **Markdown** rendering.")
    .withClass("intro-section");
```

### 2. Comprehensive Component Library
Over 50+ built-in components including:
- **Layout**: `Row`, `Column`, `Grid`, `Container`, `Section`
- **Forms**: `TextInput`, `Select`, `RadioGroup`, `DateInput`, `Form`
- **Data Display**: `DataTable`, `Card`, `Badge`, `InfoBox`
- **Navigation**: `SideNav`, `NavBar`, `Breadcrumb`
- **Media**: `Gallery`, `Video`, `Audio`

### 3. In-Place Editing Framework
A full-featured editing system allowing users to modify content directly on the page.
- **Editable Modules**: Wrap content in edit containers.
- **Modal System**: Built-in editors for text, markdown, and list items.
- **Permissions**: Granular control over who can edit, add, or delete content.

### 4. Shell & Layout Builders
Rapidly scaffold application shells with the `ShellBuilder`.

```java
ShellBuilder.create()
    .withTopBanner(new TopBanner("My App"))
    .withSideNav(SideNavBuilder.create()
        .addLink("Dashboard", "/dash", "📊")
        .addLink("Settings", "/settings", "⚙️")
        .build())
    .withContentTarget("main-content") // HTMX target
    .build();
```

---

## 📦 Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.mindspice</groupId>
    <artifactId>simplypages</artifactId>
    <version>0.1.0</version>
</dependency>
```

## 🚦 Quick Start

### Basic Controller

```java
@GetMapping("/")
@ResponseBody
public String home() {
    return Page.builder()
        .addComponents(
            Header.H1("Hello from SimplyPages"),
            new Paragraph("This page was rendered entirely in Java.")
        )
        .render();
}
```

### Using HTMX

SimplyPages components have native fluent methods for HTMX attributes:

```java
Button.create("Load Data")
    .withHxGet("/api/data")
    .withHxTarget("#result-container")
    .withHxSwap("outerHTML");
```

## 🛠️ Running the Demo

The repository includes a comprehensive demo application showcasing all features, including the editing system.

```bash
cd demo
../mvnw spring-boot:run
```
Access the demo at `http://localhost:8080`.

## 📚 Documentation & Resources

- **`AGENTS.md`**: Technical constraints and implementation details for AI assistants.
- **`/docs`**: In the demo app, browse the live documentation.

## License

Open source. Part of the **SimplyWeb Suite** by Mindspice.
