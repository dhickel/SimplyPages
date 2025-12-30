# Part 7: CSS and Styling

CSS (Cascading Style Sheets) controls how your HTML looks visually. While JHF generates HTML using Java, understanding CSS is essential for customizing your application's appearance. This comprehensive guide teaches CSS fundamentals and JHF-specific styling patterns.

## CSS Basics for Java Developers

### What is CSS?

CSS is a language for describing how HTML elements should be displayed. Think of it as:
- **HTML** = Structure (what elements exist)
- **CSS** = Presentation (how elements look)
- **JavaScript** = Behavior (how elements interact)

JHF handles HTML generation. You control CSS to make it look professional.

### CSS Syntax

CSS consists of **rules** with this structure:

```css
selector {
    property: value;
    property: value;
}
```

**Example**:
```css
h1 {
    color: blue;
    font-size: 32px;
    margin-bottom: 20px;
}
```

This rule says: "All `<h1>` elements should be blue, 32 pixels tall, with 20 pixels of space below."

### How CSS Affects HTML

Given this JHF code:
```java
Header.h1("Welcome").withClass("page-title");
```

Generates HTML:
```html
<h1 class="page-title">Welcome</h1>
```

Apply CSS:
```css
.page-title {
    color: #2c3e50;
    font-weight: bold;
    border-bottom: 2px solid #3498db;
}
```

**Result**: The heading appears dark blue, bold, with a blue underline.

### Linking Stylesheets

CSS can be applied three ways:

**1. External Stylesheet (Recommended)**:
```html
<link rel="stylesheet" href="/css/framework.css">
<link rel="stylesheet" href="/css/custom.css">
```

JHF's ShellBuilder automatically includes framework.css.

**2. Inline Styles (Use Sparingly)**:
```java
component.withStyle("color", "red");
component.withStyle("font-size", "18px");
```

Generates:
```html
<div style="color: red; font-size: 18px;">...</div>
```

**3. Internal Styles** (Avoid):
```html
<style>
    h1 { color: blue; }
</style>
```

Not recommended for JHF applications.

## CSS Selectors

Selectors determine which HTML elements the CSS rules apply to.

### Element Selectors

Target all elements of a specific type:

```css
h1 { color: blue; }           /* All h1 elements */
p { line-height: 1.6; }       /* All paragraphs */
a { text-decoration: none; }  /* All links */
```

```java
Header.h1("Title");     // Affected by h1 rule
Paragraph.create()...   // Affected by p rule
Link.create()...        // Affected by a rule
```

### Class Selectors

Target elements with a specific CSS class:

```css
.card {
    border: 1px solid #ddd;
    padding: 20px;
}

.btn-primary {
    background-color: #007bff;
    color: white;
}
```

```java
Card.create().withClass("card");              // Affected
Button.create("Click").withClass("btn-primary"); // Affected
```

**Multiple classes**:
```java
component.withClass("card").withClass("shadow");
```

Generates:
```html
<div class="card shadow">...</div>
```

### ID Selectors

Target a specific element with a unique ID:

```css
#header { background-color: #2c3e50; }
#main-content { padding: 30px; }
```

```java
Div.create().withAttribute("id", "header");
```

**Note**: IDs should be unique on a page. Use classes for styling multiple elements.

### Descendant Selectors

Target elements nested inside other elements:

```css
.card p {
    color: #666;  /* Paragraphs inside .card elements */
}

.sidebar a {
    color: #3498db;  /* Links inside .sidebar */
}
```

```java
Card.create()
    .withClass("card")
    .withChild(Paragraph.create()...);  // This paragraph affected
```

### Pseudo-Classes

Target elements in specific states:

```css
a:hover {
    color: #e74c3c;  /* Links when mouse hovers */
}

button:active {
    transform: scale(0.95);  /* Buttons when clicked */
}

input:focus {
    border-color: #3498db;  /* Inputs when focused */
}

tr:nth-child(even) {
    background-color: #f2f2f2;  /* Even table rows */
}
```

### Combining Selectors

```css
/* Multiple selectors (same rules) */
h1, h2, h3 {
    color: #2c3e50;
}

/* Element with class */
div.card {
    /* Only div elements with class="card" */
}

/* Multiple classes */
.btn.btn-large {
    /* Elements with BOTH classes */
}
```

## CSS Specificity

When multiple rules target the same element, **specificity** determines which wins.

### Specificity Hierarchy (Strongest to Weakest)

1. **Inline styles**: `style="color: red;"`
2. **IDs**: `#header { ... }`
3. **Classes, attributes, pseudo-classes**: `.card { ... }`, `[type="text"]`, `:hover`
4. **Elements**: `h1 { ... }`, `div { ... }`

### Example

```css
/* Specificity: 1 (element) */
p {
    color: black;
}

/* Specificity: 10 (class) */
.highlight {
    color: blue;
}

/* Specificity: 100 (ID) */
#special {
    color: red;
}
```

```html
<p class="highlight" id="special">What color am I?</p>
```

**Result**: Red (ID wins)

### Calculating Specificity

Count selectors:
- ID selectors: 100 points each
- Class selectors: 10 points each
- Element selectors: 1 point each

```css
/* 1 point (element) */
p { ... }

/* 10 points (class) */
.card { ... }

/* 11 points (element + class) */
div.card { ... }

/* 20 points (two classes) */
.card.shadow { ... }

/* 100 points (ID) */
#header { ... }

/* 111 points (ID + class + element) */
div#header.main { ... }
```

### !important (Use Sparingly)

Override specificity:

```css
p {
    color: red !important;  /* Wins over everything */
}
```

**Avoid `!important`** except for utility classes or debugging. It makes CSS harder to maintain.

### Resolving Conflicts

**Scenario**: Both rules apply, same specificity

```css
.card { color: blue; }
.card { color: red; }
```

**Result**: Red (last rule wins when specificity is equal)

## The Box Model

Every HTML element is a rectangular box. Understanding the box model is crucial for layout.

### Box Model Components

```
┌─────────────────────────────────┐
│        Margin (transparent)     │
│  ┌──────────────────────────┐   │
│  │    Border                │   │
│  │  ┌────────────────────┐  │   │
│  │  │   Padding          │  │   │
│  │  │  ┌──────────────┐  │  │   │
│  │  │  │   Content    │  │  │   │
│  │  │  │ (width ×     │  │  │   │
│  │  │  │  height)     │  │  │   │
│  │  │  └──────────────┘  │  │   │
│  │  └────────────────────┘  │   │
│  └──────────────────────────┘   │
└─────────────────────────────────┘
```

1. **Content**: The actual content (text, images)
2. **Padding**: Space between content and border (transparent)
3. **Border**: Line around padding
4. **Margin**: Space outside border (transparent, separates from other elements)

### Example

```css
.card {
    width: 300px;           /* Content width */
    padding: 20px;          /* Space inside border */
    border: 2px solid #ddd; /* Border */
    margin: 10px;           /* Space outside */
}
```

**Total width** = width + padding + border + margin
= 300 + (20×2) + (2×2) + (10×2) = 364px

### box-sizing: border-box

JHF uses `box-sizing: border-box` globally:

```css
* {
    box-sizing: border-box;
}
```

This makes width **include** padding and border:

```css
.card {
    width: 300px;
    padding: 20px;
    border: 2px solid #ddd;
}
```

**Total width** = 300px (padding and border included)

**Why this is better**: Easier to calculate layouts. Setting `width: 50%` means exactly 50%, not 50% + padding + border.

### Margin and Padding Shorthand

```css
/* All sides */
padding: 20px;           /* 20px on all sides */

/* Vertical | Horizontal */
padding: 10px 20px;      /* 10px top/bottom, 20px left/right */

/* Top | Horizontal | Bottom */
padding: 10px 20px 30px; /* 10px top, 20px left/right, 30px bottom */

/* Top | Right | Bottom | Left (clockwise) */
padding: 10px 20px 30px 40px;
```

Same for margin.

**Individual sides**:
```css
padding-top: 10px;
padding-right: 20px;
padding-bottom: 30px;
padding-left: 40px;
```

## Responsive Design Concepts

Your application should look good on all devices.

### Media Queries

Apply CSS only at specific screen sizes:

```css
/* Default: Mobile styles */
.container {
    width: 100%;
    padding: 10px;
}

/* Tablet (481px and up) */
@media (min-width: 481px) {
    .container {
        width: 750px;
        margin: 0 auto;
    }
}

/* Desktop (769px and up) */
@media (min-width: 769px) {
    .container {
        width: 1200px;
    }
}
```

### JHF's Breakpoints

```css
/* Mobile: < 480px */
@media (max-width: 480px) {
    /* Mobile-specific styles */
}

/* Tablet: 481px - 768px */
@media (min-width: 481px) and (max-width: 768px) {
    /* Tablet-specific styles */
}

/* Desktop: > 769px */
@media (min-width: 769px) {
    /* Desktop-specific styles */
}
```

### Mobile-First Approach

**Design for mobile first**, then enhance for larger screens:

```css
/* Base: Mobile styles */
.card {
    width: 100%;
    padding: 10px;
}

/* Enhance for tablet+ */
@media (min-width: 481px) {
    .card {
        width: 50%;
        padding: 20px;
    }
}

/* Enhance for desktop */
@media (min-width: 769px) {
    .card {
        width: 33.33%;
        padding: 30px;
    }
}
```

JHF's grid system does this automatically!

### Fluid Layouts

Use percentages instead of fixed pixels:

```css
/* Fixed (not responsive) */
.sidebar {
    width: 250px;  /* Always 250px, even on mobile */
}

/* Fluid (responsive) */
.sidebar {
    width: 25%;    /* 25% of parent, adapts to screen size */
}
```

### Viewport Units

Relative to viewport (browser window) size:

```css
.hero {
    width: 100vw;   /* 100% of viewport width */
    height: 50vh;   /* 50% of viewport height */
}

.title {
    font-size: 5vw; /* 5% of viewport width */
}
```

**Units**:
- `vw`: Viewport width
- `vh`: Viewport height
- `vmin`: Smaller of vw or vh
- `vmax`: Larger of vw or vh

## framework.css Structure

JHF includes a comprehensive stylesheet at `/src/main/resources/static/css/framework.css` (1,540 lines).

### Sections

**1. Reset & Base Styles** (lines 1-50)
```css
* {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
}

body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, ...;
    color: #333;
    line-height: 1.6;
}
```

**2. Layout System** (lines 51-200)
- `.main-container`: Grid layout (sidebar + content)
- `.main-header`: Top header styling
- `.main-sidebar`: Sidebar (250px fixed, responsive)
- `.content-wrapper`: Main content area
- `#content-area`: Page content container

**3. Grid System** (lines 201-350)
- `.row`: Flexbox container
- `.col`, `.col-1` through `.col-12`: Column widths
- `.col-auto`, `.col-fill`: Special columns
- Responsive breakpoints

**4. CSS Grid Layouts** (lines 351-400)
- `.grid`: CSS Grid container
- `.grid-cols-1` through `.grid-cols-6`: Column counts
- Gap utilities

**5. Typography** (lines 401-500)
- `h1` through `h6`: Heading styles
- `p`: Paragraph styles
- `.text-sm`, `.text-lg`, etc.: Text sizing
- `.font-light`, `.font-bold`, etc.: Font weights

**6. Forms** (lines 501-650)
- `.form`, `.form-field`, `.form-label`: Form structure
- `.form-input`, `.form-select`, `.form-textarea`: Input styles
- `.btn`: Button base styles

**7. Components** (lines 651-1000)
- `.alert`, `.alert-success`, etc.: Alert styles
- `.badge`, `.badge-primary`, etc.: Badge styles
- `.card`, `.card-header`, etc.: Card styles
- `.table`, `.table-striped`, etc.: Table styles

**8. Navigation** (lines 1001-1200)
- `.nav-bar`: Top navigation
- `.side-nav`: Sidebar navigation
- `.breadcrumb`: Breadcrumb navigation

**9. Utilities** (lines 1201-1540)
- Spacing: `.m-0` through `.m-6`, `.p-0` through `.p-6`
- Display: `.d-none`, `.d-block`, `.d-flex`, `.d-grid`
- Width: `.w-25`, `.w-50`, `.w-75`, `.w-100`
- Alignment: `.align-left`, `.justify-center`, etc.

### What's Included

**Grid System**:
- 12-column responsive grid
- Flexbox-based rows
- Auto-stacking on mobile

**All Component Styles**:
- Alerts (4 types)
- Badges (6 styles)
- Buttons (6 variants)
- Cards
- Tables (striped, bordered, hover)
- Forms
- Navigation

**Typography**:
- Heading scales (h1-h6)
- Paragraph styles
- Font sizing (.text-sm, .text-lg)
- Font weights (.font-light, .font-bold)

**Color Palette**:
- Primary: #007bff (blue)
- Success: #28a745 (green)
- Warning: #ffc107 (yellow)
- Danger: #dc3545 (red)
- Info: #17a2b8 (cyan)
- Secondary: #6c757d (gray)

**Spacing Scale**:
- `.m-0`: 0px
- `.m-1`: 5px
- `.m-2`: 10px
- `.m-3`: 15px
- `.m-4`: 20px
- `.m-5`: 30px
- `.m-6`: 40px

**Responsive Breakpoints**:
- Mobile: < 480px
- Tablet: 481-768px
- Desktop: > 769px

## How CSS is Loaded

### Spring Boot Static Resources

Spring Boot serves static files from `/src/main/resources/static/`:

```
static/
├── css/
│   ├── framework.css    →  /css/framework.css
│   └── custom.css       →  /css/custom.css
├── js/
│   └── app.js           →  /js/app.js
└── images/
    └── logo.png         →  /images/logo.png
```

### ShellBuilder Auto-Includes

`ShellBuilder` automatically includes framework.css:

```java
String html = ShellBuilder.create()
    .withContent(pageContent)
    .build();
```

Generates:
```html
<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="/css/framework.css">
    <!-- Your custom CSS can be added -->
</head>
<body>
    <!-- Shell and content -->
</body>
</html>
```

### Load Order Matters

CSS is applied in order. Later styles override earlier ones:

```html
<link rel="stylesheet" href="/css/framework.css">  <!-- First -->
<link rel="stylesheet" href="/css/custom.css">     <!-- Second (overrides framework) -->
```

## Utility Classes

JHF provides comprehensive utility classes for common styling needs.

### Text Alignment

```css
.align-left    { text-align: left; }
.align-center  { text-align: center; }
.align-right   { text-align: right; }
.align-justify { text-align: justify; }
```

```java
Header.h1("Centered Title").withClass("align-center");
Paragraph.create().withInnerText("Justified text").withClass("align-justify");
```

### Flexbox

```css
.flex              { display: flex; }
.justify-start     { justify-content: flex-start; }
.justify-end       { justify-content: flex-end; }
.justify-center    { justify-content: center; }
.justify-between   { justify-content: space-between; }
.justify-around    { justify-content: space-around; }
.justify-evenly    { justify-content: space-evenly; }
.items-start       { align-items: flex-start; }
.items-end         { align-items: flex-end; }
.items-center      { align-items: center; }
.items-stretch     { align-items: stretch; }
```

```java
Div.create()
    .withClass("flex")
    .withClass("justify-center")
    .withClass("items-center")
    .withChild(content);
```

### Spacing

**Margin** (`.m-*`, `.mt-*`, `.mr-*`, `.mb-*`, `.ml-*`, `.mx-*`, `.my-*`):

```css
.m-0  { margin: 0; }
.m-1  { margin: 5px; }
.m-2  { margin: 10px; }
.m-3  { margin: 15px; }
.m-4  { margin: 20px; }
.m-5  { margin: 30px; }
.m-6  { margin: 40px; }

.mt-4 { margin-top: 20px; }
.mr-4 { margin-right: 20px; }
.mb-4 { margin-bottom: 20px; }
.ml-4 { margin-left: 20px; }
.mx-4 { margin-left: 20px; margin-right: 20px; }  /* Horizontal */
.my-4 { margin-top: 20px; margin-bottom: 20px; }  /* Vertical */

.mx-auto { margin-left: auto; margin-right: auto; }  /* Center */
```

**Padding** (`.p-*`, `.pt-*`, `.pr-*`, `.pb-*`, `.pl-*`, `.px-*`, `.py-*`):
Same pattern as margin.

```java
Card.create()
    .withClass("p-4")     // 20px padding all sides
    .withClass("mb-3");   // 15px margin bottom
```

### Display

```css
.d-none   { display: none; }
.d-block  { display: block; }
.d-flex   { display: flex; }
.d-grid   { display: grid; }
.d-inline { display: inline; }
.d-inline-block { display: inline-block; }

/* Responsive variants */
@media (min-width: 769px) {
    .d-md-none  { display: none; }
    .d-md-block { display: block; }
}
```

```java
Div.create()
    .withClass("d-none")     // Hidden on mobile
    .withClass("d-md-block"); // Visible on desktop
```

### Width/Height

```css
.w-25  { width: 25%; }
.w-50  { width: 50%; }
.w-75  { width: 75%; }
.w-100 { width: 100%; }
.w-auto { width: auto; }

.h-25  { height: 25%; }
.h-50  { height: 50%; }
.h-75  { height: 75%; }
.h-100 { height: 100%; }
```

```java
Card.create().withClass("w-50");  // 50% width card
```

### Typography

```css
.text-sm   { font-size: 0.875rem; }   /* 14px */
.text-base { font-size: 1rem; }       /* 16px */
.text-lg   { font-size: 1.125rem; }   /* 18px */
.text-xl   { font-size: 1.25rem; }    /* 20px */
.text-2xl  { font-size: 1.5rem; }     /* 24px */

.font-light { font-weight: 300; }
.font-normal { font-weight: 400; }
.font-medium { font-weight: 500; }
.font-semibold { font-weight: 600; }
.font-bold { font-weight: 700; }

.text-muted { color: #6c757d; }
```

```java
Paragraph.create()
    .withInnerText("Small text")
    .withClass("text-sm")
    .withClass("text-muted");
```

## Customizing Styles

Three approaches for customization:

### Option 1: Override with Custom Stylesheet

**Best for**: Application-wide customizations

**Create `/src/main/resources/static/css/custom.css`**:
```css
/* Override framework defaults */
.btn-primary {
    background-color: #8e44ad;  /* Purple instead of blue */
}

.card {
    box-shadow: 0 4px 6px rgba(0,0,0,0.1);  /* Add shadow */
}

h1 {
    font-family: 'Georgia', serif;  /* Different font */
}
```

**Load in Shell**:
```java
ShellBuilder.create()
    .withAdditionalCSS("/css/custom.css")
    .withContent(...)
    .build();
```

### Option 2: Inline Styles

**Best for**: One-off component-specific styles

```java
component.withStyle("color", "#8e44ad");
component.withStyle("font-size", "18px");
component.withStyle("border", "2px solid #ddd");
```

Generates:
```html
<div style="color: #8e44ad; font-size: 18px; border: 2px solid #ddd;">...</div>
```

**Pros**: Quick, component-specific
**Cons**: Hard to maintain, highest specificity

### Option 3: Custom CSS Classes

**Best for**: Repeated custom patterns

**Define in custom.css**:
```css
.highlight-box {
    background-color: #fff3cd;
    border-left: 4px solid #ffc107;
    padding: 15px;
    margin: 10px 0;
}
```

**Use in components**:
```java
Div.create()
    .withClass("highlight-box")
    .withChild(Paragraph.create().withInnerText("Important note"));
```

### When to Use Each

| Approach | Use When | Example |
|----------|----------|---------|
| Custom Stylesheet | Application-wide changes | Changing primary color |
| Inline Styles | One-off unique styling | Specific element color |
| Custom Classes | Repeated custom patterns | Warning boxes, badges |

## Adding Custom CSS

### Step 1: Create Custom Stylesheet

Create `/src/main/resources/static/css/custom.css`:

```css
/* ===============================
   Custom Styles for My Application
   =============================== */

/* Color scheme override */
:root {
    --primary-color: #8e44ad;
    --secondary-color: #3498db;
    --success-color: #27ae60;
    --danger-color: #e74c3c;
}

/* Custom button styles */
.btn-custom {
    background-color: var(--primary-color);
    color: white;
    border: none;
    padding: 10px 20px;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.3s;
}

.btn-custom:hover {
    background-color: #732d91;
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0,0,0,0.2);
}

/* Custom card style */
.feature-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 30px;
    border-radius: 8px;
    box-shadow: 0 10px 20px rgba(0,0,0,0.1);
}

.feature-card h3 {
    margin-bottom: 15px;
    font-size: 24px;
}

/* Responsive adjustments */
@media (max-width: 480px) {
    .feature-card {
        padding: 20px;
    }

    .feature-card h3 {
        font-size: 20px;
    }
}
```

### Step 2: Load Custom CSS

```java
@Service
public class ShellService {
    public String buildShell(String title, String content) {
        return ShellBuilder.create()
            .withTitle(title)
            .withAdditionalCSS("/css/custom.css")  // Load custom CSS
            .withTopBanner(...)
            .withContent(content)
            .build();
    }
}
```

### Step 3: Use Custom Classes

```java
Button.create("Click Me").withClass("btn-custom");

Card.create()
    .withClass("feature-card")
    .withChild(Header.h3("Amazing Feature"))
    .withChild(Paragraph.create().withInnerText("Description here..."));
```

## Complete Customization Example: Dark Theme

### custom-dark.css

```css
/* ===============================
   Dark Theme for My Application
   =============================== */

/* Base colors */
body {
    background-color: #1a1a1a;
    color: #e0e0e0;
}

/* Override framework components */
.card {
    background-color: #2d2d2d;
    border-color: #404040;
    color: #e0e0e0;
}

.btn-primary {
    background-color: #3498db;
}

.btn-primary:hover {
    background-color: #2980b9;
}

/* Form elements */
.form-input,
.form-select,
.form-textarea {
    background-color: #2d2d2d;
    border-color: #404040;
    color: #e0e0e0;
}

.form-input:focus {
    border-color: #3498db;
    background-color: #353535;
}

/* Navigation */
.main-sidebar {
    background-color: #242424;
}

.side-nav a {
    color: #b0b0b0;
}

.side-nav a:hover {
    background-color: #353535;
    color: #ffffff;
}

/* Tables */
.table {
    color: #e0e0e0;
}

.table-striped tr:nth-child(even) {
    background-color: #2d2d2d;
}

.table-hover tr:hover {
    background-color: #353535;
}

/* Alerts */
.alert {
    background-color: #2d2d2d;
    border-left-width: 4px;
}

.alert-success {
    border-left-color: #27ae60;
    background-color: #1e3a2f;
}

.alert-danger {
    border-left-color: #e74c3c;
    background-color: #3a1e1e;
}
```

### Load Dark Theme

```java
ShellBuilder.create()
    .withAdditionalCSS("/css/custom-dark.css")
    .withContent(...)
    .build();
```

## CSS Organization Best Practices

### 1. Separate Concerns

Organize CSS by purpose:

```css
/* ===== LAYOUT ===== */
.container { ... }
.row { ... }
.col { ... }

/* ===== COMPONENTS ===== */
.card { ... }
.btn { ... }
.alert { ... }

/* ===== UTILITIES ===== */
.m-4 { ... }
.p-4 { ... }
.align-center { ... }

/* ===== CUSTOM ===== */
.feature-box { ... }
.pricing-card { ... }
```

### 2. Use CSS Variables (Custom Properties)

Define reusable values:

```css
:root {
    /* Colors */
    --primary: #3498db;
    --secondary: #2ecc71;
    --danger: #e74c3c;

    /* Spacing */
    --spacing-sm: 10px;
    --spacing-md: 20px;
    --spacing-lg: 40px;

    /* Typography */
    --font-family: 'Helvetica Neue', sans-serif;
    --font-size-base: 16px;
}

/* Use variables */
.btn-primary {
    background-color: var(--primary);
    padding: var(--spacing-md);
    font-family: var(--font-family);
}
```

### 3. Comment Sections Clearly

```css
/* ===============================
   CUSTOM COMPONENTS
   =============================== */

/* Feature Cards
   ----------------------------- */
.feature-card { ... }
.feature-card__icon { ... }
.feature-card__title { ... }

/* Pricing Tables
   ----------------------------- */
.pricing-table { ... }
.pricing-table__header { ... }
```

### 4. Avoid !important

Only use for utility classes:

```css
/* Acceptable */
.d-none {
    display: none !important;  /* Override anything */
}

/* Avoid */
.card {
    background: white !important;  /* Makes CSS rigid */
}
```

### 5. Keep Specificity Low

```css
/* Good (low specificity) */
.card { ... }
.card-header { ... }

/* Avoid (high specificity) */
div.container div.row div.col div.card div.card-header { ... }
```

## Key Takeaways

1. **CSS = Presentation**: Controls how HTML looks
2. **Selectors**: Target elements (element, class, ID, descendant, pseudo-class)
3. **Specificity**: Inline > ID > Class > Element
4. **Box Model**: Content + Padding + Border + Margin
5. **box-sizing: border-box**: Width includes padding and border (JHF default)
6. **Responsive**: Mobile-first with media queries
7. **framework.css**: 1,540 lines of complete styling
8. **Utility Classes**: Quick styling (.m-4, .p-4, .w-50, .d-flex)
9. **Custom CSS**: Override framework.css with custom.css
10. **Organization**: Separate concerns, use variables, comment clearly

---

**Previous**: [Part 6: Shell Configuration](06-shell-configuration.md)

**Next**: [Part 8: Forms](08-forms.md)

**Table of Contents**: [Getting Started Guide](README.md)
