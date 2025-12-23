# Part 9: HTMX and Dynamic Features

HTMX allows you to add dynamic behavior to your server-rendered pages without writing JavaScript. It's perfect for JHF applications, enabling partial page updates, lazy loading, and interactive features while keeping logic on the server.

## What is HTMX? (For Beginners)

### The Traditional Approach (Without HTMX)

**Problem**: Clicking a button requires a full page reload:

1. User clicks "Load More" button
2. Browser sends request to server
3. Server generates **entire page** HTML
4. Browser **reloads entire page** (flash, scroll jumps to top)

**Result**: Poor user experience, wasted bandwidth

### The HTMX Approach

**Solution**: Update only part of the page:

1. User clicks "Load More" button
2. Browser sends AJAX request (via HTMX)
3. Server generates **HTML fragment** (just the new items)
4. HTMX **swaps fragment** into page (smooth, no reload)

**Result**: Smooth experience, like a single-page app (SPA), but server-rendered!

### Key Concept

**HTMX = HTML attributes that trigger AJAX requests**

Instead of writing JavaScript:
```javascript
// Traditional JavaScript
button.addEventListener('click', function() {
    fetch('/api/items')
        .then(response => response.json())
        .then(data => {
            // Manually build HTML from JSON
            // Manually insert into page
        });
});
```

Use HTML attributes:
```java
Button.create("Load More")
    .withAttribute("hx-get", "/api/items")       // Request URL
    .withAttribute("hx-target", "#items-list")   // Where to put response
    .withAttribute("hx-swap", "beforeend");      // How to insert
```

**No JavaScript required!** HTMX handles everything.

## How HTMX Works

### The HTMX Request-Response Cycle

**1. User triggers action** (click, scroll, time delay, etc.)

**2. HTMX sends AJAX request**
- Includes special headers (`HX-Request: true`)
- Can be GET, POST, PUT, DELETE, PATCH

**3. Server returns HTML fragment**
- Not JSON!
- Just the HTML snippet to insert

**4. HTMX swaps fragment into page**
- Replaces, appends, prepends, etc.
- Smooth, no page reload

### Example Flow

**JHF Component**:
```java
Button.create("Load More")
    .withAttribute("hx-get", "/api/more-items")
    .withAttribute("hx-target", "#items-list")
    .withAttribute("hx-swap", "beforeend");
```

**Generated HTML**:
```html
<button hx-get="/api/more-items"
        hx-target="#items-list"
        hx-swap="beforeend">
    Load More
</button>
```

**User clicks button** → **HTMX sends GET /api/more-items** → **Server returns**:
```html
<div class="item">Item 11</div>
<div class="item">Item 12</div>
<div class="item">Item 13</div>
```

**HTMX appends to #items-list** → **User sees new items** (no page reload!)

## When to Use HTMX

### ✅ Use HTMX For

1. **Module Refreshing** - Update part of page without reload
2. **Lazy Loading** - Load content on demand (scroll, click)
3. **Form Submissions** - Submit form and show result inline
4. **User Interactions** - Voting, liking, favoriting
5. **Infinite Scroll** - Load more items as user scrolls
6. **Search/Filter** - Update results dynamically
7. **Pagination** - Load next/previous page inline

### ❌ Don't Use HTMX For

1. **Initial Page Rendering** - Use server-side rendering (JHF)
2. **Simple Navigation** - Use standard links (`<a href="...">`)
3. **Static Content** - No need for dynamic updates
4. **Complex SPA Interactions** - Use a frontend framework instead

### Decision Tree

```
Does the user action require server data?
├─ NO → Use standard HTML (link, form submit)
└─ YES → Should the entire page reload?
         ├─ YES → Use standard HTML
         └─ NO → Use HTMX for partial update
```

## Understanding HTMX Targeting (CSS Selectors)

Before diving into HTMX attributes, you need to understand **how HTMX finds elements** on your page to update them.

### What Are CSS Selectors?

**CSS Selectors** are patterns that identify HTML elements. When you write `hx-target="#items-list"`, you're telling HTMX "find the element with id='items-list' and put the response there."

Think of CSS selectors as **addresses** for elements on your page:
- `#items-list` → "The element with ID 'items-list'"
- `.card` → "Elements with class 'card'"
- `this` → "This element itself"

### The Java → HTML → HTMX Flow

**1. Java Code**: Set ID/class on component

```java
Div itemsList = new Div()
    .withAttribute("id", "items-list");  // ← Set ID in Java
```

**2. Generated HTML**: Component renders with ID

```html
<div id="items-list">
    <!-- content here -->
</div>
```

**3. HTMX Targeting**: Reference the ID in hx-target

```java
Button.create("Load More")
    .withAttribute("hx-get", "/api/items")
    .withAttribute("hx-target", "#items-list");  // ← Target the ID
//                              ^ # means "ID"
```

### Setting IDs and Classes in Java

#### Setting an ID

**Use `withAttribute("id", "your-id-here")`:**

```java
// Set ID on any component
Div container = new Div()
    .withAttribute("id", "results-container");

// Now HTMX can target it with "#results-container"
Button.create("Refresh")
    .withAttribute("hx-get", "/api/results")
    .withAttribute("hx-target", "#results-container");
```

#### Setting a Class

**Use `withClass("your-class-name")`:**

```java
// Set class on component
Card card = Card.create()
    .withClass("user-card");

// Target by class (affects all .user-card elements)
Button.create("Refresh All")
    .withAttribute("hx-get", "/api/cards")
    .withAttribute("hx-target", ".user-card");  // ← . means "class"
```

#### Setting Both ID and Class

```java
Div stats = new Div()
    .withAttribute("id", "live-stats")  // Unique ID
    .withClass("dashboard-widget");     // Shared class
```

### Common CSS Selector Patterns

| Selector | Meaning | Java Example | HTMX Target |
|----------|---------|--------------|-------------|
| `#id` | Element with specific ID | `.withAttribute("id", "posts")` | `"#posts"` |
| `.class` | Elements with class | `.withClass("card")` | `".card"` |
| `this` | The element itself | N/A | `"this"` |
| `closest .class` | Nearest parent with class | `.withClass("parent")` | `"closest .parent"` |

### Complete Example: Setting Up Targets

**Java Code (initial page load):**

```java
// 1. Create container with ID so HTMX can find it
Div itemsList = new Div()
    .withAttribute("id", "items-list")  // ← HTMX will target this
    .withClass("grid");

// 2. Add initial items
for (Item item : firstPage) {
    itemsList.withChild(createItemCard(item));
}

// 3. Create button that targets the container
Button loadMore = Button.create("Load More")
    .withAttribute("hx-get", "/api/items?page=2")
    .withAttribute("hx-target", "#items-list")  // ← Targets the ID above
    .withAttribute("hx-swap", "beforeend");     // ← Append to end

// 4. Add to page
Page.create()
    .addComponents(itemsList, loadMore)
    .render();
```

**Generated HTML:**

```html
<!-- The container HTMX will update -->
<div id="items-list" class="grid">
    <div class="card">Item 1</div>
    <div class="card">Item 2</div>
    <div class="card">Item 3</div>
</div>

<!-- Button that triggers update -->
<button hx-get="/api/items?page=2"
        hx-target="#items-list"
        hx-swap="beforeend">
    Load More
</button>
```

**What Happens When Clicked:**

1. HTMX sends GET request to `/api/items?page=2`
2. Server returns HTML: `<div class="card">Item 4</div>...`
3. HTMX **finds element with id="items-list"** (the div)
4. HTMX **appends** new items to end of that div (because swap="beforeend")

### Finding Your Targets: Common Mistakes

#### ❌ WRONG: Forgetting to Set ID

```java
// Created container but forgot to set ID
Div container = new Div();  // No ID!

// HTMX can't find it
Button.create("Load")
    .withAttribute("hx-target", "#results");  // What is #results???
```

**Error**: HTMX won't find `#results` because no element has that ID.

#### ✅ CORRECT: Set ID First

```java
// Set ID on container
Div container = new Div()
    .withAttribute("id", "results");  // ← Set ID!

// HTMX can find it
Button.create("Load")
    .withAttribute("hx-target", "#results");  // ← Matches ID above
```

#### ❌ WRONG: Typo in ID

```java
Div container = new Div()
    .withAttribute("id", "results-list");  // ← "results-list"

Button.create("Load")
    .withAttribute("hx-target", "#results");  // ← "results" (no match!)
```

**Error**: `#results` doesn't match `id="results-list"` - IDs must match **exactly**.

#### ✅ CORRECT: Matching IDs

```java
String containerId = "results-list";  // Use variable to avoid typos

Div container = new Div()
    .withAttribute("id", containerId);

Button.create("Load")
    .withAttribute("hx-target", "#" + containerId);  // Same ID
```

### Target Selection Examples

#### Example 1: Target by ID (Most Common)

```java
// Create element with unique ID
Div stats = new Div()
    .withAttribute("id", "live-stats");

// Target that specific element
stats.withAttribute("hx-get", "/api/stats")
     .withAttribute("hx-trigger", "every 30s")
     .withAttribute("hx-target", "this");  // "this" = update myself
```

#### Example 2: Target Parent Element

```java
// Card with nested content
Card card = Card.create()
    .withClass("user-card")  // ← Set class
    .withChild(new Div()
        .withAttribute("id", "user-name")
        .withInnerText("John Doe"))
    .withChild(Button.create("Edit")
        .withAttribute("hx-get", "/api/edit-form")
        .withAttribute("hx-target", "closest .user-card")  // ← Target parent
        .withAttribute("hx-swap", "outerHTML"));  // Replace entire card
```

**What happens**: Button replaces the entire `.user-card` (its parent) with edit form.

#### Example 3: Target Multiple Elements

```java
// Multiple cards with same class
for (User user : users) {
    Card card = Card.create()
        .withClass("user-status")  // ← All have same class
        .withAttribute("id", "user-" + user.getId())  // ← Each has unique ID
        .withInnerText("Status: " + user.getStatus());
}

// Refresh all at once (target by class)
Button.create("Refresh All")
    .withAttribute("hx-get", "/api/statuses")
    .withAttribute("hx-target", ".user-status");  // ← Updates ALL .user-status
```

### Module IDs and HTMX

**All modules have `withModuleId()` method** which sets the ID:

```java
ContentModule module = ContentModule.create()
    .withModuleId("about-section")  // ← Sets id="about-section"
    .withTitle("About Us")
    .withContent("...");

// Target the module
Button.create("Refresh")
    .withAttribute("hx-get", "/api/about")
    .withAttribute("hx-target", "#about-section");  // ← Targets the module
```

**Generated HTML:**

```html
<div id="about-section" class="content-module">
    <h2>About Us</h2>
    <div class="content">...</div>
</div>

<button hx-get="/api/about" hx-target="#about-section">
    Refresh
</button>
```

### Self-Updating Elements

**Use `hx-target="this"`** to update the element itself:

```java
// Button that updates its own text
Button voteBtn = Button.create("Vote (0)")
    .withAttribute("id", "vote-btn")
    .withAttribute("hx-post", "/api/vote")
    .withAttribute("hx-target", "this")  // ← Update the button itself
    .withAttribute("hx-swap", "outerHTML");
```

**Server returns new button:**

```java
@PostMapping("/api/vote")
@ResponseBody
public String vote() {
    int newCount = voteService.increment();
    return Button.create("Vote (" + newCount + ")")
        .withAttribute("id", "vote-btn")
        .withAttribute("hx-post", "/api/vote")
        .withAttribute("hx-target", "this")
        .withAttribute("hx-swap", "outerHTML")
        .render();
}
```

### Quick Reference: ID vs Class

| Feature | ID (`#`) | Class (`.`) |
|---------|----------|-------------|
| **Purpose** | Unique identifier | Group elements |
| **Uniqueness** | One per page | Many per page |
| **Java Method** | `.withAttribute("id", "name")` | `.withClass("name")` |
| **HTMX Syntax** | `#name` | `.name` |
| **Use Case** | Target specific element | Target multiple elements |
| **Example** | `#user-profile` | `.card` |

### Debugging Tips

**If HTMX isn't working:**

1. **Check browser console** - HTMX logs errors
2. **Inspect HTML** - View source to see if ID exists
3. **Verify selector** - Does `#your-id` match `id="your-id"`?
4. **Check spelling** - IDs are case-sensitive
5. **Use browser DevTools** - Type `document.querySelector('#your-id')` in console

**Browser Console Test:**

```javascript
// Test if selector works
document.querySelector('#items-list');  // Should return element, not null
```

### Key Takeaways

1. **Set IDs in Java**: Use `.withAttribute("id", "unique-name")`
2. **Target with #**: Use `hx-target="#unique-name"` to find that element
3. **Match exactly**: `#results` only matches `id="results"` (case-sensitive)
4. **Use classes for groups**: `.withClass("card")` + `hx-target=".card"` for multiple
5. **Modules have IDs**: Use `.withModuleId("name")` for modules
6. **Debug with DevTools**: Inspect HTML and test selectors in console

---

## Core HTMX Attributes

### hx-get, hx-post, hx-put, hx-delete, hx-patch

Specify HTTP method and URL:

```java
// GET request
component.withAttribute("hx-get", "/api/users");

// POST request
component.withAttribute("hx-post", "/api/create");

// PUT request (update)
component.withAttribute("hx-put", "/api/update/123");

// DELETE request
component.withAttribute("hx-delete", "/api/delete/123");

// PATCH request (partial update)
component.withAttribute("hx-patch", "/api/patch/123");
```

### hx-target

Where to put the response (CSS selector):

```java
// Replace content of element with id="result"
.withAttribute("hx-target", "#result");

// Replace content of closest parent .card
.withAttribute("hx-target", "closest .card");

// Replace this element
.withAttribute("hx-target", "this");
```

### hx-swap

How to insert the response:

```java
// Replace inner HTML (default)
.withAttribute("hx-swap", "innerHTML");

// Replace entire element
.withAttribute("hx-swap", "outerHTML");

// Insert before end (append)
.withAttribute("hx-swap", "beforeend");

// Insert at beginning (prepend)
.withAttribute("hx-swap", "afterbegin");

// Insert before element
.withAttribute("hx-swap", "beforebegin");

// Insert after element
.withAttribute("hx-swap", "afterend");
```

### hx-trigger

What triggers the request:

```java
// On click (default for buttons)
.withAttribute("hx-trigger", "click");

// On visibility (lazy loading)
.withAttribute("hx-trigger", "revealed");

// On page load
.withAttribute("hx-trigger", "load");

// Every X seconds (polling)
.withAttribute("hx-trigger", "every 30s");

// On form change
.withAttribute("hx-trigger", "change");

// On input (typing)
.withAttribute("hx-trigger", "input");

// Delayed (debounce)
.withAttribute("hx-trigger", "input delay:500ms");
```

### Other Useful Attributes

```java
// Update browser URL
.withAttribute("hx-push-url", "true");

// Show loading indicator
.withAttribute("hx-indicator", "#spinner");

// Confirmation dialog
.withAttribute("hx-confirm", "Are you sure?");

// Include specific values
.withAttribute("hx-include", "#filter-form");
```

## HTMX with JHF Components

All JHF components support HTMX attributes via `withAttribute()`.

### Example: Button with HTMX

```java
Button.create("Load More")
    .withAttribute("hx-get", "/api/more-posts")
    .withAttribute("hx-target", "#post-list")
    .withAttribute("hx-swap", "beforeend")
    .withAttribute("hx-indicator", "#loading");
```

### Example: Module with Auto-Refresh

```java
DataModule.create(Stats.class)
    .withModuleId("live-stats")
    .withAttribute("hx-get", "/api/stats")
    .withAttribute("hx-trigger", "every 30s")
    .withAttribute("hx-swap", "outerHTML");
```

### Example: Form with HTMX Submission

```java
Form.create()
    .withAttribute("hx-post", "/api/submit")
    .withAttribute("hx-target", "#result")
    .withAttribute("hx-swap", "innerHTML")
    .withCsrfToken(csrfToken.getToken())

    .addField("Email", TextInput.email("email").required())
    .addField("", Button.submit("Subscribe"));
```

## Common HTMX Patterns

### Pattern 1: Load More (Pagination)

**Use Case**: Append new items to a list

**Implementation**:
```java
// Initial page load
Div itemsList = new Div()
    .withAttribute("id", "items-list");

for (Item item : firstPage) {
    itemsList.withChild(createItemCard(item));
}

Button loadMore = Button.create("Load More")
    .withAttribute("hx-get", "/api/items?page=2")
    .withAttribute("hx-target", "#items-list")
    .withAttribute("hx-swap", "beforeend");  // Append to end

Page.create()
    .addComponents(itemsList, loadMore)
    .render();
```

**Spring Controller**:
```java
@GetMapping("/api/items")
@ResponseBody
public String loadMoreItems(@RequestParam int page) {
    List<Item> items = itemService.getPage(page);

    StringBuilder html = new StringBuilder();
    for (Item item : items) {
        html.append(createItemCard(item).render());
    }

    return html.toString();  // Return HTML fragments
}
```

### Pattern 2: Auto-Refresh (Polling)

**Use Case**: Periodically update content (live stats, notifications)

**Implementation**:
```java
Div statsContainer = new Div()
    .withAttribute("id", "live-stats")
    .withAttribute("hx-get", "/api/stats")
    .withAttribute("hx-trigger", "every 30s")  // Poll every 30 seconds
    .withAttribute("hx-swap", "innerHTML");

// Initial content
statsContainer.withChild(buildStatsContent(currentStats));
```

**Spring Controller**:
```java
@GetMapping("/api/stats")
@ResponseBody
public String refreshStats() {
    Stats stats = statsService.getLatest();
    return buildStatsContent(stats).render();
}
```

### Pattern 3: Lazy Loading (On Scroll)

**Use Case**: Load content when scrolled into view

**Implementation**:
```java
Div placeholder = new Div()
    .withAttribute("hx-get", "/api/comments")
    .withAttribute("hx-trigger", "revealed")  // Load when visible
    .withAttribute("hx-swap", "outerHTML")
    .withChild(Spinner.create().withInnerText("Loading comments..."));
```

**Spring Controller**:
```java
@GetMapping("/api/comments")
@ResponseBody
public String loadComments() {
    List<Comment> comments = commentService.getAll();

    Div commentsContainer = new Div();
    for (Comment comment : comments) {
        commentsContainer.withChild(createCommentCard(comment));
    }

    return commentsContainer.render();
}
```

### Pattern 4: Form Submission with Validation

**Use Case**: Submit form, show success/error message inline

**Implementation**:
```java
Form.create()
    .withAttribute("hx-post", "/api/contact")
    .withAttribute("hx-target", "#form-result")
    .withAttribute("hx-swap", "innerHTML")
    .withCsrfToken(csrfToken.getToken())

    .addField("Email", TextInput.email("email").required())
    .addField("Message", TextArea.create("message").required())
    .addField("", Button.submit("Send"));

// Result container
Div result = new Div().withAttribute("id", "form-result");
```

**Spring Controller**:
```java
@PostMapping("/api/contact")
@ResponseBody
public String handleContact(@RequestParam String email,
                           @RequestParam String message,
                           @Valid @ModelAttribute ContactRequest request,
                           BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
        return Alert.error("Please fix the errors").render();
    }

    contactService.send(email, message);
    return Alert.success("Message sent successfully!").render();
}
```

### Pattern 5: Inline Editing

**Use Case**: Click to edit field, save inline

**Implementation**:
```java
// Display mode
Div usernameDisplay = new Div()
    .withAttribute("id", "username-display")
    .withChild(new Span().withInnerText("Current: " + user.getUsername()))
    .withChild(Button.create("Edit")
        .withAttribute("hx-get", "/api/edit-username")
        .withAttribute("hx-target", "#username-display")
        .withAttribute("hx-swap", "outerHTML"));
```

**Controller - Show Edit Form**:
```java
@GetMapping("/api/edit-username")
@ResponseBody
public String editUsernameForm(CsrfToken csrfToken) {
    return new Div()
        .withAttribute("id", "username-display")
        .withChild(Form.create()
            .withAttribute("hx-post", "/api/save-username")
            .withAttribute("hx-target", "#username-display")
            .withAttribute("hx-swap", "outerHTML")
            .withCsrfToken(csrfToken.getToken())
            .addField("", TextInput.create("username")
                .withMaxWidth("200px")
                .required())
            .addField("", Button.submit("Save")))
        .render();
}
```

**Controller - Save Changes**:
```java
@PostMapping("/api/save-username")
@ResponseBody
public String saveUsername(@RequestParam String username) {
    userService.updateUsername(username);

    // Return display mode
    return new Div()
        .withAttribute("id", "username-display")
        .withChild(new Span().withInnerText("Current: " + username))
        .withChild(Button.create("Edit")
            .withAttribute("hx-get", "/api/edit-username")
            .withAttribute("hx-target", "#username-display")
            .withAttribute("hx-swap", "outerHTML"))
        .render();
}
```

### Pattern 6: Filtering/Sorting

**Use Case**: Filter or sort table dynamically

**Implementation**:
```java
// Filter controls
Select categoryFilter = Select.create("category")
    .addOption("", "All Categories")
    .addOption("tech", "Technology")
    .addOption("sports", "Sports")
    .withAttribute("hx-get", "/api/posts")
    .withAttribute("hx-target", "#posts-table")
    .withAttribute("hx-trigger", "change")
    .withAttribute("hx-include", "#search-input");  // Include search term

TextInput searchInput = TextInput.search("q")
    .withAttribute("id", "search-input")
    .withAttribute("hx-get", "/api/posts")
    .withAttribute("hx-target", "#posts-table")
    .withAttribute("hx-trigger", "input delay:500ms")  // Debounce
    .withAttribute("hx-include", "[name='category']");  // Include category

// Results container
Div postsTable = new Div().withAttribute("id", "posts-table");
```

**Spring Controller**:
```java
@GetMapping("/api/posts")
@ResponseBody
public String filterPosts(@RequestParam(required = false) String q,
                         @RequestParam(required = false) String category) {
    List<Post> posts = postService.filter(q, category);

    DataTable<Post> table = DataTable.create(Post.class)
        .withData(posts)
        .withColumn("Title", Post::getTitle)
        .withColumn("Author", Post::getAuthor)
        .withColumn("Date", Post::getDate);

    return table.render();
}
```

## HTMX Response Pattern in Spring

### Detecting HTMX Requests

Spring controllers can detect HTMX requests via header:

```java
@GetMapping("/api/items")
@ResponseBody
public String getItems(@RequestHeader(value = "HX-Request", required = false) String hxRequest) {
    if (hxRequest != null) {
        // HTMX request - return HTML fragment
        return buildItemsFragment().render();
    } else {
        // Normal request - return full page
        return buildFullPage().render();
    }
}
```

### Return HTML Fragments

**Critical**: HTMX endpoints return HTML, not JSON!

```java
// ✅ CORRECT - Return HTML
@GetMapping("/api/items")
@ResponseBody
public String getItems() {
    return Div.create()
        .withChild(Paragraph.create().withInnerText("Item 1"))
        .withChild(Paragraph.create().withInnerText("Item 2"))
        .render();  // HTML string
}

// ❌ WRONG - Don't return JSON for HTMX
@GetMapping("/api/items")
@ResponseBody
public List<Item> getItems() {
    return itemList;  // Returns JSON, HTMX expects HTML
}
```

### HTTP Status Codes

Use appropriate status codes:

```java
@PostMapping("/api/submit")
@ResponseBody
public ResponseEntity<String> submit(@Valid @ModelAttribute Request request,
                                    BindingResult result) {
    if (result.hasErrors()) {
        // 400 Bad Request
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Alert.error("Validation failed").render());
    }

    service.save(request);

    // 200 OK
    return ResponseEntity.ok(
        Alert.success("Saved successfully!").render()
    );
}
```

## HTMX Loading States

### Show Loading Indicator

```java
// Add spinner
Div spinner = new Div()
    .withAttribute("id", "spinner")
    .withClass("htmx-indicator")  // Hidden by default
    .withChild(Spinner.create().withInnerText("Loading..."));

// Button shows spinner during request
Button.create("Load")
    .withAttribute("hx-get", "/api/data")
    .withAttribute("hx-indicator", "#spinner");  // Show #spinner
```

### CSS for Indicators

```css
/* Hidden by default */
.htmx-indicator {
    display: none;
}

/* Shown during HTMX request */
.htmx-request .htmx-indicator {
    display: block;
}

/* Alternative: on requesting element */
.htmx-request.htmx-indicator {
    display: block;
}
```

### Disable Button During Request

```css
button.htmx-request {
    opacity: 0.5;
    cursor: not-allowed;
    pointer-events: none;
}
```

## HTMX Error Handling

### Handle HTTP Errors

HTMX triggers events on errors that you can handle:

```javascript
// Optional: Add custom error handling
document.body.addEventListener('htmx:responseError', function(evt) {
    alert('An error occurred: ' + evt.detail.xhr.status);
});
```

### Server-Side Error Responses

```java
@PostMapping("/api/submit")
@ResponseBody
public ResponseEntity<String> submit(@RequestParam String data) {
    try {
        service.process(data);
        return ResponseEntity.ok(
            Alert.success("Success!").render()
        );
    } catch (ValidationException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Alert.error(e.getMessage()).render());
    } catch (Exception e) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Alert.error("An error occurred").render());
    }
}
```

## Security Considerations

### 1. Verify HX-Request Header

Prevent direct browser access to HTMX endpoints:

```java
@GetMapping("/api/internal")
@ResponseBody
public String internalEndpoint(@RequestHeader(value = "HX-Request", required = false) String hxRequest) {
    if (hxRequest == null) {
        throw new AccessDeniedException("Direct access not allowed");
    }

    // Process HTMX request
    return buildFragment().render();
}
```

### 2. CSRF Tokens on All HTMX POST Requests

```java
Form.create()
    .withAttribute("hx-post", "/api/submit")
    .withCsrfToken(csrfToken.getToken());  // CRITICAL
```

Spring Security automatically validates the token.

### 3. Authorization Checks

```java
@PostMapping("/api/admin/action")
@PreAuthorize("hasRole('ADMIN')")  // Require ADMIN role
@ResponseBody
public String adminAction() {
    // Admin-only action
    return Alert.success("Action completed").render();
}
```

### 4. Input Validation

```java
@PostMapping("/api/submit")
@ResponseBody
public String submit(@Valid @ModelAttribute Request request,
                    BindingResult result) {
    if (result.hasErrors()) {
        return Alert.error("Invalid input").render();
    }

    // Process validated request
    return Alert.success("Success").render();
}
```

## Complete HTMX Examples

### Example 1: Voting System

```java
// Upvote/Downvote buttons
Div voteButtons = new Div()
    .withClass("vote-buttons")
    .withChild(Button.create("↑ " + post.getUpvotes())
        .withClass("vote-btn")
        .withAttribute("hx-post", "/api/vote/up/" + post.getId())
        .withAttribute("hx-target", "#vote-count-" + post.getId())
        .withAttribute("hx-swap", "innerHTML"))
    .withChild(new Span()
        .withAttribute("id", "vote-count-" + post.getId())
        .withInnerText(String.valueOf(post.getScore())))
    .withChild(Button.create("↓ " + post.getDownvotes())
        .withClass("vote-btn")
        .withAttribute("hx-post", "/api/vote/down/" + post.getId())
        .withAttribute("hx-target", "#vote-count-" + post.getId())
        .withAttribute("hx-swap", "innerHTML"));
```

**Controller**:
```java
@PostMapping("/api/vote/up/{postId}")
@ResponseBody
public String upvote(@PathVariable Long postId) {
    int newScore = voteService.upvote(postId);
    return String.valueOf(newScore);
}
```

### Example 2: Infinite Scroll

```java
// Last item triggers next page load
Div lastItem = createItemCard(lastItemInPage)
    .withAttribute("hx-get", "/api/items?page=" + (currentPage + 1))
    .withAttribute("hx-trigger", "revealed")
    .withAttribute("hx-swap", "afterend")
    .withAttribute("hx-indicator", "#loading");
```

## Advanced: Dynamic Updates with Templates (OOB Swaps)

For complex dynamic updates, such as updating multiple parts of a page from a single request or updating a Module that was built once, you should use **Templates** and **Out-of-Band (OOB) Swaps**.

Since Modules are now "build-once", you cannot simply re-instantiate a Module with new data and swap it if the Module relies on internal state. Instead, you define the Module structure as a `Template` with `SlotKey`s, and then render that Template with a new `RenderContext`.

See [Part 13: Templates and Dynamic Updates](13-templates-and-dynamic-updates.md) for a deep dive into this pattern.

## Key Takeaways

1. **HTMX = HTML Attributes**: No JavaScript required for dynamic updates
2. **Server Returns HTML**: Not JSON - return HTML fragments
3. **Core Attributes**: hx-get/post, hx-target, hx-swap, hx-trigger
4. **Common Patterns**: Load more, auto-refresh, lazy loading, inline editing
5. **Security**: Verify HX-Request header, include CSRF tokens, validate input
6. **JHF Integration**: All components support HTMX via `withAttribute()`
7. **Progressive Enhancement**: Start with SSR, add HTMX where needed
8. **Spring Pattern**: Detect HX-Request header, return HTML fragments
9. **Templates for Complex Updates**: Use Templates and OOB swaps for robust dynamic module updates

---

**Previous**: [Part 8: Forms](08-forms.md)

**Next**: [Part 10: Building a Forum Tutorial](10-forum-tutorial.md)

**Table of Contents**: [Getting Started Guide](README.md)
