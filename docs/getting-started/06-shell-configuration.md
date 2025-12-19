# Part 6: Shell Configuration

The application shell is the persistent wrapper around your page content, providing consistent navigation, branding, and structure across your entire application. JHF provides powerful builder utilities to create professional shells with minimal code.

## What is the Application Shell?

Think of the shell as the "frame" around your content:

```
┌─────────────────────────────────────────────────┐
│  Top Banner (Logo, Title, Branding)            │ ← Shell
├────────┬────────────────────────────────────────┤
│        │  Page Content                          │
│ Side   │  (Your pages render here)              │
│ Nav    │  - Home Page                           │
│        │  - About Page                          │ ← Page Content
│ (Links)│  - Contact Page                        │   (Changes)
│        │  - etc.                                │
│        │                                        │
├────────┴────────────────────────────────────────┤
│  Account Bar (User menu, logout)               │ ← Shell
└─────────────────────────────────────────────────┘
```

The shell remains **constant** while page content **changes** as users navigate.

### Shell Components

JHF's shell typically includes:
1. **Top Banner**: Header branding area (logo, title, subtitle)
2. **Account Bar**: Secondary navigation (user menu, login/logout, settings)
3. **Side Navigation**: Main navigation sidebar with links
4. **Content Area**: Where your page content renders

## ShellBuilder Overview

The `ShellBuilder` class simplifies building complete application shells.

### Basic Shell Structure

```java
import io.mindspice.jhf.builders.ShellBuilder;

String shellHtml = ShellBuilder.create()
    .withTopBanner(topBannerHtml)
    .withAccountBar(accountBarHtml)
    .withSideNav(sideNavHtml)
    .withContent(pageContentHtml)
    .build();
```

This generates a complete HTML page with:
- `<!DOCTYPE html>` declaration
- `<html>`, `<head>`, `<body>` tags
- CSS stylesheet links
- JavaScript includes (HTMX)
- Responsive layout structure

### ShellBuilder Methods

| Method | Purpose | Required? |
|--------|---------|-----------|
| `withTopBanner(String)` | Set top banner HTML | Optional |
| `withAccountBar(String)` | Set account bar HTML | Optional |
| `withSideNav(String)` | Set sidebar navigation HTML | Optional |
| `withContent(String)` | Set main page content HTML | **Required** |
| `withTitle(String)` | Set page `<title>` tag | Optional |
| `withAdditionalCSS(String)` | Add custom CSS link | Optional |
| `withAdditionalJS(String)` | Add custom JS link | Optional |
| `build()` | Generate final HTML | **Required** |

## TopBannerBuilder

The top banner is your application's header branding area.

### Creating a Top Banner

```java
import io.mindspice.jhf.builders.TopBannerBuilder;

String banner = TopBannerBuilder.create()
    .withTitle("My Application")
    .withSubtitle("Building amazing things")
    .build();
```

Generates:
```html
<div class="top-banner">
    <h1>My Application</h1>
    <p class="subtitle">Building amazing things</p>
</div>
```

### TopBannerBuilder Methods

```java
TopBannerBuilder.create()
    .withTitle(String)              // Main title (h1)
    .withSubtitle(String)           // Subtitle text (p)
    .withImageUrl(String)           // Background image URL
    .withClass(String)              // Add CSS class
    .withStyle(String, String)      // Add inline style
    .build();                       // Generate HTML
```

### Example: Banner with Background Image

```java
String banner = TopBannerBuilder.create()
    .withTitle("Welcome to Our Platform")
    .withSubtitle("Your journey starts here")
    .withImageUrl("/images/banner-bg.jpg")
    .withClass("hero-banner")
    .build();
```

### Example: Minimal Banner

```java
String banner = TopBannerBuilder.create()
    .withTitle("Dashboard")
    .build();
```

### Example: Styled Banner

```java
String banner = TopBannerBuilder.create()
    .withTitle("Admin Panel")
    .withClass("admin-header")
    .withStyle("background-color", "#2c3e50")
    .withStyle("color", "#ffffff")
    .build();
```

## AccountBarBuilder

The account bar provides secondary navigation, typically for user-related actions.

### Creating an Account Bar

```java
import io.mindspice.jhf.builders.AccountBarBuilder;

String accountBar = AccountBarBuilder.create()
    .addLeftItem(Link.create("/", "Home"))
    .addLeftItem(Link.create("/about", "About"))
    .addRightItem(new Span().withInnerText("Welcome, John"))
    .addRightItem(Link.create("/logout", "Logout"))
    .build();
```

Generates:
```html
<div class="account-bar">
    <div class="account-bar-left">
        <a href="/">Home</a>
        <a href="/about">About</a>
    </div>
    <div class="account-bar-right">
        <span>Welcome, John</span>
        <a href="/logout">Logout</a>
    </div>
</div>
```

### AccountBarBuilder Methods

```java
AccountBarBuilder.create()
    .addLeftItem(Component)     // Add item to left side
    .addRightItem(Component)    // Add item to right side
    .withClass(String)          // Add CSS class
    .build();                   // Generate HTML
```

### Example: User Menu

```java
String accountBar = AccountBarBuilder.create()
    // Left side: Main navigation
    .addLeftItem(Link.create("/", "Home"))
    .addLeftItem(Link.create("/products", "Products"))
    .addLeftItem(Link.create("/about", "About"))

    // Right side: User menu
    .addRightItem(AccountWidget.create(user.getName(), user.getAvatar())
        .withDropdownItem("Profile", "/profile")
        .withDropdownItem("Settings", "/settings")
        .withDropdownItem("Logout", "/logout"))

    .build();
```

### Example: Simple Account Bar

```java
String accountBar = AccountBarBuilder.create()
    .addLeftItem(Link.create("/", "Home"))
    .addRightItem(Link.create("/login", "Login"))
    .build();
```

### Example: Shopping Cart Bar

```java
String accountBar = AccountBarBuilder.create()
    .addLeftItem(Link.create("/", "Shop"))
    .addLeftItem(Link.create("/deals", "Deals"))
    .addLeftItem(Link.create("/contact", "Contact"))

    .addRightItem(Icon.create("shopping-cart")
        .withChild(Badge.create(String.valueOf(cartItemCount))))
    .addRightItem(Link.create("/cart", "Cart ($" + cartTotal + ")"))

    .build();
```

## SideNavBuilder

The side navigation is a vertical menu for primary application navigation.

### Creating Side Navigation

```java
import io.mindspice.jhf.builders.SideNavBuilder;

String sideNav = SideNavBuilder.create()
    .addSection("Main",
        Link.create("/", "Dashboard"),
        Link.create("/users", "Users"),
        Link.create("/reports", "Reports"))
    .addSection("Settings",
        Link.create("/profile", "Profile"),
        Link.create("/preferences", "Preferences"))
    .build();
```

Generates a sidebar with two sections, each containing links.

### SideNavBuilder Methods

```java
SideNavBuilder.create()
    .addSection(String title, Component... items)  // Add navigation section
    .addLink(String text, String url)              // Add single link
    .withCollapsible(boolean)                      // Enable collapse functionality
    .withClass(String)                             // Add CSS class
    .build();                                      // Generate HTML
```

### Example: Multi-Section Navigation

```java
String sideNav = SideNavBuilder.create()
    .addSection("Dashboard",
        Link.create("/", "Overview"),
        Link.create("/analytics", "Analytics"),
        Link.create("/reports", "Reports"))

    .addSection("Content",
        Link.create("/posts", "Posts"),
        Link.create("/pages", "Pages"),
        Link.create("/media", "Media"))

    .addSection("Users",
        Link.create("/users", "All Users"),
        Link.create("/users/roles", "Roles"),
        Link.create("/users/permissions", "Permissions"))

    .addSection("Settings",
        Link.create("/settings", "General"),
        Link.create("/settings/security", "Security"),
        Link.create("/settings/integrations", "Integrations"))

    .build();
```

### Example: Navigation with Icons and Badges

```java
String sideNav = SideNavBuilder.create()
    .addSection("Main",
        new Div().withClass("nav-item")
            .withChild(Icon.create("home"))
            .withChild(Link.create("/", "Home")),

        new Div().withClass("nav-item")
            .withChild(Icon.create("inbox"))
            .withChild(Link.create("/messages", "Messages"))
            .withChild(Badge.create("3").withClass("badge-danger")),  // Unread count

        new Div().withClass("nav-item")
            .withChild(Icon.create("bell"))
            .withChild(Link.create("/notifications", "Notifications"))
            .withChild(Badge.create("12").withClass("badge-info")))

    .build();
```

### Example: Collapsible Navigation

```java
String sideNav = SideNavBuilder.create()
    .withCollapsible(true)  // Enable collapse functionality
    .addSection("Navigation",
        Link.create("/", "Home"),
        Link.create("/about", "About"))
    .build();
```

Collapsible navigation features:
- **Desktop**: Click toggle button to collapse/expand (localStorage persistence)
- **Mobile**: Off-canvas slide-in from left
- **Smooth CSS transitions**
- **State persisted across page loads**

## Complete Shell Examples

### Example 1: Basic Application Shell

```java
// Build shell components
String topBanner = TopBannerBuilder.create()
    .withTitle("My Application")
    .withSubtitle("Welcome back!")
    .build();

String accountBar = AccountBarBuilder.create()
    .addLeftItem(Link.create("/", "Home"))
    .addRightItem(new Span().withInnerText("User: John Doe"))
    .addRightItem(Link.create("/logout", "Logout"))
    .build();

String sideNav = SideNavBuilder.create()
    .addSection("Main",
        Link.create("/", "Dashboard"),
        Link.create("/reports", "Reports"))
    .addSection("Settings",
        Link.create("/profile", "Profile"))
    .build();

// Page content
String pageContent = Page.create()
    .addComponents(Header.h1("Dashboard"))
    .render();

// Combine into shell
String shellHtml = ShellBuilder.create()
    .withTitle("Dashboard - My Application")
    .withTopBanner(topBanner)
    .withAccountBar(accountBar)
    .withSideNav(sideNav)
    .withContent(pageContent)
    .build();
```

### Example 2: E-Commerce Shell

```java
String topBanner = TopBannerBuilder.create()
    .withTitle("ShopNow")
    .withImageUrl("/images/store-banner.jpg")
    .withClass("store-banner")
    .build();

String accountBar = AccountBarBuilder.create()
    .addLeftItem(Link.create("/", "Shop"))
    .addLeftItem(Link.create("/deals", "Deals"))
    .addLeftItem(Link.create("/new", "New Arrivals"))

    .addRightItem(Link.create("/cart", "Cart (3)"))
    .addRightItem(Link.create("/account", "My Account"))

    .build();

String sideNav = SideNavBuilder.create()
    .addSection("Categories",
        Link.create("/category/electronics", "Electronics"),
        Link.create("/category/clothing", "Clothing"),
        Link.create("/category/home", "Home & Garden"),
        Link.create("/category/sports", "Sports"))

    .addSection("My Account",
        Link.create("/orders", "My Orders"),
        Link.create("/wishlist", "Wishlist"),
        Link.create("/addresses", "Addresses"))

    .build();

String pageContent = Page.create()
    .addComponents(Header.h1("Featured Products"))
    // ... product grid
    .render();

String shellHtml = ShellBuilder.create()
    .withTitle("Shop - ShopNow")
    .withTopBanner(topBanner)
    .withAccountBar(accountBar)
    .withSideNav(sideNav)
    .withContent(pageContent)
    .build();
```

### Example 3: Admin Dashboard Shell

```java
String topBanner = TopBannerBuilder.create()
    .withTitle("Admin Panel")
    .withClass("admin-header")
    .withStyle("background-color", "#2c3e50")
    .withStyle("color", "#ecf0f1")
    .build();

String accountBar = AccountBarBuilder.create()
    .addLeftItem(Link.create("/admin", "Dashboard"))
    .addLeftItem(Link.create("/admin/stats", "Statistics"))

    .addRightItem(new Span().withInnerText("Admin: " + adminName))
    .addRightItem(Dropdown.create("Actions")
        .addItem("View Site", "/")
        .addItem("Clear Cache", "/admin/cache/clear")
        .addItem("Logout", "/logout"))

    .build();

String sideNav = SideNavBuilder.create()
    .withCollapsible(true)

    .addSection("Content",
        Link.create("/admin/posts", "Posts"),
        Link.create("/admin/pages", "Pages"),
        Link.create("/admin/comments", "Comments"))

    .addSection("Users",
        Link.create("/admin/users", "All Users"),
        Link.create("/admin/users/roles", "Roles"),
        Link.create("/admin/users/banned", "Banned Users"))

    .addSection("System",
        Link.create("/admin/settings", "Settings"),
        Link.create("/admin/logs", "Logs"),
        Link.create("/admin/backup", "Backup"))

    .build();

String pageContent = Page.create()
    .addComponents(Header.h1("Dashboard"))
    // ... admin dashboard widgets
    .render();

String shellHtml = ShellBuilder.create()
    .withTitle("Dashboard - Admin Panel")
    .withTopBanner(topBanner)
    .withAccountBar(accountBar)
    .withSideNav(sideNav)
    .withContent(pageContent)
    .build();
```

## Integrating Shell with Spring Controllers

Create a reusable shell component in Spring:

### Step 1: Create Shell Service

```java
package com.example.services;

import io.mindspice.jhf.builders.*;
import org.springframework.stereotype.Service;

@Service
public class ShellService {

    public String buildShell(String pageTitle, String pageContent) {
        return ShellBuilder.create()
            .withTitle(pageTitle)
            .withTopBanner(buildTopBanner())
            .withAccountBar(buildAccountBar())
            .withSideNav(buildSideNav())
            .withContent(pageContent)
            .build();
    }

    private String buildTopBanner() {
        return TopBannerBuilder.create()
            .withTitle("My Application")
            .withSubtitle("Building amazing things")
            .build();
    }

    private String buildAccountBar() {
        return AccountBarBuilder.create()
            .addLeftItem(Link.create("/", "Home"))
            .addLeftItem(Link.create("/about", "About"))
            .addRightItem(Link.create("/login", "Login"))
            .build();
    }

    private String buildSideNav() {
        return SideNavBuilder.create()
            .addSection("Main",
                Link.create("/", "Home"),
                Link.create("/dashboard", "Dashboard"),
                Link.create("/reports", "Reports"))
            .addSection("Settings",
                Link.create("/profile", "Profile"),
                Link.create("/settings", "Settings"))
            .build();
    }
}
```

### Step 2: Use in Controllers

```java
package com.example.controller;

import com.example.services.ShellService;
import com.example.pages.HomePage;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    private final ShellService shellService;
    private final HomePage homePage;

    public HomeController(ShellService shellService, HomePage homePage) {
        this.shellService = shellService;
        this.homePage = homePage;
    }

    @GetMapping("/")
    @ResponseBody
    public String home() {
        String pageContent = homePage.buildPage();
        return shellService.buildShell("Home - My Application", pageContent);
    }
}
```

Now every page uses the same shell!

### Step 3: User-Specific Shells

Customize the shell based on logged-in user:

```java
@Service
public class ShellService {

    public String buildShell(String pageTitle, String pageContent, User user) {
        return ShellBuilder.create()
            .withTitle(pageTitle)
            .withTopBanner(buildTopBanner())
            .withAccountBar(buildAccountBar(user))  // User-specific
            .withSideNav(buildSideNav(user))        // Role-based nav
            .withContent(pageContent)
            .build();
    }

    private String buildAccountBar(User user) {
        AccountBarBuilder builder = AccountBarBuilder.create()
            .addLeftItem(Link.create("/", "Home"));

        if (user != null) {
            builder.addRightItem(new Span().withInnerText("Welcome, " + user.getName()))
                   .addRightItem(Link.create("/logout", "Logout"));
        } else {
            builder.addRightItem(Link.create("/login", "Login"))
                   .addRightItem(Link.create("/register", "Register"));
        }

        return builder.build();
    }

    private String buildSideNav(User user) {
        SideNavBuilder builder = SideNavBuilder.create()
            .addSection("Main",
                Link.create("/", "Home"),
                Link.create("/about", "About"));

        // Admin-only navigation
        if (user != null && user.isAdmin()) {
            builder.addSection("Admin",
                Link.create("/admin", "Dashboard"),
                Link.create("/admin/users", "Users"));
        }

        return builder.build();
    }
}
```

## Responsive Shell Behavior

The shell is **responsive by default**:

### Desktop (>769px)
```
┌────────────────────────────────────────┐
│  Top Banner                            │
├────────────────────────────────────────┤
│  Account Bar                           │
├──────────┬─────────────────────────────┤
│          │                             │
│ SideNav  │  Content Area               │
│ (250px)  │  (Remaining width)          │
│          │                             │
└──────────┴─────────────────────────────┘
```

### Mobile (<480px)
```
┌────────────────────────┐
│  Top Banner            │
├────────────────────────┤
│  Account Bar (stacked) │
├────────────────────────┤
│                        │
│  Content Area          │
│  (Full width)          │
│                        │
└────────────────────────┘
│ [☰] Sidebar (off-canvas, slide-in)
```

**Mobile behavior**:
- Top banner and account bar stack vertically
- Sidebar hidden by default
- Hamburger menu button reveals sidebar (slide-in from left)
- Content uses full width

## Collapsible Sidebar

Enable sidebar collapse functionality:

```java
String sideNav = SideNavBuilder.create()
    .withCollapsible(true)  // Enable collapse
    .addSection("Navigation", ...)
    .build();
```

### Features

**Desktop**:
- Click toggle button to collapse/expand
- Collapsed: Icons only (narrow sidebar)
- Expanded: Icons + text (full sidebar)
- **State persisted** in localStorage

**Mobile**:
- Off-canvas pattern (slide-in from left)
- Overlay dims background when open
- Click outside or close button to dismiss

### CSS Transitions

Smooth animations for:
- Sidebar width changes (desktop collapse)
- Slide-in/slide-out (mobile)
- Opacity transitions (overlay)

## Customization

### Custom CSS

Add custom stylesheet:

```java
String shell = ShellBuilder.create()
    .withAdditionalCSS("/css/custom-shell.css")
    .withTopBanner(...)
    .withContent(...)
    .build();
```

### Custom JavaScript

Add custom scripts:

```java
String shell = ShellBuilder.create()
    .withAdditionalJS("/js/shell-enhancements.js")
    .withTopBanner(...)
    .withContent(...)
    .build();
```

### Color Schemes

Override framework.css with your own styles:

```css
/* custom-shell.css */

/* Dark theme top banner */
.top-banner {
    background-color: #1a1a1a;
    color: #ffffff;
}

/* Custom sidebar colors */
.main-sidebar {
    background-color: #2c3e50;
}

.main-sidebar a {
    color: #ecf0f1;
}

.main-sidebar a:hover {
    background-color: #34495e;
}

/* Custom account bar */
.account-bar {
    background-color: #3498db;
    color: #ffffff;
}
```

### Branding

Add logo to top banner:

```java
String topBanner = TopBannerBuilder.create()
    .withChild(Image.create("/images/logo.png", "Company Logo")
        .withClass("logo")
        .withMaxWidth("150px"))
    .withTitle("My Company")
    .withSubtitle("Excellence in Everything")
    .build();
```

## Best Practices

### 1. Consistent Shell Across Application

Create one shell service used by all controllers:

```java
// Good - Consistent
shellService.buildShell(pageTitle, pageContent);

// Avoid - Inconsistent shells per page
ShellBuilder.create()... // Different on each page
```

### 2. Mobile-First Navigation

Design navigation for mobile first:
- **Limit navigation items** (5-7 per section max)
- **Use icons** to save space
- **Collapsible sections** for long lists
- **Test on mobile devices**

### 3. User-Specific Content

Show relevant navigation based on user role:

```java
if (user.isAdmin()) {
    sideNav.addSection("Admin", ...);
}
```

### 4. Performance

Cache shell HTML when possible:

```java
// Build shell once, reuse for multiple requests
private String cachedShellTemplate;

public String buildShell(String pageContent) {
    if (cachedShellTemplate == null) {
        cachedShellTemplate = buildShellTemplate();
    }
    return cachedShellTemplate.replace("{{CONTENT}}", pageContent);
}
```

### 5. Accessibility

- Use semantic HTML (nav, header, main)
- Provide ARIA labels
- Ensure keyboard navigation works
- Test with screen readers

## Key Takeaways

1. **Shell = Frame**: Persistent wrapper around changing page content
2. **ShellBuilder**: Combines banner, account bar, sidebar, and content
3. **TopBannerBuilder**: Create branded header banners
4. **AccountBarBuilder**: Build secondary navigation bars
5. **SideNavBuilder**: Construct primary navigation sidebars
6. **Responsive**: Mobile-friendly by default (stacks, off-canvas)
7. **Collapsible**: Optional sidebar collapse functionality
8. **Reusable**: Build shell once in service, use everywhere
9. **Customizable**: Add CSS/JS, override styles, brand it
10. **User-Specific**: Adapt navigation based on user role

---

**Previous**: [Part 5: Pages and Layouts](05-pages-and-layouts.md)

**Next**: [Part 7: CSS and Styling](07-css-and-styling.md)

**Table of Contents**: [Getting Started Guide](README.md)
