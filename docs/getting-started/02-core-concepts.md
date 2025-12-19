# Part 2: Core Concepts

Now that you've built your first JHF application, let's dive deep into the framework's architecture and key abstractions. Understanding these core concepts will help you build more complex and maintainable applications.

## Component Hierarchy

JHF is built on a simple but powerful component hierarchy. Everything in JHF is a component that can render itself to HTML.

### The Component Interface

At the root is the `Component` interface with a single method:

```java
public interface Component {
    String render();
}
```

That's it! Every renderable element in JHF implements this interface. When you call `render()`, the component generates and returns its HTML string representation.

### The HtmlTag Base Class

Most components extend the abstract `HtmlTag` class, which implements `Component` and provides common functionality:

```java
public abstract class HtmlTag implements Component {
    protected String tagName;              // HTML tag (div, p, h1, etc.)
    protected Map<String, Attribute> attributes;  // HTML attributes
    protected List<Component> children;     // Nested components
    protected String innerText;             // Text content

    // Fluent configuration methods
    public HtmlTag withAttribute(String name, String value) { ... }
    public HtmlTag withChild(Component child) { ... }
    public HtmlTag withInnerText(String text) { ... }
    public HtmlTag withClass(String className) { ... }
    public HtmlTag withStyle(String property, String value) { ... }

    @Override
    public String render() {
        // Generates HTML: <tagName attributes>children/innerText</tagName>
    }
}
```

Key features of `HtmlTag`:

1. **Attributes**: HTML attributes like `id="user-card"` or `class="btn btn-primary"`
2. **Children**: Nested components that render inside this tag
3. **Inner Text**: Simple text content (alternative to children)
4. **Fluent Methods**: All configuration methods return `this` for chaining

### Factory Methods

Every component has a static `create()` factory method for convenient instantiation:

```java
// Instead of: new Div()
Div container = Div.create();

// Instead of: new Paragraph()
Paragraph text = Paragraph.create();

// Factory methods with parameters
Header heading = Header.h1("Title");  // <h1>Title</h1>
Alert success = Alert.success("Done!"); // Success alert with green styling
```

Why factory methods?
- **Readability**: `Div.create()` is clearer than `new Div()`
- **Flexibility**: Can return subtypes or pre-configured instances
- **Fluent starting point**: Immediately chain configuration methods

### Method Chaining

All configuration methods return `this` (or the parent object), enabling fluent method chaining:

```java
Card card = Card.create()
    .withClass("user-card")
    .withAttribute("data-user-id", "123")
    .withChild(Header.h2("John Doe"))
    .withChild(Paragraph.create().withInnerText("Software Developer"))
    .withChild(Link.create("/profile/123", "View Profile"));
```

This creates a card with:
- CSS class: `user-card`
- Data attribute: `data-user-id="123"`
- Three children: heading, paragraph, and link

## Components vs Modules

JHF has two levels of abstraction: **Components** (low-level) and **Modules** (high-level).

### Components: Low-Level HTML Elements

Components are individual HTML elements or simple combinations:

**Basic Components**:
- `Div`, `Paragraph`, `Header` (H1-H6)
- `Image`, `Link`, `Blockquote`
- `Code`, `Icon`, `Spacer`, `Divider`

**Form Components**:
- `TextInput`, `TextArea`, `Select`
- `Checkbox`, `RadioGroup`, `Button`
- `Form` (container with CSRF support)

**Display Components**:
- `Alert`, `Badge`, `Label`, `Tag`
- `Card`, `CardGrid`, `InfoBox`
- `Table`, `DataTable`
- `ProgressBar`, `Spinner`

**Navigation Components**:
- `Link`, `NavBar`, `SideNav`
- `Breadcrumb`, `Dropdown`

**Media Components**:
- `Gallery`, `Image`, `Video`, `Audio`

**Layout Components**:
- `Row`, `Column`, `Grid`
- `Container`, `Section`, `Page`

Use components when:
- You need fine-grained control over HTML
- Building simple UI elements
- Composing custom structures

### Modules: High-Level Functional Units

Modules are complex, reusable sections that combine multiple components:

**Available Modules**:
- `ContentModule` - Formatted text and Markdown display
- `FormModule` - Complete forms with structure and styling
- `DataModule` - Type-safe data table display
- `GalleryModule` - Image galleries with captions
- `ForumModule` - Discussion threads and posts
- `HeroModule` - Hero banners with CTAs
- `StatsModule` - Statistics display grids
- `TimelineModule` - Timeline visualization
- `AccordionModule` - Expandable sections
- `TabsModule` - Tab navigation
- `QuoteModule` - Quote/testimonial display
- `CalloutModule` - Callout boxes
- `ComparisonModule` - Side-by-side comparisons

Use modules when:
- You have repeated UI patterns
- Building complex multi-component structures
- Need domain-specific functionality
- Want quick, opinionated defaults

### Example: Component Composition

Building a user profile card with **components**:

```java
Card profileCard = Card.create()
    .withClass("profile-card")
    .withChild(new Div()
        .withClass("profile-header")
        .withChild(Image.create("/avatars/user123.jpg", "User Avatar")
            .withClass("avatar"))
        .withChild(Header.h3("John Doe")
            .withClass("profile-name")))
    .withChild(Paragraph.create()
        .withInnerText("Senior Java Developer with 10 years experience")
        .withClass("profile-bio"))
    .withChild(new Div()
        .withClass("profile-stats")
        .withChild(Badge.create("120 Posts"))
        .withChild(Badge.create("45 Followers"))
        .withChild(Badge.create("Member since 2020")))
    .withChild(Link.create("/profile/john", "View Full Profile")
        .withClass("btn btn-primary"));
```

This is explicit and flexible, but verbose for repeated patterns.

### Example: Module Simplification

If you have many profile cards, create a `ProfileCardModule`:

```java
public class ProfileCardModule extends Module {
    private String userId;
    private String name;
    private String bio;
    private int posts;
    private int followers;
    private String memberSince;

    @Override
    protected void buildContent() {
        this.withChild(new Div()
            .withClass("profile-header")
            .withChild(Image.create("/avatars/" + userId + ".jpg", name)
                .withClass("avatar"))
            .withChild(Header.h3(name).withClass("profile-name")));

        this.withChild(Paragraph.create()
            .withInnerText(bio)
            .withClass("profile-bio"));

        this.withChild(new Div()
            .withClass("profile-stats")
            .withChild(Badge.create(posts + " Posts"))
            .withChild(Badge.create(followers + " Followers"))
            .withChild(Badge.create("Member since " + memberSince)));

        this.withChild(Link.create("/profile/" + userId, "View Full Profile")
            .withClass("btn btn-primary"));
    }

    // Fluent configuration methods
    public ProfileCardModule withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public ProfileCardModule withName(String name) {
        this.name = name;
        return this;
    }

    // ... other setters
}
```

Now usage is clean and reusable:

```java
ProfileCardModule card = new ProfileCardModule()
    .withUserId("john")
    .withName("John Doe")
    .withBio("Senior Java Developer with 10 years experience")
    .withPosts(120)
    .withFollowers(45)
    .withMemberSince("2020");
```

We'll cover module creation in detail in Part 4.

## Pages

The `Page` class is a special layout container for building complete pages.

### PageBuilder Pattern

`Page` uses a builder pattern for constructing page layouts:

```java
String html = Page.create()
    .addComponents(Header.h1("Welcome"), Paragraph.create().withInnerText("Hello!"))
    .addRow(row -> row
        .withChild(Column.create().withWidth(8)
            .withChild(ContentModule.create().withTitle("Main Content")))
        .withChild(Column.create().withWidth(4)
            .withChild(ContentModule.create().withTitle("Sidebar"))))
    .render();
```

### Adding Content

Two methods for adding content:

**1. addComponents() - Direct Addition**
```java
page.addComponents(
    Header.h1("Title"),
    Paragraph.create().withInnerText("Some text"),
    Alert.info("Information")
);
```

Use for simple, sequential content without layout control.

**2. addRow() - Grid-Based Layout**
```java
page.addRow(row -> row
    .withChild(Column.create().withWidth(6).withChild(module1))
    .withChild(Column.create().withWidth(6).withChild(module2)));
```

Use for responsive layouts with column-based control (covered in Part 5).

### Page Features

- **Independent Scrolling**: `page.withIndependentScrolling()` - Content scrolls separately from application shell
- **Sticky Sidebar**: `page.withStickySidebar()` - Sidebar follows scroll (8/4 column split)
- **Responsive**: Automatically stacks columns on mobile devices

## Rendering Flow

Understanding the rendering flow helps you build efficient UIs and avoid common pitfalls.

### The Rendering Pipeline

1. **Construction**: Create component objects
   ```java
   Paragraph p = Paragraph.create();
   ```

2. **Configuration**: Set attributes, children, and content
   ```java
   p.withInnerText("Hello").withClass("greeting");
   ```

3. **Composition**: Nest components to form structure
   ```java
   Card card = Card.create().withChild(p);
   ```

4. **Rendering**: Call `render()` to generate HTML
   ```java
   String html = card.render(); // <div class="card"><p class="greeting">Hello</p></div>
   ```

5. **Spring Response**: Controller returns HTML to client
   ```java
   @GetMapping("/page")
   @ResponseBody
   public String getPage() {
       return card.render();
   }
   ```

6. **Browser Display**: Browser receives HTML and renders visually

### Lazy Building in Modules

Modules use a **lazy building pattern** to avoid re-rendering issues:

```java
public abstract class Module extends HtmlTag {
    private boolean isBuilt = false;

    protected abstract void buildContent();

    @Override
    public String render() {
        if (!isBuilt) {
            buildContent();  // Called only once
            isBuilt = true;
        }
        return super.render();
    }
}
```

The `buildContent()` method is called **only once**, during the first `render()` call. This prevents duplicate children and ensures consistent output.

### Construction vs Rendering

**Construction Time**: When you create the object
```java
Card card = Card.create();  // Object created, but no HTML yet
```

**Render Time**: When you call `.render()`
```java
String html = card.render();  // HTML generated NOW
```

For modules, `buildContent()` runs during render time, not construction time. This allows you to:
- Pass dynamic data to modules
- Configure modules after creation
- Avoid premature rendering

## Best Practices

Follow these patterns for clean, maintainable JHF code:

### 1. Use Static Factory Methods

```java
// Good
Card card = Card.create();
Header heading = Header.h1("Title");
Alert alert = Alert.success("Done!");

// Avoid
Card card = new Card();
Header heading = new Header("h1");
Alert alert = new Alert(AlertType.SUCCESS);
```

Factory methods are clearer and often provide convenience parameters.

### 2. Embrace Fluent API

```java
// Good - Fluent chaining
Card card = Card.create()
    .withClass("user-card")
    .withChild(Header.h2("Name"))
    .withChild(Paragraph.create().withInnerText("Bio"));

// Avoid - Imperative style
Card card = Card.create();
card.withClass("user-card");
card.withChild(Header.h2("Name"));
card.withChild(Paragraph.create().withInnerText("Bio"));
```

Fluent style is more readable and aligns with JHF conventions.

### 3. Prefer Composition Over Inheritance

```java
// Good - Compose existing components
public class UserBadge {
    public static Component create(User user) {
        return new Div()
            .withClass("user-badge")
            .withChild(Image.create(user.getAvatar(), user.getName()))
            .withChild(new Span().withInnerText(user.getName()));
    }
}

// Avoid - Unnecessary inheritance
public class UserBadge extends Div {
    public UserBadge(User user) {
        super();
        this.withClass("user-badge");
        // ...
    }
}
```

Composition is more flexible and easier to test.

### 4. Use Lazy Building for Modules

```java
// Good - Lazy building in buildContent()
public class DashboardModule extends Module {
    private List<Stat> stats;

    @Override
    protected void buildContent() {
        for (Stat stat : stats) {
            this.withChild(createStatCard(stat));
        }
    }

    public DashboardModule withStats(List<Stat> stats) {
        this.stats = stats;
        return this;
    }
}

// Avoid - Building in constructor
public class DashboardModule extends Module {
    public DashboardModule(List<Stat> stats) {
        for (Stat stat : stats) {
            this.withChild(createStatCard(stat));  // Too early!
        }
    }
}
```

Lazy building prevents re-rendering issues and allows configuration after construction.

### 5. Keep Rendering Logic Separate from Controllers

```java
// Good - Separate page class
@Component
public class HomePage {
    public String buildPage() {
        return Page.create()
            .addComponents(Header.h1("Home"))
            .render();
    }
}

@Controller
public class HomeController {
    private final HomePage homePage;

    @GetMapping("/")
    @ResponseBody
    public String home() {
        return homePage.buildPage();
    }
}

// Avoid - Rendering logic in controller
@Controller
public class HomeController {
    @GetMapping("/")
    @ResponseBody
    public String home() {
        return Page.create()
            .addComponents(Header.h1("Home"))
            .render();  // Mixing concerns
    }
}
```

Separation of concerns improves testability and maintainability.

## Common Gotchas

Watch out for these common pitfalls when using JHF:

### 1. Re-Rendering Bug

**Problem**: Calling `render()` multiple times on the same component can duplicate children.

```java
// PROBLEMATIC CODE
Card card = Card.create()
    .withChild(Paragraph.create().withInnerText("Hello"));

String html1 = card.render();  // <div><p>Hello</p></div>
String html2 = card.render();  // <div><p>Hello</p><p>Hello</p></div> - DUPLICATE!
```

**Why**: `HtmlTag.render()` mutates the children list during rendering. Calling it again adds duplicates.

**Solution 1**: Render once and cache the result
```java
Card card = Card.create()
    .withChild(Paragraph.create().withInnerText("Hello"));

String html = card.render();  // Render once
// Reuse 'html' string multiple times
```

**Solution 2**: Use modules (they prevent re-rendering)
```java
public class MyModule extends Module {
    @Override
    protected void buildContent() {
        this.withChild(Paragraph.create().withInnerText("Hello"));
    }
}

MyModule module = new MyModule();
String html1 = module.render();  // Safe
String html2 = module.render();  // Safe - buildContent() called only once
```

**Best Practice**: Always render components only once in production code.

### 2. Class Attribute Handling Inconsistency

**Problem**: Using `withAttribute("class", ...)` and `withClass(...)` behaves differently.

```java
// Scenario 1: withAttribute overwrites
Card card = Card.create()
    .withClass("card")              // Adds class: "card"
    .withAttribute("class", "big"); // OVERWRITES to: "big" only

// Generated: <div class="big">...</div>  (lost "card")

// Scenario 2: withClass appends
Card card = Card.create()
    .withClass("card")    // class="card"
    .withClass("big");    // class="card big" - APPENDS

// Generated: <div class="card big">...</div>  (correct)
```

**Why**: `withAttribute()` sets the attribute value directly, overwriting any existing value. `withClass()` specifically appends to the class attribute.

**Solution**: Always use `withClass()` for CSS classes
```java
// Good
component.withClass("card").withClass("big").withClass("shadow");

// Avoid
component.withAttribute("class", "card big shadow");
```

**Exception**: Use `withAttribute("class", ...)` only if you need to replace all classes:
```java
// Explicitly replacing all classes
component.withAttribute("class", "new-class");  // Intentional replacement
```

### 3. Width Sizing Decisions

**Problem**: Confusion between grid-based layout (for modules) and CSS width constraints (for components).

JHF has two separate sizing mechanisms:

**Grid System (for Modules)**:
```java
// CORRECT: Use Column wrapper with grid width
Page.create()
    .addRow(row -> row
        .withChild(Column.create().withWidth(8)  // Grid: 8/12 = 66%
            .withChild(ContentModule.create().withTitle("Main Content"))));
```

Module classes (`ContentModule`, `FormModule`, etc.) do NOT have `withWidth()`, `withMaxWidth()`, or `withMinWidth()` methods. Size them using grid columns.

**CSS Width (for Components)**:
```java
// CORRECT: Use CSS width methods on form components
Form.create()
    .addField("Email", TextInput.email("email").withMaxWidth("400px"))
    .addField("Submit", Button.submit("Send").withMinWidth("120px"));
```

Form components, basic components, and display components have CSS width methods.

**Decision Tree**:

```
Are you sizing a Module?
├─ YES → Use Column.withWidth(1-12) wrapper
└─ NO  → Is it a form/basic component?
         ├─ YES → Use component.withMaxWidth/withMinWidth/withWidth
         └─ NO  → Check component documentation
```

**Why the separation?**
- **Grid** provides responsive layout structure (mobile-first, automatic stacking)
- **CSS width** provides pixel-perfect constraints within that structure
- Modules are layout units (use grid), components are content units (use CSS)

**Example: Grid + CSS**
```java
Page.create()
    .addRow(row -> row
        .withChild(Column.create().withWidth(10)  // Grid: 10/12 = 83% layout width
            .withChild(new Div()
                .withMaxWidth("1000px")  // CSS: Constrain to max 1000px
                .withChild(ContentModule.create()))));
```

### 4. Forgetting to Call render()

**Problem**: Components are objects, not HTML strings.

```java
// WRONG
@GetMapping("/page")
@ResponseBody
public Component getPage() {
    return Page.create().addComponents(Header.h1("Title"));  // Returns object!
}

// CORRECT
@GetMapping("/page")
@ResponseBody
public String getPage() {
    return Page.create()
        .addComponents(Header.h1("Title"))
        .render();  // Returns HTML string
}
```

**Why**: Spring expects a `String` response body for HTML, not a Java object.

### 5. Mixing Inner Text and Children

**Problem**: Using both `withInnerText()` and `withChild()` on the same component.

```java
// PROBLEMATIC
Div container = Div.create()
    .withInnerText("Some text")
    .withChild(Paragraph.create().withInnerText("More text"));

// Result: Unpredictable (inner text may overwrite children or vice versa)
```

**Solution**: Choose one approach
```java
// Option 1: Use only inner text
Div container = Div.create()
    .withInnerText("Some text");

// Option 2: Use only children
Div container = Div.create()
    .withChild(new TextNode("Some text"))
    .withChild(Paragraph.create().withInnerText("More text"));
```

**When to use each**:
- **Inner Text**: Simple text-only content
- **Children**: Mixed content (text + components)

## Understanding the Component Lifecycle

### Component Creation
```java
Card card = Card.create();  // Object allocated
```

### Configuration Phase
```java
card.withClass("user-card")
    .withChild(Header.h2("Name"))
    .withChild(Paragraph.create().withInnerText("Bio"));
```

At this point:
- Object exists in memory
- Attributes and children are stored
- No HTML generated yet

### Rendering Phase
```java
String html = card.render();
```

Now:
- `render()` traverses the component tree
- Attributes converted to HTML strings
- Children rendered recursively
- Final HTML string generated

### Browser Phase
```java
return html;  // Spring sends to browser
```

Finally:
- Browser receives HTML string
- Browser parses HTML
- Browser displays visually

## Key Takeaways

1. **Everything is a Component**: All JHF elements implement `Component` interface
2. **Fluent API Everywhere**: Method chaining is the idiomatic style
3. **Components vs Modules**: Components for low-level, modules for high-level patterns
4. **Lazy Building**: Modules use `buildContent()` called during rendering
5. **Render Once**: Avoid calling `render()` multiple times on the same object
6. **Use withClass()**: Always use `withClass()` for CSS classes, not `withAttribute("class", ...)`
7. **Grid for Modules, CSS for Components**: Different sizing mechanisms for different abstractions
8. **Separation of Concerns**: Keep rendering logic in page classes, not controllers

## Practice Exercise

Try building a blog post card that displays:
- Post title (heading)
- Author and date (small text)
- Post excerpt (paragraph)
- "Read More" link
- Tags (badges)

```java
public class BlogPostCard {
    public static Component create(String title, String author,
                                   String date, String excerpt,
                                   List<String> tags, String url) {
        Card card = Card.create()
            .withClass("blog-post-card")
            .withChild(Header.h3(title))
            .withChild(new Div()
                .withClass("post-meta")
                .withChild(new Span().withInnerText("By " + author))
                .withChild(new Span().withInnerText(" • " + date)));

        card.withChild(Paragraph.create()
            .withInnerText(excerpt)
            .withClass("post-excerpt"));

        Div tagContainer = new Div().withClass("post-tags");
        for (String tag : tags) {
            tagContainer.withChild(Badge.create(tag));
        }
        card.withChild(tagContainer);

        card.withChild(Link.create(url, "Read More →")
            .withClass("btn btn-primary"));

        return card;
    }
}
```

Test it:
```java
Component post = BlogPostCard.create(
    "Getting Started with JHF",
    "John Doe",
    "2025-01-15",
    "Learn how to build server-side rendered web applications using Java...",
    Arrays.asList("Java", "Web", "Tutorial"),
    "/blog/getting-started"
);

String html = post.render();
```

---

**Previous**: [Part 1: Introduction](01-introduction.md)

**Next**: [Part 3: Building Custom Components](03-custom-components.md)

**Table of Contents**: [Getting Started Guide](README.md)
