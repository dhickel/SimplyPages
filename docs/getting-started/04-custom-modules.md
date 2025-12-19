# Part 4: Building Custom Modules

Modules are high-level components that encapsulate complex UI patterns and business logic. While custom components (Part 3) are great for reusable HTML elements, modules provide structure for complete functional sections of your application.

## Modules vs Components (Revisited)

Let's clarify when to use each abstraction:

### Components
- **Low-level** HTML elements
- **Simple** structures (single tag or few tags)
- **General purpose** across many contexts
- **Examples**: Button, Card, Alert, Image

### Modules
- **High-level** functional units
- **Complex** structures (many components composed)
- **Domain-specific** to your application
- **Examples**: UserProfile, ShoppingCart, CommentThread, DashboardWidget

### Visual Comparison

**Component** (UserAvatar - simple):
```java
Image avatar = Image.create("/avatars/john.jpg", "John Doe")
    .withClass("avatar");
```

**Module** (UserProfileModule - complex):
```java
UserProfileModule profile = new UserProfileModule()
    .withUser(user)
    .withShowStats(true)
    .withShowRecentActivity(true);
    // Generates: Avatar + Name + Bio + Stats Grid + Recent Activity List
```

## When to Use Modules

Create a module when you have:

### 1. Repeated Component Patterns
You frequently build the same combination of components:

**Without Module**:
```java
// Repeated on every page with user profiles
Card userCard = Card.create()
    .withChild(new Div().withClass("user-header")
        .withChild(Image.create(user.getAvatar(), user.getName()))
        .withChild(Header.h3(user.getName())))
    .withChild(Paragraph.create().withInnerText(user.getBio()))
    .withChild(new Div().withClass("user-stats")
        .withChild(Badge.create(user.getPostCount() + " posts"))
        .withChild(Badge.create(user.getFollowers() + " followers")));
```

**With Module**:
```java
// Reusable across all pages
UserCardModule userCard = UserCardModule.create()
    .withUser(user);
```

### 2. Complex Multi-Element Structures
The UI has many nested components with specific logic:

**Examples**:
- Shopping cart with items, subtotals, tax, shipping, and checkout button
- Data table with sorting, filtering, pagination, and export
- Comment thread with replies, voting, moderation, and reply form
- Dashboard widget with header, chart, metrics, and refresh button

### 3. Domain-Specific UI Sections
Functionality specific to your business domain:

**E-commerce**:
- `ProductCardModule` - Product image, price, rating, add-to-cart
- `CheckoutSummaryModule` - Cart items, totals, promo codes
- `OrderHistoryModule` - Past orders with status tracking

**Social Platform**:
- `PostCardModule` - Post content, author, timestamp, likes, comments
- `NotificationFeedModule` - List of notifications with actions
- `ProfileSidebarModule` - User info, follow button, message button

**Analytics Dashboard**:
- `MetricCardModule` - Metric value, trend, sparkline chart
- `ReportModule` - Date range, filters, data table, export
- `ChartWidgetModule` - Chart with title, legend, and data controls

### 4. Encapsulation of Business Logic
The module needs to fetch data, apply formatting, or make decisions:

```java
public class DashboardStatsModule extends Module {
    private StatsService statsService;

    @Override
    protected void buildContent() {
        // Fetch data
        DashboardStats stats = statsService.getDashboardStats();

        // Business logic
        String trend = stats.getCurrentValue() > stats.getPreviousValue()
            ? "↑ Increasing"
            : "↓ Decreasing";

        // Build UI based on data and logic
        this.withChild(Header.h3(stats.getMetricName()));
        this.withChild(Paragraph.create()
            .withInnerText(String.valueOf(stats.getCurrentValue()))
            .withClass("stat-value"));
        this.withChild(Badge.create(trend)
            .withClass(trend.startsWith("↑") ? "badge-success" : "badge-danger"));
    }
}
```

## Extending Module

The `Module` abstract class provides the foundation for all modules.

### Module Base Class

```java
public abstract class Module extends HtmlTag {
    protected String moduleId;
    protected String title;
    private boolean isBuilt = false;

    public Module() {
        super("div");
        this.withClass("module");
    }

    // Subclasses implement this to build their content
    protected abstract void buildContent();

    @Override
    public String render() {
        if (!isBuilt) {
            buildContent();  // Called only once
            isBuilt = true;
        }
        return super.render();
    }

    // Common configuration methods
    public Module withModuleId(String moduleId) {
        this.moduleId = moduleId;
        this.withAttribute("id", moduleId);
        return this;
    }

    public Module withTitle(String title) {
        this.title = title;
        return this;
    }
}
```

### Key Features

1. **Lazy Building**: `buildContent()` called during first `render()`, not in constructor
2. **Once-Only Rendering**: `isBuilt` flag prevents duplicate children
3. **Common Properties**: `moduleId` and `title` available to all modules
4. **Extends HtmlTag**: Inherits all fluent methods and rendering logic

### Creating Your First Module

Basic module structure:

```java
package com.example.modules;

import io.mindspice.jhf.modules.Module;
import io.mindspice.jhf.components.*;

public class MyModule extends Module {

    // Module-specific properties
    private String data;

    public MyModule() {
        super();
        this.withClass("my-module");  // Additional CSS class
    }

    // Factory method
    public static MyModule create() {
        return new MyModule();
    }

    // Configuration methods
    public MyModule withData(String data) {
        this.data = data;
        return this;
    }

    // Build the module structure
    @Override
    protected void buildContent() {
        // Add title if provided
        if (title != null) {
            this.withChild(Header.h2(title).withClass("module-title"));
        }

        // Add your components here
        this.withChild(Paragraph.create().withInnerText(data));
    }
}
```

Usage:
```java
MyModule module = MyModule.create()
    .withTitle("My Module")
    .withData("Some data");

String html = module.render();
```

## The buildContent() Pattern

The `buildContent()` method is where you construct your module's structure.

### When buildContent() is Called

```java
MyModule module = MyModule.create();
// buildContent() NOT called yet

module.withTitle("Title");
// buildContent() NOT called yet

String html = module.render();
// buildContent() CALLED NOW (during first render())

String html2 = module.render();
// buildContent() NOT called (already built)
```

### Why Lazy Building?

**Problem without lazy building**:
```java
public class BadModule extends HtmlTag {
    public BadModule(String data) {
        super("div");
        // Building in constructor is too early
        this.withChild(Paragraph.create().withInnerText(data));
    }
}

BadModule module = new BadModule("Data");
module.withClass("custom-class");  // Can't configure after construction
```

**Solution with lazy building**:
```java
public class GoodModule extends Module {
    private String data;

    @Override
    protected void buildContent() {
        // Building here, after all configuration is done
        this.withChild(Paragraph.create().withInnerText(data));
    }

    public GoodModule withData(String data) {
        this.data = data;
        return this;
    }
}

GoodModule module = GoodModule.create()
    .withData("Data")          // Configure first
    .withClass("custom-class") // More configuration
    .withTitle("Title");       // Even more configuration
// Now render() calls buildContent() with all config applied
```

### buildContent() Best Practices

**1. Check for null properties**:
```java
@Override
protected void buildContent() {
    if (title != null) {
        this.withChild(Header.h3(title));
    }

    if (data != null && !data.isEmpty()) {
        this.withChild(Paragraph.create().withInnerText(data));
    }
}
```

**2. Use helper methods for complex sections**:
```java
@Override
protected void buildContent() {
    this.withChild(buildHeader());
    this.withChild(buildBody());
    this.withChild(buildFooter());
}

private Component buildHeader() {
    return new Div().withClass("module-header")
        .withChild(Header.h2(title));
}

private Component buildBody() {
    // ... complex body logic
}

private Component buildFooter() {
    // ... footer with actions
}
```

**3. Return nothing (void method)**:
```java
@Override
protected void buildContent() {
    this.withChild(...);
    this.withChild(...);
    // No return statement
}
```

## Module Configuration

Provide fluent methods for configuring your module.

### Basic Configuration Methods

```java
public class ArticleModule extends Module {
    private String articleId;
    private String authorName;
    private String publishedDate;
    private String content;
    private boolean showComments;
    private boolean showShareButtons;

    // Configuration methods
    public ArticleModule withArticleId(String articleId) {
        this.articleId = articleId;
        return this;
    }

    public ArticleModule withAuthor(String authorName) {
        this.authorName = authorName;
        return this;
    }

    public ArticleModule withPublishedDate(String date) {
        this.publishedDate = date;
        return this;
    }

    public ArticleModule withContent(String content) {
        this.content = content;
        return this;
    }

    public ArticleModule withComments(boolean show) {
        this.showComments = show;
        return this;
    }

    public ArticleModule withShareButtons(boolean show) {
        this.showShareButtons = show;
        return this;
    }
}
```

### Object-Based Configuration

Accept domain objects directly:

```java
public class ArticleModule extends Module {
    private Article article;
    private boolean showComments;

    public ArticleModule withArticle(Article article) {
        this.article = article;
        return this;
    }

    public ArticleModule withComments(boolean show) {
        this.showComments = show;
        return this;
    }

    @Override
    protected void buildContent() {
        this.withChild(Header.h1(article.getTitle()));
        this.withChild(Paragraph.create()
            .withInnerText("By " + article.getAuthor() + " on " + article.getDate())
            .withClass("article-meta"));
        this.withChild(new Markdown(article.getContent()));

        if (showComments) {
            this.withChild(buildCommentSection());
        }
    }
}
```

Usage:
```java
Article article = articleService.findById(123);
ArticleModule module = ArticleModule.create()
    .withArticle(article)
    .withComments(true);
```

### Validation and Defaults

```java
@Override
protected void buildContent() {
    // Validate required properties
    if (article == null) {
        throw new IllegalStateException("Article is required");
    }

    // Apply defaults
    if (showComments == null) {
        showComments = true;  // Default to showing comments
    }

    // Build content...
}
```

## Complete Example: ProfileCard Module

Let's build a complete user profile card module with avatar, name, bio, statistics, and action buttons.

### Step 1: Define Requirements

The module should display:
- User avatar (image)
- User name (heading)
- User bio (paragraph)
- Statistics (posts, followers, following)
- Action buttons (Follow, Message)

### Step 2: Implementation

```java
package com.example.modules;

import io.mindspice.jhf.modules.Module;
import io.mindspice.jhf.components.*;
import io.mindspice.jhf.components.display.*;
import io.mindspice.jhf.components.forms.Button;
import io.mindspice.jhf.components.navigation.Link;

public class ProfileCardModule extends Module {

    // Properties
    private String userId;
    private String avatarUrl;
    private String userName;
    private String bio;
    private int postCount;
    private int followerCount;
    private int followingCount;
    private boolean showFollowButton;
    private boolean showMessageButton;

    public ProfileCardModule() {
        super();
        this.withClass("profile-card-module");
        this.showFollowButton = true;  // Default
        this.showMessageButton = true; // Default
    }

    public static ProfileCardModule create() {
        return new ProfileCardModule();
    }

    // Configuration methods
    public ProfileCardModule withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public ProfileCardModule withAvatar(String url) {
        this.avatarUrl = url;
        return this;
    }

    public ProfileCardModule withUserName(String name) {
        this.userName = name;
        return this;
    }

    public ProfileCardModule withBio(String bio) {
        this.bio = bio;
        return this;
    }

    public ProfileCardModule withStats(int posts, int followers, int following) {
        this.postCount = posts;
        this.followerCount = followers;
        this.followingCount = following;
        return this;
    }

    public ProfileCardModule withFollowButton(boolean show) {
        this.showFollowButton = show;
        return this;
    }

    public ProfileCardModule withMessageButton(boolean show) {
        this.showMessageButton = show;
        return this;
    }

    @Override
    protected void buildContent() {
        // Validation
        if (userId == null || userName == null) {
            throw new IllegalStateException("User ID and name are required");
        }

        // Build module structure
        this.withChild(buildHeader());
        this.withChild(buildBody());
        this.withChild(buildStats());
        this.withChild(buildActions());
    }

    private Component buildHeader() {
        String avatar = avatarUrl != null ? avatarUrl : "/default-avatar.png";

        return new Div().withClass("profile-card-header")
            .withChild(Image.create(avatar, userName)
                .withClass("profile-avatar")
                .withMaxWidth("100px"))
            .withChild(Header.h3(userName)
                .withClass("profile-name"));
    }

    private Component buildBody() {
        if (bio == null || bio.isEmpty()) {
            return new Div(); // Empty div if no bio
        }

        return Paragraph.create()
            .withInnerText(bio)
            .withClass("profile-bio");
    }

    private Component buildStats() {
        return new Div().withClass("profile-stats")
            .withChild(createStatBadge("Posts", postCount))
            .withChild(createStatBadge("Followers", followerCount))
            .withChild(createStatBadge("Following", followingCount));
    }

    private Component createStatBadge(String label, int count) {
        return new Div().withClass("stat-item")
            .withChild(new Span()
                .withInnerText(String.valueOf(count))
                .withClass("stat-count"))
            .withChild(new Span()
                .withInnerText(label)
                .withClass("stat-label"));
    }

    private Component buildActions() {
        Div actions = new Div().withClass("profile-actions");

        if (showFollowButton) {
            actions.withChild(Button.create("Follow")
                .withClass("btn btn-primary")
                .withAttribute("hx-post", "/user/" + userId + "/follow")
                .withAttribute("hx-target", "#follow-status"));
        }

        if (showMessageButton) {
            actions.withChild(Link.create("/messages/new?to=" + userId, "Message")
                .withClass("btn btn-secondary"));
        }

        return actions;
    }
}
```

### Step 3: Usage Examples

**Basic usage**:
```java
ProfileCardModule card = ProfileCardModule.create()
    .withUserId("john123")
    .withUserName("John Doe")
    .withAvatar("/avatars/john.jpg")
    .withBio("Senior Java Developer with 10 years of experience.")
    .withStats(245, 1200, 350);
```

**With object**:
```java
public class User {
    private String id;
    private String name;
    private String avatar;
    private String bio;
    private int posts, followers, following;
    // getters...
}

// Helper method in module
public ProfileCardModule withUser(User user) {
    return this.withUserId(user.getId())
        .withUserName(user.getName())
        .withAvatar(user.getAvatar())
        .withBio(user.getBio())
        .withStats(user.getPosts(), user.getFollowers(), user.getFollowing());
}

// Usage
User user = userService.findById("john123");
ProfileCardModule card = ProfileCardModule.create()
    .withUser(user);
```

**Customize actions**:
```java
ProfileCardModule card = ProfileCardModule.create()
    .withUser(currentUser)
    .withFollowButton(false)     // Don't show follow button for own profile
    .withMessageButton(false);   // Don't message yourself
```

### Step 4: Add to Page

```java
Page page = Page.create()
    .addRow(row -> row
        .withChild(Column.create().withWidth(4)
            .withChild(ProfileCardModule.create()
                .withUser(user1)))
        .withChild(Column.create().withWidth(4)
            .withChild(ProfileCardModule.create()
                .withUser(user2)))
        .withChild(Column.create().withWidth(4)
            .withChild(ProfileCardModule.create()
                .withUser(user3))))
    .render();
```

## Composition Strategies

Modules excel at composing multiple components and other modules.

### Strategy 1: Nesting Modules

Modules can contain other modules:

```java
public class DashboardModule extends Module {
    private List<User> topUsers;
    private List<Article> recentArticles;

    @Override
    protected void buildContent() {
        // Top Users section (module within module)
        this.withChild(Header.h2("Top Contributors"));
        for (User user : topUsers) {
            this.withChild(ProfileCardModule.create().withUser(user));
        }

        // Recent Articles section
        this.withChild(Header.h2("Recent Articles"));
        for (Article article : recentArticles) {
            this.withChild(ArticleCardModule.create().withArticle(article));
        }
    }
}
```

### Strategy 2: Combining with Layout Components

Use layout components (Row, Column) within modules:

```java
public class ComparisonModule extends Module {
    private Product product1;
    private Product product2;

    @Override
    protected void buildContent() {
        this.withChild(Header.h2("Product Comparison"));

        // Two-column layout within module
        Row row = new Row()
            .withChild(Column.create().withWidth(6)
                .withChild(buildProductCard(product1)))
            .withChild(Column.create().withWidth(6)
                .withChild(buildProductCard(product2)));

        this.withChild(row);
    }

    private Component buildProductCard(Product product) {
        return Card.create()
            .withChild(Image.create(product.getImage(), product.getName()))
            .withChild(Header.h3(product.getName()))
            .withChild(Paragraph.create().withInnerText("$" + product.getPrice()));
    }
}
```

### Strategy 3: HTMX Integration (Module Refreshing)

Modules with unique IDs can be dynamically refreshed via HTMX:

```java
public class LiveStatsModule extends Module {

    public LiveStatsModule() {
        super();
        this.withModuleId("live-stats");  // Unique ID for targeting
    }

    @Override
    protected void buildContent() {
        // Auto-refresh every 30 seconds
        this.withAttribute("hx-get", "/api/stats")
            .withAttribute("hx-trigger", "every 30s")
            .withAttribute("hx-swap", "outerHTML");

        // Build stats display
        // ...
    }
}
```

Server endpoint returns fresh module HTML:
```java
@GetMapping("/api/stats")
@ResponseBody
public String refreshStats() {
    return LiveStatsModule.create()
        .withData(statsService.getLatestStats())
        .render();
}
```

## Example: ActivityFeed Module

Let's build an activity feed module with pagination via HTMX.

```java
package com.example.modules;

import io.mindspice.jhf.modules.Module;
import io.mindspice.jhf.components.*;
import io.mindspice.jhf.components.forms.Button;
import java.util.List;

public class ActivityFeedModule extends Module {

    // Activity item model
    public static class Activity {
        private String icon;
        private String timestamp;
        private String description;
        private String actionUrl;

        public Activity(String icon, String timestamp, String description, String actionUrl) {
            this.icon = icon;
            this.timestamp = timestamp;
            this.description = description;
            this.actionUrl = actionUrl;
        }

        // Getters...
        public String getIcon() { return icon; }
        public String getTimestamp() { return timestamp; }
        public String getDescription() { return description; }
        public String getActionUrl() { return actionUrl; }
    }

    private List<Activity> activities;
    private boolean showLoadMore;
    private int currentPage;

    public ActivityFeedModule() {
        super();
        this.withClass("activity-feed-module");
        this.currentPage = 1;
    }

    public static ActivityFeedModule create() {
        return new ActivityFeedModule();
    }

    public ActivityFeedModule withActivities(List<Activity> activities) {
        this.activities = activities;
        return this;
    }

    public ActivityFeedModule withLoadMore(boolean show) {
        this.showLoadMore = show;
        return this;
    }

    public ActivityFeedModule withPage(int page) {
        this.currentPage = page;
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null) {
            this.withChild(Header.h3(title).withClass("feed-title"));
        }

        // Activity list container
        Div feedContainer = new Div()
            .withClass("activity-list")
            .withAttribute("id", "activity-list");

        if (activities != null && !activities.isEmpty()) {
            for (Activity activity : activities) {
                feedContainer.withChild(buildActivityItem(activity));
            }
        } else {
            feedContainer.withChild(Paragraph.create()
                .withInnerText("No recent activity")
                .withClass("text-muted"));
        }

        this.withChild(feedContainer);

        // Load more button (HTMX pagination)
        if (showLoadMore) {
            this.withChild(Button.create("Load More")
                .withClass("btn btn-secondary btn-block")
                .withAttribute("hx-get", "/api/activities?page=" + (currentPage + 1))
                .withAttribute("hx-target", "#activity-list")
                .withAttribute("hx-swap", "beforeend"));  // Append new items
        }
    }

    private Component buildActivityItem(Activity activity) {
        return new Div().withClass("activity-item")
            .withChild(Icon.create(activity.getIcon())
                .withClass("activity-icon"))
            .withChild(new Div().withClass("activity-content")
                .withChild(Paragraph.create()
                    .withInnerText(activity.getDescription())
                    .withClass("activity-description"))
                .withChild(new Span()
                    .withInnerText(activity.getTimestamp())
                    .withClass("activity-timestamp"))
                .withChild(Link.create(activity.getActionUrl(), "View →")
                    .withClass("activity-link")));
    }
}
```

### Usage

**Initial page load**:
```java
List<Activity> activities = activityService.getRecent(1, 10);  // Page 1, 10 items

Page page = Page.create()
    .addComponents(
        ActivityFeedModule.create()
            .withTitle("Recent Activity")
            .withActivities(activities)
            .withLoadMore(true)
            .withPage(1)
    )
    .render();
```

**HTMX endpoint for pagination**:
```java
@GetMapping("/api/activities")
@ResponseBody
public String loadMoreActivities(@RequestParam int page) {
    List<Activity> activities = activityService.getRecent(page, 10);

    // Return just the activity items (not the whole module)
    StringBuilder html = new StringBuilder();
    for (Activity activity : activities) {
        Div item = new Div().withClass("activity-item")
            .withChild(Icon.create(activity.getIcon()))
            .withChild(new Div().withClass("activity-content")
                .withChild(Paragraph.create().withInnerText(activity.getDescription()))
                .withChild(new Span().withInnerText(activity.getTimestamp())));
        html.append(item.render());
    }

    return html.toString();
}
```

## Testing Modules

Test modules with JUnit:

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ProfileCardModuleTest {

    @Test
    public void testBasicProfile() {
        ProfileCardModule module = ProfileCardModule.create()
            .withUserId("test123")
            .withUserName("Test User")
            .withBio("Test bio")
            .withStats(10, 50, 25);

        String html = module.render();

        assertTrue(html.contains("Test User"));
        assertTrue(html.contains("Test bio"));
        assertTrue(html.contains("10"));  // Posts
        assertTrue(html.contains("50"));  // Followers
    }

    @Test
    public void testMissingRequiredFields() {
        ProfileCardModule module = ProfileCardModule.create()
            .withUserName("Test");  // Missing userId

        assertThrows(IllegalStateException.class, () -> {
            module.render();
        });
    }

    @Test
    public void testHiddenButtons() {
        ProfileCardModule module = ProfileCardModule.create()
            .withUserId("test")
            .withUserName("Test")
            .withFollowButton(false)
            .withMessageButton(false);

        String html = module.render();

        assertFalse(html.contains("Follow"));
        assertFalse(html.contains("Message"));
    }
}
```

## Best Practices for Modules

### 1. Extend Module, Not HtmlTag
```java
// Good
public class MyModule extends Module { ... }

// Avoid (unless you have a specific reason)
public class MyModule extends HtmlTag { ... }
```

### 2. Implement buildContent(), Not Constructor Logic
```java
// Good
@Override
protected void buildContent() {
    this.withChild(buildHeader());
    this.withChild(buildBody());
}

// Avoid
public MyModule() {
    super();
    this.withChild(buildHeader());  // Too early
}
```

### 3. Provide Object-Based Configuration
```java
// Good - Accept domain objects
public MyModule withUser(User user) {
    return this.withUserId(user.getId())
        .withName(user.getName());
}

// Also good - Individual properties for flexibility
public MyModule withUserId(String id) { ... }
public MyModule withName(String name) { ... }
```

### 4. Use Helper Methods for Sections
```java
@Override
protected void buildContent() {
    this.withChild(buildHeader());
    this.withChild(buildStats());
    this.withChild(buildActions());
}

private Component buildHeader() { ... }
private Component buildStats() { ... }
private Component buildActions() { ... }
```

### 5. Validate Required Properties
```java
@Override
protected void buildContent() {
    if (requiredData == null) {
        throw new IllegalStateException("Required data not provided");
    }
    // ... build module
}
```

## Key Takeaways

1. **Modules for Complexity**: Use modules for complex, multi-component structures
2. **Lazy Building**: Implement `buildContent()`, not constructor logic
3. **Fluent Configuration**: Provide chainable methods for all properties
4. **Composition**: Combine components and other modules
5. **HTMX Ready**: Modules with IDs can be dynamically refreshed
6. **Reusability**: Build once, use everywhere in your application
7. **Testability**: Write JUnit tests for module rendering logic

---

**Previous**: [Part 3: Building Custom Components](03-custom-components.md)

**Next**: [Part 5: Pages and Layouts](05-pages-and-layouts.md)

**Table of Contents**: [Getting Started Guide](README.md)
