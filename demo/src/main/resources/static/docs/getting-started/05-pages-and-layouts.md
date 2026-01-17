# Part 5: Pages and Layouts

Mastering page layouts is essential for building professional web applications. JHF provides a powerful 12-column responsive grid system that makes layout construction intuitive and mobile-friendly.

## Page Structure

The `Page` class is your starting point for building complete pages.

### What is a Page?

A `Page` is a container component that:
- Holds your entire page content
- Provides a consistent structure
- Renders as a `<div class="page-content">` by default
- Integrates with the application shell (covered in Part 6)

### Creating a Basic Page

```java
String html = Page.create()
    .addComponents(
        Header.h1("Welcome"),
        Paragraph.create().withInnerText("This is my page content.")
    )
    .render();
```

This generates:
```html
<div class="page-content">
    <h1>Welcome</h1>
    <p>This is my page content.</p>
</div>
```

## Adding Content to Pages

JHF provides two methods for adding content to pages, each suited for different use cases.

### Method 1: addComponents() - Simple Sequential Content

Use `addComponents()` when you want simple, top-to-bottom content without explicit layout control:

```java
Page.create()
    .addComponents(
        Header.h1("My Page"),
        Alert.info("Welcome to our site!"),
        Paragraph.create().withInnerText("Some content here..."),
        Card.create().withChild(Paragraph.create().withInnerText("Card content"))
    )
    .render();
```

**When to use**:
- Simple content flow
- No side-by-side layouts needed
- Quick prototyping
- Single-column pages

### Method 2: addRow() - Grid-Based Layout

Use `addRow()` when you need precise layout control with responsive columns:

```java
Page.create()
    .addRow(row -> row
        .withChild(Column.create().withWidth(8)
            .withChild(ContentModule.create().withTitle("Main Content")))
        .withChild(Column.create().withWidth(4)
            .withChild(ContentModule.create().withTitle("Sidebar"))))
    .render();
```

**When to use**:
- Multi-column layouts
- Responsive designs
- Precise width control
- Professional page layouts

### Mixing Both Approaches

You can combine both methods in the same page:

```java
Page.create()
    // Simple header
    .addComponents(Header.h1("Dashboard"))

    // Grid layout for content
    .addRow(row -> row
        .withChild(Column.create().withWidth(6)
            .withChild(module1))
        .withChild(Column.create().withWidth(6)
            .withChild(module2)))

    // Simple footer
    .addComponents(Divider.create(), Paragraph.create().withInnerText("© 2025"))
    .render();
```

## The 12-Column Grid System

JHF uses a **12-column responsive grid system** inspired by Bootstrap. This is the **primary mechanism for page layouts**.

### How the Grid Works

Think of each row as divided into 12 equal columns:

```
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |10 |11 |12 |
```

You specify how many columns each element should span:

```java
Row.create()
    .withChild(Column.create().withWidth(6)   // 6/12 = 50%
        .withChild(content1))
    .withChild(Column.create().withWidth(6)   // 6/12 = 50%
        .withChild(content2))
```

### Column Width Math

Common column widths as fractions of 12:

| Width | Fraction | Percentage | Use Case |
|-------|----------|------------|----------|
| 1 | 1/12 | 8.33% | Tiny elements, icons |
| 2 | 2/12 | 16.67% | Small badges, labels |
| 3 | 3/12 | 25% | Four-column grid |
| 4 | 4/12 | 33.33% | Three-column grid, sidebar |
| 6 | 6/12 | 50% | Two-column grid, split view |
| 8 | 8/12 | 66.67% | Main content with sidebar |
| 9 | 9/12 | 75% | Wide main content |
| 12 | 12/12 | 100% | Full-width content |

### Creating Rows and Columns

**Basic Row**:
```java
Row row = new Row()
    .withChild(Column.create().withWidth(12)
        .withChild(Header.h1("Full Width Header")));
```

**Two Equal Columns**:
```java
Row row = new Row()
    .withChild(Column.create().withWidth(6)
        .withChild(Paragraph.create().withInnerText("Left column")))
    .withChild(Column.create().withWidth(6)
        .withChild(Paragraph.create().withInnerText("Right column")));
```

**Three Equal Columns**:
```java
Row row = new Row()
    .withChild(Column.create().withWidth(4).withChild(module1))
    .withChild(Column.create().withWidth(4).withChild(module2))
    .withChild(Column.create().withWidth(4).withChild(module3));
```

**Main Content + Sidebar (66/33 split)**:
```java
Row row = new Row()
    .withChild(Column.create().withWidth(8)
        .withChild(ContentModule.create().withTitle("Main Article")))
    .withChild(Column.create().withWidth(4)
        .withChild(ContentModule.create().withTitle("Related Links")));
```

## Column Width Options

JHF provides three ways to specify column widths:

### 1. Fixed Width (withWidth 1-12)

Specify exact column count:

```java
Column.create().withWidth(8)   // 8 columns = 66.67%
Column.create().withWidth(4)   // 4 columns = 33.33%
Column.create().withWidth(12)  // 12 columns = 100%
```

Generates CSS class: `.col-8`, `.col-4`, `.col-12`

### 2. Auto Width (auto())

Size to content, no flex growth:

```java
Column.create().auto()  // Size to content
```

Generates CSS: `flex: 0 0 auto`

**Use case**: Sidebar that should only be as wide as its content
```java
Row.create()
    .withChild(Column.create().auto()
        .withChild(SideNavBuilder.create()...))  // Sidebar sizes to nav width
    .withChild(Column.create().fill()
        .withChild(mainContent));                 // Main content fills remaining
```

### 3. Fill Width (fill())

Expand to fill remaining space:

```java
Column.create().fill()  // Expands to available space
```

Generates CSS: `flex: 1 1 0`

**Use case**: Main content that fills the rest of the row
```java
Row.create()
    .withChild(Column.create().withWidth(3)
        .withChild(sidebar))    // Fixed 3 columns
    .withChild(Column.create().fill()
        .withChild(content));   // Fills remaining 9 columns
```

## Row Auto-Wrapping

JHF has a convenience feature: `Row.withChild()` automatically wraps non-Column components.

### Explicit Column (Recommended for Width Control)

```java
Row.create()
    .withChild(Column.create().withWidth(6)
        .withChild(module1))
    .withChild(Column.create().withWidth(6)
        .withChild(module2));
```

### Auto-Wrapped (Convenience)

```java
Row.create()
    .withChild(module1)  // Auto-wrapped in <div class="col">
    .withChild(module2); // Auto-wrapped in <div class="col">
```

Both modules get wrapped in `.col` divs with `flex: 1`, making them equal width.

### When to Use Each

**Use explicit Column when**:
- You need specific widths (`.withWidth(8)`, `.withWidth(4)`)
- You want precise control
- Building production layouts

**Use auto-wrapping when**:
- You want equal-width columns
- Prototyping quickly
- The default behavior is acceptable

**Example - Explicit vs Auto**:
```java
// Explicit (different widths)
Row.create()
    .withChild(Column.create().withWidth(8).withChild(main))
    .withChild(Column.create().withWidth(4).withChild(sidebar));

// Auto (equal widths)
Row.create()
    .withChild(card1)
    .withChild(card2)
    .withChild(card3);  // All three equal width
```

## Responsive Behavior

JHF's grid system is **mobile-first and responsive**, adapting to different screen sizes automatically.

### Breakpoints

JHF uses these responsive breakpoints:

| Device | Size | Behavior |
|--------|------|----------|
| **Mobile** | < 480px | Columns stack vertically (100% width each) |
| **Tablet** | 481px - 768px | Responsive breakpoint transitions |
| **Desktop** | > 769px | Columns display side-by-side |

### Desktop Layout (>769px)

Columns appear side-by-side:

```java
Row.create()
    .withChild(Column.create().withWidth(6).withChild(left))
    .withChild(Column.create().withWidth(6).withChild(right));
```

**Desktop view**: Two columns side-by-side (50% each)

### Mobile Layout (<480px)

Columns automatically stack vertically:

**Mobile view**:
```
┌─────────────────┐
│   Left Column   │  100% width
│   (was 50%)     │
└─────────────────┘
┌─────────────────┐
│  Right Column   │  100% width
│   (was 50%)     │
└─────────────────┘
```

### No Extra Code Needed

Responsiveness is **automatic**. The same code works on all devices:

```java
// This code is responsive by default
Row.create()
    .withChild(Column.create().withWidth(8).withChild(article))
    .withChild(Column.create().withWidth(4).withChild(sidebar));
```

- **Desktop**: Article (66%) + Sidebar (33%) side-by-side
- **Mobile**: Article (100%) then Sidebar (100%) stacked

### Understanding Mobile-First Design

**Mobile-first** means:
1. Design for mobile devices first
2. Layouts adapt UP to larger screens
3. Content remains accessible on all devices
4. No horizontal scrolling on small screens

JHF handles this automatically, but you should:
- Test your layouts on mobile
- Keep important content above the fold
- Avoid too many columns (3-4 max on desktop)

## Common Layout Patterns

### Pattern 1: Full-Width Header + Content

```java
Page.create()
    // Full-width header
    .addRow(row -> row
        .withChild(Column.create().withWidth(12)
            .withChild(Header.h1("My Application"))
            .withChild(Breadcrumb.create()
                .addItem("Home", "/")
                .addItem("About", "/about"))))

    // Two-column content
    .addRow(row -> row
        .withChild(Column.create().withWidth(8)
            .withChild(ContentModule.create().withTitle("Main Content")))
        .withChild(Column.create().withWidth(4)
            .withChild(ContentModule.create().withTitle("Sidebar"))))

    .render();
```

### Pattern 2: Three Equal Columns

```java
Page.create()
    .addRow(row -> row
        .withChild(Column.create().withWidth(4)
            .withChild(Card.create()
                .withChild(Header.h3("Feature 1"))))
        .withChild(Column.create().withWidth(4)
            .withChild(Card.create()
                .withChild(Header.h3("Feature 2"))))
        .withChild(Column.create().withWidth(4)
            .withChild(Card.create()
                .withChild(Header.h3("Feature 3")))))
    .render();
```

**Desktop**: Three columns side-by-side
**Mobile**: Three cards stacked vertically

### Pattern 3: Centered Content

```java
Page.create()
    .addRow(row -> row
        .withChild(Column.create().withWidth(6)
            .withClass("mx-auto")  // Margin auto centers the column
            .withChild(Card.create()
                .withChild(Header.h2("Sign In"))
                .withChild(Form.create()...))))
    .render();
```

The `.mx-auto` utility class centers the 6-column (50% width) container.

### Pattern 4: Main Content + Auto-Sized Sidebar

```java
Page.create()
    .addRow(row -> row
        .withChild(Column.create().auto()
            .withChild(SideNavBuilder.create()
                .addSection("Navigation", ...)
                .build()))
        .withChild(Column.create().fill()
            .withChild(ContentModule.create()
                .withTitle("Article Content"))))
    .render();
```

Sidebar sizes to its content width, main content fills the rest.

### Pattern 5: Dashboard Grid

```java
Page.create()
    // Header row
    .addRow(row -> row
        .withChild(Column.create().withWidth(12)
            .withChild(Header.h1("Dashboard"))))

    // Stats row (4 equal columns)
    .addRow(row -> row
        .withChild(Column.create().withWidth(3)
            .withChild(createStatCard("Users", 1250)))
        .withChild(Column.create().withWidth(3)
            .withChild(createStatCard("Revenue", "$45K")))
        .withChild(Column.create().withWidth(3)
            .withChild(createStatCard("Orders", 320)))
        .withChild(Column.create().withWidth(3)
            .withChild(createStatCard("Growth", "+15%"))))

    // Content row (chart + activity feed)
    .addRow(row -> row
        .withChild(Column.create().withWidth(8)
            .withChild(Card.create()
                .withChild(Header.h3("Revenue Chart"))))
        .withChild(Column.create().withWidth(4)
            .withChild(ActivityFeedModule.create()
                .withTitle("Recent Activity"))))

    .render();
```

**Desktop**:
- 4 stat cards in a row
- Chart (66%) + Activity feed (33%)

**Mobile**:
- 4 stat cards stacked
- Chart then activity feed stacked

### Pattern 6: Blog Layout

```java
Page.create()
    // Hero section
    .addRow(row -> row
        .withChild(Column.create().withWidth(12)
            .withChild(HeroModule.create()
                .withTitle("Welcome to Our Blog")
                .withSubtitle("Latest articles and insights"))))

    // Post grid (3 columns)
    .addRow(row -> row
        .withChild(Column.create().withWidth(4)
            .withChild(createPostCard(post1)))
        .withChild(Column.create().withWidth(4)
            .withChild(createPostCard(post2)))
        .withChild(Column.create().withWidth(4)
            .withChild(createPostCard(post3))))

    // Featured post + sidebar
    .addRow(row -> row
        .withChild(Column.create().withWidth(8)
            .withChild(createFeaturedPost(featuredPost)))
        .withChild(Column.create().withWidth(4)
            .withChild(InfoBox.create("Categories")
                .addItem("Technology", "/tech")
                .addItem("Business", "/business"))))

    .render();
```

## Grid vs CSS Width

**Important**: JHF has TWO separate sizing mechanisms, and understanding when to use each is critical.

### Grid System (For Modules and Layout)

Use the **12-column grid** (Row + Column) for:
- Page layout structure
- Positioning modules
- Responsive multi-column designs
- Main content vs sidebar arrangements

```java
// CORRECT: Module sizing via grid
Page.create()
    .addRow(row -> row
        .withChild(Column.create().withWidth(8)  // Grid controls layout
            .withChild(ContentModule.create())))
    .render();
```

**Why**: Modules don't have CSS width methods. They're sized by their container (Column).

### CSS Width (For Components)

Use **CSS width methods** for:
- Form input constraints
- Image sizing
- Button widths
- Basic component constraints

```java
// CORRECT: Form input sizing via CSS
Form.create()
    .addField("Email", TextInput.email("email").withMaxWidth("400px"))
    .addField("Submit", Button.submit("Send").withMinWidth("120px"));
```

**Why**: Form and basic components have CSS width methods for fine-grained control.

### Decision Tree

```
What are you sizing?
├─ Module (ContentModule, DataModule, etc.)
│  └─ Use Column.withWidth(1-12) wrapper
│
└─ Component (TextInput, Image, Button, etc.)
   └─ Use component.withMaxWidth/withMinWidth/withWidth
```

### Examples: Grid + CSS Combined

**Correct combination**:
```java
Page.create()
    .addRow(row -> row
        // Grid controls overall layout
        .withChild(Column.create().withWidth(10)
            .withChild(new Div()
                // CSS constrains within the grid column
                .withMaxWidth("1000px")
                .withChild(ContentModule.create()))))
    .render();
```

**Common mistake**:
```java
// WRONG: Can't use CSS width on modules
ContentModule.create()
    .withMaxWidth("800px");  // Compile error - method doesn't exist!

// CORRECT: Use grid instead
Column.create().withWidth(8)
    .withChild(ContentModule.create());
```

## Advanced Page Features

### Feature 1: Independent Scrolling

By default, page content scrolls with the entire window. Enable independent scrolling to scroll content separately from the application shell:

```java
Page page = Page.create()
    .withIndependentScrolling()  // Enable independent scroll
    .addComponents(...)
    .render();
```

**Use case**: Fixed header/sidebar, scrollable content area

### Feature 2: Sticky Sidebar

Create a sticky sidebar that follows the page scroll:

```java
Page page = Page.create()
    .withStickySidebar()  // Enables sticky sidebar
    .addComponents(...)
    .render();
```

This creates an **8/4 column split** with:
- Main content (8 columns, scrollable)
- Sidebar (4 columns, sticky following scroll)
- localStorage persistence of scroll position

### Feature 3: Multiple Rows

Chain multiple rows for complex layouts:

```java
Page.create()
    .addRow(row -> row.withChild(Column.create().withWidth(12).withChild(header)))
    .addRow(row -> row
        .withChild(Column.create().withWidth(8).withChild(main))
        .withChild(Column.create().withWidth(4).withChild(sidebar)))
    .addRow(row -> row.withChild(Column.create().withWidth(12).withChild(footer)))
    .render();
```

### Feature 4: Row Alignment and Justification

Control how columns are aligned within rows:

```java
Row.create()
    .withJustify("center")   // Horizontally center columns
    .withAlign("start")      // Align columns to top
    .withChild(Column.create().withWidth(6).withChild(content));
```

**Justify options**: `start`, `end`, `center`, `between`, `around`, `evenly`
**Align options**: `start`, `end`, `center`, `stretch`

## Complete Layout Examples

### Example 1: E-Commerce Product Page

```java
Page.create()
    // Breadcrumb
    .addRow(row -> row
        .withChild(Column.create().withWidth(12)
            .withChild(Breadcrumb.create()
                .addItem("Home", "/")
                .addItem("Products", "/products")
                .addItem("Laptop", "/products/laptop"))))

    // Product display
    .addRow(row -> row
        // Product images (left)
        .withChild(Column.create().withWidth(6)
            .withChild(GalleryModule.create()
                .withImages(productImages)))
        // Product details (right)
        .withChild(Column.create().withWidth(6)
            .withChild(Header.h1(product.getName()))
            .withChild(Paragraph.create().withInnerText("$" + product.getPrice()))
            .withChild(Paragraph.create().withInnerText(product.getDescription()))
            .withChild(Button.create("Add to Cart").withClass("btn btn-primary"))))

    // Product details tabs
    .addRow(row -> row
        .withChild(Column.create().withWidth(12)
            .withChild(TabsModule.create()
                .addTab("Description", product.getFullDescription())
                .addTab("Specifications", product.getSpecs())
                .addTab("Reviews", reviewsHtml))))

    .render();
```

### Example 2: Dashboard

```java
Page.create()
    // Page title
    .addComponents(Header.h1("Analytics Dashboard"))

    // KPI cards (4 columns)
    .addRow(row -> row
        .withChild(Column.create().withWidth(3)
            .withChild(StatsModule.create()
                .withTitle("Total Users")
                .withValue("12,543")
                .withTrend("+5.2%")))
        .withChild(Column.create().withWidth(3)
            .withChild(StatsModule.create()
                .withTitle("Revenue")
                .withValue("$48,290")
                .withTrend("+12.1%")))
        .withChild(Column.create().withWidth(3)
            .withChild(StatsModule.create()
                .withTitle("Orders")
                .withValue("823")
                .withTrend("-2.4%")))
        .withChild(Column.create().withWidth(3)
            .withChild(StatsModule.create()
                .withTitle("Avg. Order")
                .withValue("$58.70")
                .withTrend("+3.1%"))))

    // Charts row
    .addRow(row -> row
        // Line chart (2/3 width)
        .withChild(Column.create().withWidth(8)
            .withChild(Card.create()
                .withChild(Header.h3("Revenue Over Time"))
                .withChild(chartComponent)))
        // Pie chart (1/3 width)
        .withChild(Column.create().withWidth(4)
            .withChild(Card.create()
                .withChild(Header.h3("Traffic Sources"))
                .withChild(pieChartComponent))))

    // Recent activity + top products
    .addRow(row -> row
        .withChild(Column.create().withWidth(6)
            .withChild(ActivityFeedModule.create()
                .withTitle("Recent Activity")
                .withActivities(recentActivities)))
        .withChild(Column.create().withWidth(6)
            .withChild(DataModule.create(Product.class)
                .withTitle("Top Products")
                .withData(topProducts))))

    .render();
```

### Example 3: Forum Thread Page

```java
Page.create()
    // Thread header
    .addRow(row -> row
        .withChild(Column.create().withWidth(12)
            .withChild(Header.h1(thread.getTitle()))
            .withChild(Paragraph.create()
                .withInnerText("Posted by " + thread.getAuthor() + " on " + thread.getDate())
                .withClass("text-muted"))))

    // Main thread + sidebar
    .addRow(row -> row
        // Thread content (left, 9 columns)
        .withChild(Column.create().withWidth(9)
            .withChild(Card.create()
                .withChild(new Markdown(thread.getContent())))
            .withChild(CommentThread.create()
                .withComments(comments)))

        // Sidebar (right, 3 columns)
        .withChild(Column.create().withWidth(3)
            .withChild(Card.create()
                .withChild(Header.h4("Thread Info"))
                .withChild(Badge.create("Views: " + thread.getViews()))
                .withChild(Badge.create("Replies: " + thread.getReplies())))
            .withChild(Card.create()
                .withChild(Header.h4("Related Threads"))
                .withChild(createRelatedThreadsList(relatedThreads)))))

    .render();
```

## Best Practices for Layouts

### 1. Mobile-First Thinking
Design for mobile first, then enhance for desktop:
```java
// Good - works on mobile and desktop
Row.create()
    .withChild(Column.create().withWidth(12).withChild(header))
    .withChild(Column.create().withWidth(8).withChild(main))
    .withChild(Column.create().withWidth(4).withChild(sidebar));

// Avoid - too many columns for mobile
Row.create()
    .withChild(Column.create().withWidth(2).withChild(col1))
    .withChild(Column.create().withWidth(2).withChild(col2))
    .withChild(Column.create().withWidth(2).withChild(col3))
    .withChild(Column.create().withWidth(2).withChild(col4))
    .withChild(Column.create().withWidth(2).withChild(col5))
    .withChild(Column.create().withWidth(2).withChild(col6));
    // 6 columns stack poorly on mobile
```

### 2. Consistent Column Widths
Use common patterns (8/4, 6/6, 4/4/4) for visual consistency:
```java
// Good - familiar patterns
.withWidth(8) and .withWidth(4)  // 2/3 + 1/3
.withWidth(6) and .withWidth(6)  // 1/2 + 1/2
.withWidth(4) repeated 3 times    // 1/3 + 1/3 + 1/3

// Avoid - unusual splits
.withWidth(7) and .withWidth(5)  // Visually awkward
```

### 3. Don't Exceed 12 Columns Per Row
Columns exceeding 12 wrap to the next line:
```java
// Wraps correctly (12 + 6 = 18, so 6 wraps to next line)
Row.create()
    .withChild(Column.create().withWidth(12).withChild(header))
    .withChild(Column.create().withWidth(6).withChild(content));
    // Content appears on second line

// Better - use multiple rows explicitly
Page.create()
    .addRow(row -> row.withChild(Column.create().withWidth(12).withChild(header)))
    .addRow(row -> row.withChild(Column.create().withWidth(6).withChild(content)));
```

### 4. Use Semantic Structure
Organize content logically:
```java
Page.create()
    // Header section
    .addRow(...)

    // Main content section
    .addRow(...)

    // Footer section
    .addRow(...);
```

### 5. Test on Multiple Screen Sizes
View your layouts on:
- Mobile phone (< 480px)
- Tablet (481-768px)
- Desktop (> 769px)

## Key Takeaways

1. **Page Container**: Use `Page.create()` for all pages
2. **Two Content Methods**: `addComponents()` for simple, `addRow()` for grid layouts
3. **12-Column Grid**: Primary layout mechanism (withWidth 1-12)
4. **Column Options**: Fixed width, auto (content), fill (remaining)
5. **Responsive By Default**: Columns stack on mobile automatically
6. **Grid for Modules**: Use Column wrappers to size modules
7. **CSS for Components**: Use CSS width methods on form/basic components
8. **Common Patterns**: 8/4 (blog), 6/6 (split), 4/4/4 (features), 3/3/3/3 (grid)
9. **Mobile-First**: Design for small screens, enhance for large
10. **Test Responsiveness**: Check all breakpoints

---

**Previous**: [Part 4: Building Custom Modules](04-custom-modules.md)

**Next**: [Part 6: Shell Configuration](06-shell-configuration.md)

**Table of Contents**: [Getting Started Guide](README.md)
