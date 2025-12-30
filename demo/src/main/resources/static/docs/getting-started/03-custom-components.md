# Part 3: Building Custom Components

While JHF provides 50+ built-in components, you'll often need domain-specific components for your application. This guide teaches you how to create custom, reusable components that integrate seamlessly with the framework.

## When to Create Custom Components

Create custom components when you have:

### 1. Reusable UI Patterns
You repeatedly build the same HTML structure across multiple pages.

**Example**: User avatar with status badge
```java
// Instead of repeating this everywhere:
new Div().withClass("user-avatar")
    .withChild(Image.create(user.getAvatar(), user.getName()))
    .withChild(new Span().withClass("status-badge online").withInnerText("Online"));

// Create a reusable component:
UserAvatar.create(user);
```

### 2. Domain-Specific Elements
You need components specific to your business domain.

**Examples**:
- E-commerce: `ProductCard`, `ShoppingCartItem`, `PriceTag`
- Social media: `PostCard`, `CommentBox`, `LikeButton`
- Analytics: `MetricCard`, `ChartWidget`, `DashboardTile`
- Forums: `ThreadPreview`, `VoteButtons`, `UserReputation`

### 3. Encapsulating Complex HTML
You want to hide complex HTML structure behind a simple API.

**Example**: A complex notification with icon, title, message, and actions
```java
// Instead of exposing the complexity:
Div notification = new Div().withClass("notification notification-warning")
    .withChild(new Div().withClass("notification-header")
        .withChild(Icon.create("warning"))
        .withChild(Header.h4("Warning")))
    .withChild(Paragraph.create().withInnerText("Your session expires in 5 minutes."))
    .withChild(new Div().withClass("notification-actions")
        .withChild(Button.create("Extend Session"))
        .withChild(Button.create("Dismiss")));

// Provide a clean API:
Notification.warning("Your session expires in 5 minutes.")
    .withAction("Extend Session", "/extend")
    .withAction("Dismiss", "#");
```

### 4. Type Safety and Validation
You want compile-time checking for valid configurations.

**Example**: Status badge with enum-based states
```java
public enum StatusType {
    ONLINE, AWAY, BUSY, OFFLINE
}

StatusBadge.create(StatusType.ONLINE);  // Type-safe
// StatusBadge.create("online");  // Won't compile
```

## Extending HtmlTag

Most custom components extend `HtmlTag`, the base class that provides core HTML rendering functionality.

### Basic Structure

```java
package com.example.components;

import io.mindspice.jhf.core.HtmlTag;

public class MyComponent extends HtmlTag {

    // Constructor calls super with the HTML tag name
    public MyComponent() {
        super("div");  // This component renders as <div>...</div>
    }

    // Static factory method (convention)
    public static MyComponent create() {
        return new MyComponent();
    }
}
```

Key points:
1. **Extend HtmlTag**: Inherit rendering logic and fluent methods
2. **Call super(tagName)**: Specify the HTML tag this component generates
3. **Provide create() method**: Follow JHF conventions

### Adding Properties

Add private fields for component-specific configuration:

```java
public class StatusBadge extends HtmlTag {
    private String status;
    private String color;

    public StatusBadge() {
        super("span");
        this.withClass("status-badge");  // Default CSS class
    }

    public static StatusBadge create(String status) {
        StatusBadge badge = new StatusBadge();
        badge.status = status;
        return badge;
    }

    // Override render() to apply status-specific styling
    @Override
    public String render() {
        this.withInnerText(status);
        this.withClass("status-" + status.toLowerCase());
        return super.render();
    }
}
```

### Fluent Configuration Methods

Add methods that return `this` for method chaining:

```java
public class StatusBadge extends HtmlTag {
    private String status;
    private String color;
    private boolean showDot;

    // ... constructor and create() method

    public StatusBadge withColor(String color) {
        this.color = color;
        return this;
    }

    public StatusBadge withDot(boolean showDot) {
        this.showDot = showDot;
        return this;
    }

    @Override
    public String render() {
        this.withInnerText(status);

        if (color != null) {
            this.withStyle("background-color", color);
        }

        if (showDot) {
            this.withChild(new Span().withClass("status-dot"));
        }

        return super.render();
    }
}
```

Usage:
```java
StatusBadge badge = StatusBadge.create("Online")
    .withColor("#00ff00")
    .withDot(true);
```

## Factory Method Pattern

The static `create()` method is a JHF convention that provides several benefits.

### Why Factory Methods?

**1. Readability**
```java
// Factory method (clear intent)
Alert alert = Alert.success("Operation complete");

// Constructor (less clear)
Alert alert = new Alert(AlertType.SUCCESS, "Operation complete");
```

**2. Flexibility**
```java
// Can return subtypes or pre-configured instances
public static Card createUserCard() {
    return Card.create()
        .withClass("user-card")
        .withClass("shadow-sm");  // Pre-configured defaults
}
```

**3. Method Overloading**
```java
// Multiple factory methods with different parameters
public static UserAvatar create(User user) {
    return new UserAvatar(user.getAvatarUrl(), user.getName());
}

public static UserAvatar create(String url, String name) {
    return new UserAvatar(url, name);
}

public static UserAvatar createWithStatus(User user, StatusType status) {
    UserAvatar avatar = new UserAvatar(user.getAvatarUrl(), user.getName());
    avatar.withStatus(status);
    return avatar;
}
```

### Factory Method Patterns

**Pattern 1: Simple Factory**
```java
public static MyComponent create() {
    return new MyComponent();
}
```

**Pattern 2: Factory with Required Parameters**
```java
public static ImageCard create(String imageUrl, String title) {
    ImageCard card = new ImageCard();
    card.imageUrl = imageUrl;
    card.title = title;
    return card;
}
```

**Pattern 3: Type-Specific Factories**
```java
public static Alert success(String message) {
    return new Alert(AlertType.SUCCESS, message);
}

public static Alert error(String message) {
    return new Alert(AlertType.ERROR, message);
}

public static Alert warning(String message) {
    return new Alert(AlertType.WARNING, message);
}
```

**Pattern 4: Builder-Style Factory**
```java
public static PriceTag create() {
    return new PriceTag();
}

// Then use fluent methods:
PriceTag.create()
    .withAmount(99.99)
    .withCurrency("USD")
    .withDiscount(10);
```

## Complete Example: Custom Alert Component

Let's build a custom `Notification` component with icon, title, message, and action buttons.

### Step 1: Define the Component Structure

HTML we want to generate:
```html
<div class="notification notification-success">
    <div class="notification-header">
        <i class="icon icon-check"></i>
        <h4>Success</h4>
    </div>
    <p class="notification-message">Your changes have been saved.</p>
    <div class="notification-actions">
        <button class="btn btn-sm">Undo</button>
        <button class="btn btn-sm">Dismiss</button>
    </div>
</div>
```

### Step 2: Create the Component Class

```java
package com.example.components;

import io.mindspice.jhf.core.HtmlTag;
import io.mindspice.jhf.components.*;
import io.mindspice.jhf.components.forms.Button;
import java.util.ArrayList;
import java.util.List;

public class Notification extends HtmlTag {

    // Enum for notification types
    public enum Type {
        SUCCESS("icon-check", "notification-success"),
        ERROR("icon-x", "notification-error"),
        WARNING("icon-alert", "notification-warning"),
        INFO("icon-info", "notification-info");

        private final String iconClass;
        private final String cssClass;

        Type(String iconClass, String cssClass) {
            this.iconClass = iconClass;
            this.cssClass = cssClass;
        }

        public String getIconClass() { return iconClass; }
        public String getCssClass() { return cssClass; }
    }

    // Component properties
    private Type type;
    private String title;
    private String message;
    private List<ActionButton> actions;

    // Inner class for action buttons
    private static class ActionButton {
        String label;
        String url;

        ActionButton(String label, String url) {
            this.label = label;
            this.url = url;
        }
    }

    // Constructor
    private Notification(Type type, String message) {
        super("div");
        this.type = type;
        this.message = message;
        this.title = type.name().charAt(0) + type.name().substring(1).toLowerCase();
        this.actions = new ArrayList<>();

        // Apply base CSS classes
        this.withClass("notification");
        this.withClass(type.getCssClass());
    }

    // Factory methods
    public static Notification success(String message) {
        return new Notification(Type.SUCCESS, message);
    }

    public static Notification error(String message) {
        return new Notification(Type.ERROR, message);
    }

    public static Notification warning(String message) {
        return new Notification(Type.WARNING, message);
    }

    public static Notification info(String message) {
        return new Notification(Type.INFO, message);
    }

    // Fluent configuration methods
    public Notification withTitle(String title) {
        this.title = title;
        return this;
    }

    public Notification withAction(String label, String url) {
        this.actions.add(new ActionButton(label, url));
        return this;
    }

    // Build the component structure
    @Override
    public String render() {
        // Header with icon and title
        Div header = new Div()
            .withClass("notification-header")
            .withChild(Icon.create(type.getIconClass()))
            .withChild(Header.h4(title));
        this.withChild(header);

        // Message
        this.withChild(Paragraph.create()
            .withInnerText(message)
            .withClass("notification-message"));

        // Actions (if any)
        if (!actions.isEmpty()) {
            Div actionContainer = new Div().withClass("notification-actions");
            for (ActionButton action : actions) {
                actionContainer.withChild(Button.create(action.label)
                    .withClass("btn btn-sm")
                    .withAttribute("onclick", "window.location.href='" + action.url + "'"));
            }
            this.withChild(actionContainer);
        }

        return super.render();
    }
}
```

### Step 3: Usage Examples

```java
// Simple success notification
Notification successNotif = Notification.success("Your changes have been saved.");

// Error notification with custom title and action
Notification errorNotif = Notification.error("Failed to save changes.")
    .withTitle("Save Error")
    .withAction("Retry", "/retry")
    .withAction("Cancel", "/cancel");

// Warning with multiple actions
Notification warningNotif = Notification.warning("Your session expires in 5 minutes.")
    .withAction("Extend Session", "/extend")
    .withAction("Dismiss", "#");

// Render to HTML
String html = errorNotif.render();
```

### Step 4: Add to Page

```java
Page page = Page.create()
    .addComponents(
        Notification.success("Welcome back!"),
        Notification.info("You have 3 unread messages.")
            .withAction("View Messages", "/messages"),
        ContentModule.create().withTitle("Dashboard")
    )
    .render();
```

## Advanced Patterns

### Pattern 1: Enum-Based Styling

Use enums for type-safe styling options:

```java
public class Badge extends HtmlTag {

    public enum Style {
        PRIMARY("badge-primary", "#007bff"),
        SUCCESS("badge-success", "#28a745"),
        WARNING("badge-warning", "#ffc107"),
        DANGER("badge-danger", "#dc3545");

        private final String cssClass;
        private final String color;

        Style(String cssClass, String color) {
            this.cssClass = cssClass;
            this.color = color;
        }

        public String getCssClass() { return cssClass; }
        public String getColor() { return color; }
    }

    private Style style;
    private String text;

    private Badge(String text, Style style) {
        super("span");
        this.text = text;
        this.style = style;
        this.withClass("badge");
        this.withClass(style.getCssClass());
    }

    public static Badge create(String text, Style style) {
        return new Badge(text, style);
    }

    // Type-specific factories
    public static Badge primary(String text) {
        return new Badge(text, Style.PRIMARY);
    }

    public static Badge success(String text) {
        return new Badge(text, Style.SUCCESS);
    }

    @Override
    public String render() {
        this.withInnerText(text);
        return super.render();
    }
}
```

Usage:
```java
Badge.success("Active");
Badge.create("Custom", Badge.Style.WARNING);
```

### Pattern 2: Composition (Combining Components)

Build complex components by composing simpler ones:

```java
public class UserCard extends HtmlTag {
    private String userId;
    private String name;
    private String avatar;
    private String role;
    private int reputation;

    private UserCard() {
        super("div");
        this.withClass("user-card");
    }

    public static UserCard create() {
        return new UserCard();
    }

    public UserCard withUser(String id, String name, String avatar) {
        this.userId = id;
        this.name = name;
        this.avatar = avatar;
        return this;
    }

    public UserCard withRole(String role) {
        this.role = role;
        return this;
    }

    public UserCard withReputation(int reputation) {
        this.reputation = reputation;
        return this;
    }

    @Override
    public String render() {
        // Header: Avatar + Name
        Div header = new Div().withClass("user-card-header")
            .withChild(Image.create(avatar, name).withClass("avatar"))
            .withChild(Header.h3(name));
        this.withChild(header);

        // Body: Role badge + reputation
        Div body = new Div().withClass("user-card-body");
        if (role != null) {
            body.withChild(Badge.primary(role));
        }
        if (reputation > 0) {
            body.withChild(new Span()
                .withInnerText("★ " + reputation + " points")
                .withClass("reputation"));
        }
        this.withChild(body);

        // Footer: Profile link
        this.withChild(new Div().withClass("user-card-footer")
            .withChild(Link.create("/user/" + userId, "View Profile")));

        return super.render();
    }
}
```

Usage:
```java
UserCard card = UserCard.create()
    .withUser("john123", "John Doe", "/avatars/john.jpg")
    .withRole("Administrator")
    .withReputation(1250);
```

### Pattern 3: Conditional Rendering

Render different content based on component state:

```java
public class DataDisplay extends HtmlTag {
    private Object data;
    private boolean isLoading;
    private String errorMessage;

    private DataDisplay() {
        super("div");
        this.withClass("data-display");
    }

    public static DataDisplay create() {
        return new DataDisplay();
    }

    public DataDisplay withData(Object data) {
        this.data = data;
        return this;
    }

    public DataDisplay withLoading(boolean isLoading) {
        this.isLoading = isLoading;
        return this;
    }

    public DataDisplay withError(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    @Override
    public String render() {
        // Loading state
        if (isLoading) {
            this.withChild(Spinner.create().withInnerText("Loading..."));
            return super.render();
        }

        // Error state
        if (errorMessage != null) {
            this.withChild(Alert.error(errorMessage));
            return super.render();
        }

        // Data state
        if (data != null) {
            this.withChild(Paragraph.create().withInnerText(data.toString()));
        } else {
            this.withChild(Paragraph.create()
                .withInnerText("No data available")
                .withClass("text-muted"));
        }

        return super.render();
    }
}
```

Usage:
```java
// Loading state
DataDisplay.create().withLoading(true);

// Error state
DataDisplay.create().withError("Failed to load data");

// Success state
DataDisplay.create().withData(myData);
```

## Example: Custom Notification Component with Auto-Dismiss

Let's build a notification that can automatically dismiss itself using HTMX:

```java
public class AutoNotification extends HtmlTag {

    private Notification.Type type;
    private String message;
    private int dismissAfterSeconds;
    private String notificationId;

    private AutoNotification(Notification.Type type, String message) {
        super("div");
        this.type = type;
        this.message = message;
        this.notificationId = "notif-" + System.currentTimeMillis();
        this.withClass("notification");
        this.withClass(type.getCssClass());
        this.withAttribute("id", notificationId);
    }

    public static AutoNotification success(String message) {
        return new AutoNotification(Notification.Type.SUCCESS, message);
    }

    public static AutoNotification error(String message) {
        return new AutoNotification(Notification.Type.ERROR, message);
    }

    public static AutoNotification warning(String message) {
        return new AutoNotification(Notification.Type.WARNING, message);
    }

    public AutoNotification withAutoDismiss(int seconds) {
        this.dismissAfterSeconds = seconds;
        return this;
    }

    @Override
    public String render() {
        // Message content
        this.withChild(Paragraph.create()
            .withInnerText(message)
            .withClass("notification-message"));

        // Dismiss button
        Button dismissBtn = Button.create("×")
            .withClass("notification-close")
            .withAttribute("onclick", "this.parentElement.remove()");
        this.withChild(dismissBtn);

        // Auto-dismiss with HTMX (covered in Part 9)
        if (dismissAfterSeconds > 0) {
            this.withAttribute("hx-get", "/api/dismiss-notification")
                .withAttribute("hx-trigger", "load delay:" + dismissAfterSeconds + "s")
                .withAttribute("hx-swap", "outerHTML")
                .withAttribute("hx-target", "#" + notificationId);
        }

        return super.render();
    }
}
```

Usage:
```java
// Auto-dismiss after 5 seconds
AutoNotification.success("Changes saved!")
    .withAutoDismiss(5);

// Manual dismiss only
AutoNotification.error("An error occurred.");
```

## Testing Custom Components

Test component rendering with JUnit:

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NotificationTest {

    @Test
    public void testSuccessNotification() {
        Notification notif = Notification.success("Test message");
        String html = notif.render();

        assertTrue(html.contains("notification-success"));
        assertTrue(html.contains("Test message"));
        assertTrue(html.contains("icon-check"));
    }

    @Test
    public void testWithActions() {
        Notification notif = Notification.error("Error occurred")
            .withAction("Retry", "/retry")
            .withAction("Cancel", "/cancel");

        String html = notif.render();

        assertTrue(html.contains("Retry"));
        assertTrue(html.contains("Cancel"));
        assertTrue(html.contains("/retry"));
    }

    @Test
    public void testCustomTitle() {
        Notification notif = Notification.warning("Warning message")
            .withTitle("Custom Title");

        String html = notif.render();

        assertTrue(html.contains("Custom Title"));
        assertFalse(html.contains("Warning")); // Default title not used
    }
}
```

## Best Practices for Custom Components

### 1. Follow JHF Naming Conventions
- Class names: PascalCase (`UserCard`, `StatusBadge`)
- Factory methods: `create()`, type-specific factories (`success()`, `primary()`)
- Fluent methods: `withProperty()` pattern

### 2. Always Provide a Static create() Method
```java
public static MyComponent create() {
    return new MyComponent();
}
```

### 3. Use Enums for Limited Options
```java
public enum AlertType { SUCCESS, ERROR, WARNING, INFO }
```

### 4. Keep Rendering Logic in render()
Don't build child components in the constructor:

```java
// Good
@Override
public String render() {
    this.withChild(buildHeader());
    this.withChild(buildBody());
    return super.render();
}

// Avoid
public MyComponent() {
    super("div");
    this.withChild(buildHeader());  // Too early
}
```

### 5. Return 'this' from Configuration Methods
```java
public MyComponent withTitle(String title) {
    this.title = title;
    return this;  // Enable chaining
}
```

### 6. Validate Required Properties
```java
@Override
public String render() {
    if (title == null || title.isEmpty()) {
        throw new IllegalStateException("Title is required");
    }
    // ... build component
    return super.render();
}
```

### 7. Provide Sensible Defaults
```java
private MyComponent() {
    super("div");
    this.withClass("my-component");  // Default styling
    this.showIcon = true;             // Default behavior
}
```

## Common Mistakes to Avoid

### Mistake 1: Building Children Too Early
```java
// WRONG
public class MyComponent extends HtmlTag {
    public MyComponent(String title) {
        super("div");
        this.withChild(Header.h1(title));  // Children added before configuration
    }
}

// CORRECT
public class MyComponent extends HtmlTag {
    private String title;

    @Override
    public String render() {
        this.withChild(Header.h1(title));  // Children added at render time
        return super.render();
    }
}
```

### Mistake 2: Not Returning 'this'
```java
// WRONG
public void withTitle(String title) {
    this.title = title;
    // No return - breaks chaining
}

// CORRECT
public MyComponent withTitle(String title) {
    this.title = title;
    return this;  // Enable chaining
}
```

### Mistake 3: Exposing Mutable State
```java
// WRONG
public List<String> items;  // Public mutable field

// CORRECT
private List<String> items = new ArrayList<>();

public MyComponent addItem(String item) {
    this.items.add(item);
    return this;
}
```

## Key Takeaways

1. **Extend HtmlTag**: Inherit rendering logic and fluent API
2. **Use Factory Methods**: Static `create()` for instantiation
3. **Fluent Configuration**: Methods return `this` for chaining
4. **Render Logic in render()**: Build structure at render time, not construction
5. **Type Safety with Enums**: Use enums for limited option sets
6. **Composition Over Inheritance**: Combine existing components
7. **Test Your Components**: Write JUnit tests for rendering logic
8. **Follow Conventions**: Match JHF naming and API patterns

---

**Previous**: [Part 2: Core Concepts](02-core-concepts.md)

**Next**: [Part 4: Building Custom Modules](04-custom-modules.md)

**Table of Contents**: [Getting Started Guide](README.md)
