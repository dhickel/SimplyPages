# Java HTML Framework (JHF)

A lightweight, domain-specific framework for building server-side rendered web applications with minimal JavaScript. Built specifically for data-heavy applications like scientific portals, research databases, and content management systems.

## Philosophy

- **Server-First**: All rendering happens on the server. No complex frontend build process.
- **Java-Native**: Build your entire UI in Java using fluent, type-safe APIs.
- **Minimal JavaScript**: Uses HTMX for dynamic updates, avoiding heavy JavaScript frameworks.
- **Domain-Specific**: Optimized for data display, forms, forums, and content-heavy applications.
- **Composable**: Build complex UIs from simple, reusable components and modules.

## Key Features

### 🎨 Comprehensive Component Library

- **Forms**: TextInput, TextArea, Select, RadioGroup, Checkbox, Button, Form
- **Display**: Table, DataTable, Card, CardGrid, Alert, Badge, Tag, Label, InfoBox
- **Media**: Gallery, Image, Video, Audio
- **Forum**: ForumPost, PostList, Comment, CommentThread
- **Navigation**: Link, NavBar, SideNav, Breadcrumb
- **Lists**: UnorderedList, OrderedList

### 🧩 Module System

High-level components that combine primitives into functional units:

- **ContentModule**: Display formatted text and Markdown
- **FormModule**: Complete forms with structure and styling
- **GalleryModule**: Image galleries with captions
- **DataModule**: Type-safe data table displays
- **ForumModule**: Discussion threads and posts

### 📐 Flexible Layout System

- **Row/Column Grid**: 12-column responsive grid system
- **Grid Layout**: CSS Grid-based layouts with configurable columns
- **Container**: Constrained-width content containers
- **Section**: Semantic section divisions

### 🎯 Type Safety

All components are strongly typed, providing compile-time safety and IDE autocomplete support.

## Quick Start

### 1. Basic Page

```java
@GetMapping("/example")
@ResponseBody
public String examplePage() {
    Page page = Page.builder()
        .addComponents(Header.H1("Hello World"))
        .addRow(row -> row.withChild(
            new Markdown("This is **Markdown** content!")
        ))
        .build();

    return page.render();
}
```

### 2. Form Example

```java
Form contactForm = Form.create()
    .addField("Name", TextInput.create("name")
        .withPlaceholder("Enter your name")
        .required())
    .addField("Email", TextInput.email("email")
        .required())
    .addField("Message", TextArea.create("message")
        .withRows(5))
    .addField("", Button.submit("Send"));
```

### 3. Data Table

```java
DataTable<Strain> table = DataTable.create(Strain.class)
    .addColumn("Name", Strain::getName)
    .addColumn("Type", Strain::getType)
    .addColumn("THC %", s -> String.valueOf(s.getThcPercentage()))
    .withData(strainList)
    .striped()
    .hoverable();
```

### 4. Module Usage

```java
ContentModule module = ContentModule.create()
    .withTitle("Welcome")
    .withContent("""
        ## Getting Started

        Use this framework to build data-heavy applications
        with minimal JavaScript and maximum type safety.
    """);
```

### 5. Two-Column Layout

```java
Page page = Page.builder()
    .addRow(row -> row
        .withChild(new Column().withWidth(8).withChild(
            new Markdown("Main content area")
        ))
        .withChild(new Column().withWidth(4).withChild(
            new Markdown("Sidebar")
        ))
    )
    .build();
```

## Component Categories

### Form Components

Build complex forms with validation and HTMX support:

```java
Form.create()
    .addField("Username", TextInput.create("username"))
    .addField("Password", TextInput.password("password"))
    .addField("Role", Select.create("role")
        .addOption("admin", "Administrator")
        .addOption("user", "User"))
    .addField("", Button.submit("Login"))
    .withHxPost("/login");
```

### Display Components

Show data in various formats:

```java
// Alert
Alert.success("Saved successfully!");

// Badge
Badge.primary("NEW");

// InfoBox
InfoBox.create()
    .withIcon("📊")
    .withTitle("Total Users")
    .withValue("1,234");

// Card
Card.create()
    .withHeader("Title")
    .withBody("Content")
    .withFooter("Footer");
```

### Forum Components

Build discussion platforms:

```java
ForumPost.create()
    .withAuthor("User123")
    .withTimestamp("2 hours ago")
    .withTitle("Discussion Topic")
    .withContent("Post content in Markdown")
    .withReplies(12)
    .withLikes(34);
```

### Media Components

Display images, videos, and galleries:

```java
Gallery.create()
    .withColumns(3)
    .addImage("/img1.jpg", "Alt text", "Caption")
    .addImage("/img2.jpg", "Alt text", "Caption");
```

## Navigation

### Top Navigation

```java
NavBar topNav = NavBar.create()
    .withBrand("My Portal")
    .addItem("Home", "/home", true)  // active
    .addItem("About", "/about")
    .horizontal();
```

### Side Navigation

```java
SideNav sideNav = SideNav.create()
    .addSection("Main")
    .addItem("Dashboard", "/dashboard", true)
    .addItem("Settings", "/settings")
    .addSection("Data")
    .addItem("Reports", "/reports");
```

### Navigation Builders

```java
// Top navigation builder
NavBar topNav = TopNavBuilder.create()
    .withBrand("Cannabis Portal")
    .addPortal("Research", "/research", true)
    .addPortal("Forums", "/forums")
    .addPortal("Journals", "/journals")
    .build();

// Side navigation builder
SideNav sideNav = SideNavBuilder.create()
    .addSection("Research")
    .addLink("Strains", "/strains", "🌿")
    .addLink("Studies", "/studies", "📚")
    .addSection("Community")
    .addLink("Discussions", "/forum", "💬")
    .build();
```

## Styling

### CSS Framework

The framework includes a comprehensive CSS framework with:

- Responsive grid system (12 columns)
- Form styling with focus states
- Button variants (primary, secondary, success, danger, warning, info)
- Table styles (striped, bordered, hoverable)
- Card components
- Alert styles
- Navigation components
- Typography utilities
- Spacing utilities

### Utility Classes

```css
/* Text alignment */
.text-left, .text-center, .text-right

/* Font sizes */
.text-sm, .text-base, .text-lg, .text-xl

/* Font weights */
.font-light, .font-normal, .font-medium, .font-semibold, .font-bold

/* Spacing */
.p-sm, .p-medium, .p-lg (padding)
.m-sm, .m-medium, .m-lg (margin)

/* Grid gaps */
.gap-sm, .gap-medium, .gap-lg
```

## HTMX Integration

All components support HTMX attributes for dynamic updates:

```java
Link.create("/page", "Click me")
    .withHxGet("/dynamic-content")
    .withHxTarget("#content-area")
    .withHxPushUrl(true);

Form.create()
    .withHxPost("/submit")
    .withHxTarget("#result")
    .withHxSwap("outerHTML");
```

## Architecture

### Component Hierarchy

```
Component (interface)
├── HtmlTag (base class)
│   ├── Basic Components (Div, Paragraph, Header, Image)
│   ├── Form Components (TextInput, TextArea, Select, etc.)
│   ├── Display Components (Table, Card, Alert, etc.)
│   ├── Navigation Components (Link, NavBar, SideNav)
│   └── Layout Components (Row, Column, Grid, Container)
└── Module (abstract class)
    ├── ContentModule
    ├── FormModule
    ├── GalleryModule
    ├── DataModule
    └── ForumModule
```

### Rendering Flow

1. Build components using fluent APIs
2. Components generate HTML strings via `render()` method
3. ShellBuilder generates complete HTML shell structure
4. Controller returns HTML to Spring
5. Spring serves HTML to browser
6. HTMX handles dynamic updates

## Running the Demo

```bash
cd java-html-framework
./mvnw spring-boot:run
```

Navigate to `http://localhost:8080` to see the interactive demo showcasing all components.

## Demo Pages

- **Home**: Framework overview and features
- **Forms**: Form components and examples
- **Tables**: Data table demonstrations
- **Gallery**: Image gallery with media components
- **Forum**: Discussion and comment components
- **Cards**: Card layouts and info boxes
- **Alerts**: Notifications, badges, and tags
- **Modules**: Module system examples
- **Layouts**: Grid and layout system demonstrations

## Dependencies

- Spring Boot 3.2.3
- HTMX 1.9.10 (via WebJars)
- CommonMark 0.21.0 (Markdown rendering)

## Design Principles

1. **Simplicity Over Complexity**: Avoid over-abstraction, keep it simple
2. **Type Safety**: Leverage Java's type system for compile-time guarantees
3. **Minimal JavaScript**: Server-side rendering with progressive enhancement
4. **Developer Experience**: Fluent APIs, clear naming, good defaults
5. **Composability**: Small pieces combine into larger wholes
6. **Domain Focus**: Optimized for data-heavy, content-rich applications

## Future Enhancements

- Responsive utilities for mobile/tablet/desktop
- Form validation framework
- Component theming system
- Additional chart/graph components
- File upload components
- Pagination component
- Search/filter components
- Date picker components
- Rich text editor integration

## License

Open source - designed for the Cannabis Research Portal project but suitable for any data-heavy web application.

## Contributing

This is a prototype framework. Feedback and contributions welcome as the framework evolves based on real-world usage in the Cannabis Research Portal.
