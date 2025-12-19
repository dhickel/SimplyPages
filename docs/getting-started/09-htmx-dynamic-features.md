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

## Key Takeaways

1. **HTMX = HTML Attributes**: No JavaScript required for dynamic updates
2. **Server Returns HTML**: Not JSON - return HTML fragments
3. **Core Attributes**: hx-get/post, hx-target, hx-swap, hx-trigger
4. **Common Patterns**: Load more, auto-refresh, lazy loading, inline editing
5. **Security**: Verify HX-Request header, include CSRF tokens, validate input
6. **JHF Integration**: All components support HTMX via `withAttribute()`
7. **Progressive Enhancement**: Start with SSR, add HTMX where needed
8. **Spring Pattern**: Detect HX-Request header, return HTML fragments

---

**Previous**: [Part 8: Forms](08-forms.md)

**Next**: [Part 10: Building a Forum Tutorial](10-forum-tutorial.md)

**Table of Contents**: [Getting Started Guide](README.md)
