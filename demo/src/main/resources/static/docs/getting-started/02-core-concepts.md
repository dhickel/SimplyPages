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
    // Define SlotKeys for dynamic content (like method parameters)
    public static final SlotKey<String> USER_ID = SlotKey.of("userId");
    public static final SlotKey<String> NAME = SlotKey.of("name");
    public static final SlotKey<String> BIO = SlotKey.of("bio");
    public static final SlotKey<Integer> POSTS = SlotKey.of("posts");
    public static final SlotKey<Integer> FOLLOWERS = SlotKey.of("followers");
    public static final SlotKey<String> MEMBER_SINCE = SlotKey.of("memberSince");

    @Override
    protected void buildContent() {
        this.withChild(new Div()
            .withClass("profile-header")
            .withChild(new Div()  // Image path needs special handling with Slot
                .withClass("avatar-container")
                .withChild(Slot.of(USER_ID)))  // You'd typically wrap this more elegantly
            .withChild(Header.h3()
                .withChild(Slot.of(NAME))
                .withClass("profile-name")));

        this.withChild(Paragraph.create()
            .withChild(Slot.of(BIO))
            .withClass("profile-bio"));

        this.withChild(new Div()
            .withClass("profile-stats")
            .withChild(Badge.create()
                .withChild(Slot.of(POSTS)))
            .withChild(Badge.create()
                .withChild(Slot.of(FOLLOWERS)))
            .withChild(Badge.create()
                .withChild(Slot.of(MEMBER_SINCE))));
    }
}
```

Now we create a Template (compiled once) and render with different data each time:

```java
// Compile the template once (in a static initializer or early in app lifecycle)
public static final Template PROFILE_TEMPLATE = Template.of(new ProfileCardModule());

// Render with data at request time
String html = PROFILE_TEMPLATE.render(
    RenderContext.builder()
        .with(ProfileCardModule.NAME, "John Doe")
        .with(ProfileCardModule.BIO, "Senior Java Developer with 10 years experience")
        .with(ProfileCardModule.POSTS, 120)
        .with(ProfileCardModule.FOLLOWERS, 45)
        .with(ProfileCardModule.MEMBER_SINCE, "2020")
        .build()
);
```

**Why SlotKeys instead of instance fields?**
- **Immutability**: Once the Template is built, it's locked. Fields can't change.
- **Thread-safety**: Templates are safe to share across requests.
- **Performance**: No component tree traversal for each render, just string interpolation.
- **Type-safety**: SlotKeys are typed (`SlotKey<String>`, `SlotKey<Integer>`), so you get compile-time checking.

We'll cover module creation in detail in Part 4, and deep dive into Templates in [Part 13: Templates and Dynamic Updates](13-templates-and-dynamic-updates.md).

## Understanding Templates

**Templates are like compiled Java classes for your UI.** They're a powerful pattern for dynamic content that you need to understand before building production applications.

### The Problem: Why We Need Templates

Consider a typical module with instance fields:

```java
// Old pattern - instance fields (DON'T DO THIS anymore)
public class UserCardModule extends Module {
    private String name;
    private String email;

    public UserCardModule withName(String name) {
        this.name = name;
        return this;
    }

    // ... more methods
}

// Usage
UserCardModule card = new UserCardModule()
    .withName("Alice")
    .withEmail("alice@example.com");
String html = card.render();  // Works once
String html2 = card.render(); // Problem: What if I want different data?
```

**The issue**: Modules use a "build-once" lifecycle. Once `buildContent()` runs, the module structure is locked. You cannot change a field and re-render expecting different output. The module doesn't rebuild—it just renders the same structure again.

### The Solution: Templates and SlotKeys

Templates separate **structure** (built once) from **data** (provided at render time):

```java
// Modern pattern - SlotKeys and Templates
public class UserCardModule extends Module {
    public static final SlotKey<String> NAME = SlotKey.of("name");
    public static final SlotKey<String> EMAIL = SlotKey.of("email");

    @Override
    protected void buildContent() {
        this.withChild(Header.h2().withChild(Slot.of(NAME)));
        this.withChild(Paragraph.create().withChild(Slot.of(EMAIL)));
    }
}

// Compile template once
public static final Template USER_TEMPLATE = Template.of(new UserCardModule());

// Render with different data each time
String html1 = USER_TEMPLATE.render(
    RenderContext.builder()
        .with(UserCardModule.NAME, "Alice")
        .with(UserCardModule.EMAIL, "alice@example.com")
        .build()
);

String html2 = USER_TEMPLATE.render(
    RenderContext.builder()
        .with(UserCardModule.NAME, "Bob")
        .with(UserCardModule.EMAIL, "bob@example.com")
        .build()
);
```

### How Templates Work

**Think of it like Java compilation**:

1. **Source Code** (your component structure with Slots)
   ```java
   Header.h2().withChild(Slot.of(NAME))
   ```

2. **Compilation** (Template.of() pre-processes the tree)
   - Walks the component tree
   - Builds static HTML fragments (`<h2>`, `</h2>`)
   - Notes where dynamic slots are (`Slot.of(NAME)`)
   - Stores everything as compiled segments

3. **Execution** (template.render(context) fills in the blanks)
   - Concatenates static fragments
   - Looks up values from RenderContext
   - Inserts them into the HTML
   - Returns the result

**Benefits**:
- **Performance**: No tree traversal on every render, just string concatenation
- **Thread-safe**: Templates are immutable after compilation, safe to share across requests
- **Type-safe**: SlotKeys enforce type checking at compile time
- **Predictable**: Structure doesn't change, only data does

### SlotKey Analogy

Think of **SlotKeys like method parameters**:

```java
// Java method with parameters
public void greetUser(String name, int age) {
    System.out.println("Hello " + name + ", age " + age);
}

greetUser("Alice", 30);  // Data at call time
greetUser("Bob", 25);    // Different data, same code

// Template with SlotKeys (similar concept)
public static final SlotKey<String> NAME = SlotKey.of("name");
public static final SlotKey<Integer> AGE = SlotKey.of("age");

// Template structure (like the method body)
Template template = Template.of(
    Paragraph.create()
        .withChild(Slot.of(NAME))
        .withChild(Slot.of(AGE))
);

// Render with data (like calling the method)
String html1 = template.render(
    RenderContext.builder()
        .with(NAME, "Alice")
        .with(AGE, 30)
        .build()
);

String html2 = template.render(
    RenderContext.builder()
        .with(NAME, "Bob")
        .with(AGE, 25)
        .build()
);
```

### When to Use Templates

**Use Templates when**:
- Rendering the same structure with different data multiple times
- Building dynamic content that changes per request
- Using HTMX for live updates (out-of-band swaps)
- Performance matters (fewer tree traversals)

**Simple components don't need Templates**:
```java
// Fine as-is - no template needed
return Card.create()
    .withClass("notification")
    .withChild(Alert.success("All systems operational"));
```

**Common use cases**:
- User cards, product listings, data tables
- Forum posts, comments, social feeds
- Dashboard widgets with live stats
- Search results, filter results

For a deep dive with advanced patterns, see [Part 13: Templates and Dynamic Updates](13-templates-and-dynamic-updates.md).

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

**Standard Components** (one-time render):

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

**Templates** (compile once, render many times):

1. **Construction**: Create component structure with Slots
   ```java
   Module module = new UserModule();
   ```

2. **Compilation** (Template.of()): Pre-process component tree
   ```java
   Template compiled = Template.of(module);
   // - Walks component tree
   // - Pre-generates static HTML fragments
   // - Identifies dynamic Slots
   // - Stores optimized segments
   ```

3. **Configuration** (RenderContext): Prepare data for slots
   ```java
   RenderContext context = RenderContext.builder()
       .with(UserModule.NAME, "Alice")
       .with(UserModule.EMAIL, "alice@example.com")
       .build();
   ```

4. **Rendering** (template.render(context)): Fill in the blanks
   ```java
   String html = compiled.render(context);
   // - Concatenates static fragments
   // - Inserts values from context
   // - Returns HTML string
   ```

5. **Spring Response**: Return rendered HTML
   ```java
   @GetMapping("/user/{id}")
   public String getUserCard(@PathVariable String id) {
       User user = userService.findById(id);
       return USER_TEMPLATE.render(
           RenderContext.builder()
               .with(UserModule.NAME, user.getName())
               .with(UserModule.EMAIL, user.getEmail())
               .build()
       );
   }
   ```

6. **Browser Display**: Browser receives HTML and renders visually

### Lazy Building in Modules

Modules use a **lazy building pattern** with a "build-once" lifecycle. This ensures that a module's structure is computed only once and then reused, which is essential for performance and the new **Template** system.

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

The `buildContent()` method is called **only once**, during the first `render()` or `build()` call.

**Important for Dynamic Content**: Since `buildContent()` only runs once, you cannot update the structure of a Module by changing its fields and re-rendering. Instead, you should use **Slots** and **Templates** for dynamic content (covered in detail in [Part 13: Templates and Dynamic Updates](13-templates-and-dynamic-updates.md)).

### Construction vs Rendering

**Construction Time**: When you create the object
```java
Card card = Card.create();  // Object created, but no HTML yet
```

**Render Time**: When you call `.render()`
```java
String html = card.render();  // HTML generated NOW
```

For modules, `buildContent()` runs during the first render time. This allows you to configure modules after creation, but before the structure is locked in.

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

### 6. Why My Module Fields Don't Update (The Template Solution)

**Problem**: You change a field on a Module and re-render, expecting different output. Nothing changes.

```java
// FRUSTRATING - This doesn't work as expected
public class StatCardModule extends Module {
    private int count;

    public StatCardModule withCount(int count) {
        this.count = count;
        return this;
    }

    @Override
    protected void buildContent() {
        this.withChild(Header.h2().withInnerText("Count: " + count));
    }
}

// Usage
StatCardModule card = new StatCardModule().withCount(5);
String html1 = card.render();  // <h2>Count: 5</h2> ✓

card.withCount(10);  // Changed the field
String html2 = card.render();  // STILL <h2>Count: 5</h2> ✗ No change!
```

**Why this happens**: Modules use a **build-once** lifecycle. The first time you call `render()`, `buildContent()` runs and locks the structure. Subsequent renders skip `buildContent()` and just render the locked structure. Changing fields afterwards has no effect.

**The Solution - Use Templates and SlotKeys**:

```java
// CORRECT - Use Templates for dynamic content
public class StatCardModule extends Module {
    public static final SlotKey<Integer> COUNT = SlotKey.of("count");

    @Override
    protected void buildContent() {
        // Use Slot instead of direct field access
        this.withChild(Header.h2()
            .withChild(new Span().withInnerText("Count: "))
            .withChild(Slot.of(COUNT)));
    }
}

// Compile the template once (typically as a static field)
public static final Template STAT_TEMPLATE = Template.of(new StatCardModule());

// Render with different data each time
String html1 = STAT_TEMPLATE.render(
    RenderContext.builder()
        .with(StatCardModule.COUNT, 5)
        .build()
);  // <h2>Count: 5</h2>

String html2 = STAT_TEMPLATE.render(
    RenderContext.builder()
        .with(StatCardModule.COUNT, 10)
        .build()
);  // <h2>Count: 10</h2> ✓ Works!
```

**Key Difference**:
- **Fields**: Structure is locked after first build. Changing fields is ignored.
- **SlotKeys + Templates**: Structure is locked, but data is flexible. Change the RenderContext, get different output.

**When to Use Each**:

| Scenario | Solution | Why |
|----------|----------|-----|
| Rendering once per request | Either approach | Both work fine |
| Rendering same structure, different data | Templates + SlotKeys | Performance, immutability, thread-safety |
| Dynamic HTMX updates | Templates + SlotKeys | Data changes, not structure |
| Simple static content | Either approach | Overhead not justified |
| Building lists of items | Templates + SlotKeys | Compile once, render many |

**Rule of Thumb**: If you catch yourself thinking "I want to re-render this with new data," reach for Templates and SlotKeys.

For comprehensive Template patterns, see [Part 13: Templates and Dynamic Updates](13-templates-and-dynamic-updates.md).

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
4. **Build-Once Lifecycle**: Modules build their structure once, then reuse it
5. **Templates for Dynamic Content**: Use Templates and SlotKeys for rendering same structure with different data
6. **Render Once**: Avoid calling `render()` multiple times on the same object (or use Templates instead)
7. **Use withClass()**: Always use `withClass()` for CSS classes, not `withAttribute("class", ...)`
8. **Grid for Modules, CSS for Components**: Different sizing mechanisms for different abstractions
9. **Separation of Concerns**: Keep rendering logic in page classes, not controllers

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
