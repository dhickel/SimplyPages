# Example: Wiki-Style In-Place Editing

This example shows how to build in-place editing interfaces using multiple Template views that swap via HTMX. This pattern is perfect for wikis, collaborative documents, comment systems, and any interface where users edit content inline without page reloads.

## What We're Building

A wiki article module with two views:
- **Display View**: Shows the article content with an "Edit" button
- **Edit View**: Shows a form to edit the content with "Save" and "Cancel" buttons

Clicking "Edit" swaps to the edit form. Clicking "Save" saves the changes and swaps back to display view. Clicking "Cancel" swaps back without saving.

## The Pattern: Multiple Template Views

The key insight is that the same data can have multiple visual representations (templates), and HTMX makes it seamless to swap between them.

```
┌─────────────────┐
│  DISPLAY_TEMPLATE │ ─── "Edit" button ────> │  EDIT_TEMPLATE  │
│  (Read-only)      │                          │  (Form)         │
└─────────────────┘ <─── "Save" button ────── └─────────────────┘
         ▲                                               │
         └────────────── "Cancel" button ───────────────┘
```

## The Complete Example

### Step 1: Define SlotKeys for Article Data

```java
package com.example.pages;

import io.mindspice.jhf.components.*;
import io.mindspice.jhf.core.*;
import io.mindspice.jhf.layout.Page;
import io.mindspice.jhf.modules.ContentModule;

public class WikiArticlePage {

    // SlotKeys for article data
    public static final SlotKey<String> ARTICLE_ID = SlotKey.of("article_id");
    public static final SlotKey<String> ARTICLE_TITLE = SlotKey.of("article_title");
    public static final SlotKey<String> ARTICLE_CONTENT = SlotKey.of("article_content");
    public static final SlotKey<String> ARTICLE_AUTHOR = SlotKey.of("article_author");
    public static final SlotKey<String> ARTICLE_UPDATED = SlotKey.of("article_updated");
}
```

**Key Points:**
- We define SlotKeys for all article properties
- Same data (title, content, etc.) will be used in both templates
- Think of these as the "data schema" for the article

### Step 2: Create the Display Template

```java
public class WikiArticlePage {
    // ... (SlotKeys from Step 1) ...

    // Template for displaying article (read-only view)
    public static final Template DISPLAY_TEMPLATE = Template.of(
        ContentModule.create()
            .withModuleId(Slot.of(ARTICLE_ID))  // Dynamic ID for HTMX targeting
            .withCustomContent(
                new Div()
                    .withClass("wiki-article")
                    // Article header
                    .withChild(
                        new Header.H2()
                            .withClass("article-title")
                            .withChild(Slot.of(ARTICLE_TITLE))
                    )
                    // Article metadata
                    .withChild(
                        new Div()
                            .withClass("article-meta text-muted")
                            .withInnerText("By ")
                            .withChild(new Span().withChild(Slot.of(ARTICLE_AUTHOR)))
                            .withInnerText(" • Last updated: ")
                            .withChild(new Span().withChild(Slot.of(ARTICLE_UPDATED)))
                    )
                    // Article content (rendered as Markdown)
                    .withChild(
                        new Div()
                            .withClass("article-content mt-3")
                            .withChild(new Markdown(Slot.of(ARTICLE_CONTENT)))
                    )
                    // Edit button with HTMX
                    .withChild(
                        new Button("Edit Article")
                            .withClass("btn btn-primary mt-3")
                            .withAttribute("hx-get", "/api/wiki/edit/" + Slot.of(ARTICLE_ID))
                            .withAttribute("hx-target", "#" + Slot.of(ARTICLE_ID))
                            .withAttribute("hx-swap", "outerHTML")  // Replace entire module
                    )
            )
    );
}
```

**Key Points:**
- Display view is read-only - shows content using Markdown component
- "Edit" button uses `hx-get` to fetch the edit form
- `hx-swap="outerHTML"` replaces the entire module with the edit form
- Notice how Slot placeholders work even in attribute values

### Step 3: Create the Edit Template

```java
public class WikiArticlePage {
    // ... (SlotKeys and DISPLAY_TEMPLATE from above) ...

    // Template for editing article (form view)
    public static final Template EDIT_TEMPLATE = Template.of(
        ContentModule.create()
            .withModuleId(Slot.of(ARTICLE_ID))  // Same ID as display template!
            .withCustomContent(
                new Div()
                    .withClass("wiki-article-edit")
                    // Form with HTMX
                    .withChild(
                        new HtmlTag("form")
                            .withAttribute("hx-post", "/api/wiki/save/" + Slot.of(ARTICLE_ID))
                            .withAttribute("hx-target", "#" + Slot.of(ARTICLE_ID))
                            .withAttribute("hx-swap", "outerHTML")  // Replace with display view after save
                            .withClass("wiki-edit-form")

                            // Title input
                            .withChild(
                                new Div()
                                    .withClass("form-group mb-3")
                                    .withChild(new Label("Title"))
                                    .withChild(
                                        new TextInput("title")
                                            .withValue(Slot.of(ARTICLE_TITLE))
                                            .withClass("form-control")
                                            .withRequired(true)
                                    )
                            )

                            // Content textarea
                            .withChild(
                                new Div()
                                    .withClass("form-group mb-3")
                                    .withChild(new Label("Content (Markdown)"))
                                    .withChild(
                                        new TextArea("content")
                                            .withValue(Slot.of(ARTICLE_CONTENT))
                                            .withClass("form-control")
                                            .withAttribute("rows", "15")
                                            .withRequired(true)
                                    )
                            )

                            // Action buttons
                            .withChild(
                                new Div()
                                    .withClass("form-actions")
                                    .withChild(
                                        new Button("Save Changes")
                                            .withType("submit")
                                            .withClass("btn btn-success me-2")
                                    )
                                    .withChild(
                                        new Button("Cancel")
                                            .withType("button")
                                            .withClass("btn btn-secondary")
                                            .withAttribute("hx-get", "/api/wiki/view/" + Slot.of(ARTICLE_ID))
                                            .withAttribute("hx-target", "#" + Slot.of(ARTICLE_ID))
                                            .withAttribute("hx-swap", "outerHTML")
                                    )
                            )
                    )
            )
    );
}
```

**Key Points:**
- Edit view is a form with text inputs
- Form submits via `hx-post` to save endpoint
- "Cancel" button uses `hx-get` to fetch display view without saving
- Both templates use the same module ID - this is how HTMX knows what to replace

### Step 4: Create the Page

```java
public class WikiArticlePage {
    // ... (SlotKeys and Templates from above) ...

    private final String articleId;
    private final String title;
    private final String content;
    private final String author;
    private final String updated;

    public WikiArticlePage(String articleId, String title, String content, String author, String updated) {
        this.articleId = articleId;
        this.title = title;
        this.content = content;
        this.author = author;
        this.updated = updated;
    }

    public String render() {
        return Page.builder()
            .addComponents(Header.H1("Wiki Article"))

            // Render the article in display mode initially
            .addComponents(renderDisplayView())

            .build()
            .render();
    }

    // Helper to render display view
    private Component renderDisplayView() {
        return new Component() {
            @Override
            public String render(RenderContext context) {
                return DISPLAY_TEMPLATE.render(
                    RenderContext.builder()
                        .with(ARTICLE_ID, "article-" + articleId)
                        .with(ARTICLE_TITLE, title)
                        .with(ARTICLE_CONTENT, content)
                        .with(ARTICLE_AUTHOR, author)
                        .with(ARTICLE_UPDATED, updated)
                        .build()
                );
            }
        };
    }
}
```

**Key Points:**
- Initial page render shows the display template
- We wrap the template in a Component for cleaner composition
- Article data comes from constructor (loaded from database in real app)

### Step 5: Create Controller Endpoints

```java
package com.example.controllers;

import org.springframework.web.bind.annotation.*;
import com.example.pages.WikiArticlePage;
import com.example.models.Article;  // Your data model
import com.example.services.ArticleService;  // Your service layer
import io.mindspice.jhf.core.RenderContext;

@RestController
@RequestMapping("/api/wiki")
public class WikiController {

    private final ArticleService articleService;

    public WikiController(ArticleService articleService) {
        this.articleService = articleService;
    }

    // Endpoint to fetch edit form
    @GetMapping("/edit/{articleId}")
    @ResponseBody
    public String getEditForm(@PathVariable String articleId) {
        // Load article from database
        Article article = articleService.findById(articleId);

        // Render EDIT_TEMPLATE with article data
        return WikiArticlePage.EDIT_TEMPLATE.render(
            RenderContext.builder()
                .with(WikiArticlePage.ARTICLE_ID, "article-" + article.getId())
                .with(WikiArticlePage.ARTICLE_TITLE, article.getTitle())
                .with(WikiArticlePage.ARTICLE_CONTENT, article.getContent())
                .build()
        );
    }

    // Endpoint to save changes and return display view
    @PostMapping("/save/{articleId}")
    @ResponseBody
    public String saveArticle(
            @PathVariable String articleId,
            @RequestParam String title,
            @RequestParam String content
    ) {
        // Update article in database
        Article article = articleService.findById(articleId);
        article.setTitle(title);
        article.setContent(content);
        article.setUpdated(java.time.LocalDateTime.now());
        articleService.save(article);

        // Render DISPLAY_TEMPLATE with updated data
        return WikiArticlePage.DISPLAY_TEMPLATE.render(
            RenderContext.builder()
                .with(WikiArticlePage.ARTICLE_ID, "article-" + article.getId())
                .with(WikiArticlePage.ARTICLE_TITLE, article.getTitle())
                .with(WikiArticlePage.ARTICLE_CONTENT, article.getContent())
                .with(WikiArticlePage.ARTICLE_AUTHOR, article.getAuthor())
                .with(WikiArticlePage.ARTICLE_UPDATED, article.getUpdated().toString())
                .build()
        );
    }

    // Endpoint to cancel editing and return display view (no save)
    @GetMapping("/view/{articleId}")
    @ResponseBody
    public String getDisplayView(@PathVariable String articleId) {
        // Load article from database (unchanged)
        Article article = articleService.findById(articleId);

        // Render DISPLAY_TEMPLATE
        return WikiArticlePage.DISPLAY_TEMPLATE.render(
            RenderContext.builder()
                .with(WikiArticlePage.ARTICLE_ID, "article-" + article.getId())
                .with(WikiArticlePage.ARTICLE_TITLE, article.getTitle())
                .with(WikiArticlePage.ARTICLE_CONTENT, article.getContent())
                .with(WikiArticlePage.ARTICLE_AUTHOR, article.getAuthor())
                .with(WikiArticlePage.ARTICLE_UPDATED, article.getUpdated().toString())
                .build()
        );
    }
}
```

**Key Points:**
- Three endpoints: `/edit` (get form), `/save` (save and show), `/view` (cancel and show)
- `/edit` returns EDIT_TEMPLATE
- `/save` saves to database then returns DISPLAY_TEMPLATE
- `/view` returns DISPLAY_TEMPLATE without saving
- All endpoints render the same templates with fresh data from database

## How It Works: The Flow

### Flow 1: User Clicks "Edit"

1. User sees article in display view
2. User clicks "Edit Article" button
3. Browser sends: `GET /api/wiki/edit/123`
4. Server loads article from database
5. Server renders EDIT_TEMPLATE with article data
6. Server responds with HTML form
7. HTMX swaps `<div id="article-123">...</div>` (display) with form HTML (edit)
8. User sees editable form in place of article

### Flow 2: User Clicks "Save"

1. User edits title and content in form
2. User clicks "Save Changes" button
3. Browser sends: `POST /api/wiki/save/123` with form data
4. Server updates article in database
5. Server renders DISPLAY_TEMPLATE with updated data
6. Server responds with updated article HTML
7. HTMX swaps form with article display view
8. User sees updated article

### Flow 3: User Clicks "Cancel"

1. User is editing in form view
2. User clicks "Cancel" button (without submitting)
3. Browser sends: `GET /api/wiki/view/123`
4. Server loads unchanged article from database
5. Server renders DISPLAY_TEMPLATE with original data
6. Server responds with article HTML
7. HTMX swaps form with article display view
8. User sees original article (edits discarded)

## Key Concepts Explained

### Why Multiple Templates?

The same data can be represented in different ways:

```
Article Data:
{
  id: "123",
  title: "Getting Started",
  content: "# Welcome\n\nThis is a wiki...",
  author: "Alice",
  updated: "2025-01-15"
}

DISPLAY_TEMPLATE renders as:
┌─────────────────────────┐
│ Getting Started         │ (read-only heading)
│ By Alice • Updated ...  │ (metadata)
│ Welcome                 │ (rendered markdown)
│ This is a wiki...       │
│ [Edit Article]          │ (button)
└─────────────────────────┘

EDIT_TEMPLATE renders as:
┌─────────────────────────┐
│ Title: [Getting Started]│ (editable input)
│ Content:                │ (editable textarea)
│ [# Welcome...]          │
│                         │
│ [Save] [Cancel]         │ (buttons)
└─────────────────────────┘
```

### Why outerHTML Swap?

- `outerHTML` replaces the **entire element** including its tag
- This allows complete replacement: display module → form → display module
- Both templates generate `<div id="article-123">` so HTMX can swap them

Alternative swaps wouldn't work:
- `innerHTML` would only replace content inside the module, not the module itself
- `beforebegin`/`afterend` would duplicate elements instead of replacing

### The Module ID Must Match

**Critical**: Both templates must generate elements with the same ID:

```java
// Display template
.withModuleId(Slot.of(ARTICLE_ID))  // Generates: <div id="article-123">

// Edit template
.withModuleId(Slot.of(ARTICLE_ID))  // Generates: <div id="article-123">
                                    // (same ID!)
```

If IDs don't match, HTMX won't know what to replace.

## Common Mistakes & Solutions

### Mistake 1: Different IDs in Templates

**Problem**: Edit form appears as a new element instead of replacing display view.

**Cause**: Display and edit templates generate different IDs.

**Solution**: Use the same SlotKey for module ID in both templates:

```java
// Both templates use this:
.withModuleId(Slot.of(ARTICLE_ID))
```

### Mistake 2: Using innerHTML Instead of outerHTML

**Problem**: After edit, the module structure gets nested incorrectly.

**Solution**: Use `hx-swap="outerHTML"` to replace the entire element:

```java
.withAttribute("hx-swap", "outerHTML")  // Not "innerHTML"!
```

### Mistake 3: Forgetting to Load Fresh Data

**Problem**: After save, display view shows old data.

**Solution**: Always reload from database before rendering:

```java
@PostMapping("/save/{id}")
public String save(...) {
    // Save to DB
    articleService.save(article);

    // Reload fresh data (includes updated timestamp, etc.)
    Article fresh = articleService.findById(id);

    // Render with fresh data
    return DISPLAY_TEMPLATE.render(buildContext(fresh));
}
```

### Mistake 4: Re-Creating Templates

**Problem**: Templates are recreated on every request (slow).

**Solution**: Templates must be `static final` constants:

```java
// ✅ GOOD: Created once at class load
public static final Template DISPLAY_TEMPLATE = Template.of(...);
public static final Template EDIT_TEMPLATE = Template.of(...);

// ❌ BAD: Creates new template every request
public String getEditForm(String id) {
    Template editTemplate = Template.of(...);  // DON'T DO THIS
    return editTemplate.render(...);
}
```

## Variations & Extensions

### Variation 1: Add a "Preview" View

Add a third template for previewing changes before saving:

```java
public static final Template PREVIEW_TEMPLATE = Template.of(
    ContentModule.create()
        .withModuleId(Slot.of(ARTICLE_ID))
        .withCustomContent(
            new Div()
                .withClass("wiki-preview")
                .withChild(new Alert("Preview Mode - Changes not saved").withClass("alert-info"))
                .withChild(new Header.H2().withChild(Slot.of(ARTICLE_TITLE)))
                .withChild(new Markdown(Slot.of(ARTICLE_CONTENT)))
                .withChild(new Button("Back to Edit").withHxGet(...))
                .withChild(new Button("Save").withHxPost(...))
        )
);
```

Flow: Display → Edit → Preview → Edit or Save → Display

### Variation 2: Optimistic UI Updates

Show changes immediately, revert if save fails:

```java
.withAttribute("hx-swap", "outerHTML swap:1s")  // Swap after 1s delay
.withAttribute("hx-sync", "this:replace")       // Cancel pending requests
```

### Variation 3: Collaborative Editing Conflict Detection

Check if article was modified by another user:

```java
@PostMapping("/save/{id}")
public String save(..., @RequestParam long version) {
    Article article = articleService.findById(id);

    if (article.getVersion() != version) {
        // Show conflict template instead
        return CONFLICT_TEMPLATE.render(...);
    }

    // Normal save flow
    ...
}
```

### Variation 4: Auto-Save Draft

Save form data to draft every 30 seconds:

```java
new HtmlTag("form")
    .withAttribute("hx-post", "/api/wiki/save-draft/" + id)
    .withAttribute("hx-trigger", "every 30s")
    .withAttribute("hx-swap", "none")  // Don't swap, just save in background
```

## Real-World Usage

This pattern is perfect for:

- **Wiki Systems**: In-place article editing (Wikipedia-style)
- **Comment Systems**: Edit comments inline
- **Task Management**: Toggle tasks between view and edit
- **Profile Pages**: Edit profile information inline
- **Note-Taking Apps**: Switch between read and edit modes
- **CMS Interfaces**: Inline content editing

## Performance Benefits

Using templates for view swapping is highly efficient:

```
Without Templates (Pattern A):
Edit click → Build 50+ component tree → Render → Send HTML (slow!)
Save click → Build 50+ component tree → Render → Send HTML (slow!)

With Templates (Pattern B):
Edit click → Inject data into pre-compiled template → Send HTML (fast!)
Save click → Inject data into pre-compiled template → Send HTML (fast!)

Performance Gain: ~10-50x faster
```

## Next Steps

- Read [Advanced Rendering Patterns](../advanced/rendering-patterns.md) for Pattern A vs Pattern B comparison
- Read [Templates and Dynamic Updates](../getting-started/13-templates-and-dynamic-updates.md) for Template deep dive
- See [Live Dashboard Example](live-dashboard.md) for polling + OOB swaps
- Check [HTMX Dynamic Features](../getting-started/09-htmx-dynamic-features.md) for more HTMX patterns

## Complete Code Reference

This pattern is demonstrated in the framework's documentation viewer:
- Pattern: `src/main/java/io/mindspice/demo/pages/DocsPage.java`
- Live demo: Visit `/docs` when running the application
