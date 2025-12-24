# Quick Reference: Template System

A quick cheat sheet for using Templates, SlotKeys, and RenderContext in the Java HTML Framework.

## Table of Contents

- [Basic Pattern](#basic-pattern)
- [SlotKey Reference](#slotkey-reference)
- [Template Creation](#template-creation)
- [RenderContext Patterns](#rendercontext-patterns)
- [Common Slot Types](#common-slot-types)
- [HTMX Integration](#htmx-integration)
- [Decision Trees](#decision-trees)
- [Common Mistakes](#common-mistakes)

---

## Basic Pattern

```java
// 1. Define SlotKeys (static final fields)
public static final SlotKey<String> TITLE = SlotKey.of("title");
public static final SlotKey<String> BODY = SlotKey.of("body");

// 2. Create Template (static final field)
public static final Template MY_TEMPLATE = Template.of(
    ContentModule.create()
        .withChild(new Header.H2().withChild(Slot.of(TITLE)))
        .withChild(new Paragraph().withChild(Slot.of(BODY)))
);

// 3. Render with RenderContext
String html = MY_TEMPLATE.render(
    RenderContext.builder()
        .with(TITLE, "Hello World")
        .with(BODY, "This is the content.")
        .build()
);
```

**Think of it like:**
- SlotKeys = method parameters
- Template = compiled Java bytecode
- RenderContext = method arguments at runtime

---

## SlotKey Reference

### Creating SlotKeys

```java
// String slot
SlotKey<String> NAME = SlotKey.of("name");

// Integer slot
SlotKey<Integer> COUNT = SlotKey.of("count");

// Component slot (for dynamic components)
SlotKey<Component> CONTENT = SlotKey.of("content");

// List slot (for collections)
SlotKey<List<String>> ITEMS = SlotKey.of("items");

// Custom object slot
SlotKey<User> USER = SlotKey.of("user");
```

### Naming Conventions

```java
// ✅ GOOD: Uppercase constants, descriptive names
public static final SlotKey<String> CARD_TITLE = SlotKey.of("card_title");
public static final SlotKey<String> USER_EMAIL = SlotKey.of("user_email");

// ❌ BAD: Lowercase, generic names
public static final SlotKey<String> title = SlotKey.of("title");
public static final SlotKey<String> DATA = SlotKey.of("data");
```

### Type Safety

```java
SlotKey<String> NAME = SlotKey.of("name");
SlotKey<Integer> AGE = SlotKey.of("age");

RenderContext ctx = RenderContext.builder()
    .with(NAME, "Alice")      // ✅ Correct type
    .with(AGE, 25)            // ✅ Correct type
    .with(NAME, 123)          // ❌ Compile error - type mismatch!
    .build();
```

---

## Template Creation

### From Modules

```java
// Create template from module
public static final Template MODULE_TEMPLATE = Template.of(
    ContentModule.create()
        .withTitle("My Module")
        .withChild(Slot.of(CONTENT))
);
```

### From Components

```java
// Create template from any component
public static final Template DIV_TEMPLATE = Template.of(
    new Div()
        .withClass("card")
        .withChild(new Header.H3().withChild(Slot.of(TITLE)))
        .withChild(new Paragraph().withChild(Slot.of(BODY)))
);
```

### Composite Templates

```java
// Template containing multiple slots
public static final Template CARD_TEMPLATE = Template.of(
    new Div()
        .withClass("card")
        .withAttribute("id", Slot.of(CARD_ID))  // Slot in attribute
        .withChild(new Header.H3().withChild(Slot.of(TITLE)))
        .withChild(new Div().withChild(Slot.of(CONTENT)))  // Component slot
        .withChild(new Span().withChild(Slot.of(FOOTER)))
);
```

### Templates for Lists

```java
// Template for repeating items
public static final SlotKey<Component> LIST_ITEMS = SlotKey.of("list_items");

public static final Template LIST_TEMPLATE = Template.of(
    new HtmlTag("ul")
        .withClass("item-list")
        .withChild(Slot.of(LIST_ITEMS))
);

// Render with generated list items
Component items = new Component() {
    @Override
    public String render(RenderContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (String item : myList) {
            sb.append(new HtmlTag("li").withInnerText(item).render(ctx));
        }
        return sb.toString();
    }
};

String html = LIST_TEMPLATE.render(
    RenderContext.builder().with(LIST_ITEMS, items).build()
);
```

---

## RenderContext Patterns

### Basic Context

```java
RenderContext ctx = RenderContext.builder()
    .with(TITLE, "My Title")
    .with(BODY, "My Body")
    .build();
```

### All Supported Types

```java
RenderContext ctx = RenderContext.builder()
    // Strings
    .with(NAME, "Alice")

    // Numbers
    .with(AGE, 25)
    .with(PRICE, 19.99)

    // Components
    .with(CONTENT, new Paragraph().withInnerText("Hello"))

    // Lists
    .with(TAGS, List.of("java", "web", "htmx"))

    // Custom objects (toString() will be called)
    .with(USER, userObject)

    .build();
```

### Conditional Context

```java
// Build context conditionally
RenderContext.Builder builder = RenderContext.builder()
    .with(TITLE, article.getTitle())
    .with(CONTENT, article.getContent());

// Add optional data
if (article.hasAuthor()) {
    builder.with(AUTHOR, article.getAuthor());
}

if (showMetadata) {
    builder.with(CREATED, article.getCreated());
    builder.with(UPDATED, article.getUpdated());
}

RenderContext ctx = builder.build();
```

### Reusable Context Builder

```java
// Helper method to build context from domain object
private RenderContext buildArticleContext(Article article) {
    return RenderContext.builder()
        .with(ARTICLE_ID, article.getId())
        .with(ARTICLE_TITLE, article.getTitle())
        .with(ARTICLE_CONTENT, article.getContent())
        .with(ARTICLE_AUTHOR, article.getAuthor())
        .with(ARTICLE_UPDATED, article.getUpdated().toString())
        .build();
}

// Use in multiple places
String displayHtml = DISPLAY_TEMPLATE.render(buildArticleContext(article));
String editHtml = EDIT_TEMPLATE.render(buildArticleContext(article));
```

---

## Common Slot Types

### String Slots (Most Common)

```java
SlotKey<String> TITLE = SlotKey.of("title");

Template.of(
    new Header.H1().withChild(Slot.of(TITLE))
)

// Render
.with(TITLE, "Hello World")
```

### Component Slots (For Dynamic Content)

```java
SlotKey<Component> CONTENT = SlotKey.of("content");

Template.of(
    new Div().withChild(Slot.of(CONTENT))
)

// Render with different components
.with(CONTENT, new Paragraph().withInnerText("Text"))
.with(CONTENT, new Markdown("# Heading"))
.with(CONTENT, Table.create(...))
```

### Slots in Attributes

```java
SlotKey<String> MODULE_ID = SlotKey.of("module_id");
SlotKey<String> CSS_CLASS = SlotKey.of("css_class");

Template.of(
    new Div()
        .withAttribute("id", Slot.of(MODULE_ID))
        .withClass(Slot.of(CSS_CLASS))
)

// Render
.with(MODULE_ID, "widget-1")
.with(CSS_CLASS, "highlight active")
```

### Multiple Slots in One Element

```java
SlotKey<String> HREF = SlotKey.of("href");
SlotKey<String> LABEL = SlotKey.of("label");

Template.of(
    new Link(Slot.of(HREF), Slot.of(LABEL))
)

// Render
.with(HREF, "/articles/123")
.with(LABEL, "Read Article")
```

---

## HTMX Integration

### Template with HTMX Attributes

```java
public static final SlotKey<String> ENDPOINT = SlotKey.of("endpoint");
public static final SlotKey<String> TARGET = SlotKey.of("target");

public static final Template BUTTON_TEMPLATE = Template.of(
    new Button("Click Me")
        .withAttribute("hx-get", Slot.of(ENDPOINT))
        .withAttribute("hx-target", Slot.of(TARGET))
        .withAttribute("hx-swap", "outerHTML")
);

// Render
.with(ENDPOINT, "/api/data/123")
.with(TARGET, "#content-area")
```

### Out-of-Band Swap Pattern

```java
// 1. Template defines module ID as slot
public static final Template WIDGET_TEMPLATE = Template.of(
    ContentModule.create()
        .withModuleId(Slot.of(MODULE_ID))
        .withChild(Slot.of(CONTENT))
);

// 2. Render widget
String html = WIDGET_TEMPLATE.render(
    RenderContext.builder()
        .with(MODULE_ID, "stats-widget")
        .with(CONTENT, "Value: 123")
        .build()
);

// 3. Add OOB attribute for dynamic update
String oobHtml = html.replace(
    "id=\"stats-widget\"",
    "id=\"stats-widget\" hx-swap-oob=\"true\""
);

// 4. Return from controller
return oobHtml;
```

### Polling Pattern

```java
// Page includes polling trigger
new Div()
    .withAttribute("hx-get", "/api/stats")
    .withAttribute("hx-trigger", "every 3s")
    .withAttribute("hx-swap", "none")

// Controller returns template with OOB swaps
@GetMapping("/api/stats")
public String getStats() {
    String widget1 = renderWidget("widget-1", getData1());
    String widget2 = renderWidget("widget-2", getData2());
    return addOOB(widget1) + addOOB(widget2);
}
```

---

## Decision Trees

### When to Use Templates?

```
Do you need to render the same structure with different data?
├─ YES → Will it render more than ~10 times?
│         ├─ YES → Use Template (Pattern B) ✅
│         └─ NO  → Either works (Template still better for consistency)
└─ NO  → Does the structure change based on logic?
          ├─ YES → Use request-scoped components (Pattern A) ✅
          └─ NO  → Use Template anyway (easier to maintain) ✅
```

### Where to Define Templates?

```
Is the template used in multiple classes?
├─ YES → Define in shared utility class or the Module class itself
└─ NO  → Is it used in multiple methods of one class?
          ├─ YES → Define as static final field in that class
          └─ NO  → Define as static final field anyway (consistency)
```

### SlotKey Type Selection?

```
What type of data goes in the slot?
├─ Simple text → SlotKey<String>
├─ Number → SlotKey<Integer> or SlotKey<Double>
├─ Variable component → SlotKey<Component>
├─ List of items → SlotKey<Component> (render list in Component)
└─ Complex object → SlotKey<YourClass> (toString() will be called)
```

---

## Common Mistakes

### ❌ Mistake 1: Non-Static Template

```java
// ❌ BAD: Creates new template every request
public String render() {
    Template template = Template.of(...);  // DON'T!
    return template.render(ctx);
}

// ✅ GOOD: Template created once at class load
public static final Template MY_TEMPLATE = Template.of(...);

public String render() {
    return MY_TEMPLATE.render(ctx);
}
```

### ❌ Mistake 2: Wrong Slot Type

```java
// ❌ BAD: Type mismatch
SlotKey<String> COUNT = SlotKey.of("count");
ctx.with(COUNT, 42);  // Compile error!

// ✅ GOOD: Correct type
SlotKey<Integer> COUNT = SlotKey.of("count");
ctx.with(COUNT, 42);
```

### ❌ Mistake 3: Missing Slot Data

```java
// Template expects TITLE slot
Template.of(new Header.H1().withChild(Slot.of(TITLE)))

// ❌ BAD: Forgot to provide TITLE
RenderContext ctx = RenderContext.builder().build();

// ✅ GOOD: Provide all required slots
RenderContext ctx = RenderContext.builder()
    .with(TITLE, "My Title")
    .build();
```

### ❌ Mistake 4: Slot Name Collision

```java
// ❌ BAD: Same slot name, different keys
SlotKey<String> TITLE = SlotKey.of("title");
SlotKey<Integer> TITLE = SlotKey.of("title");  // Confusing!

// ✅ GOOD: Unique, descriptive names
SlotKey<String> ARTICLE_TITLE = SlotKey.of("article_title");
SlotKey<String> PAGE_TITLE = SlotKey.of("page_title");
```

### ❌ Mistake 5: Modifying After Render

```java
// ❌ BAD: Trying to change template after creation
MY_TEMPLATE.withChild(...);  // Templates are immutable!

// ✅ GOOD: Use different RenderContext for variations
TEMPLATE.render(ctx1);  // Render with one dataset
TEMPLATE.render(ctx2);  // Render with different dataset
```

---

## Code Examples by Use Case

### Use Case: Stat Widget

```java
// Define
public static final SlotKey<String> LABEL = SlotKey.of("label");
public static final SlotKey<String> VALUE = SlotKey.of("value");

public static final Template STAT = Template.of(
    new Div().withClass("stat")
        .withChild(new Span().withClass("label").withChild(Slot.of(LABEL)))
        .withChild(new Span().withClass("value").withChild(Slot.of(VALUE)))
);

// Use
String html = STAT.render(
    RenderContext.builder()
        .with(LABEL, "Active Users")
        .with(VALUE, "1,234")
        .build()
);
```

### Use Case: Article Card

```java
// Define
public static final SlotKey<String> ID = SlotKey.of("id");
public static final SlotKey<String> TITLE = SlotKey.of("title");
public static final SlotKey<String> EXCERPT = SlotKey.of("excerpt");
public static final SlotKey<String> AUTHOR = SlotKey.of("author");

public static final Template ARTICLE_CARD = Template.of(
    new Div()
        .withAttribute("id", Slot.of(ID))
        .withClass("article-card")
        .withChild(new Header.H3().withChild(Slot.of(TITLE)))
        .withChild(new Paragraph().withChild(Slot.of(EXCERPT)))
        .withChild(new Span().withClass("author").withChild(Slot.of(AUTHOR)))
);

// Use
String html = ARTICLE_CARD.render(
    RenderContext.builder()
        .with(ID, "article-123")
        .with(TITLE, "Getting Started")
        .with(EXCERPT, "Learn the basics...")
        .with(AUTHOR, "Alice")
        .build()
);
```

### Use Case: Form Field

```java
// Define
public static final SlotKey<String> FIELD_NAME = SlotKey.of("field_name");
public static final SlotKey<String> FIELD_LABEL = SlotKey.of("field_label");
public static final SlotKey<String> FIELD_VALUE = SlotKey.of("field_value");

public static final Template TEXT_FIELD = Template.of(
    new Div().withClass("form-group")
        .withChild(new Label(Slot.of(FIELD_LABEL)))
        .withChild(
            new TextInput(Slot.of(FIELD_NAME))
                .withValue(Slot.of(FIELD_VALUE))
                .withClass("form-control")
        )
);

// Use
String html = TEXT_FIELD.render(
    RenderContext.builder()
        .with(FIELD_NAME, "email")
        .with(FIELD_LABEL, "Email Address")
        .with(FIELD_VALUE, "alice@example.com")
        .build()
);
```

---

## Performance Tips

### ✅ DO: Pre-compile Templates

```java
// Creates template once at class load time
public static final Template MY_TEMPLATE = Template.of(...);
```

### ✅ DO: Reuse Templates

```java
// Render same template many times with different data
for (Article article : articles) {
    String html = ARTICLE_TEMPLATE.render(buildContext(article));
    results.add(html);
}
```

### ✅ DO: Cache Rendered HTML (If Data Rarely Changes)

```java
// Cache rendered output for expensive templates
private final Map<String, String> cache = new ConcurrentHashMap<>();

public String getRenderedWidget(String id) {
    return cache.computeIfAbsent(id, k ->
        WIDGET_TEMPLATE.render(buildContext(k))
    );
}
```

### ❌ DON'T: Create Templates in Loops

```java
// ❌ BAD: Creating templates repeatedly
for (int i = 0; i < 1000; i++) {
    Template t = Template.of(...);  // Expensive!
    results.add(t.render(ctx));
}

// ✅ GOOD: Reuse pre-compiled template
public static final Template T = Template.of(...);

for (int i = 0; i < 1000; i++) {
    results.add(T.render(buildContext(i)));  // Fast!
}
```

---

## Next Steps

- **Full Guide**: [Templates and Dynamic Updates](../getting-started/13-templates-and-dynamic-updates.md)
- **Examples**: [Live Dashboard](../examples/live-dashboard.md), [Wiki Editing](../examples/wiki-style-editing.md)
- **Advanced**: [Rendering Patterns](../advanced/rendering-patterns.md)
- **HTMX Integration**: [HTMX Dynamic Features](../getting-started/09-htmx-dynamic-features.md)
