# SimplyPages - Claude Init

## Project Overview
SimplyPages is a lightweight, domain-specific framework for building server-side rendered web applications with minimal JavaScript. Built for data-heavy applications like research portals, community platforms, and content management systems. Foundation for the Cannabis Research Portal, planned for eventual open source release.

## Framework Philosophy

### Core Principles
- **Server-First Rendering**: All HTML generation happens on the server using Java. No complex frontend build process.
- **Java-Native Development**: Build entire UIs using fluent, type-safe Java APIs. Target audience is Java developers with limited web experience.
- **Minimal JavaScript**: Use HTMX sparingly for dynamic updates only where needed (module refreshing, lazy loading, user interactions).
- **Simplicity Over Abstraction**: Avoid over-engineering. Keep patterns simple and maintainable.
- **Composable Architecture**: Build complex UIs from simple, reusable primitives using composition.
- **Domain-Specific Optimization**: Optimized for data display, forms, forums, and content-heavy applications.

### When to Use SimplyPages
- Research portals with heavy data visualization
- Community platforms (forums, discussions, journals)
- Content management systems with user-generated content
- Applications requiring minimal client-side JavaScript
- Projects maintained by Java developers with limited web experience

## Architecture & Core Concepts

### Component Hierarchy

```
Component (interface)
├── render(): String
│
└── HtmlTag (abstract base class)
    ├── Core Properties: tagName, attributes, children, innerText
    ├── Fluent Methods: withAttribute(), withChild(), withInnerText()
    │
    ├── Basic Components: Div, Paragraph, Header, Image, Markdown
    ├── Form Components: TextInput, TextArea, Select, Checkbox, RadioGroup, Button, Form
    ├── Display Components: Table, DataTable, Card, CardGrid, Alert, Badge, Tag, Label, InfoBox
    ├── Media Components: Gallery, Image, Video, Audio
    ├── Forum Components: ForumPost, PostList, Comment, CommentThread
    ├── Navigation Components: Link, NavBar, SideNav, Breadcrumb
    └── Layout Components: Row, Column, Grid, Container, Section, Page

Module (abstract class extends HtmlTag)
├── buildContent(): void (abstract)
├── Combines multiple components into functional units
├── ContentModule, FormModule, GalleryModule, DataModule, ForumModule
└── Purpose: High-level reusable page sections
```

### Rendering Flow

1. **Component Construction**: Build components using fluent builder APIs in Java
2. **Composition**: Nest components to create complex structures
3. **Rendering**: Call `render()` method to generate HTML string
4. **Controller Response**: Spring controller returns HTML via `@ResponseBody`
5. **Shell Generation**: ShellBuilder generates complete HTML shell structure programmatically
6. **Dynamic Updates**: HTMX handles partial page updates when needed

### Package Structure

```
io.mindspice.simplypages/
├── core/
│   ├── Component.java         # Base interface for all renderable elements
│   ├── HtmlTag.java          # Abstract base class for HTML elements
│   ├── Attribute.java        # HTML attribute representation
│   └── Style.java            # CSS style utilities
├── components/
│   ├── forms/                # Form input components
│   ├── display/              # Data display components
│   ├── media/                # Media components (images, video, audio)
│   ├── forum/                # Discussion/community components
│   ├── navigation/           # Navigation components
│   └── [basic components]    # Div, Paragraph, Header, etc.
├── layout/
│   ├── Page.java             # Page container with builder pattern
│   ├── Row.java              # 12-column grid row
│   ├── Column.java           # Grid column with responsive widths
│   ├── Grid.java             # CSS Grid-based layouts
│   ├── Container.java        # Constrained-width containers
│   ├── Section.java          # Semantic sections
│   └── Shell.java            # Base HTML shell wrapper
├── modules/
│   ├── Module.java           # Abstract base class for modules
│   ├── ContentModule.java    # Markdown/rich content display
│   ├── FormModule.java       # Complete forms with structure
│   ├── GalleryModule.java    # Image galleries
│   ├── DataModule.java       # Type-safe data tables
│   └── ForumModule.java      # Discussion threads
└── builders/
    ├── TopNavBuilder.java    # Navigation bar builder utilities
    └── SideNavBuilder.java   # Side navigation builder utilities
```

## Component System

All components implement the `Component` interface with a single `render()` method. Most components extend `HtmlTag`, which provides attribute management, child component nesting, fluent builder pattern, and HTML rendering logic.

### Key Design Patterns
- **Fluent API**: All components support method chaining for configuration
- **Static Factory Methods**: Every component has a static `create()` method
- **Lazy Building**: Content built at render time, not in constructors
- **Composition Over Inheritance**: Complex components built by nesting simple ones

### Markdown Support
The `Markdown` component renders rich text content server-side using CommonMark library with GitHub-Flavored Markdown table support.

### Sizing System

SimplyPages uses a **two-tier sizing system** with clear separation of concerns:

#### 1. Grid-Based Layout (Primary for Modules)
Use the **12-column grid system** for module layout via `Column.withWidth(1-12)`:

**Example - Two-column layout (66/33 split):**
```java
Row row = new Row()
    .withChild(Column.create().withWidth(8)  // 8/12 = 66%
        .withChild(ContentModule.create()
            .withTitle("Main Content")))
    .withChild(Column.create().withWidth(4)  // 4/12 = 33%
        .withChild(ContentModule.create()
            .withTitle("Sidebar")));
```

**Grid Width Options**:
- `withWidth(1-12)` - Specific column count (6 = 50%, 4 = 33%, 3 = 25%)
- `auto()` - Size to content
- `fill()` - Expand to fill remaining space

#### 2. CSS Width Constraints (For Components)
Use **CSS width methods** for constraining form inputs, images, and basic components:

**Example - Constrained form inputs:**
```java
Form.create()
    .addField("Username", TextInput.create("username").withMaxWidth("300px"))
    .addField("Email", TextInput.create("email").withMaxWidth("400px"))
    .addField("Submit", Button.create("Submit").withMinWidth("120px"));
```

**Available Methods** (inherited from `HtmlTag`):
- `withWidth(String)` - Sets exact width
- `withMaxWidth(String)` - Sets maximum width (responsive-friendly)
- `withMinWidth(String)` - Sets minimum width

**Supported CSS Units**: `px`, `%`, `rem`, `em`, `vw`, `vh`, `vmin`, `vmax`, `ch`, `auto`

**Utility Classes**: `.w-25` (25%), `.w-50` (50%), `.w-75` (75%), `.w-100` (100%), `.w-auto`

#### Sizing Decision Tree

**When sizing modules:**
- ✅ **Use Grid**: Wrap module in `Column.withWidth(1-12)`
- ❌ **Don't use CSS width on modules** - Modules don't have CSS width methods

**When sizing components (forms, images, basic elements):**
- ✅ **Use CSS width methods**: `withWidth()`, `withMaxWidth()`, `withMinWidth()`
- ✅ **Use utility classes**: `.w-50`, `.w-100` (for quick prototyping)

**Good vs Bad Examples:**

```java
// ✅ GOOD: Grid for module layout
Page.create()
    .addRow(row -> row
        .withChild(Column.create().withWidth(6)
            .withChild(ContentModule.create().withTitle("Left")))
        .withChild(Column.create().withWidth(6)
            .withChild(ContentModule.create().withTitle("Right"))));

// ✅ GOOD: CSS for form constraints
Form.create()
    .addField("Username", TextInput.create("user").withMaxWidth("300px"))
    .addField("Bio", TextArea.create("bio").withMaxWidth("600px"));

// ✅ GOOD: Utility classes for quick layouts
Card.create().withClass("w-75");  // 75% width card

// ✅ GOOD: Combine grid with component constraints
Row.create()
    .withChild(Column.create().withWidth(8)  // Grid controls layout
        .withChild(DataTable.create(User.class)
            .withMaxWidth("1200px")));  // Component constrained within column

// ❌ BAD: Can't use CSS width on modules (methods removed)
// module.withWidth("800px")  // Compile error - method doesn't exist
```

**Security**: All CSS width values are validated server-side using regex patterns to prevent CSS injection attacks. Invalid values throw `IllegalArgumentException`.

## Module System

Modules are high-level components that combine multiple primitives into complete functional units. They extend the `Module` abstract class and implement `buildContent()`.

### Module Base Class Pattern

```java
public abstract class Module extends HtmlTag {
    protected String moduleId;
    protected String title;

    // Subclasses implement this to build their content
    protected abstract void buildContent();

    @Override
    public String render() {
        buildContent();  // Lazy building at render time
        return super.render();
    }
}
```

### Built-in Modules
- **ContentModule**: Display formatted text and Markdown
- **DataModule**: Type-safe data table display with generics
- **FormModule**: Structured forms with styling
- **GalleryModule**: Image galleries with captions and columns
- **ForumModule**: Discussion threads and posts

### Creating Custom Modules
Extend the `Module` base class, implement `buildContent()`, and use composition to combine existing components. Add fluent configuration methods that return `this`.

## Layout System

### Page Structure
The `Page` class provides a builder pattern for constructing complete pages. Uses `addComponents()` and `addRow()` methods for layout composition.

### Grid System

SimplyPages uses a **12-column responsive grid system** for page layouts. This is the **primary mechanism for sizing modules**.

**Core Concepts**:
- **Row**: Flexbox container for columns (CSS class: `.row`)
- **Column**: Grid cell with width specification (CSS classes: `.col`, `.col-{width}`)
- **12-column math**: Column widths are fractions of 12
  - `withWidth(6)` = 6/12 = 50%
  - `withWidth(4)` = 4/12 = 33.33%
  - `withWidth(3)` = 3/12 = 25%
  - `withWidth(8)` = 8/12 = 66.67%

**Column Width Options**:
```java
Column.create().withWidth(8)   // 8/12 = 66% width
Column.create().withWidth(6)   // 6/12 = 50% width
Column.create().withWidth(4)   // 4/12 = 33% width
Column.create().withWidth(3)   // 3/12 = 25% width
Column.create().auto()         // Size to content (CSS: flex: 0 0 auto)
Column.create().fill()         // Expand to remaining space (CSS: flex: 1 1 0)
```

**Responsive Behavior**:
- **Desktop** (>769px): Columns display side-by-side
- **Mobile** (<480px): Columns stack vertically (100% width)
- **Tablet** (481-768px): Responsive breakpoint behavior

**Row Auto-Wrapping**:
`Row.withChild()` automatically wraps non-Column components in a `.col` div for convenience:

```java
// These are equivalent:
row.withChild(module);  // Auto-wrapped in div.col (flex: 1)
row.withChild(Column.create().withChild(module));  // Explicit Column

// For specific widths, ALWAYS use Column explicitly:
row.withChild(Column.create().withWidth(6).withChild(module));
```

**Common Layout Patterns**:

```java
// Two-column (66/33 split)
Row.create()
    .withChild(Column.create().withWidth(8).withChild(mainContent))
    .withChild(Column.create().withWidth(4).withChild(sidebar));

// Three-column equal width
Row.create()
    .withChild(Column.create().withWidth(4).withChild(module1))
    .withChild(Column.create().withWidth(4).withChild(module2))
    .withChild(Column.create().withWidth(4).withChild(module3));

// Auto-size sidebar, fill remaining with content
Row.create()
    .withChild(Column.create().auto().withChild(sidebar))
    .withChild(Column.create().fill().withChild(mainContent));

// Center content (6 columns centered in row)
Row.create()
    .withChild(Column.create().withWidth(6)
        .withClass("mx-auto")  // Margin auto centers column
        .withChild(centeredModule));
```

**Grid Best Practices**:
- Always use grid for module layout (not CSS width)
- Column widths should sum to 12 or less per row
- Rows automatically wrap excess columns to next line
- Use `withAlign()` and `withJustify()` for row alignment
- Use `auto()` for sidebars, `fill()` for main content areas

### Sizing System Migration Guide

**Breaking Change**: Module classes no longer have `withWidth()`, `withMaxWidth()`, or `withMinWidth()` methods as of the grid-first architecture refactor.

**Why the Change**:
To eliminate confusion between grid-based layout (primary for modules) and CSS constraints (for components). Modules should always be sized using the responsive grid system via Column wrappers.

**Migration Examples**:

```java
// ❌ OLD (no longer compiles):
ContentModule.create()
    .withTitle("Content")
    .withMaxWidth("800px");  // Error: method doesn't exist

// ✅ NEW (use Column wrapper with grid width):
Column.create()
    .withWidth(8)  // Use appropriate grid width (8/12 = 66%)
    .withChild(ContentModule.create()
        .withTitle("Content"));

// ❌ OLD (trying to center with CSS):
module.withWidth("600px").withClass("mx-auto");

// ✅ NEW (center using grid):
Column.create()
    .withWidth(6)  // 50% width
    .withClass("mx-auto")  // Center the column
    .withChild(module);
```

**Form Components Unchanged**:
`TextInput`, `TextArea`, `Select`, `Button`, and other form/basic components retain all CSS width methods. This change only affects Module subclasses (ContentModule, FormModule, DataModule, GalleryModule, ForumModule).

**If You Need Pixel-Perfect Constraint**:
Use a wrapper div with max-width around the module:

```java
Column.create()
    .withWidth(10)  // Grid width
    .withChild(new Div()
        .withMaxWidth("1000px")  // Pixel constraint
        .withChild(module));
```

### Shell Builder System
The shell builder system constructs the entire application layout programmatically:

- **ShellBuilder**: Complete application shell with top banner, account bar, side navigation, and content area
- **TopBannerBuilder**: Flexible top banners with optional images, titles, subtitles, and styling
- **AccountBarBuilder**: Secondary navigation bars with flexible left/right alignment
- **SideNavBuilder**: Navigation sidebars with sections and collapsible support

### Collapsible Side Navigation
Framework supports collapsible sidebars with:
- Desktop: Click toggle to collapse/expand (saves state in localStorage)
- Mobile: Sidebar slides in from left (off-canvas pattern)
- Smooth CSS transitions and persistent state

## HTMX Integration Patterns

### When to Use HTMX

**Use HTMX for:**
- Module refreshing (update without full page reload)
- Lazy loading (load data/content on demand)
- User interactions (form submissions, inline editing)
- Dynamic content (search results, filters, pagination)

**DO NOT use HTMX for:**
- Initial page rendering (use SSR)
- Simple navigation (use standard links)
- Static content display

### HTMX Response Pattern
Controller endpoints for HTMX return partial HTML (just the module/component HTML, not full page). All components support HTMX attributes via fluent API (`.withHxGet()`, `.withHxPost()`, `.withHxTarget()`, `.withHxSwap()`).

## Extension & Customization

### Creating Custom Components
Extend `HtmlTag` for new HTML elements. Override `render()` for custom HTML generation. Use composition to build complex structures.

### Extension via Interfaces
For cross-cutting concerns (like `Editable`, `Votable`, `Shareable`), use interfaces that components can implement.

### Builder Pattern Utilities
Create builder classes for common page patterns to encapsulate repeated layout structures.

## Responsive Design

### Breakpoints
- **Mobile**: < 480px (phones)
- **Tablet**: 481px - 768px
- **Desktop**: > 769px

### Responsive Behavior
All SimplyPages components and layouts are responsive by default using mobile-first design:
- Sidebar: Fixed 250px on desktop, off-canvas on mobile
- Grid: Auto-adjusts column widths, stacks on mobile
- Top Banner: Horizontal on desktop, vertical on mobile
- Account Bar: Horizontal on desktop, stacked on mobile

### Utility Classes
Framework provides comprehensive utility classes:
- **Text Alignment**: `.align-left`, `.align-center`, `.align-right`, `.align-justify`
- **Flexbox**: `.flex`, `.justify-*`, `.items-*` (start, end, center, between, around, evenly, stretch)
- **Spacing**: Tailwind-style margin/padding (`.m-0` to `.m-6`, `.mt-*`, `.mr-*`, `.mx-*`, `.my-*`, `.p-*`, etc.)
- **Display**: `.d-none`, `.d-block`, `.d-flex`, `.d-grid`, `.d-md-*` (responsive)
- **Width/Height**: `.w-25`, `.w-50`, `.w-75`, `.w-100`, `.w-auto`, `.h-*`

**Key Principle**: Framework provides structure and sensible defaults, but you have full control to customize through CSS classes, inline styles, or custom attributes.

## Security Considerations

### Critical Security Rules
1. **User Input Handling**: ALWAYS sanitize user input with `HtmlUtils.htmlEscape()` before rendering
2. **CSRF Protection**: Spring Security handles automatically, but configure for HTMX requests
3. **Server-Side Validation**: Never trust client-side validation alone
4. **HTMX Verification**: Validate `HX-Request` header to verify HTMX origin
5. **Authorization**: Check permissions for all HTMX endpoints with `@PreAuthorize`
6. **Content Security**:
   - CommonMark handles Markdown escaping
   - Validate file uploads (type, size, content)
   - Use parameterized queries (JPA handles this)

## Development Workflow

### Component Development
1. Design component API with fluent methods
2. Extend HtmlTag with base tag name
3. Implement render() if custom HTML generation needed
4. Add to demo controller
5. Test HTML output

### Module Development
1. Identify repeated component patterns
2. Extend Module and implement buildContent()
3. Add fluent configuration methods
4. Test with real data

### Git Workflow
Commit after completing each discrete feature or fix with clear, descriptive messages using imperative mood.

## Build & Deployment

### Core Dependencies
- `spring-boot-starter-web`
- `htmx.org` (via WebJars)
- `commonmark` + `commonmark-ext-gfm-tables`

### Build Commands
```bash
./mvnw clean install        # Build
./mvnw spring-boot:run      # Run
./mvnw test                 # Test
./mvnw package              # Package for deployment
```

### Using as Library
Install to local Maven repo with `./mvnw clean install`, then add dependency with groupId `io.mindspice` and artifactId `simplypages`.

## Development Guidelines

### Code Style
- Use constructor injection over field injection
- Follow Java conventions (camelCase methods, PascalCase classes)
- Keep methods focused and single-purpose
- Prefer composition over inheritance
- Use fluent builder patterns for complex objects
- Write self-documenting code with clear naming

### Component Design Principles
- Static `create()` factory method for all components
- Method chaining for all configuration
- Return `this` from configuration methods
- Lazy-build content in `render()`
- Avoid side effects in constructors

### Module Design Principles
- Implement `buildContent()` for all module logic
- Use composition to combine existing components
- Provide sensible defaults for optional properties
- Support both simple and advanced use cases

### Security Checklist
- [ ] Sanitize all user input before rendering
- [ ] Use parameterized queries for database access
- [ ] Validate file uploads (type, size, content)
- [ ] Implement CSRF protection on forms
- [ ] Verify authorization for protected endpoints
- [ ] Escape HTML in user-generated content
- [ ] Use HTTPS in production

### Git Commit Standards
Use imperative mood ("Add feature" not "Added feature"), be specific, include context about what and why changed, reference issue numbers when applicable.

## Future Roadmap

### Planned Features
**Core Framework**: Form validation, component theming, pagination, search/filter, date picker, rich text editor, file upload

**Data & Visualization**: Chart/graph components, data export (CSV/JSON/PDF), advanced DataTable features, real-time updates

**User Experience**: Modal/dialog, toast notifications, loading states, drag-and-drop, accessibility improvements

**Developer Experience**: Component playground, documentation generation, hot reload, testing utilities, performance profiling

**Cannabis Portal Specific**: Research data templates, voting/rating, user reputation, version control for collaborative pages, advanced forum features, grow journal timeline, strain comparison

### Open Source Plans
Documentation, testing, example applications, licensing, community guidelines, semantic versioning

## Resources

### Framework Documentation
- This CLAUDE.md for comprehensive guidance
- README.md for quick reference
- Demo application at `http://localhost:8080`

### External Documentation
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [HTMX Documentation](https://htmx.org/docs/)
- [CommonMark Spec](https://commonmark.org/)

### Cannabis Portal Context
- `/home/hickelpickle/Code/Java/cannasite/CLAUDE.md` - Main portal documentation

## Maintenance Philosophy
Designed for Java developers with limited web experience. Avoid over-abstraction and complex patterns. Prefer explicit over implicit behavior. Keep dependency count minimal. Favor server-side rendering over client-side complexity. Make the common case easy, advanced cases possible.
