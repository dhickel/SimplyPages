# Troubleshooting Common Issues

This guide covers the most common issues developers encounter when using the Java HTML Framework, with clear explanations and solutions.

## Module & Template Issues

### Issue: "My Module doesn't update when I change a field"

**Symptoms:**
```java
MyModule module = new MyModule().withTitle("Original");
String html1 = module.render(); // Shows "Original"

module.withTitle("Updated");  // Try to change it
String html2 = module.render(); // Still shows "Original"!
```

**Why This Happens:**

Modules follow a **build-once lifecycle**. When you first call `render()`, the module calls `buildContent()` internally and locks its structure. Subsequent changes to fields don't affect the rendered output because `buildContent()` only runs once.

Think of it like a compiled Java class - once it's compiled, changing the source doesn't affect the already-compiled bytecode.

**Solution: Use Templates and SlotKeys**

```java
// 1. Define SlotKeys for dynamic content
public class MyModule extends Module {
    public static final SlotKey<String> TITLE = SlotKey.of("title");

    @Override
    protected void buildContent() {
        // Use slots instead of instance fields
        withChild(Header.h2(Slot.of(TITLE)));
    }
}

// 2. Create template once (can be static)
public static final Template MODULE_TEMPLATE = Template.of(new MyModule());

// 3. Render with different data each time
String html1 = MODULE_TEMPLATE.render(
    RenderContext.builder().with(MyModule.TITLE, "Original").build()
);

String html2 = MODULE_TEMPLATE.render(
    RenderContext.builder().with(MyModule.TITLE, "Updated").build()
);
// Now it works!
```

**When to Use Which Approach:**
- **Static modules** (content never changes): Old pattern is fine
- **Dynamic modules** (content changes per request): Use Templates
- **HTMX updates** (module refreshed via AJAX): **Must** use Templates

---

### Issue: "Template slot not rendering / shows nothing"

**Symptoms:**
```java
// Template renders but slot content is missing
String html = template.render(context);
// Expected: <div>Hello John</div>
// Got: <div></div>
```

**Common Causes:**

**1. Forgot to provide value in RenderContext**

```java
// ❌ WRONG: SlotKey defined but not provided
public static final SlotKey<String> NAME = SlotKey.of("name");

Template template = Template.of(module);
String html = template.render(RenderContext.builder().build());
// Empty context - no NAME value!

// ✅ CORRECT: Provide the value
String html = template.render(
    RenderContext.builder()
        .with(MyModule.NAME, "John")  // Provide value
        .build()
);
```

**2. Typo in SlotKey name**

```java
// ❌ WRONG: Key mismatch
public static final SlotKey<String> USER_NAME = SlotKey.of("userName");
context.with(MyModule.USER_NAME, "John");  // Correct key

// But in buildContent():
withChild(Slot.of(SlotKey.of("username")));  // New key! Doesn't match!

// ✅ CORRECT: Reuse the same SlotKey instance
withChild(Slot.of(MyModule.USER_NAME));
```

**3. Using wrong render method**

```java
// ❌ WRONG: Calling render() without context
String html = module.render();  // Uses empty context!

// ✅ CORRECT: Use Template.render(context)
String html = MY_TEMPLATE.render(context);
```

**Debugging Tips:**

1. **Check the RenderContext**: Print it to see what values are actually there
   ```java
   System.out.println("Context: " + context);
   ```

2. **Use default values**: SlotKeys can have defaults for debugging
   ```java
   public static final SlotKey<String> NAME = SlotKey.of("name", "DEFAULT_NAME");
   // If you see "DEFAULT_NAME" in output, the value wasn't provided
   ```

3. **Check slot type matches**: String slot needs String value
   ```java
   SlotKey<String> NAME = SlotKey.of("name");
   // Must provide String, not Integer!
   ```

---

### Issue: "How do I know when I need a Template?"

**Decision Tree:**

```
Will this module's content change between requests?
├─ NO (always same content)
│  └─ Simple Module is fine, no Template needed
│      Example: Static "About Us" page
│
└─ YES (content changes)
    └─ How does it change?
        ├─ Different data per user/request?
        │  └─ Use Template with SlotKeys
        │      Example: User profile, "Welcome {username}"
        │
        ├─ Updated via HTMX without page reload?
        │  └─ **Must** use Template for HTMX target
        │      Example: Live stats widget, refresh button
        │
        └─ Built conditionally (show Module A or B based on logic)?
            └─ Request-scoped composition (Pattern A)
                Example: Admin panel vs user panel
```

**Simple Rule:**
- **Static content** → Regular Module
- **Dynamic data** → Template + Slots
- **HTMX updates** → Template + Slots (required)
- **Conditional layout** → Build different modules in controller

---

## HTMX Issues

### Issue: "HTMX isn't targeting my element"

**Symptoms:**
```java
// Button click does nothing, or console shows error
Button.create("Update")
    .withAttribute("hx-get", "/api/data")
    .withAttribute("hx-target", "#results")
    .withAttribute("hx-swap", "innerHTML");
```

**Common Causes:**

**1. Element ID doesn't exist**

```java
// ❌ WRONG: No element with id="results" on page
.withAttribute("hx-target", "#results");

// ✅ CORRECT: Make sure element exists
Div results = new Div()
    .withAttribute("id", "results");  // Create element with ID

// Add it to page
page.addComponents(results, updateButton);
```

**2. ID typo or mismatch**

```java
// ❌ WRONG: Case mismatch
Div container = new Div()
    .withAttribute("id", "Results");  // Capital R

Button.create("Update")
    .withAttribute("hx-target", "#results");  // Lowercase r - won't match!

// ✅ CORRECT: IDs are case-sensitive, must match exactly
String containerId = "results";  // Use variable to avoid typos

Div container = new Div()
    .withAttribute("id", containerId");

Button.create("Update")
    .withAttribute("hx-target", "#" + containerId);
```

**3. Using class selector instead of ID**

```java
// ❌ WRONG: Set a class, target as ID
Div container = new Div()
    .withClass("results");  // This is a CLASS

Button.create("Update")
    .withAttribute("hx-target", "#results");  // Looking for ID!

// ✅ CORRECT: Match selector type
// Option 1: Use ID
Div container = new Div()
    .withAttribute("id", "results");
Button.create("Update")
    .withAttribute("hx-target", "#results");

// Option 2: Use class selector
Div container = new Div()
    .withClass("results");
Button.create("Update")
    .withAttribute("hx-target", ".results");  // . for class
```

**Debugging HTMX Targeting:**

1. **Check browser DevTools**: Open browser console (F12)
   ```javascript
   // Type in console to test selector
   document.querySelector('#results')
   // Should return element, not null
   ```

2. **Inspect HTML**: View page source, verify ID exists
   ```html
   <!-- Should see: -->
   <div id="results"></div>
   ```

3. **Check HTMX logs**: HTMX logs errors to console
   ```
   GET /api/data 200  ← Request worked
   htmx:targetError   ← Can't find target!
   ```

**Quick Reference:**
- `#id` → Element with `id="id"`
- `.class` → Elements with `class="class"`
- `this` → The element itself
- `closest .class` → Nearest parent with class

---

### Issue: "HTMX request works but nothing updates"

**Symptoms:**
- Browser console shows `200 OK` response
- Network tab shows HTML returned
- But page doesn't change

**Common Causes:**

**1. Wrong swap strategy**

```java
// ❌ WRONG: Using wrong swap for the structure
Div container = new Div()
    .withAttribute("id", "container")
    .withChild(new Paragraph("Original"));

Button.create("Replace")
    .withAttribute("hx-get", "/api/update")
    .withAttribute("hx-target", "#container")
    .withAttribute("hx-swap", "innerHTML");

// Server returns: <div id="container"><p>New</p></div>
// Result: Double divs! <div id="container"><div id="container">...
```

**Solution:** Match swap to response structure

```java
// If server returns INNER content only:
// Server: <p>New</p>
.withAttribute("hx-swap", "innerHTML");  // Replace inside

// If server returns WHOLE element:
// Server: <div id="container"><p>New</p></div>
.withAttribute("hx-swap", "outerHTML");  // Replace entire element
```

**2. Module not set up for HTMX (no ID)**

```java
// ❌ WRONG: Module has no ID, can't be targeted
ContentModule module = ContentModule.create()
    .withTitle("Stats");

// Server returns module, but HTMX can't find it to replace!

// ✅ CORRECT: Set module ID
ContentModule module = ContentModule.create()
    .withModuleId("stats-module")  // Now HTMX can target #stats-module
    .withTitle("Stats");
```

**3. Response HTML doesn't have matching ID for OOB swap**

```java
// HTMX config: hx-swap-oob="true"

// ❌ WRONG: Response missing ID
String response = ContentModule.create()
    .withTitle("Updated")
    .render();
// No ID! HTMX doesn't know where to swap it

// ✅ CORRECT: Include ID in response
String response = ContentModule.create()
    .withModuleId("stats-module")  // Same ID as page element
    .withTitle("Updated")
    .withAttribute("hx-swap-oob", "true")  // Enable OOB
    .render();
```

---

## Sizing & Layout Issues

### Issue: "Module has no withWidth() method"

**Symptoms:**
```java
ContentModule module = ContentModule.create()
    .withWidth("800px");  // Compile error!
// Error: Cannot resolve method 'withWidth(String)'
```

**Why This Happens:**

Modules are **layout units**, not **content units**. They're sized using the **grid system** (responsive columns), not CSS width properties.

**Solution: Use Column wrapper**

```java
// ❌ WRONG: Try to set width on module
ContentModule module = ContentModule.create()
    .withMaxWidth("800px");  // Doesn't exist!

// ✅ CORRECT: Wrap in Column with grid width
Page.create()
    .addRow(row -> row
        .withChild(Column.create()
            .withWidth(8)  // 8/12 = 66% width (responsive!)
            .withChild(ContentModule.create()
                .withTitle("Content"))));
```

**When You Need Pixel-Perfect Sizing:**

```java
// Wrap module in a div with max-width
Page.create()
    .addRow(row -> row
        .withChild(Column.create()
            .withWidth(10)  // Grid layout (83%)
            .withChild(new Div()
                .withMaxWidth("900px")  // CSS constraint
                .withClass("mx-auto")  // Center it
                .withChild(ContentModule.create()))));
```

**What DOES have CSS width methods:**
- Form components: `TextInput`, `TextArea`, `Select`, `Button`
- Basic components: `Div`, `Paragraph`, `Image`
- Display components: `Card`, `Table`

**Decision Tree:**
```
What are you sizing?
├─ Module (ContentModule, FormModule, etc.)
│  └─ Use Column.withWidth(1-12)
│
└─ Component (TextInput, Card, Div, etc.)
    └─ Use .withWidth() / .withMaxWidth()
```

---

### Issue: "Columns don't line up horizontally"

**Symptoms:**
```java
Page.create()
    .addRow(row -> row
        .withChild(Column.create().withWidth(6).withChild(module1))
        .withChild(Column.create().withWidth(6).withChild(module2)));
// On desktop: Columns stack vertically instead of side-by-side!
```

**Common Causes:**

**1. Forgot to use Row**

```java
// ❌ WRONG: Columns without Row container
Page.create()
    .addComponents(
        Column.create().withWidth(6).withChild(module1),
        Column.create().withWidth(6).withChild(module2)
    );
// Columns stack because no Row wrapper

// ✅ CORRECT: Use addRow()
Page.create()
    .addRow(row -> row
        .withChild(Column.create().withWidth(6).withChild(module1))
        .withChild(Column.create().withWidth(6).withChild(module2)));
```

**2. Total width exceeds 12**

```java
// ❌ WRONG: 8 + 8 = 16 > 12
row.withChild(Column.create().withWidth(8).withChild(module1))
   .withChild(Column.create().withWidth(8).withChild(module2));
// Second column wraps to next line!

// ✅ CORRECT: Keep total ≤ 12
row.withChild(Column.create().withWidth(6).withChild(module1))
   .withChild(Column.create().withWidth(6).withChild(module2));
// 6 + 6 = 12, perfect!
```

**3. Mobile responsive behavior**

On mobile (width < 480px), columns automatically stack to 100% width. This is intentional!

```java
// This is NOT a bug, it's responsive design
row.withChild(Column.create().withWidth(6).withChild(module1))
   .withChild(Column.create().withWidth(6).withChild(module2));

// Desktop (>769px):  [module1 50%] [module2 50%]
// Mobile (<480px):   [module1 100%]
//                    [module2 100%]
```

---

## Rendering Issues

### Issue: "Calling render() multiple times creates duplicates"

**Symptoms:**
```java
Card card = Card.create()
    .withChild(Paragraph.create().withInnerText("Hello"));

String html1 = card.render();  // <div><p>Hello</p></div>
String html2 = card.render();  // <div><p>Hello</p><p>Hello</p></div> ???
```

**Why This Happens:**

For performance reasons, components may mutate their internal state during rendering. Calling `render()` multiple times can cause unexpected behavior.

**Solution 1: Render once, reuse HTML string**

```java
Card card = Card.create()
    .withChild(Paragraph.create().withInnerText("Hello"));

String html = card.render();  // Render once

// Reuse the string
response1.setBody(html);
response2.setBody(html);
```

**Solution 2: Use Templates for reusable components**

```java
// Build structure once
Card card = Card.create()
    .withChild(Paragraph.create().withInnerText("Hello"));

// Create template
Template cardTemplate = Template.of(card);

// Render multiple times safely
String html1 = cardTemplate.render(RenderContext.empty());
String html2 = cardTemplate.render(RenderContext.empty());
// Works correctly!
```

**Best Practice:** Always render components once in production code.

---

## Spring Integration Issues

### Issue: "CSRF token validation failed"

**Symptoms:**
```java
// Form submission returns 403 Forbidden
// Console: "Invalid CSRF token"
```

**Solution: Include CSRF token in form**

```java
// ✅ CORRECT: Add CSRF token to form
@PostMapping("/submit")
@ResponseBody
public String submit(CsrfToken csrfToken) {
    return Form.create()
        .withAttribute("hx-post", "/api/process")
        .withCsrfToken(csrfToken.getToken())  // ← Critical!
        .addField("Name", TextInput.create("name"))
        .addField("", Button.submit("Send"))
        .render();
}
```

**For HTMX POST requests:**
```java
// Controller method signature
public String processForm(@RequestParam String name, CsrfToken csrfToken)
```

---

### Issue: "HX-Request header is null"

**Symptoms:**
```java
@GetMapping("/api/data")
@ResponseBody
public String getData(
    @RequestHeader(value = "HX-Request", required = false) String hxRequest
) {
    if (hxRequest == null) {
        // Always null, even from HTMX!
    }
}
```

**Common Causes:**

**1. Testing with browser directly**

If you navigate to `/api/data` in browser address bar, it's NOT an HTMX request, so header is null. This is correct behavior.

**2. HTMX not loaded**

```html
<!-- Missing from page -->
<script src="/webjars/htmx.org/dist/htmx.min.js"></script>
```

Check ShellBuilder includes HTMX:
```java
ShellBuilder.create()
    .withHtmxSupport()  // ← Must include this
```

**3. Typo in header name**

```java
// ❌ WRONG: Case matters
@RequestHeader(value = "hx-request") String hxRequest

// ✅ CORRECT: Exact case
@RequestHeader(value = "HX-Request") String hxRequest
```

---

## Getting Help

If you're still stuck after trying these solutions:

1. **Check the demo pages**: Visit `/demo/dynamic-updates` and similar pages to see working examples
2. **Review the guides**:
   - [Part 13: Templates and Dynamic Updates](../getting-started/13-templates-and-dynamic-updates.md)
   - [Part 9: HTMX and Dynamic Features](../getting-started/09-htmx-dynamic-features.md)
   - [Part 2: Core Concepts](../getting-started/02-core-concepts.md)
3. **Inspect browser console**: Most HTMX and rendering issues show errors in DevTools (F12)
4. **Check framework source**: Look at `../jhf/src/main/java/io/mindspice/jhf/` for API details
5. **Review example code**: Check `src/main/java/io/mindspice/demo/pages/` for working patterns

---

**Previous**: [Getting Started Guide](../getting-started/README.md)

**Related**:
- [Part 13: Templates and Dynamic Updates](../getting-started/13-templates-and-dynamic-updates.md)
- [Part 9: HTMX and Dynamic Features](../getting-started/09-htmx-dynamic-features.md)
- [Part 2: Core Concepts](../getting-started/02-core-concepts.md)
